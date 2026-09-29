package com.ruoyi.workorder.domain.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.domain.model.WorkOrder;

public class WorkOrderAllowedActionResolverTest
{
    private final WorkOrderAllowedActionResolver resolver = new WorkOrderAllowedActionResolver();

    @Test
    public void reporterCanCancelOwnWaitingOrderButCannotAssign()
    {
        WorkOrder order = order("WAIT_ASSIGN", 7L, null);
        List<String> actions = resolver.resolve(order, actor(7L));
        assertTrue(actions.contains("CANCEL"));
        assertFalse(actions.contains("ASSIGN"));
    }

    @Test
    public void dispatcherCanAssignWaitingOrder()
    {
        WorkOrder order = order("WAIT_ASSIGN", 7L, null);
        WorkOrderActor dispatcher = new WorkOrderActor(11L, "dispatcher", "Dispatcher", "13800000000",
                2L, "Dept", new HashSet<String>(Arrays.asList("workorder:order:list", "workorder:order:assign")));
        assertTrue(resolver.resolve(order, dispatcher).contains("ASSIGN"));
    }

    @Test
    public void onlyCurrentAssigneeCanAccept()
    {
        WorkOrder order = order("WAIT_ACCEPT", 7L, 12L);
        assertTrue(resolver.resolve(order, actor(12L)).contains("ACCEPT"));
        assertFalse(resolver.resolve(order, actor(13L)).contains("ACCEPT"));
    }

    @Test
    public void currentAssigneeCanOpenPendingDelayForViewingButCannotWhenSlaDisallowsIt()
    {
        WorkOrder order = order("PROCESSING", 7L, 12L);
        order.setSlaAllowExtension("1");
        order.setDelayPendingFlag("1");
        WorkOrderActor engineer = new WorkOrderActor(12L, "engineer", "Engineer", null,
                1L, "Dept", new HashSet<String>(Arrays.asList("workorder:delay:add")));
        assertTrue(resolver.resolve(order, engineer).contains("REQUEST_DELAY"));

        order.setSlaAllowExtension("0");
        assertFalse(resolver.resolve(order, engineer).contains("REQUEST_DELAY"));
    }

    private WorkOrder order(String status, Long applicantId, Long assigneeId)
    {
        WorkOrder order = new WorkOrder();
        order.setStatus(status);
        order.setApplicantId(applicantId);
        order.setCurrentAssigneeId(assigneeId);
        return order;
    }

    private WorkOrderActor actor(Long id)
    {
        return new WorkOrderActor(id, "user", "User", "13800000000", 1L, "Dept",
                new HashSet<String>(Arrays.asList("workorder:order:cancel", "workorder:order:accept")));
    }
}
