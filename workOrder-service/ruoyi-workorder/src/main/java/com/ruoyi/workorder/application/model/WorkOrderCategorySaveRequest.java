package com.ruoyi.workorder.application.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class WorkOrderCategorySaveRequest
{
    @NotNull(message = "父分类不能为空")
    @Min(value = 0, message = "父分类不正确")
    private Long parentId;

    @NotBlank(message = "分类编码不能为空")
    @Pattern(regexp = "[A-Z][A-Z0-9_]{1,31}", message = "分类编码须为2到32位大写字母、数字或下划线")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称不能超过64个字符")
    private String categoryName;

    private Long managerDeptId;

    @NotNull(message = "显示顺序不能为空")
    @Min(value = 0, message = "显示顺序不能小于0")
    private Integer orderNum;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "0|1", message = "分类状态不正确")
    private String status;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public Long getParentId() { return parentId; } public void setParentId(Long v) { parentId = v; }
    public String getCategoryCode() { return categoryCode; } public void setCategoryCode(String v) { categoryCode = v; }
    public String getCategoryName() { return categoryName; } public void setCategoryName(String v) { categoryName = v; }
    public Long getManagerDeptId() { return managerDeptId; } public void setManagerDeptId(Long v) { managerDeptId = v; }
    public Integer getOrderNum() { return orderNum; } public void setOrderNum(Integer v) { orderNum = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
}
