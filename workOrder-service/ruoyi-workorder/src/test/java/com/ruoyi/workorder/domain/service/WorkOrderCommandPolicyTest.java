package com.ruoyi.workorder.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderCommand;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;

public class WorkOrderCommandPolicyTest
{
    private final WorkOrderCommandPolicy policy = new WorkOrderCommandPolicy();

    @Test
    public void shouldAllowLegalDispatchAndEngineerChain()
    {
        WorkOrder order = order(WorkOrderStatus.WAIT_ASSIGN, 0, null);
        assertTarget(order, WorkOrderCommand.assignment(1L, WorkOrderAction.ASSIGN, "assign-key", 0, 9L, null),
                actor(2L, "workorder:order:assign"), WorkOrderStatus.WAIT_ACCEPT);
        order.setStatus("WAIT_ACCEPT"); order.setVersion(1); order.setCurrentAssigneeId(9L);
        assertTarget(order, WorkOrderCommand.simple(1L, WorkOrderAction.ACCEPT, "accept-key", 1),
                actor(9L, "workorder:order:accept"), WorkOrderStatus.ACCEPTED);
        order.setStatus("ACCEPTED"); order.setVersion(2);
        assertTarget(order, WorkOrderCommand.process(1L, WorkOrderAction.ARRIVE, "arrival-key", 2, "到场", Arrays.asList(1L)),
                actor(9L, "workorder:order:arrive"), WorkOrderStatus.PROCESSING);
        order.setStatus("PROCESSING"); order.setVersion(3);
        assertTarget(order, WorkOrderCommand.process(1L, WorkOrderAction.FINISH, "finish-key", 3, "完成", Arrays.asList(2L)),
                actor(9L, "workorder:order:finish"), WorkOrderStatus.WAIT_CONFIRM);
    }

    @Test
    public void shouldRejectWrongAssignee()
    {
        WorkOrder order = order(WorkOrderStatus.WAIT_ACCEPT, 1, 9L);
        expect(HttpStatus.FORBIDDEN, order, WorkOrderCommand.simple(1L, WorkOrderAction.ACCEPT, "accept-key", 1),
                actor(10L, "workorder:order:accept"));
    }

    @Test
    public void shouldRejectStaleVersionBeforeTransition()
    {
        WorkOrder order = order(WorkOrderStatus.WAIT_ACCEPT, 2, 9L);
        expect(HttpStatus.CONFLICT, order, WorkOrderCommand.simple(1L, WorkOrderAction.ACCEPT, "accept-key", 1),
                actor(9L, "workorder:order:accept"));
    }

    @Test
    public void shouldEnforceReporterAndDispatcherOwnershipForB03Actions()
    {
        WorkOrder cancel = order(WorkOrderStatus.WAIT_ASSIGN, 0, null);
        assertTarget(cancel, WorkOrderCommand.reasoned(1L, WorkOrderAction.CANCEL, "cancel-key", 0, null),
                actor(1L, "workorder:order:cancel"), WorkOrderStatus.CANCELLED);
        expect(HttpStatus.FORBIDDEN, cancel,
                WorkOrderCommand.reasoned(1L, WorkOrderAction.CANCEL, "cancel-key-2", 0, null),
                actor(2L, "workorder:order:cancel"));
        assertTarget(cancel, WorkOrderCommand.reasoned(1L, WorkOrderAction.CANCEL, "cancel-key-3", 0, null),
                actor(2L, "workorder:order:list", "workorder:order:cancel"), WorkOrderStatus.CANCELLED);

        WorkOrder confirm = order(WorkOrderStatus.WAIT_CONFIRM, 4, 9L);
        assertTarget(confirm, WorkOrderCommand.simple(1L, WorkOrderAction.CONFIRM, "confirm-key", 4),
                actor(1L, "workorder:order:confirm"), WorkOrderStatus.COMPLETED);
        expect(HttpStatus.FORBIDDEN, confirm,
                WorkOrderCommand.simple(1L, WorkOrderAction.CONFIRM, "confirm-key-2", 4),
                actor(2L, "workorder:order:list", "workorder:order:confirm"));

        assertTarget(confirm, WorkOrderCommand.reasoned(1L, WorkOrderAction.RETURN, "return-key", 4, "需返工"),
                actor(2L, "workorder:order:list", "workorder:order:return"), WorkOrderStatus.PROCESSING);

        WorkOrder completed = order(WorkOrderStatus.COMPLETED, 5, 9L);
        assertTarget(completed, WorkOrderCommand.evaluation(1L, "evaluate-key", 5, 5, null, 4, 5, "很好"),
                actor(1L, "workorder:evaluation:add"), WorkOrderStatus.CLOSED);
        expect(HttpStatus.FORBIDDEN, completed,
                WorkOrderCommand.evaluation(1L, "evaluate-key-2", 5, 5, null, null, null, null),
                actor(2L, "workorder:evaluation:add"));
    }

    private void assertTarget(WorkOrder order, WorkOrderCommand command, WorkOrderActor actor, WorkOrderStatus expected)
    { assertEquals(expected, policy.target(order, command, actor)); }
    private void expect(int code, WorkOrder order, WorkOrderCommand command, WorkOrderActor actor)
    {
        try { policy.target(order, command, actor); fail("expected ServiceException"); }
        catch (ServiceException error) { assertEquals(Integer.valueOf(code), error.getCode()); }
    }
    private WorkOrder order(WorkOrderStatus status, int version, Long assignee)
    { WorkOrder value = new WorkOrder(); value.setId(1L); value.setApplicantId(1L); value.setStatus(status.getCode()); value.setVersion(version); value.setCurrentAssigneeId(assignee); return value; }
    private WorkOrderActor actor(Long id, String... permissions)
    { return new WorkOrderActor(id, "user", "User", "13800000000", 1L, "Dept", new HashSet<String>(Arrays.asList(permissions))); }
}
