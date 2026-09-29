package com.ruoyi.framework.config.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 生产环境配置启动保护器。
 *
 * <p>该类把分散在数据库、缓存、令牌、调试入口和跨域配置中的生产安全约束
 * 集中到一个边界中。应用使用 {@code prod} 配置启动时，只要存在一项不安全配置，
 * 就会在对外提供服务前终止启动，避免把示例密码、开放文档或宽泛跨域带到生产环境。</p>
 *
 * <p>校验结果只包含配置项名称和修复方向，不回显任何真实密码或密钥。</p>
 */
@Component
public class ProductionConfigurationGuard implements ApplicationRunner
{
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[A-Za-z]:[\\\\/].+");

    private static final List<String> UNSAFE_SECRET_MARKERS = Arrays.asList(
            "change-me", "changeme", "replace-me", "replace_me", "development-only", "example", "default");

    private final Environment environment;

    public ProductionConfigurationGuard(Environment environment)
    {
        this.environment = environment;
    }

    /**
     * Spring 容器完成初始化后执行生产配置检查。
     *
     * @param args 启动参数，本校验不读取参数内容
     */
    @Override
    public void run(ApplicationArguments args)
    {
        List<String> violations = validate();
        if (!violations.isEmpty())
        {
            throw new IllegalStateException("生产配置校验失败：" + String.join("；", violations));
        }
    }

    /**
     * 返回当前环境的全部配置问题。非生产环境不执行这些强制约束，保证本地开发便利性。
     *
     * @return 不包含敏感值的配置问题列表
     */
    public List<String> validate()
    {
        List<String> violations = new ArrayList<>();
        if (!isProductionProfile())
        {
            return violations;
        }

        requireText(violations, "spring.datasource.druid.master.url");
        requireNonRootDatabaseUser(violations);
        requireText(violations, "spring.datasource.druid.master.password");
        requireText(violations, "spring.redis.host");
        requireText(violations, "spring.redis.password");
        requireSafeTokenSecret(violations);
        requireDisabled(violations, "swagger.enabled");
        requireDisabled(violations,
                "spring.datasource.druid.statViewServlet.enabled",
                "spring.datasource.druid.stat-view-servlet.enabled");
        requireDisabled(violations, "spring.devtools.restart.enabled");
        requireRestrictedCors(violations);
        requireAbsoluteProfilePath(violations);
        requireSafeLogLevel(violations);
        return violations;
    }

    private boolean isProductionProfile()
    {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private void requireText(List<String> violations, String key)
    {
        if (isMissingOrTemplate(property(key)))
        {
            violations.add(key + " 必须通过环境变量配置");
        }
    }

    private void requireNonRootDatabaseUser(List<String> violations)
    {
        String key = "spring.datasource.druid.master.username";
        String username = property(key);
        if (isMissingOrTemplate(username))
        {
            violations.add(key + " 必须通过环境变量配置");
        }
        else if ("root".equalsIgnoreCase(username.trim()))
        {
            violations.add(key + " 禁止使用 root 账户");
        }
    }

    private void requireSafeTokenSecret(List<String> violations)
    {
        String key = "token.secret";
        String secret = property(key);
        if (isMissingOrTemplate(secret) || secret.trim().length() < 32)
        {
            violations.add(key + " 必须配置为至少 32 位的随机密钥");
            return;
        }

        String normalized = secret.toLowerCase(Locale.ROOT);
        if (UNSAFE_SECRET_MARKERS.stream().anyMatch(normalized::contains))
        {
            violations.add(key + " 禁止使用示例或默认密钥");
        }
    }

    private void requireDisabled(List<String> violations, String... keys)
    {
        String value = property(keys);
        if (!StringUtils.hasText(value) || Boolean.parseBoolean(value.trim()))
        {
            violations.add(keys[0] + " 在生产环境必须显式设置为 false");
        }
    }

    private void requireRestrictedCors(List<String> violations)
    {
        String key = "security.cors.allowed-origins";
        String origins = property(key);
        if (!StringUtils.hasText(origins))
        {
            violations.add(key + " 必须配置明确的 HTTPS 来源");
            return;
        }

        for (String origin : origins.split(","))
        {
            String normalized = origin.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("*") || !normalized.startsWith("https://"))
            {
                violations.add(key + " 只能包含明确的 HTTPS 来源，禁止通配符");
                return;
            }
        }
    }

    private void requireAbsoluteProfilePath(List<String> violations)
    {
        String key = "ruoyi.profile";
        String path = property(key);
        if (!StringUtils.hasText(path)
                || !(path.startsWith("/") || WINDOWS_ABSOLUTE_PATH.matcher(path).matches()))
        {
            violations.add(key + " 必须配置为绝对路径");
        }
    }

    private void requireSafeLogLevel(List<String> violations)
    {
        String key = "logging.level.com.ruoyi";
        String level = property(key);
        if ("DEBUG".equalsIgnoreCase(level) || "TRACE".equalsIgnoreCase(level))
        {
            violations.add(key + " 在生产环境不得使用 DEBUG 或 TRACE");
        }
    }

    private String property(String... keys)
    {
        for (String key : keys)
        {
            String value = environment.getProperty(key);
            if (value != null)
            {
                return value;
            }
        }
        return null;
    }

    private boolean isMissingOrTemplate(String value)
    {
        if (!StringUtils.hasText(value))
        {
            return true;
        }
        String normalized = value.trim();
        return (normalized.startsWith("<") && normalized.endsWith(">"))
                || (normalized.startsWith("${") && normalized.endsWith("}"));
    }
}
