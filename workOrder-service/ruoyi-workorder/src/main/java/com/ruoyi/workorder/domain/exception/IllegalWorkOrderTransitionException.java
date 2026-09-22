package com.ruoyi.workorder.domain.exception;

import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;

/**
 * 工单动作不允许从当前状态执行。
 */
public class IllegalWorkOrderTransitionException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final WorkOrderStatus sourceStatus;
    private final WorkOrderAction action;

    public IllegalWorkOrderTransitionException(WorkOrderStatus sourceStatus, WorkOrderAction action)
    {
        super("工单状态 " + sourceStatus.getCode() + " 不允许执行动作 " + action.name());
        this.sourceStatus = sourceStatus;
        this.action = action;
    }

    public WorkOrderStatus getSourceStatus()
    {
        return sourceStatus;
    }

    public WorkOrderAction getAction()
    {
        return action;
    }
}
