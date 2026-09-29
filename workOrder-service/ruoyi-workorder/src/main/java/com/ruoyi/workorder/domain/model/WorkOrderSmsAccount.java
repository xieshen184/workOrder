package com.ruoyi.workorder.domain.model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 短信账户的安全运营视图。
 *
 * <p>数据库和接口都不保存供应商密钥，只保存不可逆的脱敏标识及最近一次余额快照。</p>
 */
public class WorkOrderSmsAccount
{
    private boolean configured;
    private String providerName;
    private String accountAlias;
    private String maskedCredential;
    private String status;
    private BigDecimal availableBalance;
    private Integer todaySent;
    private Integer todayFailed;
    private Integer monthSent;
    private BigDecimal warningThreshold;
    private boolean retryEnabled;
    private Date lastSuccessTime;
    private String lastError;

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean v) { configured = v; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String v) { providerName = v; }
    public String getAccountAlias() { return accountAlias; }
    public void setAccountAlias(String v) { accountAlias = v; }
    public String getMaskedCredential() { return maskedCredential; }
    public void setMaskedCredential(String v) { maskedCredential = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal v) { availableBalance = v; }
    public Integer getTodaySent() { return todaySent; }
    public void setTodaySent(Integer v) { todaySent = v; }
    public Integer getTodayFailed() { return todayFailed; }
    public void setTodayFailed(Integer v) { todayFailed = v; }
    public Integer getMonthSent() { return monthSent; }
    public void setMonthSent(Integer v) { monthSent = v; }
    public BigDecimal getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(BigDecimal v) { warningThreshold = v; }
    public boolean isRetryEnabled() { return retryEnabled; }
    public void setRetryEnabled(boolean v) { retryEnabled = v; }
    public Date getLastSuccessTime() { return lastSuccessTime; }
    public void setLastSuccessTime(Date v) { lastSuccessTime = v; }
    public String getLastError() { return lastError; }
    public void setLastError(String v) { lastError = v; }
}
