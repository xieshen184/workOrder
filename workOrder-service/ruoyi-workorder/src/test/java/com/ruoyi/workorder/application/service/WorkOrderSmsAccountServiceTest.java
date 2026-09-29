package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderSmsAccountSettingRequest;
import com.ruoyi.workorder.domain.model.WorkOrderSmsAccount;
import com.ruoyi.workorder.mapper.WorkOrderSmsAccountMapper;
import org.junit.Test;

public class WorkOrderSmsAccountServiceTest
{
    @Test
    public void shouldReturnRealTaskStatisticsWithUnconfiguredAccount()
    {
        WorkOrderSmsAccount account = new WorkOrderSmsAccount();
        account.setConfigured(false); account.setStatus("UNCONFIGURED");
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("todaySent", 0); stats.put("todayFailed", 2); stats.put("monthSent", 5);
        WorkOrderSmsAccountMapper mapper = proxy((method, args) -> "selectAccount".equals(method) ? account : stats);

        WorkOrderSmsAccount result = new WorkOrderSmsAccountService(mapper).get();

        assertEquals(Integer.valueOf(2), result.getTodayFailed());
        assertEquals(Integer.valueOf(5), result.getMonthSent());
        assertEquals(null, result.getAvailableBalance());
    }

    @Test
    public void shouldNotEnableRetryBeforeProviderIsConfigured()
    {
        WorkOrderSmsAccount account = new WorkOrderSmsAccount(); account.setConfigured(false);
        WorkOrderSmsAccountMapper mapper = proxy((method, args) -> account);
        WorkOrderSmsAccountSettingRequest request = new WorkOrderSmsAccountSettingRequest();
        request.setWarningThreshold(new BigDecimal("100")); request.setRetryEnabled(true);

        try { new WorkOrderSmsAccountService(mapper).updateSettings(request, "admin"); }
        catch (ServiceException error)
        {
            assertTrue(error.getMessage().contains("不能启用")); return;
        }
        throw new AssertionError("未配置供应商时不应启用重试");
    }

    private interface Call { Object invoke(String method, Object[] args); }
    private WorkOrderSmsAccountMapper proxy(Call call)
    {
        return (WorkOrderSmsAccountMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WorkOrderSmsAccountMapper.class}, (object, method, args) -> call.invoke(method.getName(), args));
    }
}
