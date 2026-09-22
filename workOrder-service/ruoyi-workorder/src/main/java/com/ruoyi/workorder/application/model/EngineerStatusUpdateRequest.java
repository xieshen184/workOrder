package com.ruoyi.workorder.application.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class EngineerStatusUpdateRequest
{
    @NotBlank(message = "请选择值班状态")
    @Pattern(regexp = "ON_DUTY|OFF_DUTY", message = "值班状态不正确")
    private String dutyStatus;

    @NotBlank(message = "请选择工作状态")
    @Pattern(regexp = "AVAILABLE|WORKING|BUSY", message = "工作状态不正确")
    private String workStatus;

    public String getDutyStatus() { return dutyStatus; } public void setDutyStatus(String v) { dutyStatus = v; }
    public String getWorkStatus() { return workStatus; } public void setWorkStatus(String v) { workStatus = v; }
}
