package com.ruoyi.workorder.application.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import com.ruoyi.workorder.domain.model.WorkOrderAction;

/** Immutable command accepted by the work-order command module. */
public final class WorkOrderCommand
{
    private final Long orderId;
    private final WorkOrderAction action;
    private final String idempotencyKey;
    private final Integer version;
    private final Long engineerId;
    private final String reason;
    private final String content;
    private final List<Long> attachmentIds;
    private final Boolean requiresParts;
    private final String partsDescription;
    private final BigDecimal assessedHours;
    private final Integer assessedUrgency;
    private final Integer assessedScope;
    private final Boolean requiresExtension;
    private final Integer overallScore;
    private final Integer responseScore;
    private final Integer qualityScore;
    private final Integer attitudeScore;

    private WorkOrderCommand(Long orderId, WorkOrderAction action, String idempotencyKey, Integer version,
            Long engineerId, String reason, String content, List<Long> attachmentIds,
            Boolean requiresParts, String partsDescription, BigDecimal assessedHours,
            Integer assessedUrgency, Integer assessedScope, Boolean requiresExtension,
            Integer overallScore, Integer responseScore, Integer qualityScore, Integer attitudeScore)
    {
        this.orderId = orderId;
        this.action = action;
        this.idempotencyKey = idempotencyKey;
        this.version = version;
        this.engineerId = engineerId;
        this.reason = reason;
        this.content = content;
        this.attachmentIds = immutableUniqueIds(attachmentIds);
        this.requiresParts = requiresParts;
        this.partsDescription = partsDescription;
        this.assessedHours = assessedHours;
        this.assessedUrgency = assessedUrgency;
        this.assessedScope = assessedScope;
        this.requiresExtension = requiresExtension;
        this.overallScore = overallScore;
        this.responseScore = responseScore;
        this.qualityScore = qualityScore;
        this.attitudeScore = attitudeScore;
    }

    public static WorkOrderCommand assignment(Long orderId, WorkOrderAction action, String key,
            Integer version, Long engineerId, String reason)
    {
        return new WorkOrderCommand(orderId, action, key, version, engineerId, reason,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static WorkOrderCommand simple(Long orderId, WorkOrderAction action, String key, Integer version)
    {
        return new WorkOrderCommand(orderId, action, key, version, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static WorkOrderCommand reasoned(Long orderId, WorkOrderAction action, String key,
            Integer version, String reason)
    {
        return new WorkOrderCommand(orderId, action, key, version, null, reason,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static WorkOrderCommand process(Long orderId, WorkOrderAction action, String key,
            Integer version, String content, List<Long> attachmentIds)
    {
        return new WorkOrderCommand(orderId, action, key, version, null, null,
                content, attachmentIds, null, null, null, null, null, null, null, null, null, null);
    }

    public static WorkOrderCommand assessment(Long orderId, String key, Integer version, String content,
            Boolean requiresParts, String partsDescription, BigDecimal assessedHours,
            Integer assessedUrgency, Integer assessedScope, Boolean requiresExtension)
    {
        return new WorkOrderCommand(orderId, WorkOrderAction.ASSESS, key, version, null, null,
                content, null, requiresParts, partsDescription, assessedHours,
                assessedUrgency, assessedScope, requiresExtension, null, null, null, null);
    }

    public static WorkOrderCommand evaluation(Long orderId, String key, Integer version,
            Integer overallScore, Integer responseScore, Integer qualityScore, Integer attitudeScore,
            String evaluationContent)
    {
        return new WorkOrderCommand(orderId, WorkOrderAction.EVALUATE, key, version, null, null,
                evaluationContent, null, null, null, null, null, null, null,
                overallScore, responseScore, qualityScore, attitudeScore);
    }

    private static List<Long> immutableUniqueIds(List<Long> ids)
    {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        LinkedHashSet<Long> unique = new LinkedHashSet<Long>();
        for (Long id : ids) if (id != null) unique.add(id);
        return Collections.unmodifiableList(new ArrayList<Long>(unique));
    }

    /** Canonical request material persisted as a hash to reject key reuse with a different command. */
    public String fingerprintSource()
    {
        List<Long> sortedIds = new ArrayList<Long>(attachmentIds);
        Collections.sort(sortedIds);
        return String.valueOf(orderId) + '|' + action + '|' + version + '|' + engineerId + '|'
                + value(reason) + '|' + value(content) + '|' + sortedIds + '|' + requiresParts + '|'
                + value(partsDescription) + '|' + assessedHours + '|' + assessedUrgency + '|'
                + assessedScope + '|' + requiresExtension + '|' + overallScore + '|'
                + responseScore + '|' + qualityScore + '|' + attitudeScore;
    }

    private String value(String value) { return value == null ? "" : value.trim(); }

    public Long getOrderId() { return orderId; }
    public WorkOrderAction getAction() { return action; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Integer getVersion() { return version; }
    public Long getEngineerId() { return engineerId; }
    public String getReason() { return reason; }
    public String getContent() { return content; }
    public List<Long> getAttachmentIds() { return attachmentIds; }
    public Boolean getRequiresParts() { return requiresParts; }
    public String getPartsDescription() { return partsDescription; }
    public BigDecimal getAssessedHours() { return assessedHours; }
    public Integer getAssessedUrgency() { return assessedUrgency; }
    public Integer getAssessedScope() { return assessedScope; }
    public Boolean getRequiresExtension() { return requiresExtension; }
    public Integer getOverallScore() { return overallScore; }
    public Integer getResponseScore() { return responseScore; }
    public Integer getQualityScore() { return qualityScore; }
    public Integer getAttitudeScore() { return attitudeScore; }
}
