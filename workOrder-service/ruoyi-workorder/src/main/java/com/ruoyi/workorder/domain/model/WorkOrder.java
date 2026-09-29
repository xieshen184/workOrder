package com.ruoyi.workorder.domain.model;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

public class WorkOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String orderNo;
    private String title;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Integer sourceType;
    private Long applicantId;
    private String applicantName;
    private String applicantPhone;
    private Long applicantDeptId;
    private String applicantDeptName;
    private String location;
    private Integer urgencyLevel;
    private Integer impactScope;
    private String description;
    private String possibleCause;
    private String status;
    private Long currentAssigneeId;
    private String currentAssigneeName;
    private Long slaRuleId;
    private Date responseDeadline;
    private Date arrivalDeadline;
    private Date finishDeadline;
    private Integer slaReminderBeforeMin;
    private String slaAllowExtension;
    private Date submittedAt;
    private Date assignedAt;
    private Date acceptedAt;
    private Date arrivedAt;
    private Date processingAt;
    private Date finishedAt;
    private Date confirmedAt;
    private Date closedAt;
    private String overdueFlag;
    private String warningFlag;
    private String delayPendingFlag;
    private String responseOverdue;
    private String arrivalOverdue;
    private String finishOverdue;
    private Date extensionDeadline;
    private Integer version;
    private List<WorkOrderAttachment> attachments;
    private List<WorkOrderActionLog> timeline;
    private WorkOrderEvaluation evaluation;
    private List<String> allowedActions;

    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public String getOrderNo() { return orderNo; } public void setOrderNo(String v) { orderNo=v; }
    public String getTitle() { return title; } public void setTitle(String v) { title=v; }
    public Long getCategoryId() { return categoryId; } public void setCategoryId(Long v) { categoryId=v; }
    public String getCategoryCode() { return categoryCode; } public void setCategoryCode(String v) { categoryCode=v; }
    public String getCategoryName() { return categoryName; } public void setCategoryName(String v) { categoryName=v; }
    public Integer getSourceType() { return sourceType; } public void setSourceType(Integer v) { sourceType=v; }
    public Long getApplicantId() { return applicantId; } public void setApplicantId(Long v) { applicantId=v; }
    public String getApplicantName() { return applicantName; } public void setApplicantName(String v) { applicantName=v; }
    public String getApplicantPhone() { return applicantPhone; } public void setApplicantPhone(String v) { applicantPhone=v; }
    public Long getApplicantDeptId() { return applicantDeptId; } public void setApplicantDeptId(Long v) { applicantDeptId=v; }
    public String getApplicantDeptName() { return applicantDeptName; } public void setApplicantDeptName(String v) { applicantDeptName=v; }
    public String getLocation() { return location; } public void setLocation(String v) { location=v; }
    public Integer getUrgencyLevel() { return urgencyLevel; } public void setUrgencyLevel(Integer v) { urgencyLevel=v; }
    public Integer getImpactScope() { return impactScope; } public void setImpactScope(Integer v) { impactScope=v; }
    public String getDescription() { return description; } public void setDescription(String v) { description=v; }
    public String getPossibleCause() { return possibleCause; } public void setPossibleCause(String v) { possibleCause=v; }
    public String getStatus() { return status; } public void setStatus(String v) { status=v; }
    public Long getCurrentAssigneeId() { return currentAssigneeId; } public void setCurrentAssigneeId(Long v) { currentAssigneeId=v; }
    public String getCurrentAssigneeName() { return currentAssigneeName; } public void setCurrentAssigneeName(String v) { currentAssigneeName=v; }
    public Long getSlaRuleId() { return slaRuleId; } public void setSlaRuleId(Long v) { slaRuleId=v; }
    public Date getResponseDeadline() { return responseDeadline; } public void setResponseDeadline(Date v) { responseDeadline=v; }
    public Date getArrivalDeadline() { return arrivalDeadline; } public void setArrivalDeadline(Date v) { arrivalDeadline=v; }
    public Date getFinishDeadline() { return finishDeadline; } public void setFinishDeadline(Date v) { finishDeadline=v; }
    public Integer getSlaReminderBeforeMin() { return slaReminderBeforeMin; } public void setSlaReminderBeforeMin(Integer v) { slaReminderBeforeMin=v; }
    public String getSlaAllowExtension() { return slaAllowExtension; } public void setSlaAllowExtension(String v) { slaAllowExtension=v; }
    public Date getSubmittedAt() { return submittedAt; } public void setSubmittedAt(Date v) { submittedAt=v; }
    public Date getAssignedAt() { return assignedAt; } public void setAssignedAt(Date v) { assignedAt=v; }
    public Date getAcceptedAt() { return acceptedAt; } public void setAcceptedAt(Date v) { acceptedAt=v; }
    public Date getArrivedAt() { return arrivedAt; } public void setArrivedAt(Date v) { arrivedAt=v; }
    public Date getProcessingAt() { return processingAt; } public void setProcessingAt(Date v) { processingAt=v; }
    public Date getFinishedAt() { return finishedAt; } public void setFinishedAt(Date v) { finishedAt=v; }
    public Date getConfirmedAt() { return confirmedAt; } public void setConfirmedAt(Date v) { confirmedAt=v; }
    public Date getClosedAt() { return closedAt; } public void setClosedAt(Date v) { closedAt=v; }
    public String getOverdueFlag() { return overdueFlag; } public void setOverdueFlag(String v) { overdueFlag=v; }
    public String getWarningFlag() { return warningFlag; } public void setWarningFlag(String v) { warningFlag=v; }
    public String getDelayPendingFlag() { return delayPendingFlag; } public void setDelayPendingFlag(String v) { delayPendingFlag=v; }
    public String getResponseOverdue() { return responseOverdue; } public void setResponseOverdue(String v) { responseOverdue=v; }
    public String getArrivalOverdue() { return arrivalOverdue; } public void setArrivalOverdue(String v) { arrivalOverdue=v; }
    public String getFinishOverdue() { return finishOverdue; } public void setFinishOverdue(String v) { finishOverdue=v; }
    public Date getExtensionDeadline() { return extensionDeadline; } public void setExtensionDeadline(Date v) { extensionDeadline=v; }
    public Integer getVersion() { return version; } public void setVersion(Integer v) { version=v; }
    public List<WorkOrderAttachment> getAttachments() { return attachments; } public void setAttachments(List<WorkOrderAttachment> v) { attachments=v; }
    public List<WorkOrderActionLog> getTimeline() { return timeline; } public void setTimeline(List<WorkOrderActionLog> v) { timeline=v; }
    public WorkOrderEvaluation getEvaluation() { return evaluation; } public void setEvaluation(WorkOrderEvaluation v) { evaluation=v; }
    public List<String> getAllowedActions() { return allowedActions; } public void setAllowedActions(List<String> v) { allowedActions=v; }
}
