package com.ruoyi.workorder.domain.model;

import java.math.BigDecimal;
import java.util.Date;

public class WorkOrderProcessRecord
{
    private Long id;
    private Long orderId;
    private String recordStage;
    private Long engineerId;
    private String engineerName;
    private String requiresParts;
    private String partsDescription;
    private BigDecimal assessedHours;
    private Integer assessedUrgency;
    private Integer assessedScope;
    private String requiresExtension;
    private String content;
    private Date occurredAt;
    private Date createTime;

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long v) { orderId = v; }
    public String getRecordStage() { return recordStage; } public void setRecordStage(String v) { recordStage = v; }
    public Long getEngineerId() { return engineerId; } public void setEngineerId(Long v) { engineerId = v; }
    public String getEngineerName() { return engineerName; } public void setEngineerName(String v) { engineerName = v; }
    public String getRequiresParts() { return requiresParts; } public void setRequiresParts(String v) { requiresParts = v; }
    public String getPartsDescription() { return partsDescription; } public void setPartsDescription(String v) { partsDescription = v; }
    public BigDecimal getAssessedHours() { return assessedHours; } public void setAssessedHours(BigDecimal v) { assessedHours = v; }
    public Integer getAssessedUrgency() { return assessedUrgency; } public void setAssessedUrgency(Integer v) { assessedUrgency = v; }
    public Integer getAssessedScope() { return assessedScope; } public void setAssessedScope(Integer v) { assessedScope = v; }
    public String getRequiresExtension() { return requiresExtension; } public void setRequiresExtension(String v) { requiresExtension = v; }
    public String getContent() { return content; } public void setContent(String v) { content = v; }
    public Date getOccurredAt() { return occurredAt; } public void setOccurredAt(Date v) { occurredAt = v; }
    public Date getCreateTime() { return createTime; } public void setCreateTime(Date v) { createTime = v; }
}
