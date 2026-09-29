package com.ruoyi.workorder.application.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 维修绩效页的指标与人员明细快照。 */
public class WorkOrderPerformanceResult
{
    private Date generatedAt;
    private Metrics metrics = new Metrics();
    private List<EngineerRow> engineerRows = new ArrayList<EngineerRow>();

    public Date getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Date value) { generatedAt = value; }
    public Metrics getMetrics() { return metrics; }
    public void setMetrics(Metrics value) { metrics = value; }
    public List<EngineerRow> getEngineerRows() { return engineerRows; }
    public void setEngineerRows(List<EngineerRow> value) { engineerRows = value; }

    public static class Metrics
    {
        private int completedCount;
        private BigDecimal avgResponseMinutes;
        private BigDecimal avgArrivalMinutes;
        private BigDecimal onTimeRate;
        private BigDecimal satisfactionScore;
        private BigDecimal reworkRate;
        private int onTimeSample;
        private int satisfactionSample;
        private int reworkSample;
        public int getCompletedCount() { return completedCount; }
        public void setCompletedCount(int v) { completedCount = v; }
        public BigDecimal getAvgResponseMinutes() { return avgResponseMinutes; }
        public void setAvgResponseMinutes(BigDecimal v) { avgResponseMinutes = v; }
        public BigDecimal getAvgArrivalMinutes() { return avgArrivalMinutes; }
        public void setAvgArrivalMinutes(BigDecimal v) { avgArrivalMinutes = v; }
        public BigDecimal getOnTimeRate() { return onTimeRate; }
        public void setOnTimeRate(BigDecimal v) { onTimeRate = v; }
        public BigDecimal getSatisfactionScore() { return satisfactionScore; }
        public void setSatisfactionScore(BigDecimal v) { satisfactionScore = v; }
        public BigDecimal getReworkRate() { return reworkRate; }
        public void setReworkRate(BigDecimal v) { reworkRate = v; }
        public int getOnTimeSample() { return onTimeSample; }
        public void setOnTimeSample(int v) { onTimeSample = v; }
        public int getSatisfactionSample() { return satisfactionSample; }
        public void setSatisfactionSample(int v) { satisfactionSample = v; }
        public int getReworkSample() { return reworkSample; }
        public void setReworkSample(int v) { reworkSample = v; }
    }

    public static class EngineerRow extends Metrics
    {
        private Long engineerId;
        private String engineerName;
        private String deptName;
        public Long getEngineerId() { return engineerId; }
        public void setEngineerId(Long v) { engineerId = v; }
        public String getEngineerName() { return engineerName; }
        public void setEngineerName(String v) { engineerName = v; }
        public String getDeptName() { return deptName; }
        public void setDeptName(String v) { deptName = v; }
    }
}
