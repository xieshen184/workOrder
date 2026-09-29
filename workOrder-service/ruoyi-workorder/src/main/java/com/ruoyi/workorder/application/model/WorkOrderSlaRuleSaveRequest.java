package com.ruoyi.workorder.application.model;

import java.util.Date;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 管理端保存 SLA 规则的稳定输入；截止时间始终由后端按分钟计算。 */
public class WorkOrderSlaRuleSaveRequest
{
    @NotBlank(message = "规则编码不能为空")
    @Pattern(regexp = "[A-Z][A-Z0-9_]{1,31}", message = "规则编码必须为2到32位大写字母、数字或下划线")
    private String ruleCode;
    @NotBlank(message = "规则名称不能为空") @Size(max = 64, message = "规则名称不能超过64个字符")
    private String ruleName;
    private Long categoryId;
    @NotNull(message = "请选择紧急程度") @Min(1) @Max(3)
    private Integer urgencyLevel;
    @NotNull(message = "优先级不能为空") @Min(0)
    private Integer priority;
    @NotNull(message = "生效时间不能为空")
    private Date effectiveFrom;
    private Date effectiveTo;
    @NotNull(message = "响应时限不能为空") @Min(1)
    private Integer responseMinutes;
    @Min(1) private Integer arrivalMinutes;
    @Min(1) private Integer finishMinutes;
    @NotNull(message = "预警提前量不能为空") @Min(0)
    private Integer reminderBeforeMin;
    @NotBlank(message = "请选择是否允许延期") @Pattern(regexp = "[01]", message = "延期设置不正确")
    private String allowExtension;
    @NotBlank(message = "状态不能为空") @Pattern(regexp = "[01]", message = "状态不正确")
    private String status;
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public String getRuleCode() { return ruleCode; } public void setRuleCode(String v) { ruleCode = v; }
    public String getRuleName() { return ruleName; } public void setRuleName(String v) { ruleName = v; }
    public Long getCategoryId() { return categoryId; } public void setCategoryId(Long v) { categoryId = v; }
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
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
}
