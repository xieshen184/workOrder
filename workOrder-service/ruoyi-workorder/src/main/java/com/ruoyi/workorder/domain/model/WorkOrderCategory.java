package com.ruoyi.workorder.domain.model;

public class WorkOrderCategory
{
    private Long id;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Long managerDeptId;
    private String managerDeptName;
    private Long defaultSlaRuleId;
    private Integer orderNum;
    private String status;
    private String remark;
    private Integer referenceCount;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public Long getParentId() { return parentId; } public void setParentId(Long v) { parentId=v; }
    public String getCategoryCode() { return categoryCode; } public void setCategoryCode(String v) { categoryCode=v; }
    public String getCategoryName() { return categoryName; } public void setCategoryName(String v) { categoryName=v; }
    public Long getManagerDeptId() { return managerDeptId; } public void setManagerDeptId(Long v) { managerDeptId=v; }
    public String getManagerDeptName() { return managerDeptName; } public void setManagerDeptName(String v) { managerDeptName=v; }
    public Long getDefaultSlaRuleId() { return defaultSlaRuleId; } public void setDefaultSlaRuleId(Long v) { defaultSlaRuleId=v; }
    public Integer getOrderNum() { return orderNum; } public void setOrderNum(Integer v) { orderNum=v; }
    public String getStatus() { return status; } public void setStatus(String v) { status=v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark=v; }
    public Integer getReferenceCount() { return referenceCount; } public void setReferenceCount(Integer v) { referenceCount=v; }
    public String getCreateBy() { return createBy; } public void setCreateBy(String v) { createBy=v; }
    public String getUpdateBy() { return updateBy; } public void setUpdateBy(String v) { updateBy=v; }
}
