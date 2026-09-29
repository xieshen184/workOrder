package com.ruoyi.workorder.application.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 驾驶舱一次加载所需的完整快照，避免前端拼接多个口径不一致的接口。 */
public class WorkOrderDashboardResult
{
    private Date generatedAt;
    private Metrics metrics = new Metrics();
    private List<TrendPoint> trend = new ArrayList<TrendPoint>();
    private List<CategoryShare> categoryShare = new ArrayList<CategoryShare>();
    private List<SlaRisk> slaRisks = new ArrayList<SlaRisk>();
    private List<OperationAlert> alerts = new ArrayList<OperationAlert>();

    public Date getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Date generatedAt) { this.generatedAt = generatedAt; }
    public Metrics getMetrics() { return metrics; }
    public void setMetrics(Metrics metrics) { this.metrics = metrics; }
    public List<TrendPoint> getTrend() { return trend; }
    public void setTrend(List<TrendPoint> trend) { this.trend = trend; }
    public List<CategoryShare> getCategoryShare() { return categoryShare; }
    public void setCategoryShare(List<CategoryShare> categoryShare) { this.categoryShare = categoryShare; }
    public List<SlaRisk> getSlaRisks() { return slaRisks; }
    public void setSlaRisks(List<SlaRisk> slaRisks) { this.slaRisks = slaRisks; }
    public List<OperationAlert> getAlerts() { return alerts; }
    public void setAlerts(List<OperationAlert> alerts) { this.alerts = alerts; }

    public static class Metrics
    {
        private int todayNew;
        private int pendingAssign;
        private int processing;
        private int slaWarning;
        public int getTodayNew() { return todayNew; }
        public void setTodayNew(int value) { todayNew = value; }
        public int getPendingAssign() { return pendingAssign; }
        public void setPendingAssign(int value) { pendingAssign = value; }
        public int getProcessing() { return processing; }
        public void setProcessing(int value) { processing = value; }
        public int getSlaWarning() { return slaWarning; }
        public void setSlaWarning(int value) { slaWarning = value; }
    }

    public static class TrendPoint
    {
        private String date;
        private int total;
        private int completed;
        public String getDate() { return date; }
        public void setDate(String value) { date = value; }
        public int getTotal() { return total; }
        public void setTotal(int value) { total = value; }
        public int getCompleted() { return completed; }
        public void setCompleted(int value) { completed = value; }
    }

    public static class CategoryShare
    {
        private Long categoryId;
        private String categoryName;
        private int total;
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long value) { categoryId = value; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String value) { categoryName = value; }
        public int getTotal() { return total; }
        public void setTotal(int value) { total = value; }
    }

    public static class SlaRisk
    {
        private Long id;
        private String orderNo;
        private String title;
        private String status;
        private String assigneeName;
        private Date deadline;
        private String overdueFlag;
        private String warningFlag;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String value) { orderNo = value; }
        public String getTitle() { return title; }
        public void setTitle(String value) { title = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
        public String getAssigneeName() { return assigneeName; }
        public void setAssigneeName(String value) { assigneeName = value; }
        public Date getDeadline() { return deadline; }
        public void setDeadline(Date value) { deadline = value; }
        public String getOverdueFlag() { return overdueFlag; }
        public void setOverdueFlag(String value) { overdueFlag = value; }
        public String getWarningFlag() { return warningFlag; }
        public void setWarningFlag(String value) { warningFlag = value; }
    }

    public static class OperationAlert
    {
        private String code;
        private String level;
        private String title;
        private String message;
        private String route;
        public String getCode() { return code; }
        public void setCode(String value) { code = value; }
        public String getLevel() { return level; }
        public void setLevel(String value) { level = value; }
        public String getTitle() { return title; }
        public void setTitle(String value) { title = value; }
        public String getMessage() { return message; }
        public void setMessage(String value) { message = value; }
        public String getRoute() { return route; }
        public void setRoute(String value) { route = value; }
    }
}
