package com.ruoyi.workorder.mapper;

import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

/** Verifies mapper XML at build time instead of waiting for the first application request. */
public class WorkOrderMapperXmlTest
{
    @Test
    public void allWorkOrderMappersShouldParse() throws Exception
    {
        List<String> resources = Arrays.asList(
                "mapper/workorder/WorkOrderMapper.xml",
                "mapper/workorder/WorkOrderAttachmentMapper.xml",
                "mapper/workorder/WorkOrderActionLogMapper.xml",
                "mapper/workorder/WorkOrderCategoryMapper.xml",
                "mapper/workorder/WorkOrderAssignmentMapper.xml",
                "mapper/workorder/WorkOrderProcessRecordMapper.xml",
                "mapper/workorder/WorkOrderEvaluationMapper.xml",
                "mapper/workorder/WorkOrderEngineerMapper.xml",
                "mapper/workorder/WorkOrderSlaRuleMapper.xml");
        Configuration configuration = new Configuration();
        for (String resource : resources)
        {
            try (InputStream input = Resources.getResourceAsStream(resource))
            {
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        assertTrue(configuration.hasMapper(WorkOrderMapper.class));
        assertTrue(configuration.hasMapper(WorkOrderAttachmentMapper.class));
        assertTrue(configuration.hasMapper(WorkOrderActionLogMapper.class));
        assertTrue(configuration.hasMapper(WorkOrderCategoryMapper.class));
        assertTrue(configuration.hasMapper(WorkOrderAssignmentMapper.class));
        assertTrue(configuration.hasMapper(WorkOrderProcessRecordMapper.class));
        assertTrue(configuration.hasMapper(WorkOrderEvaluationMapper.class));
        assertTrue(configuration.hasMapper(WorkOrderEngineerMapper.class));
        assertTrue(configuration.hasMapper(WorkOrderSlaRuleMapper.class));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderMapper.applyCommand"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderEvaluationMapper.insert"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderEvaluationMapper.selectByOrderId"));
    }

    @Test
    public void stateChangeSqlShouldKeepStatusAndVersionCompareAndSetConditions() throws Exception
    {
        Configuration configuration = new Configuration();
        String resource = "mapper/workorder/WorkOrderMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("actionType", "ACCEPT");
        String sql = configuration.getMappedStatement("com.ruoyi.workorder.mapper.WorkOrderMapper.applyCommand")
                .getBoundSql(parameters).getSql().replaceAll("\\s+", " ").toLowerCase();

        // 三个条件共同保证不同幂等键的旧版本并发请求最多只有一个能够更新成功。
        assertTrue(sql.contains("where id = ? and status = ? and version = ? and del_flag = '0'"));
        assertTrue(sql.contains("version = version + 1"));
    }
}
