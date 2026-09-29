package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.ruoyi.workorder.application.model.WorkOrderAnalyticsQuery;
import com.ruoyi.workorder.application.model.WorkOrderDashboardResult;
import com.ruoyi.workorder.application.model.WorkOrderPerformanceResult;
import com.ruoyi.workorder.mapper.WorkOrderAnalyticsMapper;
import org.junit.Test;

public class WorkOrderAnalyticsServiceTest
{
    @Test
    public void shouldBuildStableDashboardSnapshotAndDefaultRange()
    {
        AtomicReference<WorkOrderAnalyticsQuery> captured = new AtomicReference<WorkOrderAnalyticsQuery>();
        WorkOrderAnalyticsMapper mapper = proxy((method, args) -> {
            if ("selectOperationAlerts".equals(method)) return Collections.emptyList();
            captured.set((WorkOrderAnalyticsQuery) args[0]);
            if ("selectDashboardMetrics".equals(method)) return row("todayNew", 2, "pendingAssign", 3, "processing", 4, "slaWarning", 1);
            if ("selectDashboardTrend".equals(method)) return Collections.singletonList(row("date", "2026-09-28", "total", 2, "completed", 1));
            if ("selectCategoryShare".equals(method)) return Collections.emptyList();
            if ("selectSlaRisks".equals(method)) return Collections.emptyList();
            return null;
        });

        WorkOrderDashboardResult result = new WorkOrderAnalyticsService(mapper).dashboard(new WorkOrderAnalyticsQuery());

        assertEquals(2, result.getMetrics().getTodayNew());
        assertEquals(1, result.getTrend().size());
        assertNotNull(result.getGeneratedAt());
        assertNotNull(captured.get().getBeginTime());
        assertNotNull(captured.get().getEndTime());
    }

    @Test
    public void shouldKeepNullPerformanceRatiosWhenThereIsNoSample()
    {
        WorkOrderAnalyticsMapper mapper = proxy((method, args) -> {
            if ("selectPerformanceMetrics".equals(method)) return row("completedCount", 0, "onTimeSample", 0, "satisfactionSample", 0, "reworkSample", 0);
            if ("selectEngineerPerformance".equals(method)) return Arrays.asList(row("engineerId", 9L, "engineerName", "工程师", "completedCount", 1, "onTimeRate", new BigDecimal("100.0")));
            return Collections.emptyList();
        });

        WorkOrderPerformanceResult result = new WorkOrderAnalyticsService(mapper).performance(new WorkOrderAnalyticsQuery());

        assertEquals(null, result.getMetrics().getOnTimeRate());
        assertEquals(1, result.getEngineerRows().size());
        assertEquals(new BigDecimal("100.0"), result.getEngineerRows().get(0).getOnTimeRate());
    }

    private interface Call { Object invoke(String method, Object[] args); }
    private WorkOrderAnalyticsMapper proxy(Call call)
    {
        return (WorkOrderAnalyticsMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WorkOrderAnalyticsMapper.class}, (object, method, args) -> call.invoke(method.getName(), args));
    }
    private Map<String, Object> row(Object... values)
    {
        Map<String, Object> row = new HashMap<String, Object>();
        for (int index = 0; index < values.length; index += 2) row.put(String.valueOf(values[index]), values[index + 1]);
        return row;
    }
}
