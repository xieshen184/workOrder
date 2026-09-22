package com.ruoyi.workorder.application.model;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AssessmentRequest
{
    @NotBlank(message = "请填写评估内容")
    @Size(max = 2000, message = "评估内容不能超过2000个字符")
    private String content;
    @NotNull(message = "请选择是否需要零部件")
    private Boolean requiresParts;
    @Size(max = 500, message = "零部件说明不能超过500个字符")
    private String partsDescription;
    @NotNull(message = "请填写评估工时")
    @DecimalMin(value = "0.01", message = "评估工时必须大于0")
    private BigDecimal assessedHours;
    @NotNull(message = "请选择现场紧急程度")
    @Min(value = 1, message = "现场紧急程度不正确")
    @Max(value = 3, message = "现场紧急程度不正确")
    private Integer assessedUrgency;
    @NotNull(message = "请选择现场影响范围")
    @Min(value = 1, message = "现场影响范围不正确")
    @Max(value = 4, message = "现场影响范围不正确")
    private Integer assessedScope;
    @NotNull(message = "请选择是否需要延期")
    private Boolean requiresExtension;
    @NotNull(message = "缺少工单版本")
    @Min(value = 0, message = "工单版本不正确")
    private Integer version;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getRequiresParts() { return requiresParts; }
    public void setRequiresParts(Boolean requiresParts) { this.requiresParts = requiresParts; }
    public String getPartsDescription() { return partsDescription; }
    public void setPartsDescription(String partsDescription) { this.partsDescription = partsDescription; }
    public BigDecimal getAssessedHours() { return assessedHours; }
    public void setAssessedHours(BigDecimal assessedHours) { this.assessedHours = assessedHours; }
    public Integer getAssessedUrgency() { return assessedUrgency; }
    public void setAssessedUrgency(Integer assessedUrgency) { this.assessedUrgency = assessedUrgency; }
    public Integer getAssessedScope() { return assessedScope; }
    public void setAssessedScope(Integer assessedScope) { this.assessedScope = assessedScope; }
    public Boolean getRequiresExtension() { return requiresExtension; }
    public void setRequiresExtension(Boolean requiresExtension) { this.requiresExtension = requiresExtension; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
