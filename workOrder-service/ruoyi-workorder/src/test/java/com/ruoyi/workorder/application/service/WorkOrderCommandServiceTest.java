package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderCommand;
import com.ruoyi.workorder.application.model.WorkOrderCommandResult;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import org.junit.Test;

public class WorkOrderCommandServiceTest
{
    @Test
    public void shouldCheckVisibilityBeforeLookingUpIdempotentReplay()
    {
        List<String> calls = new ArrayList<String>();
        StubActionLogMapper logs = new StubActionLogMapper(calls, null);
        StubTransaction transaction = new StubTransaction(calls);
        WorkOrderQueryService visibility = new WorkOrderQueryService()
        {
            @Override public void assertVisible(Long id, WorkOrderActor actor)
            {
                calls.add("visibility");
                throw new ServiceException("forbidden", HttpStatus.FORBIDDEN);
            }
        };
        WorkOrderCommandService service = new WorkOrderCommandService(logs, transaction, visibility);

        try
        {
            service.execute(WorkOrderCommand.simple(1L, WorkOrderAction.ACCEPT, "accept-key", 1), actor());
            fail("expected ServiceException");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), error.getCode());
        }
        assertEquals(Arrays.asList("visibility"), calls);
    }

    @Test
    public void shouldReplayOnlyAfterVisibilitySucceeds()
    {
        List<String> calls = new ArrayList<String>();
        WorkOrderCommand command = WorkOrderCommand.simple(1L, WorkOrderAction.ACCEPT, "accept-key", 1);
        WorkOrderActionLog replay = new WorkOrderActionLog();
        replay.setOrderId(1L); replay.setToStatus("ACCEPTED");
        JSONObject ext = new JSONObject();
        ext.put("orderNo", "WO-1"); ext.put("version", 2);
        ext.put("requestFingerprint", WorkOrderCommandService.hash(command));
        replay.setExtJson(ext.toJSONString());
        StubActionLogMapper logs = new StubActionLogMapper(calls, replay);
        StubTransaction transaction = new StubTransaction(calls);
        WorkOrderQueryService visibility = new WorkOrderQueryService()
        {
            @Override public void assertVisible(Long id, WorkOrderActor actor) { calls.add("visibility"); }
        };

        WorkOrderCommandResult result = new WorkOrderCommandService(logs, transaction, visibility)
                .execute(command, actor());

        assertEquals(Arrays.asList("visibility", "idempotency"), calls);
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals(true, result.isIdempotentReplay());
    }

    @Test
    public void reusedKeyWithDifferentRequestShouldConflictBeforeTransaction()
    {
        List<String> calls = new ArrayList<String>();
        WorkOrderCommand command = WorkOrderCommand.simple(1L, WorkOrderAction.ACCEPT, "shared-key", 1);
        WorkOrderCommand different = WorkOrderCommand.simple(2L, WorkOrderAction.ACCEPT, "shared-key", 1);
        WorkOrderActionLog replay = new WorkOrderActionLog();
        replay.setOrderId(2L); replay.setToStatus("ACCEPTED");
        JSONObject ext = new JSONObject();
        ext.put("orderNo", "WO-2"); ext.put("version", 2);
        ext.put("requestFingerprint", WorkOrderCommandService.hash(different));
        replay.setExtJson(ext.toJSONString());
        WorkOrderQueryService visibility = new WorkOrderQueryService()
        {
            @Override public void assertVisible(Long id, WorkOrderActor actor) { calls.add("visibility"); }
        };

        try
        {
            new WorkOrderCommandService(new StubActionLogMapper(calls, replay), new StubTransaction(calls), visibility)
                    .execute(command, actor());
            fail("expected ServiceException");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), error.getCode());
        }
        assertEquals(Arrays.asList("visibility", "idempotency"), calls);
    }

    private WorkOrderActor actor()
    {
        return new WorkOrderActor(9L, "engineer", "Engineer", null, 1L, "Dept",
                new HashSet<String>(Arrays.asList("workorder:order:accept")));
    }

    private static class StubActionLogMapper implements WorkOrderActionLogMapper
    {
        private final List<String> calls;
        private final WorkOrderActionLog replay;
        StubActionLogMapper(List<String> calls, WorkOrderActionLog replay)
        { this.calls = calls; this.replay = replay; }
        @Override public int insert(WorkOrderActionLog log) { return 1; }
        @Override public WorkOrderActionLog selectByIdempotency(Long operatorId, String actionType, String key)
        { calls.add("idempotency"); return replay; }
        @Override public List<WorkOrderActionLog> selectByOrderId(Long orderId) { return null; }
    }

    private static class StubTransaction extends WorkOrderCommandTransaction
    {
        private final List<String> calls;
        StubTransaction(List<String> calls)
        { super(null, null, null, null, null, null, null, null); this.calls = calls; }
        @Override public WorkOrderCommandResult execute(WorkOrderCommand command, WorkOrderActor actor)
        { calls.add("transaction"); return null; }
    }
}
