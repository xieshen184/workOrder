package com.ruoyi.workorder.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * 完整数据库初始化入口契约测试。
 *
 * <p>联调人员只执行一个 SQL 文件，因此这里同时校验分片顺序、分片内容、联调账号和
 * Compose 挂载入口，防止新增脚本只提交了分片却漏进完整初始化文件。</p>
 */
public class WorkOrderFullInitSqlTest
{
    private static final List<String> SOURCE_FILES = Arrays.asList(
            "ry_20250522.sql",
            "quartz.sql",
            "03_workorder_core_schema.sql",
            "04_workorder_dict.sql",
            "05_workorder_menu.sql",
            "06_workorder_role_grant.sql",
            "07_workorder_seed.sql",
            "08_workorder_m1_core_adjust.sql",
            "09_workorder_m1_b02.sql",
            "10_workorder_m1_d03.sql",
            "11_workorder_f01_sla_delay.sql",
            "12_workorder_f02_notification.sql",
            "13_workorder_f04_operations.sql",
            "14_workorder_integration_seed.sql",
            "15_workorder_dept_selector_permission.sql");

    @Test
    public void bundleShouldContainEverySourceInDependencyOrder() throws IOException
    {
        String bundle = readSql("00_workorder_full_init.sql");
        int previousMarkerIndex = -1;

        for (String sourceFile : SOURCE_FILES)
        {
            String marker = "-- BEGIN SOURCE: " + sourceFile;
            int markerIndex = bundle.indexOf(marker);
            assertTrue("完整初始化文件缺少分片标记: " + sourceFile, markerIndex >= 0);
            assertTrue("完整初始化文件中的分片顺序错误: " + sourceFile,
                    markerIndex > previousMarkerIndex);
            previousMarkerIndex = markerIndex;

            String sourceContent = readSql(sourceFile).trim();
            assertTrue("完整初始化文件未包含分片的最新内容: " + sourceFile,
                    bundle.contains(sourceContent));
        }
    }

    @Test
    public void bundleShouldKeepDatabaseAndIntegrationTestContracts() throws IOException
    {
        String bundle = readSql("00_workorder_full_init.sql");
        assertContains(bundle,
                "CREATE DATABASE IF NOT EXISTS `workorder`",
                "USE `workorder`;",
                "SET FOREIGN_KEY_CHECKS = 0;",
                "SET FOREIGN_KEY_CHECKS = 1;",
                "'wo_reporter'",
                "'wo_engineer'",
                "'wo_dispatcher'",
                "'workorder_reporter'",
                "'workorder_engineer'",
                "'workorder_dispatcher'",
                "'system:dept:list'",
                "'workorder:engineer:list'",
                "'workorder:category:list'",
                "enabled_notification_template_count",
                "enabled_workorder_job_count");
    }

    @Test
    public void composeShouldMountOnlyCompleteInitializationBundle() throws IOException
    {
        String compose = readProjectFile("docker-compose.yml");
        assertContains(compose, "./sql/00_workorder_full_init.sql:/docker-entrypoint-initdb.d/00-workorder-full-init.sql:ro");
        for (String sourceFile : SOURCE_FILES)
        {
            assertFalse("Compose 不应再单独挂载 SQL 分片: " + sourceFile,
                    compose.contains("./sql/" + sourceFile + ":"));
        }
    }

    private String readSql(String fileName) throws IOException
    {
        Path primary = Paths.get("sql", fileName);
        Path fallback = Paths.get("..", "sql", fileName);
        return read(Files.exists(primary) ? primary : fallback);
    }

    private String readProjectFile(String fileName) throws IOException
    {
        Path primary = Paths.get(fileName);
        Path fallback = Paths.get("..", fileName);
        return read(Files.exists(primary) ? primary : fallback);
    }

    private String read(Path path) throws IOException
    {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private void assertContains(String value, String... expected)
    {
        for (String item : expected)
        {
            assertTrue("缺少完整初始化契约: " + item, value.contains(item));
        }
    }
}
