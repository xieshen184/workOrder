package com.ruoyi.workorder.domain.model;

import java.util.Date;

/**
 * 工单通知事件快照。
 *
 * <p>调用方只描述已经发生的业务事实；收件人选择、模板渲染、渠道拆分和幂等任务生成
 * 都由通知模块隐藏。{@code occurrenceKey} 必须能唯一标识一次业务事实。</p>
 */
public class WorkOrderNotificationEvent
{
    private final String eventCode;
    private final String occurrenceKey;
    private final Long orderId;
    private final String orderNo;
    private final String title;
    private final Long applicantId;
    private final Long assigneeId;
    private final Long targetUserId;
    private final String operatorName;
    private final String reason;
    private final Date deadline;
    private final Date occurredAt;

    public WorkOrderNotificationEvent(String eventCode, String occurrenceKey, WorkOrder order,
            Long targetUserId, String operatorName, String reason, Date deadline, Date occurredAt)
    {
        this.eventCode = eventCode;
        this.occurrenceKey = occurrenceKey;
        this.orderId = order.getId();
        this.orderNo = order.getOrderNo();
        this.title = order.getTitle();
        this.applicantId = order.getApplicantId();
        this.assigneeId = order.getCurrentAssigneeId();
        this.targetUserId = targetUserId;
        this.operatorName = operatorName;
        this.reason = reason;
        this.deadline = deadline;
        this.occurredAt = occurredAt;
    }

    public String getEventCode() { return eventCode; }
    public String getOccurrenceKey() { return occurrenceKey; }
    public Long getOrderId() { return orderId; }
    public String getOrderNo() { return orderNo; }
    public String getTitle() { return title; }
    public Long getApplicantId() { return applicantId; }
    public Long getAssigneeId() { return assigneeId; }
    public Long getTargetUserId() { return targetUserId; }
    public String getOperatorName() { return operatorName; }
    public String getReason() { return reason; }
    public Date getDeadline() { return deadline; }
    public Date getOccurredAt() { return occurredAt; }
}
