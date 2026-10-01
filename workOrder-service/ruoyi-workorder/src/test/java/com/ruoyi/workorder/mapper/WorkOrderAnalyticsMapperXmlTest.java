package com.ruoyi.workorder.mapper;

import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import com.ruoyi.workorder.application.model.WorkOrderAnalyticsQuery;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

/** 构建期解析 F04 统计 SQL，并锁定数据范围和绩效样本口径。 */
public class WorkOrderAnalyticsMapperXmlTest
{
    @Test
    public void analyticsStatementsShouldParseAndApplyDataScope() throws Exception
    {
        Configuration configuration = parse("mapper/workorder/WorkOrderAnalyticsMapper.xml");
        WorkOrderAnalyticsQuery query = new WorkOrderAnalyticsQuery();
        query.setBeginTime("2026-09-01"); query.setEndTime("2026-09-30");
        query.getParams().put("dataScope", " AND d.dept_id = 23 ");
        String sql = configuration.getMappedStatement(
                "com.ruoyi.workorder.mapper.WorkOrderAnalyticsMapper.selectPerformanceMetrics")
                .getBoundSql(query).getSql().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("d.dept_id = 23"));
        assertTrue(sql.contains("coalesce(o.extension_deadline, o.finish_deadline) is not null"));
        assertTrue(sql.contains("action_type = 'return'"));
        assertTrue(sql.contains("timestampdiff(second, o.assigned_at, o.accepted_at) / 60.0"));
        assertTrue(sql.contains("timestampdiff(second, o.accepted_at, o.arrived_at) / 60.0"));
    }

    @Test
    public void smsAccountMapperShouldParse() throws Exception
    {
        Configuration configuration = parse("mapper/workorder/WorkOrderSmsAccountMapper.xml");
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderSmsAccountMapper.selectAccount"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderSmsAccountMapper.selectSendStatistics"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderSmsAccountMapper.updateSettings"));
    }

    private Configuration parse(String resource) throws Exception
    {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
