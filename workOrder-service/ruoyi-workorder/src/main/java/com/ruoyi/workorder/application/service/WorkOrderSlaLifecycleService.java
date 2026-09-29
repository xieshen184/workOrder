package com.ruoyi.workorder.application.service;

import java.util.Date;
import java.util.List;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.workorder.application.model.WorkOrderSlaScanResult;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationEvent;
import com.ruoyi.workorder.domain.service.WorkOrderStateMachine;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SLA 生命周期深模块。
 *
 * <p>调用者只需提供当前时间；阶段截止选择、预警窗口、超期累计、审计幂等和自动关闭并发保护均隐藏在实现内。</p>
 */
@Service
public class WorkOrderSlaLifecycleService
{
    private static final Long SYSTEM_OPERATOR_ID = 0L;
    private final WorkOrderMapper orderMapper;
    private final WorkOrderActionLogMapper actionLogMapper;
    private final WorkOrderStateMachine stateMachine = new WorkOrderStateMachine();
    @Autowired private WorkOrderNotificationOutboxService notificationOutbox;

    public WorkOrderSlaLifecycleService(WorkOrderMapper orderMapper, WorkOrderActionLogMapper actionLogMapper)
    {
        this.orderMapper = orderMapper;
        this.actionLogMapper = actionLogMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrderSlaScanResult refresh(Date now)
    {
        List<WorkOrder> orders = orderMapper.selectSlaOpenOrders();
        int changed = 0;
        for (WorkOrder order : orders)
        {
            Flags next = flags(order, now);
            boolean flagChanged = flagsChanged(order, next);
            int updated = orderMapper.updateSlaFlags(order.getId(), order.getStatus(), next.warning, next.overdue,
                    next.responseOverdue, next.arrivalOverdue, next.finishOverdue, now);
            if (updated == 1) changed++;
            else if (flagChanged) continue;
            else
            {
                if (!"1".equals(next.warning)) continue;
                // 标志无需更新时仍可能需要补写“新阶段、相同预警值”的日志；重读主状态和截止时间，
                // 避免并发业务动作已切换阶段后，根据扫描快照发送过期通知。
                WorkOrder current = orderMapper.selectById(order.getId());
                if (current == null || !order.getStatus().equals(current.getStatus())
                        || !sameTime(next.deadline, activeDeadline(current))) continue;
            }
            // 预警按“当前阶段截止时间”幂等。即使上一阶段 warning_flag 仍为 1，
            // 新阶段拥有不同截止时间时也必须生成一次新预警；日志键负责抑制每分钟重复扫描。
            if ("1".equals(next.warning))
                writeSystemLog(order, "SLA_WARNING", "进入SLA预警窗口", next.deadline, now);
            // 总 overdue_flag 会跨阶段累计，因此必须比较当前阶段的细分超期标志，避免响应超期后漏掉完成超期。
            if (becameCurrentStageOverdue(order, next))
                writeSystemLog(order, "SLA_OVERDUE", "工单已超过SLA截止时间", next.deadline, now);
        }
        return new WorkOrderSlaScanResult(orders.size(), changed, 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrderSlaScanResult autoClose(Date now, int evaluationWindowDays)
    {
        if (evaluationWindowDays < 1) throw new IllegalArgumentException("评价窗口天数必须大于0");
        Date cutoff = new Date(now.getTime() - evaluationWindowDays * 86_400_000L);
        List<WorkOrder> orders = orderMapper.selectAutoCloseCandidates(cutoff);
        int closed = 0;
        for (WorkOrder order : orders)
        {
            if (!stateMachine.canApply(WorkOrderStatus.fromCode(order.getStatus()), WorkOrderAction.AUTO_CLOSE))
                continue;
            if (orderMapper.autoClose(order.getId(), order.getVersion(), now) != 1) continue;
            writeSystemLog(order, WorkOrderAction.AUTO_CLOSE.name(),
                    "确认完成后超过" + evaluationWindowDays + "天未评价，系统自动关闭", order.getConfirmedAt(), now);
            closed++;
        }
        return new WorkOrderSlaScanResult(orders.size(), 0, closed);
    }

    private Flags flags(WorkOrder order, Date now)
    {
        String response = flag(order.getResponseOverdue());
        String arrival = flag(order.getArrivalOverdue());
        String finish = flag(order.getFinishOverdue());
        Date deadline = activeDeadline(order);
        boolean overdueNow = deadline != null && now.after(deadline);
        if (overdueNow)
        {
            if ("WAIT_ASSIGN".equals(order.getStatus()) || "WAIT_ACCEPT".equals(order.getStatus())) response = "1";
            else if ("ACCEPTED".equals(order.getStatus())) arrival = "1";
            else if ("PROCESSING".equals(order.getStatus())) finish = "1";
        }
        long warningMillis = Math.max(0, value(order.getSlaReminderBeforeMin(), 15)) * 60_000L;
        boolean warning = deadline != null && !overdueNow && deadline.getTime() - now.getTime() <= warningMillis;
        String overdue = "1".equals(response) || "1".equals(arrival) || "1".equals(finish) ? "1" : "0";
        return new Flags(warning ? "1" : "0", overdue, response, arrival, finish, deadline);
    }

    private Date activeDeadline(WorkOrder order)
    {
        if ("WAIT_ASSIGN".equals(order.getStatus()) || "WAIT_ACCEPT".equals(order.getStatus()))
            return order.getResponseDeadline();
        if ("ACCEPTED".equals(order.getStatus())) return order.getArrivalDeadline();
        if ("PROCESSING".equals(order.getStatus()))
            return order.getExtensionDeadline() == null ? order.getFinishDeadline() : order.getExtensionDeadline();
        return null;
    }

    private boolean becameCurrentStageOverdue(WorkOrder order, Flags next)
    {
        if ("WAIT_ASSIGN".equals(order.getStatus()) || "WAIT_ACCEPT".equals(order.getStatus()))
            return !"1".equals(order.getResponseOverdue()) && "1".equals(next.responseOverdue);
        if ("ACCEPTED".equals(order.getStatus()))
            return !"1".equals(order.getArrivalOverdue()) && "1".equals(next.arrivalOverdue);
        if ("PROCESSING".equals(order.getStatus()))
            return !"1".equals(order.getFinishOverdue()) && "1".equals(next.finishOverdue);
        return false;
    }

    private boolean flagsChanged(WorkOrder order, Flags next)
    {
        return !flag(order.getWarningFlag()).equals(next.warning)
                || !flag(order.getOverdueFlag()).equals(next.overdue)
                || !flag(order.getResponseOverdue()).equals(next.responseOverdue)
                || !flag(order.getArrivalOverdue()).equals(next.arrivalOverdue)
                || !flag(order.getFinishOverdue()).equals(next.finishOverdue);
    }

    private void writeSystemLog(WorkOrder order, String action, String content, Date deadline, Date now)
    {
        long marker = deadline == null ? 0L : deadline.getTime();
        String key = action.toLowerCase() + "-" + order.getId() + "-" + slaPhase(order) + "-" + marker;
        if (actionLogMapper.selectByIdempotency(SYSTEM_OPERATOR_ID, action, key) != null) return;
        JSONObject ext = new JSONObject();
        ext.put("orderNo", order.getOrderNo()); ext.put("deadline", deadline); ext.put("version", order.getVersion());
        WorkOrderActionLog log = new WorkOrderActionLog();
        log.setOrderId(order.getId()); log.setActionType(action); log.setFromStatus(order.getStatus());
        log.setToStatus(WorkOrderAction.AUTO_CLOSE.name().equals(action) ? WorkOrderStatus.CLOSED.getCode() : order.getStatus());
        log.setOperatorId(SYSTEM_OPERATOR_ID); log.setOperatorName("系统任务"); log.setOperatorRole("SYSTEM");
        log.setActionContent(content); log.setExtJson(ext.toJSONString()); log.setIdempotencyKey(key); log.setActionTime(now);
        try
        {
            actionLogMapper.insert(log);
            if (notificationOutbox != null)
            {
                notificationOutbox.publish(new WorkOrderNotificationEvent(action, key, order, null,
                        "系统任务", content, deadline, now));
            }
        }
        catch (DuplicateKeyException ignored) { /* 并发任务只保留一条审计记录和一组通知任务。 */ }
    }

    private String flag(String value) { return "1".equals(value) ? "1" : "0"; }
    private int value(Integer value, int fallback) { return value == null ? fallback : value; }
    private boolean sameTime(Date left, Date right)
    {
        return left == null ? right == null : right != null && left.getTime() == right.getTime();
    }
    private String slaPhase(WorkOrder order)
    {
        if ("WAIT_ASSIGN".equals(order.getStatus()) || "WAIT_ACCEPT".equals(order.getStatus())) return "response";
        if ("ACCEPTED".equals(order.getStatus())) return "arrival";
        if ("PROCESSING".equals(order.getStatus())) return "finish";
        return "lifecycle";
    }

    private static class Flags
    {
        private final String warning;
        private final String overdue;
        private final String responseOverdue;
        private final String arrivalOverdue;
        private final String finishOverdue;
        private final Date deadline;

        private Flags(String warning, String overdue, String responseOverdue,
                String arrivalOverdue, String finishOverdue, Date deadline)
        {
            this.warning = warning; this.overdue = overdue; this.responseOverdue = responseOverdue;
            this.arrivalOverdue = arrivalOverdue; this.finishOverdue = finishOverdue; this.deadline = deadline;
        }
    }
}
