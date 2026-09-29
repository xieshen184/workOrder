package com.ruoyi.workorder.mapper;

import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import com.ruoyi.workorder.application.model.WorkOrderDelayRequestQuery;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

/** 在构建期解析 F01 Mapper，避免首个延期请求到达时才暴露 XML 错误。 */
public class WorkOrderDelayRequestMapperXmlTest
{
    @Test
    public void delayMapperXmlShouldParseAndExposeAllStatements() throws Exception
    {
        Configuration configuration = parse();
        assertTrue(configuration.hasMapper(WorkOrderDelayRequestMapper.class));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.insert"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.selectByIdForUpdate"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.markDelayPending"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.applyApprovedDelay"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.clearDelayPending"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.updateDecision"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.cancelPendingByOrderId"));
    }

    @Test
    public void adminListShouldApplyServerSideDepartmentDataScope() throws Exception
    {
        Configuration configuration = parse();
        WorkOrderDelayRequestQuery query = new WorkOrderDelayRequestQuery();
        query.getParams().put("dataScope", " AND d.dept_id = 23 ");
        String sql = configuration
                .getMappedStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.selectList")
                .getBoundSql(query).getSql().replaceAll("\\s+", " ").toLowerCase();

        assertTrue(sql.contains("join wo_order o"));
        assertTrue(sql.contains("left join sys_dept d"));
        assertTrue(sql.contains("d.dept_id = 23"));
    }

    @Test
    public void approvalSqlMustKeepPendingAndVersionGuards() throws Exception
    {
        Configuration configuration = parse();
        String sql = configuration
                .getMappedStatement("com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper.applyApprovedDelay")
                .getBoundSql(new HashMap<String, Object>())
                .getSql().replaceAll("\\s+", " ").toLowerCase();

        assertTrue(sql.contains("extension_deadline = ?"));
        assertTrue(sql.contains("version = ?"));
        assertTrue(sql.contains("delay_pending_flag = '1'"));
    }

    private Configuration parse() throws Exception
    {
        Configuration configuration = new Configuration();
        String resource = "mapper/workorder/WorkOrderDelayRequestMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
