package com.ruoyi.workorder.application.model;

/** 模板可编辑字段；事件和渠道不在保存模型中，防止把既有事件改成未注册事件。 */
public class WorkOrderNotificationTemplateSaveRequest
{
    private String eventCode;
    private String channel;
    private String templateName;
    private String titleTemplate;
    private String contentTemplate;
    private String enabledFlag;
    private Integer maxRetry;
    private String remark;

    public String getEventCode() { return eventCode; }
    public void setEventCode(String value) { eventCode = value; }
    public String getChannel() { return channel; }
    public void setChannel(String value) { channel = value; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String value) { templateName = value; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String value) { titleTemplate = value; }
    public String getContentTemplate() { return contentTemplate; }
    public void setContentTemplate(String value) { contentTemplate = value; }
    public String getEnabledFlag() { return enabledFlag; }
    public void setEnabledFlag(String value) { enabledFlag = value; }
    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer value) { maxRetry = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
}
