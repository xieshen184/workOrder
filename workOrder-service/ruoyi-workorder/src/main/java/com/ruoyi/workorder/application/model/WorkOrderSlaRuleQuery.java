package com.ruoyi.workorder.application.model;

public class WorkOrderSlaRuleQuery
{
    private String keyword;
    private Long categoryId;
    private Integer urgencyLevel;
    private String status;

    public String getKeyword() { return keyword; } public void setKeyword(String v) { keyword = v; }
    public Long getCategoryId() { return categoryId; } public void setCategoryId(Long v) { categoryId = v; }
    public Integer getUrgencyLevel() { return urgencyLevel; } public void setUrgencyLevel(Integer v) { urgencyLevel = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
}
