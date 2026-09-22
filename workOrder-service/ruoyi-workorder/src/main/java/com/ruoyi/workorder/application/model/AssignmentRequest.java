package com.ruoyi.workorder.application.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AssignmentRequest
{
    @NotNull(message = "请选择维修人员")
    private Long engineerId;
    @Size(max = 500, message = "派单原因不能超过500个字符")
    private String reason;
    @NotNull(message = "缺少工单版本")
    @Min(value = 0, message = "工单版本不正确")
    private Integer version;

    public Long getEngineerId() { return engineerId; }
    public void setEngineerId(Long engineerId) { this.engineerId = engineerId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
