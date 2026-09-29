package com.ruoyi.workorder.domain.model;

import java.util.Date;

/** 移动端站内消息只读投影。 */
public class WorkOrderNotificationMessage
{
    private Long id;
    private Long taskId;
    private Long recipientId;
    private String category;
    private String eventCode;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private String orderNo;
    private String routePath;
    private String routeParams;
    private String readFlag;
    private Date readAt;
    private Date createTime;

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getTaskId() { return taskId; } public void setTaskId(Long v) { taskId = v; }
    public Long getRecipientId() { return recipientId; } public void setRecipientId(Long v) { recipientId = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public String getEventCode() { return eventCode; } public void setEventCode(String v) { eventCode = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getContent() { return content; } public void setContent(String v) { content = v; }
    public String getBusinessType() { return businessType; } public void setBusinessType(String v) { businessType = v; }
    public Long getBusinessId() { return businessId; } public void setBusinessId(Long v) { businessId = v; }
    public String getOrderNo() { return orderNo; } public void setOrderNo(String v) { orderNo = v; }
    public String getRoutePath() { return routePath; } public void setRoutePath(String v) { routePath = v; }
    public String getRouteParams() { return routeParams; } public void setRouteParams(String v) { routeParams = v; }
    public String getReadFlag() { return readFlag; } public void setReadFlag(String v) { readFlag = v; }
    public Date getReadAt() { return readAt; } public void setReadAt(Date v) { readAt = v; }
    public Date getCreateTime() { return createTime; } public void setCreateTime(Date v) { createTime = v; }
}
