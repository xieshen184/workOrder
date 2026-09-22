package com.ruoyi.workorder.domain.model;

import java.util.Date;

public class WorkOrderActionLog
{
    private Long id;
    private Long orderId;
    private String actionType;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String actionContent;
    private String extJson;
    private String idempotencyKey;
    private Date actionTime;

    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long v) { orderId=v; }
    public String getActionType() { return actionType; } public void setActionType(String v) { actionType=v; }
    public String getFromStatus() { return fromStatus; } public void setFromStatus(String v) { fromStatus=v; }
    public String getToStatus() { return toStatus; } public void setToStatus(String v) { toStatus=v; }
    public Long getOperatorId() { return operatorId; } public void setOperatorId(Long v) { operatorId=v; }
    public String getOperatorName() { return operatorName; } public void setOperatorName(String v) { operatorName=v; }
    public String getOperatorRole() { return operatorRole; } public void setOperatorRole(String v) { operatorRole=v; }
    public String getActionContent() { return actionContent; } public void setActionContent(String v) { actionContent=v; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false)
    public String getExtJson() { return extJson; } public void setExtJson(String v) { extJson=v; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false)
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String v) { idempotencyKey=v; }
    public Date getActionTime() { return actionTime; } public void setActionTime(Date v) { actionTime=v; }
}
