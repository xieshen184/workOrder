package com.ruoyi.workorder.application.model;

import java.math.BigDecimal;

/** 短信运营策略；供应商凭据必须通过部署环境配置，不能经管理接口提交。 */
public class WorkOrderSmsAccountSettingRequest
{
    private BigDecimal warningThreshold;
    private Boolean retryEnabled;
    public BigDecimal getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(BigDecimal v) { warningThreshold = v; }
    public Boolean getRetryEnabled() { return retryEnabled; }
    public void setRetryEnabled(Boolean v) { retryEnabled = v; }
}
