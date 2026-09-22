package com.ruoyi.workorder.application.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class VersionedRequest
{
    @NotNull(message = "缺少工单版本")
    @Min(value = 0, message = "工单版本不正确")
    private Integer version;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
