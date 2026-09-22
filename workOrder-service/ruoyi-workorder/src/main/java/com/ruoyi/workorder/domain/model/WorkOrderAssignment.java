package com.ruoyi.workorder.domain.model;

import java.util.Date;

public class WorkOrderAssignment
{
    private Long id;
    private Long orderId;
    private Long engineerId;
    private String engineerName;
    private Long engineerDeptId;
    private Long assignedBy;
    private String assignedByName;
    private Date assignedAt;
    private Integer responseMinutes;
    private Date responseDeadline;
    private String assignmentStatus;
    private Date createTime;

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long v) { orderId = v; }
    public Long getEngineerId() { return engineerId; } public void setEngineerId(Long v) { engineerId = v; }
    public String getEngineerName() { return engineerName; } public void setEngineerName(String v) { engineerName = v; }
    public Long getEngineerDeptId() { return engineerDeptId; } public void setEngineerDeptId(Long v) { engineerDeptId = v; }
    public Long getAssignedBy() { return assignedBy; } public void setAssignedBy(Long v) { assignedBy = v; }
    public String getAssignedByName() { return assignedByName; } public void setAssignedByName(String v) { assignedByName = v; }
    public Date getAssignedAt() { return assignedAt; } public void setAssignedAt(Date v) { assignedAt = v; }
    public Integer getResponseMinutes() { return responseMinutes; } public void setResponseMinutes(Integer v) { responseMinutes = v; }
    public Date getResponseDeadline() { return responseDeadline; } public void setResponseDeadline(Date v) { responseDeadline = v; }
    public String getAssignmentStatus() { return assignmentStatus; } public void setAssignmentStatus(String v) { assignmentStatus = v; }
    public Date getCreateTime() { return createTime; } public void setCreateTime(Date v) { createTime = v; }
}
