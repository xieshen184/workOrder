package com.ruoyi.workorder.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/** 防止初始化脚本在后续维护中把跨角色动作误授给普通用户。 */
public class WorkOrderRoleGrantSqlTest
{
    @Test
    public void threeRolesShouldKeepMinimumActionPermissions() throws IOException
    {
        String sql = readSql();
        String reporter = block(sql, "SELECT @wo_reporter_role_id", "SELECT @wo_engineer_role_id");
        String engineer = block(sql, "SELECT @wo_engineer_role_id", "SELECT @wo_dispatcher_role_id");
        String dispatcher = sql.substring(sql.indexOf("SELECT @wo_dispatcher_role_id"));

        assertContains(reporter, "workorder:order:add", "workorder:order:confirm",
                "workorder:evaluation:add", "workorder:evaluation:query");
        assertFalse(reporter.contains("'workorder:order:assign'"));
        assertFalse(reporter.contains("'workorder:order:finish'"));

        assertContains(engineer, "workorder:order:assigned:list", "workorder:order:accept",
                "workorder:order:arrive", "workorder:order:finish");
        assertFalse(engineer.contains("'workorder:order:assign'"));
        assertFalse(engineer.contains("'workorder:evaluation:add'"));

        assertContains(dispatcher, "workorder:order:list", "workorder:order:assign",
                "workorder:order:reassign", "workorder:engineer:list", "workorder:evaluation:query");
        assertFalse(dispatcher.contains("'workorder:order:confirm'"));
        assertFalse(dispatcher.contains("'workorder:evaluation:add'"));
    }

    @Test
    public void roleMenuGrantShouldRemainIdempotent() throws IOException
    {
        String sql = readSql();
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertContains(sql, "grant_row.role_id = @wo_reporter_role_id",
                "grant_row.role_id = @wo_engineer_role_id",
                "grant_row.role_id = @wo_dispatcher_role_id");
    }

    @Test
    public void evaluationQueryEndpointShouldRequireTheGrantedPermission() throws IOException
    {
        Path path = Paths.get("ruoyi-admin", "src", "main", "java", "com", "ruoyi", "web",
                "controller", "workorder", "WorkOrderController.java");
        if (!Files.exists(path)) path = Paths.get("..", "ruoyi-admin", "src", "main", "java", "com",
                "ruoyi", "web", "controller", "workorder", "WorkOrderController.java");
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(source.contains("@ss.hasPermi('workorder:evaluation:query')"));
        assertFalse(source.matches("(?s).*@PreAuthorize\\(\\\"isAuthenticated\\(\\)\\\"\\)\\s*"
                + "@GetMapping\\(\\\"/\\{id\\}/evaluation\\\"\\).*"));
    }

    private String readSql() throws IOException
    {
        Path path = Paths.get("sql", "06_workorder_role_grant.sql");
        if (!Files.exists(path)) path = Paths.get("..", "sql", "06_workorder_role_grant.sql");
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String block(String value, String start, String end)
    {
        int from = value.indexOf(start);
        int to = value.indexOf(end, from + start.length());
        assertTrue("未找到角色授权区段: " + start, from >= 0 && to > from);
        return value.substring(from, to);
    }

    private void assertContains(String value, String... expected)
    {
        for (String item : expected) assertTrue("缺少权限或幂等条件: " + item, value.contains(item));
    }
}
