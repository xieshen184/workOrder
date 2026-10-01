package com.ruoyi.workorder.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/** 锁定 F04 菜单权限、短信脱敏存储和容器初始化入口。 */
public class WorkOrderF04SqlContractTest
{
    @Test
    public void scriptShouldKeepOperationsContracts() throws IOException
    {
        String sql = read(Paths.get("sql", "13_workorder_f04_operations.sql"),
                Paths.get("..", "sql", "13_workorder_f04_operations.sql"));
        assertContains(sql, "CREATE TABLE IF NOT EXISTS wo_sms_account_setting", "credential_mask",
                "workorder:dashboard:view", "workorder:performance:view", "workorder:message:template",
                "workorder:sms:view", "workorder:sms:edit", "workorder:sms:refresh",
                "workorder/performance/index", "workorder/message/index", "workorder/sms/index");
        assertFalse("F04 SQL 不得保存供应商明文密钥", sql.toLowerCase().contains("secret_key"));
        assertFalse("F04 SQL 不得保存供应商明文密钥", sql.toLowerCase().contains("access_key"));
    }

    @Test
    public void composeShouldExecuteCompleteInitializationBundle() throws IOException
    {
        String compose = read(Paths.get("docker-compose.yml"), Paths.get("..", "docker-compose.yml"));
        assertContains(compose, "00_workorder_full_init.sql");
    }

    private String read(Path primary, Path fallback) throws IOException
    {
        Path path = Files.exists(primary) ? primary : fallback;
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
    private void assertContains(String value, String... expected)
    {
        for (String item : expected) assertTrue("缺少 F04 契约: " + item, value.contains(item));
    }
}
