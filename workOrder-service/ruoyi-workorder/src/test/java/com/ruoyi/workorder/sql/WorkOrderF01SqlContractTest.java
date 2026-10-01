package com.ruoyi.workorder.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * F01 数据库与容器初始化契约测试。
 *
 * <p>延期审批依赖“同一工单最多一个待审申请”的数据库约束，SLA 又依赖创建时快照。
 * 这些约束一旦从部署脚本中丢失，单元测试仍可能通过，因此在这里直接锁定交付脚本。</p>
 */
public class WorkOrderF01SqlContractTest
{
    @Test
    public void f01ScriptShouldKeepDelayAndSlaInvariants() throws IOException
    {
        String sql = read(Paths.get("sql", "11_workorder_f01_sla_delay.sql"),
                Paths.get("..", "sql", "11_workorder_f01_sla_delay.sql"));

        assertContains(sql,
                "sla_reminder_before_min",
                "sla_allow_extension",
                "CREATE TABLE IF NOT EXISTS wo_delay_request",
                "UNIQUE KEY uk_wo_delay_pending_order",
                "workOrderSlaTask.scan",
                "workOrderSlaTask.autoClose",
                "workorder:delay:add",
                "workorder:delay:approve",
                "workorder:sla:edit");

        assertFalse(sql.contains("@wo_reporter_role_id, 'workorder:delay:add'"));
        assertTrue(sql.contains("@wo_engineer_role_id, 'workorder:delay:add'"));
        assertFalse(sql.contains("@wo_engineer_role_id, 'workorder:delay:approve'"));
        assertTrue(sql.contains("@wo_dispatcher_role_id, 'workorder:delay:approve'"));
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
        for (String item : expected) assertTrue("缺少 F01 契约: " + item, value.contains(item));
    }
}
