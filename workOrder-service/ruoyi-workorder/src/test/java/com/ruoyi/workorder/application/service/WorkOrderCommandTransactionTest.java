package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderCommand;
import com.ruoyi.workorder.application.model.WorkOrderCommandResult;
import com.ruoyi.workorder.application.model.WorkOrderQuery;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.domain.model.WorkOrderEvaluation;
import com.ruoyi.workorder.domain.model.WorkOrderEngineer;
import com.ruoyi.workorder.domain.model.WorkOrderProcessRecord;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import com.ruoyi.workorder.mapper.WorkOrderAssignmentMapper;
import com.ruoyi.workorder.mapper.WorkOrderAttachmentMapper;
import com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper;
import com.ruoyi.workorder.mapper.WorkOrderEngineerMapper;
import com.ruoyi.workorder.mapper.WorkOrderEvaluationMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import com.ruoyi.workorder.mapper.WorkOrderProcessRecordMapper;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

public class WorkOrderCommandTransactionTest
{
    @Test
    public void evaluationShouldUpdateInsertAndLogInOneTransactionalMethod() throws Exception
    {
        RecordingOrderMapper orders = new RecordingOrderMapper(order(WorkOrderStatus.COMPLETED, 5));
        RecordingActionLogMapper logs = new RecordingActionLogMapper();
        RecordingEvaluationMapper evaluations = new RecordingEvaluationMapper();
        RecordingProcessMapper processes = new RecordingProcessMapper();
        WorkOrderCommandTransaction transaction = transaction(orders, logs, evaluations, processes);

        WorkOrderCommandResult result = transaction.execute(WorkOrderCommand.evaluation(1L,
                "evaluate-key", 5, 5, 4, 5, 4, "  服务很好  "), actor(1L, "workorder:evaluation:add"));

        assertEquals("CLOSED", result.getStatus());
        assertEquals(Integer.valueOf(6), result.getVersion());
        assertEquals("EVALUATE", orders.actionType);
        assertEquals(Integer.valueOf(5), evaluations.inserted.getOverallScore());
        assertEquals("服务很好", evaluations.inserted.getEvaluationContent());
        assertEquals("提交评价（总体5分）：服务很好", logs.inserted.getActionContent());
        JSONObject ext = JSON.parseObject(logs.inserted.getExtJson());
        assertEquals(Long.valueOf(88L), ext.getLong("evaluationId"));
        assertNotNull(WorkOrderCommandTransaction.class
                .getMethod("execute", WorkOrderCommand.class, WorkOrderActor.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    public void repeatedEvaluationWithDifferentKeyShouldBeExplicitConflict()
    {
        RecordingOrderMapper orders = new RecordingOrderMapper(order(WorkOrderStatus.CLOSED, 6));
        RecordingEvaluationMapper evaluations = new RecordingEvaluationMapper();
        evaluations.existing = new WorkOrderEvaluation();
        WorkOrderCommandTransaction transaction = transaction(orders, new RecordingActionLogMapper(),
                evaluations, new RecordingProcessMapper());
        try
        {
            transaction.execute(WorkOrderCommand.evaluation(1L, "other-eval-key", 6,
                    5, null, null, null, null), actor(1L, "workorder:evaluation:add"));
            fail("expected ServiceException");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), error.getCode());
            assertEquals(true, error.getMessage().contains("WO_EVALUATION_CONFLICT"));
        }
        assertNull(orders.actionType);
    }

    @Test
    public void returnShouldOnlyAppendActionLogAndKeepFinishProcessHistoryUntouched()
    {
        RecordingOrderMapper orders = new RecordingOrderMapper(order(WorkOrderStatus.WAIT_CONFIRM, 4));
        RecordingActionLogMapper logs = new RecordingActionLogMapper();
        RecordingProcessMapper processes = new RecordingProcessMapper();
        WorkOrderCommandResult result = transaction(orders, logs, new RecordingEvaluationMapper(), processes)
                .execute(WorkOrderCommand.reasoned(1L, WorkOrderAction.RETURN, "return-key", 4, "照片不清晰"),
                        actor(1L, "workorder:order:return"));

        assertEquals("PROCESSING", result.getStatus());
        assertEquals(0, processes.insertCount);
        assertEquals("退回返工：照片不清晰", logs.inserted.getActionContent());
    }

    @Test
    public void sameIdempotencyKeyShouldReplayWithoutSecondStateChangeOrAuditLog()
    {
        RecordingOrderMapper orders = new RecordingOrderMapper(order(WorkOrderStatus.WAIT_ASSIGN, 0));
        RecordingActionLogMapper logs = new RecordingActionLogMapper();
        WorkOrderCommandTransaction transaction = transaction(orders, logs,
                new RecordingEvaluationMapper(), new RecordingProcessMapper());
        WorkOrderCommand command = WorkOrderCommand.reasoned(1L, WorkOrderAction.CANCEL,
                "cancel-retry-key", 0, "重复报修");

        WorkOrderCommandResult first = transaction.execute(command, actor(1L, "workorder:order:cancel"));
        WorkOrderCommandResult replay = transaction.execute(command, actor(1L, "workorder:order:cancel"));

        assertFalse(first.isIdempotentReplay());
        assertEquals(true, replay.isIdempotentReplay());
        assertEquals(1, orders.applyCount);
        assertEquals(1, logs.insertCount);
    }

    @Test
    public void auditLogShouldContainActorTransitionTimeAndReplayFingerprint()
    {
        RecordingOrderMapper orders = new RecordingOrderMapper(order(WorkOrderStatus.WAIT_ASSIGN, 0));
        RecordingActionLogMapper logs = new RecordingActionLogMapper();
        WorkOrderCommand command = WorkOrderCommand.reasoned(1L, WorkOrderAction.CANCEL,
                "cancel-audit-key", 0, "重复报修");

        transaction(orders, logs, new RecordingEvaluationMapper(), new RecordingProcessMapper())
                .execute(command, actor(1L, "workorder:order:cancel"));

        assertEquals(Long.valueOf(1L), logs.inserted.getOrderId());
        assertEquals("CANCEL", logs.inserted.getActionType());
        assertEquals("WAIT_ASSIGN", logs.inserted.getFromStatus());
        assertEquals("CANCELLED", logs.inserted.getToStatus());
        assertEquals(Long.valueOf(1L), logs.inserted.getOperatorId());
        assertEquals("User", logs.inserted.getOperatorName());
        assertEquals("cancel-audit-key", logs.inserted.getIdempotencyKey());
        assertNotNull(logs.inserted.getActionTime());
        JSONObject ext = JSON.parseObject(logs.inserted.getExtJson());
        assertEquals(Integer.valueOf(1), ext.getInteger("version"));
        assertEquals(WorkOrderCommandService.hash(command), ext.getString("requestFingerprint"));
    }

    @Test
    public void finishShouldCancelThePendingDelayInTheSameTransaction()
    {
        WorkOrder order = order(WorkOrderStatus.PROCESSING, 3);
        order.setCurrentAssigneeId(1L);
        order.setDelayPendingFlag("1");
        RecordingOrderMapper orders = new RecordingOrderMapper(order);
        AtomicInteger cancelled = new AtomicInteger();
        WorkOrderDelayRequestMapper delays = proxy(WorkOrderDelayRequestMapper.class, (method, args) -> {
            if ("cancelPendingByOrderId".equals(method))
            {
                cancelled.incrementAndGet();
                assertEquals(Long.valueOf(1L), args[0]);
                assertEquals("工单已完工，待审延期自动取消", args[3]);
                return 1;
            }
            return null;
        });
        WorkOrderAttachmentMapper attachments = proxy(WorkOrderAttachmentMapper.class,
                (method, args) -> "bindToProcess".equals(method) ? ((List<?>) args[4]).size() : null);
        WorkOrderCommandTransaction transaction = new WorkOrderCommandTransaction(orders,
                new RecordingActionLogMapper(), noOp(WorkOrderAssignmentMapper.class),
                new RecordingProcessMapper(), attachments, noOp(WorkOrderEngineerMapper.class),
                new RecordingEvaluationMapper(), delays);

        transaction.execute(WorkOrderCommand.process(1L, WorkOrderAction.FINISH,
                "finish-delay-key", 3, "维修完成", Arrays.asList(99L)),
                actor(1L, "workorder:order:finish"));

        assertEquals(1, cancelled.get());
        assertEquals("FINISH", orders.actionType);
    }

    @Test
    public void reassignShouldCancelThePreviousEngineersPendingDelay()
    {
        WorkOrder order = order(WorkOrderStatus.PROCESSING, 3);
        order.setCurrentAssigneeId(1L);
        order.setDelayPendingFlag("1");
        order.setSlaRuleId(8L);
        order.setSubmittedAt(new Date(1_000L));
        order.setResponseDeadline(new Date(61_000L));
        AtomicInteger cancelled = new AtomicInteger();
        WorkOrderDelayRequestMapper delays = proxy(WorkOrderDelayRequestMapper.class, (method, args) -> {
            if ("cancelPendingByOrderId".equals(method))
            {
                cancelled.incrementAndGet();
                assertEquals("工单已改派，原处理人的待审延期自动取消", args[3]);
                return 1;
            }
            return null;
        });
        WorkOrderEngineer engineer = new WorkOrderEngineer();
        engineer.setId(2L); engineer.setName("Engineer B"); engineer.setDeptId(2L);
        WorkOrderEngineerMapper engineers = proxy(WorkOrderEngineerMapper.class,
                (method, args) -> "selectEligibleById".equals(method) ? engineer : null);
        WorkOrderAssignmentMapper assignments = proxy(WorkOrderAssignmentMapper.class,
                (method, args) -> ("closeActive".equals(method) || "insert".equals(method)) ? 1 : null);
        RecordingOrderMapper orders = new RecordingOrderMapper(order);
        WorkOrderCommandTransaction transaction = new WorkOrderCommandTransaction(orders,
                new RecordingActionLogMapper(), assignments, new RecordingProcessMapper(),
                noOp(WorkOrderAttachmentMapper.class), engineers, new RecordingEvaluationMapper(), delays);

        transaction.execute(WorkOrderCommand.assignment(1L, WorkOrderAction.REASSIGN,
                "reassign-delay-key", 3, 2L, "人员调整"),
                actor(9L, "workorder:order:list", "workorder:order:reassign"));

        assertEquals(1, cancelled.get());
        assertEquals("REASSIGN", orders.actionType);
    }

    private WorkOrderCommandTransaction transaction(WorkOrderMapper orders, WorkOrderActionLogMapper logs,
            WorkOrderEvaluationMapper evaluations, WorkOrderProcessRecordMapper processes)
    {
        return new WorkOrderCommandTransaction(orders, logs, noOp(WorkOrderAssignmentMapper.class), processes,
                noOp(WorkOrderAttachmentMapper.class), noOp(WorkOrderEngineerMapper.class), evaluations,
                noOp(WorkOrderDelayRequestMapper.class));
    }

    @SuppressWarnings("unchecked")
    private <T> T noOp(Class<T> type)
    {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> method.getReturnType() == Integer.TYPE ? 0 : null);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, MethodHandler handler)
    {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> {
                    Object value = handler.invoke(method.getName(), args);
                    if (value != null) return value;
                    return method.getReturnType() == Integer.TYPE ? 0 : null;
                });
    }

    private interface MethodHandler
    {
        Object invoke(String method, Object[] args);
    }

    private WorkOrder order(WorkOrderStatus status, int version)
    {
        WorkOrder order = new WorkOrder();
        order.setId(1L); order.setOrderNo("WO-1"); order.setApplicantId(1L);
        order.setStatus(status.getCode()); order.setVersion(version);
        return order;
    }

    private WorkOrderActor actor(Long id, String... permissions)
    {
        return new WorkOrderActor(id, "user", "User", null, 1L, "Dept",
                new HashSet<String>(Arrays.asList(permissions)));
    }

    private static class RecordingOrderMapper implements WorkOrderMapper
    {
        private final WorkOrder order;
        private String actionType;
        private int applyCount;
        RecordingOrderMapper(WorkOrder order) { this.order = order; }
        @Override public WorkOrder selectByIdForUpdate(Long id) { return order; }
        @Override public int applyCommand(Long id, String source, String target, Integer version,
                String action, Long engineerId, String engineerName, Date time, String updateBy)
        { actionType = action; applyCount++; return 1; }
        @Override public int insert(WorkOrder value) { return 0; }
        @Override public WorkOrder selectById(Long id) { return order; }
        @Override public WorkOrder selectByOrderNo(String orderNo) { return null; }
        @Override public WorkOrder selectScopedById(WorkOrderQuery query) { return null; }
        @Override public List<WorkOrder> selectList(WorkOrderQuery query) { return null; }
        @Override public List<WorkOrder> selectSlaOpenOrders() { return java.util.Collections.emptyList(); }
        @Override public List<WorkOrder> selectAutoCloseCandidates(Date before) { return java.util.Collections.emptyList(); }
        @Override public int updateSlaFlags(Long id, String status, String warning, String overdue, String response,
                String arrival, String finish, Date time) { return 0; }
        @Override public int autoClose(Long id, Integer version, Date closedAt) { return 0; }
    }

    private static class RecordingActionLogMapper implements WorkOrderActionLogMapper
    {
        private WorkOrderActionLog inserted;
        private int insertCount;
        @Override public int insert(WorkOrderActionLog log) { inserted = log; insertCount++; return 1; }
        @Override public WorkOrderActionLog selectByIdempotency(Long operatorId, String action, String key)
        {
            if (inserted == null) return null;
            return inserted.getOperatorId().equals(operatorId)
                    && inserted.getActionType().equals(action)
                    && inserted.getIdempotencyKey().equals(key) ? inserted : null;
        }
        @Override public List<WorkOrderActionLog> selectByOrderId(Long orderId) { return null; }
    }

    private static class RecordingEvaluationMapper implements WorkOrderEvaluationMapper
    {
        private WorkOrderEvaluation existing;
        private WorkOrderEvaluation inserted;
        @Override public int insert(WorkOrderEvaluation evaluation)
        { evaluation.setId(88L); inserted = evaluation; return 1; }
        @Override public WorkOrderEvaluation selectByOrderId(Long orderId) { return existing; }
    }

    private static class RecordingProcessMapper implements WorkOrderProcessRecordMapper
    {
        private int insertCount;
        @Override public int insert(WorkOrderProcessRecord record) { insertCount++; return 1; }
    }
}
