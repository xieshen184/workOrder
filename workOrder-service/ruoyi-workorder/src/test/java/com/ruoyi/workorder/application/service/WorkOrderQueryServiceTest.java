package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderQuery;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderEvaluation;
import com.ruoyi.workorder.mapper.WorkOrderEvaluationMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import org.junit.Test;

public class WorkOrderQueryServiceTest
{
    @Test
    public void applicantCanReadEvaluationWithoutAdminQueryPermission() throws Exception
    {
        WorkOrder order = order();
        WorkOrderEvaluation evaluation = new WorkOrderEvaluation();
        WorkOrderQueryService service = service(order, evaluation);

        assertSame(evaluation, service.getVisibleEvaluation(1L, actor(7L)));
    }

    @Test
    public void unrelatedUserWithoutPermissionCannotReadEvaluation() throws Exception
    {
        WorkOrderQueryService service = service(order(), new WorkOrderEvaluation());
        try
        {
            service.getVisibleEvaluation(1L, actor(8L));
            fail("expected ServiceException");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), error.getCode());
        }
    }

    @Test
    public void evaluationAdminLookupShouldDeclareItsOwnDataScopePermission() throws Exception
    {
        DataScope scope = WorkOrderQueryService.class
                .getMethod("selectEvaluationScopedById", WorkOrderQuery.class)
                .getAnnotation(DataScope.class);
        assertEquals("workorder:evaluation:query", scope.permission());
        assertEquals("d", scope.deptAlias());
        assertEquals("u", scope.userAlias());
    }

    private WorkOrderQueryService service(WorkOrder order, WorkOrderEvaluation evaluation) throws Exception
    {
        WorkOrderQueryService service = new WorkOrderQueryService();
        set(service, "orderMapper", new FixedOrderMapper(order));
        set(service, "evaluationMapper", new FixedEvaluationMapper(evaluation));
        return service;
    }

    private void set(Object target, String fieldName, Object value) throws Exception
    {
        Field field = WorkOrderQueryService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private WorkOrder order()
    {
        WorkOrder order = new WorkOrder();
        order.setId(1L); order.setApplicantId(7L); order.setStatus("CLOSED");
        return order;
    }

    private WorkOrderActor actor(Long id)
    {
        return new WorkOrderActor(id, "user", "User", null, 1L, "Dept", Collections.emptySet());
    }

    private static class FixedOrderMapper implements WorkOrderMapper
    {
        private final WorkOrder order;
        FixedOrderMapper(WorkOrder order) { this.order = order; }
        @Override public WorkOrder selectById(Long id) { return order; }
        @Override public int insert(WorkOrder value) { return 0; }
        @Override public WorkOrder selectByIdForUpdate(Long id) { return order; }
        @Override public WorkOrder selectByOrderNo(String orderNo) { return null; }
        @Override public WorkOrder selectScopedById(WorkOrderQuery query) { return null; }
        @Override public List<WorkOrder> selectList(WorkOrderQuery query) { return null; }
        @Override public List<WorkOrder> selectSlaOpenOrders() { return Collections.emptyList(); }
        @Override public List<WorkOrder> selectAutoCloseCandidates(Date before) { return Collections.emptyList(); }
        @Override public int updateSlaFlags(Long id, String status, String warning, String overdue, String response,
                String arrival, String finish, Date time) { return 0; }
        @Override public int autoClose(Long id, Integer version, Date closedAt) { return 0; }
        @Override public int applyCommand(Long id, String source, String target, Integer version,
                String action, Long engineerId, String engineerName, Date time, String updateBy) { return 0; }
    }

    private static class FixedEvaluationMapper implements WorkOrderEvaluationMapper
    {
        private final WorkOrderEvaluation evaluation;
        FixedEvaluationMapper(WorkOrderEvaluation evaluation) { this.evaluation = evaluation; }
        @Override public int insert(WorkOrderEvaluation value) { return 0; }
        @Override public WorkOrderEvaluation selectByOrderId(Long orderId) { return evaluation; }
    }
}
