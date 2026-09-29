package com.ruoyi.workorder.application.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 工程师提交延期申请时的请求 DTO。
 *
 * <p>DTO 只负责 HTTP 层的字段形状和基础长度校验；是否处于处理中、是否为当前处理人、
 * 当前 SLA 是否允许延期以及新截止时间是否确实晚于原截止时间，统一由
 * {@code WorkOrderDelayService} 在数据库行锁内校验。</p>
 */
public class WorkOrderDelayRequestCreateRequest
{
    /** 申请延期到的完成截止时间。 */
    @NotNull(message = "请选择申请延期后的完成截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date requestedDeadline;

    /** 延期原因由业务表保存，长度与 F01 表 VARCHAR(500) 保持一致。 */
    @NotNull(message = "请填写延期原因")
    @Size(min = 1, max = 500, message = "延期原因长度必须在1到500个字符之间")
    private String reason;

    /** 上传接口先产生临时附件，提交延期申请时再由服务绑定到申请记录。 */
    @Size(max = 10, message = "单次最多绑定10个附件")
    private List<Long> attachmentIds = new ArrayList<Long>();

    /** 工单乐观锁版本，避免客户端基于旧工单状态提交申请。 */
    @NotNull(message = "缺少工单版本")
    @Min(value = 0, message = "工单版本不能为负数")
    private Integer version;

    public Date getRequestedDeadline() { return requestedDeadline; }
    public void setRequestedDeadline(Date requestedDeadline) { this.requestedDeadline = requestedDeadline; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<Long> getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(List<Long> attachmentIds) { this.attachmentIds = attachmentIds; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
