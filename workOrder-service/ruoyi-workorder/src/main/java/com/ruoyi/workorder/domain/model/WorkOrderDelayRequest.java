package com.ruoyi.workorder.domain.model;

import java.util.Date;
import java.util.List;

/**
 * 工单延期申请聚合外的持久化实体。
 *
 * <p>延期申请不改变工单主状态，但审批会改变工单的有效完成截止时间和延期待审标记。
 * 因此申请记录、工单行和动作日志必须在同一个应用事务内完成变更。</p>
 */
public class WorkOrderDelayRequest
{
    private Long id;
    private Long orderId;
    private String orderNo;
    private String orderTitle;
    private Long applicantId;
    private String applicantName;
    private Date originalDeadline;
    private Date requestedDeadline;
    private String reason;
    private String requestStatus;
    private Long approverId;
    private String approverName;
    private String approvalComment;
    private Date approvedAt;
    private Date createTime;
    private Date updateTime;

    /** 查询详情时返回已绑定附件编号，数据库表本身不保存数组字段。 */
    private List<Long> attachmentIds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getOrderTitle() { return orderTitle; }
    public void setOrderTitle(String orderTitle) { this.orderTitle = orderTitle; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public Date getOriginalDeadline() { return originalDeadline; }
    public void setOriginalDeadline(Date originalDeadline) { this.originalDeadline = originalDeadline; }
    public Date getRequestedDeadline() { return requestedDeadline; }
    public void setRequestedDeadline(Date requestedDeadline) { this.requestedDeadline = requestedDeadline; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }
    public Date getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Date approvedAt) { this.approvedAt = approvedAt; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public List<Long> getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(List<Long> attachmentIds) { this.attachmentIds = attachmentIds; }

    /*
     * 数据库 F01 表使用 engineer/decided/status 命名，而接口历史草案使用
     * applicant/approver/requestStatus 命名。下面的别名让领域对象同时兼容两套
     * 语义，持久化映射只需要选择一种列名，不把数据库命名差异泄漏给 Controller。
     */
    public Long getEngineerId() { return applicantId; }
    public void setEngineerId(Long engineerId) { applicantId = engineerId; }
    public String getEngineerName() { return applicantName; }
    public void setEngineerName(String engineerName) { applicantName = engineerName; }
    public String getRequestReason() { return reason; }
    public void setRequestReason(String requestReason) { reason = requestReason; }
    public String getStatus() { return requestStatus; }
    public void setStatus(String status) { requestStatus = status; }
    public Long getDecidedBy() { return approverId; }
    public void setDecidedBy(Long decidedBy) { approverId = decidedBy; }
    public String getDecidedByName() { return approverName; }
    public void setDecidedByName(String decidedByName) { approverName = decidedByName; }
    public String getDecisionReason() { return approvalComment; }
    public void setDecisionReason(String decisionReason) { approvalComment = decisionReason; }
}
