package com.ruoyi.workorder.sql;

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/** 锁定通知幂等、重试索引、权限和容器初始化脚本，防止部署交付缺项。 */
public class WorkOrderF02SqlContractTest
{
    @Test
    public void scriptShouldKeepNotificationInvariants() throws IOException
    {
        String sql = read(Paths.get("sql", "12_workorder_f02_notification.sql"),
                Paths.get("..", "sql", "12_workorder_f02_notification.sql"));
        assertContains(sql, "CREATE TABLE IF NOT EXISTS wo_notification_template",
                "CREATE TABLE IF NOT EXISTS wo_notification_task",
                "CREATE TABLE IF NOT EXISTS wo_notification_message",
                "UNIQUE KEY uk_wo_notification_task_event_recipient",
                "UNIQUE KEY uk_wo_notification_message_task",
                "idx_wo_notification_task_due", "workOrderNotificationTask.dispatch",
                "workorder:notification:list", "workorder:notification:read",
                "workorder:message:record", "workorder:message:retry",
                "'WORK_ORDER'", "'APPROVAL'", "'SYSTEM'",
                "'SUBMIT'", "'ACCEPT'", "'ARRIVE'", "'PROGRESS'", "'CONFIRM'", "'EVALUATE'", "'CANCEL'");
    }

    @Test
    public void composeShouldExecuteF02Script() throws IOException
    {
        String compose = read(Paths.get("docker-compose.yml"), Paths.get("..", "docker-compose.yml"));
        assertContains(compose, "12_workorder_f02_notification.sql");
    }

    private String read(Path primary, Path fallback) throws IOException
    {
        Path path = Files.exists(primary) ? primary : fallback;
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
    private void assertContains(String value, String... expected)
    {
        for (String item : expected) assertTrue("缺少 F02 契约: " + item, value.contains(item));
    }
}
