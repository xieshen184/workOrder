package com.ruoyi.workorder.application.model;

import com.ruoyi.common.core.domain.BaseEntity;

/** 站内消息及后台发送记录查询条件。分页参数由若依分页拦截器统一处理。 */
public class WorkOrderNotificationQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private String category;
    private String readStatus;
    private String status;
    private String channel;
    private String eventCode;
    private String orderNo;

    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public String getReadStatus() { return readStatus; } public void setReadStatus(String v) { readStatus = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getChannel() { return channel; } public void setChannel(String v) { channel = v; }
    public String getEventCode() { return eventCode; } public void setEventCode(String v) { eventCode = v; }
    public String getOrderNo() { return orderNo; } public void setOrderNo(String v) { orderNo = v; }
}
