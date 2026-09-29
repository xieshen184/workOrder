package com.ruoyi.workorder.application.model;

import javax.validation.constraints.Size;

/**
 * 延期审批请求 DTO。
 *
 * <p>同意操作允许不填写意见；拒绝操作由应用服务强制要求填写原因。保留
 * {@code approvalComment} 作为兼容别名，是为了兼容已经上线的管理端表单字段，
 * 对外契约仍以 {@code reason} 为准。</p>
 */
public class WorkOrderDelayDecisionRequest
{
    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String reason;

    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String approvalComment;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getApprovalComment() { return approvalComment; }
    public void setApprovalComment(String approvalComment) { this.approvalComment = approvalComment; }

    /** 返回契约字段 reason，空缺时兼容读取旧管理端使用的 approvalComment。 */
    public String effectiveReason()
    {
        return hasText(reason) ? reason : approvalComment;
    }

    private boolean hasText(String value)
    {
        return value != null && value.trim().length() > 0;
    }
}
