package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderDelayDecisionRequest;
import com.ruoyi.workorder.application.model.WorkOrderDelayRequestCreateRequest;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.domain.model.WorkOrderDelayRequest;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

/** 延期申请的并发边界、权限可见性、截止时间和审计副作用测试。 */
public class WorkOrderDelayServiceTest
{
    private static final Date ORIGINAL = new Date(2_000_000L);
    private static final Date REQUESTED = new Date(3_000_000L);

    @Test
    public void createShouldLockOrderSetPendingBindAttachmentsAndWriteAuditLog() throws Exception
    {
        Fixture fixture = fixture(false);
        WorkOrderDelayRequestCreateRequest request = createRequest(4);
        request.setAttachmentIds(Arrays.asList(101L));

        WorkOrderDelayRequest result = fixture.service.create(9L, request, fixture.actor, "delay-create-key");

        assertEquals(Long.valueOf(11L), result.getId());
        assertEquals("PENDING", result.getRequestStatus());
        assertEquals(REQUESTED, result.getRequestedDeadline());
        assertEquals("1", fixture.order.getDelayPendingFlag());
        assertEquals(Integer.valueOf(5), fixture.order.getVersion());
        assertEquals(Arrays.asList(101L), fixture.boundAttachmentIds);
        assertEquals("REQUEST_DELAY", fixture.logs.get(0).getActionType());
        assertEquals(1, fixture.visibilityChecks);
    }

    @Test
    public void createShouldReplayTheFirstResultAndRejectKeyReuseForDifferentContent()
    {
        Fixture fixture = fixture(false);
        WorkOrderDelayRequestCreateRequest request = createRequest(4);

        WorkOrderDelayRequest first = fixture.service.create(9L, request, fixture.actor, "delay-replay-key");
        WorkOrderDelayRequest replay = fixture.service.create(9L, request, fixture.actor, "delay-replay-key");

        assertEquals(first.getId(), replay.getId());
        assertEquals(1, fixture.logs.size());
        assertEquals(Integer.valueOf(5), fixture.order.getVersion());

        WorkOrderDelayRequestCreateRequest changed = createRequest(4);
        changed.setReason("另一份延期原因");
        try
        {
            fixture.service.create(9L, changed, fixture.actor, "delay-replay-key");
            fail("同一个幂等键不能用于不同延期申请");
        }
        catch (com.ruoyi.common.exception.ServiceException expected)
        {
            assertEquals(Integer.valueOf(409), expected.getCode());
        }
        assertEquals(1, fixture.logs.size());
    }

    @Test
    public void approveShouldUpdateEffectiveDeadlineOnlyOnceAndRequireVisibility() throws Exception
    {
        Fixture fixture = fixture(true);
        WorkOrderDelayDecisionRequest decision = new WorkOrderDelayDecisionRequest();
        decision.setReason("已确认备件到货时间");

        WorkOrderDelayRequest result = fixture.service.approve(11L, decision, fixture.actor);

        assertEquals("APPROVED", result.getRequestStatus());
        assertEquals(REQUESTED, fixture.order.getExtensionDeadline());
        assertEquals("0", fixture.order.getDelayPendingFlag());
        assertEquals(Integer.valueOf(5), fixture.order.getVersion());
        assertEquals("APPROVE_DELAY", fixture.logs.get(0).getActionType());
        assertTrue(fixture.visibilityChecks > 0);

        try
        {
            fixture.service.approve(11L, decision, fixture.actor);
            fail("同一延期申请不能重复生效");
        }
        catch (RuntimeException expected)
        {
            assertEquals(Integer.valueOf(409), ((com.ruoyi.common.exception.ServiceException) expected).getCode());
        }
        assertEquals(Integer.valueOf(5), fixture.order.getVersion());
        assertEquals(1, fixture.logs.size());
    }

    @Test
    public void rejectShouldNotWriteExtensionDeadlineAndMustRequireReason() throws Exception
    {
        Fixture fixture = fixture(true);
        WorkOrderDelayDecisionRequest decision = new WorkOrderDelayDecisionRequest();
        decision.setReason("现场已恢复处理，不再需要延长时限");

        WorkOrderDelayRequest result = fixture.service.reject(11L, decision, fixture.actor);

        assertEquals("REJECTED", result.getRequestStatus());
        assertEquals(null, fixture.order.getExtensionDeadline());
        assertEquals("0", fixture.order.getDelayPendingFlag());
        assertEquals("REJECT_DELAY", fixture.logs.get(0).getActionType());

        Fixture invalid = fixture(true);
        try
        {
            invalid.service.reject(11L, new WorkOrderDelayDecisionRequest(), invalid.actor);
            fail("拒绝必须填写原因");
        }
        catch (com.ruoyi.common.exception.ServiceException expected)
        {
            assertEquals(Integer.valueOf(400), expected.getCode());
        }
    }

    @Test
    public void createShouldRejectNonAssigneeOrDisallowedSlaBeforeMutation()
    {
        Fixture fixture = fixture(false);
        fixture.order.setCurrentAssigneeId(99L);
        try
        {
            fixture.service.create(9L, createRequest(4), fixture.actor, "delay-create-key");
            fail("非当前处理人不能申请延期");
        }
        catch (com.ruoyi.common.exception.ServiceException expected)
        {
            assertEquals(Integer.valueOf(403), expected.getCode());
        }
        assertEquals(null, fixture.order.getDelayPendingFlag());
        assertTrue(fixture.logs.isEmpty());
    }

    @Test
    public void mutatingOperationsShouldBeTransactional() throws Exception
    {
        assertNotNull(WorkOrderDelayService.class
                .getMethod("create", Long.class, WorkOrderDelayRequestCreateRequest.class,
                        WorkOrderActor.class, String.class)
                .getAnnotation(Transactional.class));
        assertNotNull(WorkOrderDelayService.class
                .getMethod("approve", Long.class, WorkOrderDelayDecisionRequest.class, WorkOrderActor.class)
                .getAnnotation(Transactional.class));
        assertNotNull(WorkOrderDelayService.class
                .getMethod("reject", Long.class, WorkOrderDelayDecisionRequest.class, WorkOrderActor.class)
                .getAnnotation(Transactional.class));
    }

    private WorkOrderDelayRequestCreateRequest createRequest(int version)
    {
        WorkOrderDelayRequestCreateRequest request = new WorkOrderDelayRequestCreateRequest();
        request.setRequestedDeadline(REQUESTED);
        request.setReason("等待外部备件到货");
        request.setVersion(version);
        return request;
    }

    private Fixture fixture(boolean pending)
    {
        Fixture fixture = new Fixture();
        fixture.order.setId(9L);
        fixture.order.setOrderNo("WO-9");
        fixture.order.setTitle("设备故障");
        fixture.order.setStatus("PROCESSING");
        fixture.order.setCurrentAssigneeId(7L);
        fixture.order.setCurrentAssigneeName("工程师");
        fixture.order.setFinishDeadline(ORIGINAL);
        fixture.order.setSlaAllowExtension("1");
        fixture.order.setVersion(4);
        if (pending)
        {
            fixture.order.setDelayPendingFlag("1");
            fixture.delay.setId(11L);
            fixture.delay.setOrderId(9L);
            fixture.delay.setOrderNo("WO-9");
            fixture.delay.setOrderTitle("设备故障");
            fixture.delay.setApplicantId(7L);
            fixture.delay.setApplicantName("工程师");
            fixture.delay.setOriginalDeadline(ORIGINAL);
            fixture.delay.setRequestedDeadline(REQUESTED);
            fixture.delay.setReason("等待外部备件到货");
            fixture.delay.setRequestStatus("PENDING");
        }
        fixture.actor = new WorkOrderActor(7L, "engineer", "工程师", null, 1L, "维修部",
                new java.util.HashSet<String>(Arrays.asList(
                        "workorder:delay:add", "workorder:delay:approve")));
        fixture.orderMapper = orderMapper(fixture.order);
        fixture.delayMapper = delayMapper(fixture);
        fixture.service = new WorkOrderDelayService(fixture.delayMapper, fixture.orderMapper, fixture.actionMapper,
                new WorkOrderQueryService()
                {
                    @Override public void assertVisible(Long orderId, WorkOrderActor actor)
                    {
                        fixture.visibilityChecks++;
                    }
                });
        return fixture;
    }

    private WorkOrderMapper orderMapper(final WorkOrder order)
    {
        return (WorkOrderMapper) Proxy.newProxyInstance(WorkOrderMapper.class.getClassLoader(),
                new Class<?>[] { WorkOrderMapper.class }, (proxy, method, args) -> {
                    if ("selectByIdForUpdate".equals(method.getName()) || "selectById".equals(method.getName()))
                        return order;
                    return defaultValue(method.getReturnType());
                });
    }

    private WorkOrderDelayRequestMapper delayMapper(final Fixture fixture)
    {
        return (WorkOrderDelayRequestMapper) Proxy.newProxyInstance(WorkOrderDelayRequestMapper.class.getClassLoader(),
                new Class<?>[] { WorkOrderDelayRequestMapper.class }, (proxy, method, args) -> {
                    String name = method.getName();
                    if ("selectById".equals(name) || "selectByIdForUpdate".equals(name))
                        return fixture.delay.getId() != null && Long.valueOf(11L).equals(args[0]) ? fixture.delay : null;
                    if ("selectPendingByOrderId".equals(name))
                        return "1".equals(fixture.order.getDelayPendingFlag()) && fixture.delay.getId() != null
                                ? fixture.delay : null;
                    if ("selectLatestByOrderId".equals(name)) return fixture.delay.getId() == null ? null : fixture.delay;
                    if ("selectList".equals(name)) return Collections.emptyList();
                    if ("insert".equals(name))
                    {
                        WorkOrderDelayRequest value = (WorkOrderDelayRequest) args[0];
                        value.setId(11L);
                        fixture.delay = value;
                        return 1;
                    }
                    if ("bindAttachments".equals(name))
                    {
                        fixture.boundAttachmentIds = new ArrayList<Long>((List<Long>) args[3]);
                        return fixture.boundAttachmentIds.size();
                    }
                    if ("markDelayPending".equals(name))
                    {
                        fixture.order.setDelayPendingFlag("1");
                        fixture.order.setVersion(fixture.order.getVersion() + 1);
                        return 1;
                    }
                    if ("applyApprovedDelay".equals(name))
                    {
                        fixture.order.setExtensionDeadline((Date) args[2]);
                        fixture.order.setDelayPendingFlag("0");
                        fixture.order.setVersion(fixture.order.getVersion() + 1);
                        return 1;
                    }
                    if ("clearDelayPending".equals(name))
                    {
                        fixture.order.setDelayPendingFlag("0");
                        fixture.order.setVersion(fixture.order.getVersion() + 1);
                        return 1;
                    }
                    if ("updateDecision".equals(name))
                    {
                        fixture.delay.setRequestStatus((String) args[1]);
                        fixture.delay.setApproverId((Long) args[2]);
                        fixture.delay.setApproverName((String) args[3]);
                        fixture.delay.setApprovalComment((String) args[4]);
                        fixture.delay.setApprovedAt((Date) args[5]);
                        return 1;
                    }
                    if ("selectAttachmentIdsByRequestId".equals(name)) return fixture.boundAttachmentIds;
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type)
    {
        if (type == Integer.TYPE) return 0;
        if (type == Long.TYPE) return 0L;
        if (type == Boolean.TYPE) return false;
        if (List.class.isAssignableFrom(type)) return Collections.emptyList();
        return null;
    }

    private static class Fixture
    {
        private final WorkOrder order = new WorkOrder();
        private WorkOrderDelayRequest delay = new WorkOrderDelayRequest();
        private WorkOrderActor actor;
        private WorkOrderMapper orderMapper;
        private WorkOrderDelayRequestMapper delayMapper;
        private final RecordingActionLogMapper actionMapper = new RecordingActionLogMapper();
        private WorkOrderDelayService service;
        private int visibilityChecks;
        private List<Long> boundAttachmentIds = Collections.emptyList();
        private final List<WorkOrderActionLog> logs = actionMapper.logs;
    }

    private static class RecordingActionLogMapper implements WorkOrderActionLogMapper
    {
        private final List<WorkOrderActionLog> logs = new ArrayList<WorkOrderActionLog>();

        @Override public int insert(WorkOrderActionLog log)
        {
            logs.add(log);
            return 1;
        }

        @Override public WorkOrderActionLog selectByIdempotency(Long operatorId, String actionType,
                String idempotencyKey)
        {
            for (WorkOrderActionLog log : logs)
            {
                if (operatorId.equals(log.getOperatorId()) && actionType.equals(log.getActionType())
                        && idempotencyKey.equals(log.getIdempotencyKey())) return log;
            }
            return null;
        }

        @Override public List<WorkOrderActionLog> selectByOrderId(Long orderId) { return logs; }
    }
}
