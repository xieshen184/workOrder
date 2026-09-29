package com.ruoyi.workorder.domain.model;

import java.util.Date;

public class WorkOrderNotificationTemplate
{
    private Long id;
    private String eventCode;
    private String eventName;
    private String category;
    private String channel;
    private String templateName;
    private String titleTemplate;
    private String contentTemplate;
    private String enabledFlag;
    private Integer maxRetry;
    private String updateBy;
    private Date updateTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getContentTemplate() { return contentTemplate; }
    public void setContentTemplate(String contentTemplate) { this.contentTemplate = contentTemplate; }
    public String getEnabledFlag() { return enabledFlag; }
    public void setEnabledFlag(String enabledFlag) { this.enabledFlag = enabledFlag; }
    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
