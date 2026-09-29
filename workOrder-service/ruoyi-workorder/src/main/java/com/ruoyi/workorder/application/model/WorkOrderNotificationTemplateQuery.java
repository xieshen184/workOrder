package com.ruoyi.workorder.application.model;

import com.ruoyi.common.core.domain.BaseEntity;

/** 消息模板列表筛选条件。 */
public class WorkOrderNotificationTemplateQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private String eventCode;
    private String category;
    private String channel;
    private String enabledFlag;

    public String getEventCode() { return eventCode; }
    public void setEventCode(String value) { eventCode = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { category = value; }
    public String getChannel() { return channel; }
    public void setChannel(String value) { channel = value; }
    public String getEnabledFlag() { return enabledFlag; }
    public void setEnabledFlag(String value) { enabledFlag = value; }
}
