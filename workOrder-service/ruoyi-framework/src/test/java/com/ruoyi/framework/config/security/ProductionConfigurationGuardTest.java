package com.ruoyi.framework.config.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * 生产配置保护器测试。
 */
public class ProductionConfigurationGuardTest
{
    @Test
    public void shouldAcceptCompleteProductionConfiguration()
    {
        ProductionConfigurationGuard guard = guard(validProductionProperties(), "prod");

        Assert.assertTrue(guard.validate().isEmpty());
    }

    @Test
    public void shouldRejectUnsafeConfigurationWithoutLeakingSecret()
    {
        Map<String, Object> properties = validProductionProperties();
        properties.put("spring.datasource.druid.master.username", "root");
        properties.put("spring.datasource.druid.master.password", "<database-password>");
        properties.put("token.secret", "replace-me-super-secret-value");
        properties.put("swagger.enabled", "true");
        properties.put("security.cors.allowed-origins", "*");

        List<String> violations = guard(properties, "prod").validate();
        String message = String.join(";", violations);

        Assert.assertTrue(message.contains("spring.datasource.druid.master.username"));
        Assert.assertTrue(message.contains("spring.datasource.druid.master.password"));
        Assert.assertTrue(message.contains("token.secret"));
        Assert.assertTrue(message.contains("swagger.enabled"));
        Assert.assertTrue(message.contains("security.cors.allowed-origins"));
        Assert.assertFalse(message.contains("replace-me-super-secret-value"));
    }

    @Test
    public void shouldSkipStrictChecksOutsideProduction()
    {
        ProductionConfigurationGuard guard = guard(new HashMap<>(), "local");

        Assert.assertTrue(guard.validate().isEmpty());
    }

    private ProductionConfigurationGuard guard(Map<String, Object> properties, String profile)
    {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles(profile);
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return new ProductionConfigurationGuard(environment);
    }

    private Map<String, Object> validProductionProperties()
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.druid.master.url", "jdbc:mysql://db.example.internal:3306/workorder");
        properties.put("spring.datasource.druid.master.username", "workorder_app");
        properties.put("spring.datasource.druid.master.password", "database-password");
        properties.put("spring.redis.host", "redis.example.internal");
        properties.put("spring.redis.password", "redis-password");
        properties.put("token.secret", "2b3ec6ca70e84134b2d413331a8b92da");
        properties.put("swagger.enabled", "false");
        properties.put("spring.datasource.druid.statViewServlet.enabled", "false");
        properties.put("spring.devtools.restart.enabled", "false");
        properties.put("security.cors.allowed-origins", "https://workorder.example.com");
        properties.put("ruoyi.profile", "/data/workorder/upload");
        properties.put("logging.level.com.ruoyi", "INFO");
        return properties;
    }
}
