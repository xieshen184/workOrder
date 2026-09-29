package com.ruoyi.workorder.application.model;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 管理端延期申请查询条件。
 *
 * <p>分页参数由 RuoYi 的 {@code startPage()} 从 HTTP 参数读取；这里仅承载延期业务
 * 筛选条件，避免把分页或数据权限拼接逻辑泄漏到延期服务。</p>
 */
public class WorkOrderDelayRequestQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long applicantId;
    private String requestStatus;
    /** 与 F01 表字段 status 对齐的兼容别名，requestStatus 优先。 */
    private String status;
    private String keyword;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
