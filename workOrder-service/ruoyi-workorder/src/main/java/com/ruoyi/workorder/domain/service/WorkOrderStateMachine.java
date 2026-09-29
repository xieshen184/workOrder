package com.ruoyi.workorder.domain.service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.ruoyi.workorder.domain.exception.IllegalWorkOrderTransitionException;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;

/**
 * 工单主状态机。
 *
 * <p>该模块只判断状态动作是否合法并计算目标状态。人员归属、角色权限、
 * 附件要求和 SLA 等上下文条件由命令处理层校验。</p>
 */
public final class WorkOrderStateMachine
{
    private static final Map<WorkOrderStatus, Map<WorkOrderAction, WorkOrderStatus>> TRANSITIONS = buildTransitions();

    public WorkOrderStatus transition(WorkOrderStatus sourceStatus, WorkOrderAction action)
    {
        Objects.requireNonNull(sourceStatus, "sourceStatus 不能为空");
        Objects.requireNonNull(action, "action 不能为空");

        Map<WorkOrderAction, WorkOrderStatus> actions = TRANSITIONS.get(sourceStatus);
        WorkOrderStatus targetStatus = actions == null ? null : actions.get(action);
        if (targetStatus == null)
        {
            throw new IllegalWorkOrderTransitionException(sourceStatus, action);
        }
        return targetStatus;
    }

    public boolean canApply(WorkOrderStatus sourceStatus, WorkOrderAction action)
    {
        if (sourceStatus == null || action == null)
        {
            return false;
        }
        Map<WorkOrderAction, WorkOrderStatus> actions = TRANSITIONS.get(sourceStatus);
        return actions != null && actions.containsKey(action);
    }

    public Set<WorkOrderAction> allowedActions(WorkOrderStatus sourceStatus)
    {
        Objects.requireNonNull(sourceStatus, "sourceStatus 不能为空");
        Map<WorkOrderAction, WorkOrderStatus> actions = TRANSITIONS.get(sourceStatus);
        if (actions == null || actions.isEmpty())
        {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(actions.keySet()));
    }

    private static Map<WorkOrderStatus, Map<WorkOrderAction, WorkOrderStatus>> buildTransitions()
    {
        EnumMap<WorkOrderStatus, Map<WorkOrderAction, WorkOrderStatus>> transitions =
                new EnumMap<WorkOrderStatus, Map<WorkOrderAction, WorkOrderStatus>>(WorkOrderStatus.class);

        register(transitions, WorkOrderStatus.DRAFT, WorkOrderAction.SUBMIT, WorkOrderStatus.WAIT_ASSIGN);
        register(transitions, WorkOrderStatus.WAIT_ASSIGN, WorkOrderAction.ASSIGN, WorkOrderStatus.WAIT_ACCEPT);
        register(transitions, WorkOrderStatus.WAIT_ASSIGN, WorkOrderAction.CANCEL, WorkOrderStatus.CANCELLED);
        register(transitions, WorkOrderStatus.WAIT_ACCEPT, WorkOrderAction.ACCEPT, WorkOrderStatus.ACCEPTED);
        register(transitions, WorkOrderStatus.WAIT_ACCEPT, WorkOrderAction.REASSIGN, WorkOrderStatus.WAIT_ACCEPT);
        register(transitions, WorkOrderStatus.ACCEPTED, WorkOrderAction.ARRIVE, WorkOrderStatus.PROCESSING);
        register(transitions, WorkOrderStatus.ACCEPTED, WorkOrderAction.REASSIGN, WorkOrderStatus.WAIT_ACCEPT);
        register(transitions, WorkOrderStatus.PROCESSING, WorkOrderAction.REASSIGN, WorkOrderStatus.WAIT_ACCEPT);
        register(transitions, WorkOrderStatus.PROCESSING, WorkOrderAction.ASSESS, WorkOrderStatus.PROCESSING);
        register(transitions, WorkOrderStatus.PROCESSING, WorkOrderAction.PROGRESS, WorkOrderStatus.PROCESSING);
        register(transitions, WorkOrderStatus.PROCESSING, WorkOrderAction.REQUEST_DELAY, WorkOrderStatus.PROCESSING);
        register(transitions, WorkOrderStatus.PROCESSING, WorkOrderAction.APPROVE_DELAY, WorkOrderStatus.PROCESSING);
        register(transitions, WorkOrderStatus.PROCESSING, WorkOrderAction.REJECT_DELAY, WorkOrderStatus.PROCESSING);
        register(transitions, WorkOrderStatus.PROCESSING, WorkOrderAction.FINISH, WorkOrderStatus.WAIT_CONFIRM);
        register(transitions, WorkOrderStatus.WAIT_CONFIRM, WorkOrderAction.CONFIRM, WorkOrderStatus.COMPLETED);
        register(transitions, WorkOrderStatus.WAIT_CONFIRM, WorkOrderAction.RETURN, WorkOrderStatus.PROCESSING);
        register(transitions, WorkOrderStatus.COMPLETED, WorkOrderAction.EVALUATE, WorkOrderStatus.CLOSED);
        register(transitions, WorkOrderStatus.COMPLETED, WorkOrderAction.AUTO_CLOSE, WorkOrderStatus.CLOSED);

        for (Map.Entry<WorkOrderStatus, Map<WorkOrderAction, WorkOrderStatus>> entry : transitions.entrySet())
        {
            entry.setValue(Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(transitions);
    }

    private static void register(Map<WorkOrderStatus, Map<WorkOrderAction, WorkOrderStatus>> transitions,
            WorkOrderStatus sourceStatus, WorkOrderAction action, WorkOrderStatus targetStatus)
    {
        Map<WorkOrderAction, WorkOrderStatus> actions = transitions.get(sourceStatus);
        if (actions == null)
        {
            actions = new EnumMap<WorkOrderAction, WorkOrderStatus>(WorkOrderAction.class);
            transitions.put(sourceStatus, actions);
        }
        actions.put(action, targetStatus);
    }
}
