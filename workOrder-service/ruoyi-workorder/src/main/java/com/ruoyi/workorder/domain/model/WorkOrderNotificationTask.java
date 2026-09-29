package com.ruoyi.workorder.domain.model;

import java.util.Date;

/** 一次面向单个收件人和单个渠道的可靠发送任务。 */
public class WorkOrderNotificationTask
{
    private Long id;
    private String eventKey;
    private String eventCode;
    private String category;
    private String channel;
    private Long recipientId;
    private String recipientName;
    private String recipientAddress;
    private String businessType;
    private Long businessId;
    private String orderNo;
    private String title;
    private String content;
    private String routePath;
    private String routeParams;
    private String status;
    private Integer retryCount;
    private Integer maxRetry;
    private Date nextRetryAt;
    private String lockedBy;
    private Date lockedAt;
    private String lastError;
    private Date sentAt;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public String getEventKey() { return eventKey; } public void setEventKey(String v) { eventKey = v; }
    public String getEventCode() { return eventCode; } public void setEventCode(String v) { eventCode = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public String getChannel() { return channel; } public void setChannel(String v) { channel = v; }
    public Long getRecipientId() { return recipientId; } public void setRecipientId(Long v) { recipientId = v; }
    public String getRecipientName() { return recipientName; } public void setRecipientName(String v) { recipientName = v; }
    public String getRecipientAddress() { return recipientAddress; } public void setRecipientAddress(String v) { recipientAddress = v; }
    public String getBusinessType() { return businessType; } public void setBusinessType(String v) { businessType = v; }
    public Long getBusinessId() { return businessId; } public void setBusinessId(Long v) { businessId = v; }
    public String getOrderNo() { return orderNo; } public void setOrderNo(String v) { orderNo = v; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getContent() { return content; } public void setContent(String v) { content = v; }
    public String getRoutePath() { return routePath; } public void setRoutePath(String v) { routePath = v; }
    public String getRouteParams() { return routeParams; } public void setRouteParams(String v) { routeParams = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public Integer getRetryCount() { return retryCount; } public void setRetryCount(Integer v) { retryCount = v; }
    public Integer getMaxRetry() { return maxRetry; } public void setMaxRetry(Integer v) { maxRetry = v; }
    public Date getNextRetryAt() { return nextRetryAt; } public void setNextRetryAt(Date v) { nextRetryAt = v; }
    public String getLockedBy() { return lockedBy; } public void setLockedBy(String v) { lockedBy = v; }
    public Date getLockedAt() { return lockedAt; } public void setLockedAt(Date v) { lockedAt = v; }
    public String getLastError() { return lastError; } public void setLastError(String v) { lastError = v; }
    public Date getSentAt() { return sentAt; } public void setSentAt(Date v) { sentAt = v; }
    public Date getCreateTime() { return createTime; } public void setCreateTime(Date v) { createTime = v; }
    public Date getUpdateTime() { return updateTime; } public void setUpdateTime(Date v) { updateTime = v; }
}
