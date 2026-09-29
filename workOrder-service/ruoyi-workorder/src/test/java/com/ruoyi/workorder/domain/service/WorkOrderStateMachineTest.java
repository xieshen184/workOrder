package com.ruoyi.workorder.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Set;
import org.junit.Test;
import com.ruoyi.workorder.domain.exception.IllegalWorkOrderTransitionException;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;

public class WorkOrderStateMachineTest
{
    private final WorkOrderStateMachine stateMachine = new WorkOrderStateMachine();

    @Test
    public void shouldApplyMainLifecycleTransitions()
    {
        assertTransition(WorkOrderStatus.DRAFT, WorkOrderAction.SUBMIT, WorkOrderStatus.WAIT_ASSIGN);
        assertTransition(WorkOrderStatus.WAIT_ASSIGN, WorkOrderAction.ASSIGN, WorkOrderStatus.WAIT_ACCEPT);
        assertTransition(WorkOrderStatus.WAIT_ACCEPT, WorkOrderAction.ACCEPT, WorkOrderStatus.ACCEPTED);
        assertTransition(WorkOrderStatus.ACCEPTED, WorkOrderAction.ARRIVE, WorkOrderStatus.PROCESSING);
        assertTransition(WorkOrderStatus.PROCESSING, WorkOrderAction.FINISH, WorkOrderStatus.WAIT_CONFIRM);
        assertTransition(WorkOrderStatus.WAIT_CONFIRM, WorkOrderAction.CONFIRM, WorkOrderStatus.COMPLETED);
        assertTransition(WorkOrderStatus.COMPLETED, WorkOrderAction.EVALUATE, WorkOrderStatus.CLOSED);
    }

    @Test
    public void shouldApplySecondaryTransitions()
    {
        assertTransition(WorkOrderStatus.WAIT_ASSIGN, WorkOrderAction.CANCEL, WorkOrderStatus.CANCELLED);
        assertTransition(WorkOrderStatus.WAIT_ACCEPT, WorkOrderAction.REASSIGN, WorkOrderStatus.WAIT_ACCEPT);
        assertTransition(WorkOrderStatus.ACCEPTED, WorkOrderAction.REASSIGN, WorkOrderStatus.WAIT_ACCEPT);
        assertTransition(WorkOrderStatus.PROCESSING, WorkOrderAction.REASSIGN, WorkOrderStatus.WAIT_ACCEPT);
        assertTransition(WorkOrderStatus.PROCESSING, WorkOrderAction.ASSESS, WorkOrderStatus.PROCESSING);
        assertTransition(WorkOrderStatus.PROCESSING, WorkOrderAction.PROGRESS, WorkOrderStatus.PROCESSING);
        assertTransition(WorkOrderStatus.WAIT_CONFIRM, WorkOrderAction.RETURN, WorkOrderStatus.PROCESSING);
        assertTransition(WorkOrderStatus.PROCESSING, WorkOrderAction.REQUEST_DELAY, WorkOrderStatus.PROCESSING);
        assertTransition(WorkOrderStatus.PROCESSING, WorkOrderAction.APPROVE_DELAY, WorkOrderStatus.PROCESSING);
        assertTransition(WorkOrderStatus.PROCESSING, WorkOrderAction.REJECT_DELAY, WorkOrderStatus.PROCESSING);
        assertTransition(WorkOrderStatus.COMPLETED, WorkOrderAction.AUTO_CLOSE, WorkOrderStatus.CLOSED);
    }

    @Test(expected = IllegalWorkOrderTransitionException.class)
    public void shouldRejectUndefinedTransition()
    {
        stateMachine.transition(WorkOrderStatus.WAIT_ASSIGN, WorkOrderAction.FINISH);
    }

    @Test
    public void shouldExposeAllowedActionsWithoutAllowingMutation()
    {
        Set<WorkOrderAction> actions = stateMachine.allowedActions(WorkOrderStatus.WAIT_CONFIRM);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(WorkOrderAction.CONFIRM));
        assertTrue(actions.contains(WorkOrderAction.RETURN));
        assertFalse(stateMachine.canApply(WorkOrderStatus.WAIT_CONFIRM, WorkOrderAction.FINISH));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void allowedActionsShouldBeImmutable()
    {
        stateMachine.allowedActions(WorkOrderStatus.WAIT_CONFIRM).add(WorkOrderAction.FINISH);
    }

    @Test
    public void shouldParseStatusCodeAndIdentifyTerminalStatus()
    {
        assertEquals(WorkOrderStatus.WAIT_ASSIGN, WorkOrderStatus.fromCode("WAIT_ASSIGN"));
        assertTrue(WorkOrderStatus.CLOSED.isTerminal());
        assertTrue(WorkOrderStatus.CANCELLED.isTerminal());
        assertFalse(WorkOrderStatus.COMPLETED.isTerminal());
    }

    private void assertTransition(WorkOrderStatus source, WorkOrderAction action, WorkOrderStatus target)
    {
        assertTrue(stateMachine.canApply(source, action));
        assertEquals(target, stateMachine.transition(source, action));
    }
}
