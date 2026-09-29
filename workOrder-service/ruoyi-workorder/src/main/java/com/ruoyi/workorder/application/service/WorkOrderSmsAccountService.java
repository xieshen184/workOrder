package com.ruoyi.workorder.application.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderSmsAccountSettingRequest;
import com.ruoyi.workorder.domain.model.WorkOrderSmsAccount;
import com.ruoyi.workorder.mapper.WorkOrderSmsAccountMapper;
import org.springframework.stereotype.Service;

/**
 * 短信账户安全运营模块。
 *
 * <p>当前项目尚未选定短信供应商，因此本模块只管理脱敏快照和重试策略；刷新接口会明确返回
 * “未配置”，而不是构造余额。供应商确定后再在这里接入真实查询实现。</p>
 */
@Service
public class WorkOrderSmsAccountService
{
    private final WorkOrderSmsAccountMapper mapper;

    public WorkOrderSmsAccountService(WorkOrderSmsAccountMapper mapper)
    {
        this.mapper = mapper;
    }

    public WorkOrderSmsAccount get()
    {
        WorkOrderSmsAccount account = mapper.selectAccount();
        if (account == null)
        {
            account = new WorkOrderSmsAccount();
            account.setConfigured(false); account.setProviderName("未配置"); account.setStatus("UNCONFIGURED");
            return account;
        }
        Map<String, Object> statistics = mapper.selectSendStatistics();
        account.setTodaySent(integer(statistics, "todaySent"));
        account.setTodayFailed(integer(statistics, "todayFailed"));
        account.setMonthSent(integer(statistics, "monthSent"));
        if (account.isConfigured() && account.getAvailableBalance() != null && account.getWarningThreshold() != null
                && account.getAvailableBalance().compareTo(account.getWarningThreshold()) <= 0)
            account.setStatus("WARNING");
        return account;
    }

    public WorkOrderSmsAccount updateSettings(WorkOrderSmsAccountSettingRequest request, String username)
    {
        if (request == null || request.getWarningThreshold() == null) throw bad("余额预警阈值不能为空");
        if (request.getWarningThreshold().compareTo(BigDecimal.ZERO) < 0) throw bad("余额预警阈值不能小于0");
        if (request.getRetryEnabled() == null) throw bad("失败重试开关不能为空");
        WorkOrderSmsAccount current = mapper.selectAccount();
        if (current == null) throw new ServiceException("短信账户初始化数据不存在", HttpStatus.CONFLICT);
        if (request.getRetryEnabled() && !current.isConfigured())
            throw new ServiceException("短信供应商未配置，不能启用失败重试", HttpStatus.CONFLICT);
        int updated = mapper.updateSettings(request.getWarningThreshold(), request.getRetryEnabled() ? "1" : "0",
                username == null ? "" : username, new Date());
        if (updated != 1) throw new ServiceException("短信设置更新失败，请刷新后重试", HttpStatus.CONFLICT);
        return get();
    }

    public WorkOrderSmsAccount refresh()
    {
        WorkOrderSmsAccount account = mapper.selectAccount();
        if (account == null || !account.isConfigured())
            throw new ServiceException("短信供应商尚未配置，无法刷新账户余额", HttpStatus.CONFLICT);
        // 不在供应商未确定时伪造通用适配器；配置完成前保持最后一次成功快照不变。
        throw new ServiceException("短信供应商余额查询尚未接入", HttpStatus.CONFLICT);
    }

    private Integer integer(Map<String, Object> row, String key)
    {
        if (row == null) return 0;
        String expected = key.replace("_", "").toLowerCase();
        for (Map.Entry<String, Object> item : row.entrySet())
            if (item.getKey() != null && item.getKey().replace("_", "").toLowerCase().equals(expected))
                return item.getValue() == null ? 0 : new BigDecimal(String.valueOf(item.getValue())).intValue();
        return 0;
    }
    private ServiceException bad(String message) { return new ServiceException(message, HttpStatus.BAD_REQUEST); }
}
