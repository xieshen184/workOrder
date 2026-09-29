package com.ruoyi.workorder.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderAnalyticsQuery;
import com.ruoyi.workorder.application.model.WorkOrderDashboardResult;
import com.ruoyi.workorder.application.model.WorkOrderPerformanceResult;
import com.ruoyi.workorder.mapper.WorkOrderAnalyticsMapper;
import org.springframework.stereotype.Service;

/**
 * 驾驶舱与绩效统计深模块。
 *
 * <p>控制器只传入筛选条件；日期默认值、区间保护、数据范围以及数据库聚合结果到稳定页面模型的
 * 转换均封装在这里，使 PC 页面不会依赖 SQL 列名和空值细节。</p>
 */
@Service
public class WorkOrderAnalyticsService
{
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MAX_RANGE_DAYS = 366;

    private final WorkOrderAnalyticsMapper mapper;

    public WorkOrderAnalyticsService(WorkOrderAnalyticsMapper mapper)
    {
        this.mapper = mapper;
    }

    @DataScope(deptAlias = "d", userAlias = "u", permission = "workorder:dashboard:view")
    public WorkOrderDashboardResult dashboard(WorkOrderAnalyticsQuery source)
    {
        WorkOrderAnalyticsQuery query = prepare(source, 6);
        WorkOrderDashboardResult result = new WorkOrderDashboardResult();
        result.setGeneratedAt(new Date());
        result.setMetrics(dashboardMetrics(mapper.selectDashboardMetrics(query)));
        result.setTrend(trend(mapper.selectDashboardTrend(query)));
        result.setCategoryShare(categoryShare(mapper.selectCategoryShare(query)));
        result.setSlaRisks(slaRisks(mapper.selectSlaRisks(query)));
        result.setAlerts(alerts(mapper.selectOperationAlerts()));
        return result;
    }

    @DataScope(deptAlias = "d", userAlias = "u", permission = "workorder:performance:view")
    public WorkOrderPerformanceResult performance(WorkOrderAnalyticsQuery source)
    {
        WorkOrderAnalyticsQuery query = prepare(source, 29);
        WorkOrderPerformanceResult result = new WorkOrderPerformanceResult();
        result.setGeneratedAt(new Date());
        result.setMetrics(performanceMetrics(mapper.selectPerformanceMetrics(query)));
        result.setEngineerRows(engineerRows(mapper.selectEngineerPerformance(query)));
        return result;
    }

    private WorkOrderAnalyticsQuery prepare(WorkOrderAnalyticsQuery query, int defaultLookbackDays)
    {
        if (query == null) query = new WorkOrderAnalyticsQuery();
        LocalDate end = parse(query.getEndTime(), LocalDate.now(), "结束日期");
        LocalDate begin = parse(query.getBeginTime(), end.minusDays(defaultLookbackDays), "开始日期");
        if (begin.isAfter(end)) throw new ServiceException("开始日期不能晚于结束日期", HttpStatus.BAD_REQUEST);
        if (ChronoUnit.DAYS.between(begin, end) >= MAX_RANGE_DAYS)
            throw new ServiceException("单次统计区间不能超过366天", HttpStatus.BAD_REQUEST);
        query.setBeginTime(begin.format(DATE));
        query.setEndTime(end.format(DATE));
        return query;
    }

    private LocalDate parse(String value, LocalDate fallback, String label)
    {
        if (value == null || value.trim().isEmpty()) return fallback;
        try { return LocalDate.parse(value.trim(), DATE); }
        catch (DateTimeParseException exception)
        {
            throw new ServiceException(label + "格式应为yyyy-MM-dd", HttpStatus.BAD_REQUEST);
        }
    }

    private WorkOrderDashboardResult.Metrics dashboardMetrics(Map<String, Object> row)
    {
        WorkOrderDashboardResult.Metrics value = new WorkOrderDashboardResult.Metrics();
        value.setTodayNew(integer(row, "todayNew"));
        value.setPendingAssign(integer(row, "pendingAssign"));
        value.setProcessing(integer(row, "processing"));
        value.setSlaWarning(integer(row, "slaWarning"));
        return value;
    }

    private List<WorkOrderDashboardResult.TrendPoint> trend(List<Map<String, Object>> rows)
    {
        List<WorkOrderDashboardResult.TrendPoint> result = new ArrayList<WorkOrderDashboardResult.TrendPoint>();
        for (Map<String, Object> row : safe(rows))
        {
            WorkOrderDashboardResult.TrendPoint item = new WorkOrderDashboardResult.TrendPoint();
            item.setDate(text(row, "date")); item.setTotal(integer(row, "total"));
            item.setCompleted(integer(row, "completed")); result.add(item);
        }
        return result;
    }

    private List<WorkOrderDashboardResult.CategoryShare> categoryShare(List<Map<String, Object>> rows)
    {
        List<WorkOrderDashboardResult.CategoryShare> result = new ArrayList<WorkOrderDashboardResult.CategoryShare>();
        for (Map<String, Object> row : safe(rows))
        {
            WorkOrderDashboardResult.CategoryShare item = new WorkOrderDashboardResult.CategoryShare();
            item.setCategoryId(longValue(row, "categoryId")); item.setCategoryName(text(row, "categoryName"));
            item.setTotal(integer(row, "total")); result.add(item);
        }
        return result;
    }

    private List<WorkOrderDashboardResult.SlaRisk> slaRisks(List<Map<String, Object>> rows)
    {
        List<WorkOrderDashboardResult.SlaRisk> result = new ArrayList<WorkOrderDashboardResult.SlaRisk>();
        for (Map<String, Object> row : safe(rows))
        {
            WorkOrderDashboardResult.SlaRisk item = new WorkOrderDashboardResult.SlaRisk();
            item.setId(longValue(row, "id")); item.setOrderNo(text(row, "orderNo"));
            item.setTitle(text(row, "title")); item.setStatus(text(row, "status"));
            item.setAssigneeName(text(row, "assigneeName")); item.setDeadline(date(row, "deadline"));
            item.setOverdueFlag(text(row, "overdueFlag")); item.setWarningFlag(text(row, "warningFlag"));
            result.add(item);
        }
        return result;
    }

    private List<WorkOrderDashboardResult.OperationAlert> alerts(List<Map<String, Object>> rows)
    {
        List<WorkOrderDashboardResult.OperationAlert> result = new ArrayList<WorkOrderDashboardResult.OperationAlert>();
        for (Map<String, Object> row : safe(rows))
        {
            WorkOrderDashboardResult.OperationAlert item = new WorkOrderDashboardResult.OperationAlert();
            item.setCode(text(row, "code")); item.setLevel(text(row, "level")); item.setTitle(text(row, "title"));
            item.setMessage(text(row, "message")); item.setRoute(text(row, "route")); result.add(item);
        }
        return result;
    }

    private WorkOrderPerformanceResult.Metrics performanceMetrics(Map<String, Object> row)
    {
        WorkOrderPerformanceResult.Metrics value = new WorkOrderPerformanceResult.Metrics();
        fill(value, row);
        return value;
    }

    private List<WorkOrderPerformanceResult.EngineerRow> engineerRows(List<Map<String, Object>> rows)
    {
        List<WorkOrderPerformanceResult.EngineerRow> result = new ArrayList<WorkOrderPerformanceResult.EngineerRow>();
        for (Map<String, Object> row : safe(rows))
        {
            WorkOrderPerformanceResult.EngineerRow item = new WorkOrderPerformanceResult.EngineerRow();
            item.setEngineerId(longValue(row, "engineerId")); item.setEngineerName(text(row, "engineerName"));
            item.setDeptName(text(row, "deptName")); fill(item, row); result.add(item);
        }
        return result;
    }

    private void fill(WorkOrderPerformanceResult.Metrics value, Map<String, Object> row)
    {
        value.setCompletedCount(integer(row, "completedCount"));
        value.setAvgResponseMinutes(decimal(row, "avgResponseMinutes"));
        value.setAvgArrivalMinutes(decimal(row, "avgArrivalMinutes"));
        value.setOnTimeRate(decimal(row, "onTimeRate"));
        value.setSatisfactionScore(decimal(row, "satisfactionScore"));
        value.setReworkRate(decimal(row, "reworkRate"));
        value.setOnTimeSample(integer(row, "onTimeSample"));
        value.setSatisfactionSample(integer(row, "satisfactionSample"));
        value.setReworkSample(integer(row, "reworkSample"));
    }

    private List<Map<String, Object>> safe(List<Map<String, Object>> rows)
    {
        return rows == null ? Collections.<Map<String, Object>>emptyList() : rows;
    }

    private Object value(Map<String, Object> row, String key)
    {
        if (row == null) return null;
        String expected = key.replace("_", "").toLowerCase();
        for (Map.Entry<String, Object> item : row.entrySet())
            if (item.getKey() != null && item.getKey().replace("_", "").toLowerCase().equals(expected)) return item.getValue();
        return null;
    }
    private String text(Map<String, Object> row, String key) { Object v = value(row, key); return v == null ? null : String.valueOf(v); }
    private int integer(Map<String, Object> row, String key) { Object v = value(row, key); return v == null ? 0 : new BigDecimal(String.valueOf(v)).intValue(); }
    private Long longValue(Map<String, Object> row, String key) { Object v = value(row, key); return v == null ? null : new BigDecimal(String.valueOf(v)).longValue(); }
    private BigDecimal decimal(Map<String, Object> row, String key) { Object v = value(row, key); return v == null ? null : new BigDecimal(String.valueOf(v)); }
    private Date date(Map<String, Object> row, String key) { Object v = value(row, key); return v instanceof Date ? (Date) v : null; }
}
