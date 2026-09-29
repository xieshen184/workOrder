package com.ruoyi.framework.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;
import com.ruoyi.framework.config.properties.PermitAllUrlProperties;
import com.ruoyi.framework.security.filter.JwtAuthenticationTokenFilter;
import com.ruoyi.framework.security.handle.AuthenticationEntryPointImpl;
import com.ruoyi.framework.security.handle.LogoutSuccessHandlerImpl;

/**
 * Spring Security 统一配置。
 *
 * <p>公共路径在此集中声明，未列入白名单的请求一律要求 JWT 认证。Swagger 和
 * Druid 入口由配置开关控制，生产环境启动保护器会强制关闭这两个入口。</p>
 */
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfig
{
    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;

    @Autowired
    private CorsFilter corsFilter;

    @Autowired
    private PermitAllUrlProperties permitAllUrl;

    /** 是否开放接口文档入口。 */
    @Value("${swagger.enabled:false}")
    private boolean swaggerEnabled;

    /** 是否开放 Druid 管理入口。 */
    @Value("${spring.datasource.druid.statViewServlet.enabled:false}")
    private boolean druidEnabled;

    /**
     * 组装用户名密码认证器。
     *
     * @return 使用 BCrypt 校验密码的认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager()
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(bCryptPasswordEncoder());
        return new ProviderManager(provider);
    }

    /**
     * 定义无状态 JWT 请求的安全过滤链。
     *
     * @param httpSecurity Spring Security 配置对象
     * @return 应用安全过滤链
     * @throws Exception 安全配置构建失败时抛出
     */
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception
    {
        return httpSecurity
            // 系统使用无状态 JWT，不依赖浏览器 Session，因此关闭 CSRF Token 校验。
            .csrf(csrf -> csrf.disable())
            // 保留 Spring Security 默认安全响应头，仅允许同源页面使用 frame。
            .headers(headers -> headers.frameOptions(options -> options.sameOrigin()))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requests -> {
                // 控制器上通过 @Anonymous 声明的接口进入公共白名单。
                permitAllUrl.getUrls().forEach(url -> requests.antMatchers(url).permitAll());
                // 建立用户身份之前必须访问的公共入口。
                requests.antMatchers("/login", "/register", "/captchaImage").permitAll();
                // 只公开确定的前端资源和头像目录；工单附件仍经过业务接口鉴权。
                requests.antMatchers(HttpMethod.GET,
                        "/", "/index.html", "/favicon.ico", "/static/**", "/profile/avatar/**").permitAll();

                String[] swaggerPaths = {
                        "/swagger-ui.html", "/swagger-ui/**", "/swagger-resources/**",
                        "/webjars/**", "/v2/api-docs", "/v3/api-docs/**", "/*/api-docs"
                };
                if (swaggerEnabled)
                {
                    requests.antMatchers(swaggerPaths).permitAll();
                }
                else
                {
                    requests.antMatchers(swaggerPaths).denyAll();
                }

                if (druidEnabled)
                {
                    // Druid 自带登录页无法携带业务 JWT；只有明确启用时才允许进入。
                    requests.antMatchers("/druid/**").permitAll();
                }
                else
                {
                    requests.antMatchers("/druid/**").denyAll();
                }

                requests.anyRequest().authenticated();
            })
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler))
            .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class)
            .addFilterBefore(corsFilter, LogoutFilter.class)
            .build();
    }

    /**
     * 密码采用 BCrypt 单向哈希。
     *
     * @return BCrypt 编码器
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}
