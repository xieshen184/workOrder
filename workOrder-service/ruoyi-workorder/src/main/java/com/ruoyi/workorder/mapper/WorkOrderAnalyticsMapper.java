package com.ruoyi.workorder.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.workorder.application.model.WorkOrderAnalyticsQuery;

/** 运营统计读模型；这里只暴露页面需要的聚合结果，不泄漏底层表结构。 */
public interface WorkOrderAnalyticsMapper
{
    Map<String, Object> selectDashboardMetrics(WorkOrderAnalyticsQuery query);
    List<Map<String, Object>> selectDashboardTrend(WorkOrderAnalyticsQuery query);
    List<Map<String, Object>> selectCategoryShare(WorkOrderAnalyticsQuery query);
    List<Map<String, Object>> selectSlaRisks(WorkOrderAnalyticsQuery query);
    List<Map<String, Object>> selectOperationAlerts();
    Map<String, Object> selectPerformanceMetrics(WorkOrderAnalyticsQuery query);
    List<Map<String, Object>> selectEngineerPerformance(WorkOrderAnalyticsQuery query);
}
