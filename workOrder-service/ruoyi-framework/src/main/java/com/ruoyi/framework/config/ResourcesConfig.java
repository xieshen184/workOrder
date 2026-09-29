package com.ruoyi.framework.config;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.framework.interceptor.RepeatSubmitInterceptor;

/**
 * Web 资源、拦截器与跨域策略配置。
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Value("${security.cors.allowed-origins:http://localhost}")
    private String allowedOrigins;

    @Value("${security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${security.cors.allowed-headers:Authorization,Content-Type,X-Requested-With,Idempotency-Key}")
    private String allowedHeaders;

    @Value("${security.cors.exposed-headers:Content-Disposition,download-filename}")
    private String exposedHeaders;

    @Value("${security.cors.max-age:1800}")
    private long maxAge;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        // 文件映射只负责读取物理目录，实际访问权限由 SecurityConfig 控制。
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + RuoYiConfig.getProfile() + "/");

        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

    /**
     * 创建受配置约束的跨域过滤器。
     *
     * <p>本地开发可以配置带通配符的 origin pattern；生产启动保护器会拒绝任何
     * 通配符和非 HTTPS 来源。小程序原生请求不受浏览器 CORS 限制，该规则主要服务 PC 端。</p>
     *
     * @return 跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter()
    {
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : split(allowedOrigins))
        {
            if (origin.contains("*"))
            {
                config.addAllowedOriginPattern(origin);
            }
            else
            {
                config.addAllowedOrigin(origin);
            }
        }
        addEach(allowedHeaders, config::addAllowedHeader);
        addEach(allowedMethods, config::addAllowedMethod);
        addEach(exposedHeaders, config::addExposedHeader);
        config.setAllowCredentials(false);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    private void addEach(String values, Consumer<String> consumer)
    {
        split(values).forEach(consumer);
    }

    private List<String> split(String values)
    {
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }
}
