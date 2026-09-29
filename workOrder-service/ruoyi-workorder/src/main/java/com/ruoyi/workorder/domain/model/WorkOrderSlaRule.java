package com.ruoyi.workorder.domain.model;

import java.util.Date;

/**
 * SLA 规则及其管理投影。
 *
 * <p>工单创建时只复制规则 ID、三个截止时间、预警提前量和延期许可，之后修改规则不会回写历史工单。</p>
 */
public class WorkOrderSlaRule
{
    private Long id;
    private String ruleCode;
    private String ruleName;
    private Long categoryId;
    private String categoryName;
    private Integer urgencyLevel;
    private Integer priority;
    private Date effectiveFrom;
    private Date effectiveTo;
    private Integer responseMinutes;
    private Integer arrivalMinutes;
    private Integer finishMinutes;
    private Integer reminderBeforeMin;
    private String allowExtension;
    private String status;
    private Long referenceCount;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    public Date responseDeadline(Date submittedAt) { return plusMinutes(submittedAt, responseMinutes); }
    public Date arrivalDeadline(Date submittedAt) { return plusMinutes(submittedAt, arrivalMinutes); }
    public Date finishDeadline(Date submittedAt) { return plusMinutes(submittedAt, finishMinutes); }

    private Date plusMinutes(Date source, Integer minutes)
    {
        return minutes == null ? null : new Date(source.getTime() + minutes.longValue() * 60_000L);
    }

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public String getRuleCode() { return ruleCode; } public void setRuleCode(String v) { ruleCode = v; }
    public String getRuleName() { return ruleName; } public void setRuleName(String v) { ruleName = v; }
    public Long getCategoryId() { return categoryId; } public void setCategoryId(Long v) { categoryId = v; }
    public String getCategoryName() { return categoryName; } public void setCategoryName(String v) { categoryName = v; }
    public Integer getUrgencyLevel() { return urgencyLevel; } public void setUrgencyLevel(Integer v) { urgencyLevel = v; }
    public Integer getPriority() { return priority; } public void setPriority(Integer v) { priority = v; }
    public Date getEffectiveFrom() { return effectiveFrom; } public void setEffectiveFrom(Date v) { effectiveFrom = v; }
    public Date getEffectiveTo() { return effectiveTo; } public void setEffectiveTo(Date v) { effectiveTo = v; }
    public Integer getResponseMinutes() { return responseMinutes; } public void setResponseMinutes(Integer v) { responseMinutes = v; }
    public Integer getArrivalMinutes() { return arrivalMinutes; } public void setArrivalMinutes(Integer v) { arrivalMinutes = v; }
    public Integer getFinishMinutes() { return finishMinutes; } public void setFinishMinutes(Integer v) { finishMinutes = v; }
    public Integer getReminderBeforeMin() { return reminderBeforeMin; } public void setReminderBeforeMin(Integer v) { reminderBeforeMin = v; }
    public String getAllowExtension() { return allowExtension; } public void setAllowExtension(String v) { allowExtension = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public Long getReferenceCount() { return referenceCount; } public void setReferenceCount(Long v) { referenceCount = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
    public String getCreateBy() { return createBy; } public void setCreateBy(String v) { createBy = v; }
    public Date getCreateTime() { return createTime; } public void setCreateTime(Date v) { createTime = v; }
    public String getUpdateBy() { return updateBy; } public void setUpdateBy(String v) { updateBy = v; }
    public Date getUpdateTime() { return updateTime; } public void setUpdateTime(Date v) { updateTime = v; }
}
