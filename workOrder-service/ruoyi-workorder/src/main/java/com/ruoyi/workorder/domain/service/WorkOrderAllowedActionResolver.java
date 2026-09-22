package com.ruoyi.workorder.domain.service;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;

public class WorkOrderAllowedActionResolver
{
    private final WorkOrderStateMachine stateMachine = new WorkOrderStateMachine();

    public List<String> resolve(WorkOrder order, WorkOrderActor actor)
    {
        List<String> result = new ArrayList<String>();
        WorkOrderStatus status = WorkOrderStatus.fromCode(order.getStatus());
        boolean applicant = actor.getUserId().equals(order.getApplicantId());
        boolean assignee = actor.getUserId().equals(order.getCurrentAssigneeId());
        boolean dispatcher = actor.hasPermission("workorder:order:list");

        add(result, status, WorkOrderAction.CANCEL, applicant || dispatcher, actor, "workorder:order:cancel");
        add(result, status, WorkOrderAction.ASSIGN, dispatcher, actor, "workorder:order:assign");
        add(result, status, WorkOrderAction.REASSIGN, dispatcher, actor, "workorder:order:reassign");
        add(result, status, WorkOrderAction.ACCEPT, assignee, actor, "workorder:order:accept");
        add(result, status, WorkOrderAction.ARRIVE, assignee, actor, "workorder:order:arrive");
        add(result, status, WorkOrderAction.ASSESS, assignee, actor, "workorder:order:assess");
        add(result, status, WorkOrderAction.PROGRESS, assignee, actor, "workorder:order:progress");
        add(result, status, WorkOrderAction.FINISH, assignee, actor, "workorder:order:finish");
        add(result, status, WorkOrderAction.CONFIRM, applicant, actor, "workorder:order:confirm");
        add(result, status, WorkOrderAction.RETURN, applicant || dispatcher, actor, "workorder:order:return");
        add(result, status, WorkOrderAction.EVALUATE, applicant, actor, "workorder:evaluation:add");
        return result;
    }

    private void add(List<String> target, WorkOrderStatus status, WorkOrderAction action, boolean ownerMatch,
            WorkOrderActor actor, String permission)
    {
        if (ownerMatch && actor.hasPermission(permission) && stateMachine.canApply(status, action))
            target.add(action.name());
    }
}
