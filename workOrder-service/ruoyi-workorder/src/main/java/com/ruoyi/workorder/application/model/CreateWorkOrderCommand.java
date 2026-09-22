package com.ruoyi.workorder.application.model;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateWorkOrderCommand
{
    @Size(max = 128, message = "工单标题不能超过128个字符")
    private String title;

    @NotNull(message = "请选择故障分类")
    private Long categoryId;

    @NotBlank(message = "请填写故障位置")
    @Size(max = 255, message = "故障位置不能超过255个字符")
    private String location;

    @NotNull(message = "请选择紧急程度")
    @Min(value = 1, message = "紧急程度不正确")
    @Max(value = 3, message = "紧急程度不正确")
    private Integer urgencyLevel;

    @NotNull(message = "请选择影响范围")
    @Min(value = 1, message = "影响范围不正确")
    @Max(value = 4, message = "影响范围不正确")
    private Integer impactScope;

    @NotBlank(message = "请填写故障描述")
    @Size(max = 1000, message = "故障描述不能超过1000个字符")
    private String description;

    @Size(max = 1000, message = "补充说明不能超过1000个字符")
    private String possibleCause;

    @Min(value = 1, message = "来源类型不正确")
    @Max(value = 3, message = "来源类型不正确")
    private Integer sourceType = 1;

    @Size(max = 10, message = "单次最多绑定10个附件")
    private List<Long> attachmentIds = new ArrayList<Long>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(Integer urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public Integer getImpactScope() { return impactScope; }
    public void setImpactScope(Integer impactScope) { this.impactScope = impactScope; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPossibleCause() { return possibleCause; }
    public void setPossibleCause(String possibleCause) { this.possibleCause = possibleCause; }
    public Integer getSourceType() { return sourceType; }
    public void setSourceType(Integer sourceType) { this.sourceType = sourceType; }
    public List<Long> getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(List<Long> attachmentIds) { this.attachmentIds = attachmentIds; }
}
