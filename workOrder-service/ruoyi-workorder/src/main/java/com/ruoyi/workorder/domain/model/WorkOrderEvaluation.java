package com.ruoyi.workorder.domain.model;

import java.util.Date;

/** One immutable business evaluation per work order. */
public class WorkOrderEvaluation
{
    private Long id;
    private Long orderId;
    private Long evaluatorId;
    private Integer overallScore;
    private Integer responseScore;
    private Integer qualityScore;
    private Integer attitudeScore;
    private String evaluationContent;
    private Date evaluatedAt;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(Long evaluatorId) { this.evaluatorId = evaluatorId; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    public Integer getResponseScore() { return responseScore; }
    public void setResponseScore(Integer responseScore) { this.responseScore = responseScore; }
    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
    public Integer getAttitudeScore() { return attitudeScore; }
    public void setAttitudeScore(Integer attitudeScore) { this.attitudeScore = attitudeScore; }
    public String getEvaluationContent() { return evaluationContent; }
    public void setEvaluationContent(String evaluationContent) { this.evaluationContent = evaluationContent; }
    public Date getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Date evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
