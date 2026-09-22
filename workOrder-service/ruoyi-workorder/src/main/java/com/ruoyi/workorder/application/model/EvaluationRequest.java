package com.ruoyi.workorder.application.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class EvaluationRequest
{
    @NotNull(message = "总体评分不能为空")
    @Min(value = 1, message = "总体评分必须为1到5")
    @Max(value = 5, message = "总体评分必须为1到5")
    private Integer overallScore;
    @Min(value = 1, message = "响应速度评分必须为1到5")
    @Max(value = 5, message = "响应速度评分必须为1到5")
    private Integer responseScore;
    @Min(value = 1, message = "维修质量评分必须为1到5")
    @Max(value = 5, message = "维修质量评分必须为1到5")
    private Integer qualityScore;
    @Min(value = 1, message = "服务态度评分必须为1到5")
    @Max(value = 5, message = "服务态度评分必须为1到5")
    private Integer attitudeScore;
    @Size(max = 500, message = "评价内容不能超过500个字符")
    private String evaluationContent;
    @NotNull(message = "缺少工单版本")
    @Min(value = 0, message = "工单版本不正确")
    private Integer version;

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
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
