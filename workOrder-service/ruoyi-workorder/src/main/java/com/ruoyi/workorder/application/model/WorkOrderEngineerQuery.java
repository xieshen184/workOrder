package com.ruoyi.workorder.application.model;

/** 调度端人员状态筛选；只暴露页面实际使用的稳定条件。 */
public class WorkOrderEngineerQuery
{
    private String keyword;
    private Long deptId;
    private String dutyStatus;
    private String workStatus;
    private Boolean online;

    public String getKeyword() { return keyword; } public void setKeyword(String v) { keyword = v; }
    public Long getDeptId() { return deptId; } public void setDeptId(Long v) { deptId = v; }
    public String getDutyStatus() { return dutyStatus; } public void setDutyStatus(String v) { dutyStatus = v; }
    public String getWorkStatus() { return workStatus; } public void setWorkStatus(String v) { workStatus = v; }
    public Boolean getOnline() { return online; } public void setOnline(Boolean v) { online = v; }
}
