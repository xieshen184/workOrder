package com.ruoyi.workorder.domain.model;

import java.util.Date;

/** Eligible engineer projection. Names and department data always come from the server user directory. */
public class WorkOrderEngineer
{
    private Long id;
    private String name;
    private Long deptId;
    private String dept;
    private String dutyStatus;
    private String status;
    private Integer load;
    private Long currentOrderId;
    private Integer overdueOrderCount;
    private String statusSource;
    private Date lastChangedAt;
    private Date lastHeartbeatAt;
    private Date updateTime;
    private Boolean online;

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public Long getDeptId() { return deptId; } public void setDeptId(Long v) { deptId = v; }
    public String getDept() { return dept; } public void setDept(String v) { dept = v; }
    public String getDutyStatus() { return dutyStatus; } public void setDutyStatus(String v) { dutyStatus = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public Integer getLoad() { return load; } public void setLoad(Integer v) { load = v; }
    public Long getCurrentOrderId() { return currentOrderId; } public void setCurrentOrderId(Long v) { currentOrderId = v; }
    public Integer getOverdueOrderCount() { return overdueOrderCount; } public void setOverdueOrderCount(Integer v) { overdueOrderCount = v; }
    public String getStatusSource() { return statusSource; } public void setStatusSource(String v) { statusSource = v; }
    public Date getLastChangedAt() { return lastChangedAt; } public void setLastChangedAt(Date v) { lastChangedAt = v; }
    public Date getLastHeartbeatAt() { return lastHeartbeatAt; } public void setLastHeartbeatAt(Date v) { lastHeartbeatAt = v; }
    public Date getUpdateTime() { return updateTime; } public void setUpdateTime(Date v) { updateTime = v; }
    public Boolean getOnline() { return online; } public void setOnline(Boolean v) { online = v; }
}
