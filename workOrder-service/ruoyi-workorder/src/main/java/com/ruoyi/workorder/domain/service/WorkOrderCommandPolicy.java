package com.ruoyi.workorder.domain.service;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderCommand;
import com.ruoyi.workorder.domain.exception.IllegalWorkOrderTransitionException;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;

/** Pure command policy: authorization, ownership, version and state transition rules. */
public class WorkOrderCommandPolicy
{
    private final WorkOrderStateMachine stateMachine = new WorkOrderStateMachine();

    public WorkOrderStatus target(WorkOrder order, WorkOrderCommand command, WorkOrderActor actor)
    {
        if (command.getVersion() == null || !command.getVersion().equals(order.getVersion()))
            throw new ServiceException("WO_VERSION_CONFLICT: 工单版本已变化，请刷新后重试", HttpStatus.CONFLICT);
        assertActor(order, command.getAction(), actor);
        try
        {
            return stateMachine.transition(WorkOrderStatus.fromCode(order.getStatus()), command.getAction());
        }
        catch (IllegalWorkOrderTransitionException error)
        {
            throw new ServiceException("WO_STATE_CONFLICT: 当前状态不允许执行该动作", HttpStatus.CONFLICT);
        }
    }

    /** Checks permission and ownership without exposing state-dependent outcomes. */
    public void assertActor(WorkOrder order, WorkOrderAction action, WorkOrderActor actor)
    {
        String permission = permission(action);
        if (!actor.hasPermission(permission))
            throw new ServiceException("WO_FORBIDDEN: 无权执行该工单动作", HttpStatus.FORBIDDEN);
        if (isEngineerAction(action)
                && !actor.getUserId().equals(order.getCurrentAssigneeId()))
            throw new ServiceException("WO_FORBIDDEN: 仅当前处理人可执行该动作", HttpStatus.FORBIDDEN);
        assertReporterOrDispatcherOwnership(order, action, actor);
    }

    private boolean isEngineerAction(WorkOrderAction action)
    {
        return action == WorkOrderAction.ACCEPT || action == WorkOrderAction.ARRIVE
                || action == WorkOrderAction.ASSESS || action == WorkOrderAction.PROGRESS
                || action == WorkOrderAction.FINISH;
    }

    private void assertReporterOrDispatcherOwnership(WorkOrder order, WorkOrderAction action,
            WorkOrderActor actor)
    {
        boolean applicant = actor.getUserId().equals(order.getApplicantId());
        boolean dispatcher = actor.hasPermission("workorder:order:list");
        if ((action == WorkOrderAction.CANCEL || action == WorkOrderAction.RETURN)
                && !applicant && !dispatcher)
            throw new ServiceException("WO_FORBIDDEN: 仅创建人或调度管理员可执行该动作", HttpStatus.FORBIDDEN);
        if ((action == WorkOrderAction.CONFIRM || action == WorkOrderAction.EVALUATE) && !applicant)
            throw new ServiceException("WO_FORBIDDEN: 仅工单创建人可执行该动作", HttpStatus.FORBIDDEN);
    }

    private String permission(WorkOrderAction action)
    {
        if (action == WorkOrderAction.ASSIGN) return "workorder:order:assign";
        if (action == WorkOrderAction.REASSIGN) return "workorder:order:reassign";
        if (action == WorkOrderAction.ACCEPT) return "workorder:order:accept";
        if (action == WorkOrderAction.ARRIVE) return "workorder:order:arrive";
        if (action == WorkOrderAction.ASSESS) return "workorder:order:assess";
        if (action == WorkOrderAction.PROGRESS) return "workorder:order:progress";
        if (action == WorkOrderAction.FINISH) return "workorder:order:finish";
        if (action == WorkOrderAction.CANCEL) return "workorder:order:cancel";
        if (action == WorkOrderAction.CONFIRM) return "workorder:order:confirm";
        if (action == WorkOrderAction.RETURN) return "workorder:order:return";
        if (action == WorkOrderAction.EVALUATE) return "workorder:evaluation:add";
        throw new ServiceException("不支持的工单动作", HttpStatus.BAD_REQUEST);
    }
}
