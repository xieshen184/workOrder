package com.ruoyi.workorder.application.model;

import com.ruoyi.common.core.domain.BaseEntity;

public class WorkOrderQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String keyword;
    private String status;
    private Long categoryId;
    private Integer urgencyLevel;
    private Long applicantId;
    private Long currentAssigneeId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(Integer urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public Long getCurrentAssigneeId() { return currentAssigneeId; }
    public void setCurrentAssigneeId(Long currentAssigneeId) { this.currentAssigneeId = currentAssigneeId; }
}
