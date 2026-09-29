package com.ruoyi.workorder.mapper;

import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.ruoyi.workorder.application.model.WorkOrderNotificationQuery;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

/** 在构建期解析 F02 Mapper，并锁定领取与用户隔离条件。 */
public class WorkOrderNotificationMapperXmlTest
{
    @Test
    public void notificationMapperShouldParseAndExposeLifecycleStatements() throws Exception
    {
        Configuration configuration = parse();
        assertTrue(configuration.hasMapper(WorkOrderNotificationMapper.class));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderNotificationMapper.insertTask"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderNotificationMapper.claimTask"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderNotificationMapper.insertMessage"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderNotificationMapper.markFailure"));
        assertTrue(configuration.hasStatement("com.ruoyi.workorder.mapper.WorkOrderNotificationMapper.manualRetry"));
    }

    @Test
    public void messageQueryMustAlwaysUseCurrentRecipient() throws Exception
    {
        Configuration configuration = parse();
        Map<String, Object> params = new HashMap<String, Object>();
        WorkOrderNotificationQuery query = new WorkOrderNotificationQuery(); query.setCategory("WORK_ORDER");
        params.put("recipientId", 7L); params.put("query", query);
        String sql = configuration.getMappedStatement(
                "com.ruoyi.workorder.mapper.WorkOrderNotificationMapper.selectMessages")
                .getBoundSql(params).getSql().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("where recipient_id = ?"));
        assertTrue(sql.contains("and category = ?"));
    }

    @Test
    public void claimMustGuardDueAndExpiredProcessingTasks() throws Exception
    {
        Configuration configuration = parse();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", 1L); params.put("workerId", "worker"); params.put("now", new Date());
        params.put("lockExpiredAt", new Date());
        String sql = configuration.getMappedStatement(
                "com.ruoyi.workorder.mapper.WorkOrderNotificationMapper.claimTask")
                .getBoundSql(params).getSql().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("status in ('pending', 'retry')"));
        assertTrue(sql.contains("next_retry_at &lt;= ?") || sql.contains("next_retry_at <= ?"));
        assertTrue(sql.contains("status = 'processing'"));
        assertTrue(sql.contains("locked_at &lt;= ?") || sql.contains("locked_at <= ?"));
    }

    @Test
    public void adminTaskListMustApplyDepartmentDataScope() throws Exception
    {
        Configuration configuration = parse();
        WorkOrderNotificationQuery query = new WorkOrderNotificationQuery();
        query.getParams().put("dataScope", " AND d.dept_id = 23 ");
        String sql = configuration.getMappedStatement(
                "com.ruoyi.workorder.mapper.WorkOrderNotificationMapper.selectTasks")
                .getBoundSql(query).getSql().replaceAll("\\s+", " ").toLowerCase();
        assertTrue(sql.contains("left join wo_order o"));
        assertTrue(sql.contains("left join sys_dept d"));
        assertTrue(sql.contains("d.dept_id = 23"));
    }

    private Configuration parse() throws Exception
    {
        Configuration configuration = new Configuration();
        String resource = "mapper/workorder/WorkOrderNotificationMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
