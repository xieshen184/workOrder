package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderSlaRuleQuery;
import com.ruoyi.workorder.application.model.WorkOrderSlaRuleSaveRequest;
import com.ruoyi.workorder.domain.model.WorkOrderCategory;
import com.ruoyi.workorder.domain.model.WorkOrderSlaRule;
import com.ruoyi.workorder.mapper.WorkOrderCategoryMapper;
import com.ruoyi.workorder.mapper.WorkOrderSlaRuleMapper;
import org.junit.Test;

public class WorkOrderSlaRuleServiceTest
{
    @Test
    public void ruleCodeShouldRemainImmutable()
    {
        FakeRuleMapper rules = new FakeRuleMapper();
        rules.current = rule(1L, "SLA_OLD");
        try
        {
            new WorkOrderSlaRuleService(rules, new FakeCategoryMapper()).update(1L, request("SLA_NEW"), "admin");
            fail("expected conflict");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), error.getCode());
        }
    }

    @Test
    public void sameScopePriorityAndPeriodShouldNotOverlap()
    {
        FakeRuleMapper rules = new FakeRuleMapper(); rules.overlap = 1;
        try
        {
            new WorkOrderSlaRuleService(rules, new FakeCategoryMapper()).create(request("SLA_NEW"), "admin");
            fail("expected conflict");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), error.getCode());
        }
    }

    @Test
    public void disabledRuleShouldNotReserveAnEffectivePeriod()
    {
        FakeRuleMapper rules = new FakeRuleMapper(); rules.overlap = 1;
        WorkOrderSlaRuleSaveRequest request = request("SLA_DISABLED");
        request.setStatus("1");

        WorkOrderSlaRule result = new WorkOrderSlaRuleService(rules, new FakeCategoryMapper())
                .create(request, "admin");

        assertEquals("1", result.getStatus());
    }

    @Test
    public void editingReferencedRuleShouldOnlyChangeFutureMatching()
    {
        FakeRuleMapper rules = new FakeRuleMapper();
        rules.current = rule(1L, "SLA_KEEP"); rules.current.setReferenceCount(12L);
        WorkOrderSlaRule result = new WorkOrderSlaRuleService(rules, new FakeCategoryMapper())
                .update(1L, request("SLA_KEEP"), "admin");
        assertEquals(Long.valueOf(12L), result.getReferenceCount());
        assertEquals(1, rules.updateCount);
    }

    private WorkOrderSlaRuleSaveRequest request(String code)
    {
        WorkOrderSlaRuleSaveRequest request = new WorkOrderSlaRuleSaveRequest();
        request.setRuleCode(code); request.setRuleName("规则"); request.setUrgencyLevel(1); request.setPriority(100);
        request.setEffectiveFrom(new Date(1_000L)); request.setResponseMinutes(60);
        request.setArrivalMinutes(120); request.setFinishMinutes(240); request.setReminderBeforeMin(15);
        request.setAllowExtension("1"); request.setStatus("0"); return request;
    }

    private WorkOrderSlaRule rule(Long id, String code)
    {
        WorkOrderSlaRule rule = new WorkOrderSlaRule();
        rule.setId(id); rule.setRuleCode(code); rule.setRuleName("规则"); rule.setReferenceCount(0L); return rule;
    }

    private static class FakeRuleMapper implements WorkOrderSlaRuleMapper
    {
        private WorkOrderSlaRule current; private int overlap; private int updateCount;
        @Override public WorkOrderSlaRule selectEffective(Long categoryId, Integer urgency, Date at) { return current; }
        @Override public WorkOrderSlaRule selectById(Long id) { return current; }
        @Override public WorkOrderSlaRule selectByCode(String code) { return null; }
        @Override public List<WorkOrderSlaRule> selectList(WorkOrderSlaRuleQuery query) { return Collections.emptyList(); }
        @Override public int countOverlapping(Long id, Long categoryId, Integer urgency, Integer priority,
                Date from, Date to) { return overlap; }
        @Override public int insert(WorkOrderSlaRule rule) { current = rule; rule.setId(1L); return 1; }
        @Override public int update(WorkOrderSlaRule rule)
        { rule.setReferenceCount(current.getReferenceCount()); current = rule; updateCount++; return 1; }
    }

    private static class FakeCategoryMapper implements WorkOrderCategoryMapper
    {
        @Override public WorkOrderCategory selectById(Long id) { return null; }
        @Override public WorkOrderCategory selectActiveById(Long id) { return null; }
        @Override public WorkOrderCategory selectByCode(String code) { return null; }
        @Override public List<WorkOrderCategory> selectActiveList() { return Collections.emptyList(); }
        @Override public List<WorkOrderCategory> selectManageList() { return Collections.emptyList(); }
        @Override public int countActiveChildren(Long id) { return 0; }
        @Override public int insert(WorkOrderCategory category) { return 0; }
        @Override public int update(WorkOrderCategory category) { return 0; }
    }
}
