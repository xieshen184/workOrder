package com.ruoyi.workorder.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.workorder.application.model.WorkOrderDelayRequestQuery;
import com.ruoyi.workorder.domain.model.WorkOrderDelayRequest;

/**
 * 延期申请专用持久化入口。
 *
 * <p>项目约定不扩展共享 {@code WorkOrderMapper}。延期涉及的工单标志和截止时间更新
 * 因而集中放在此 Mapper 中，避免把 F01 的事务语义散落到通用工单命令 Mapper。</p>
 */
public interface WorkOrderDelayRequestMapper
{
    WorkOrderDelayRequest selectById(Long id);
    WorkOrderDelayRequest selectByIdForUpdate(Long id);
    WorkOrderDelayRequest selectLatestByOrderId(Long orderId);
    WorkOrderDelayRequest selectPendingByOrderId(Long orderId);
    List<WorkOrderDelayRequest> selectList(WorkOrderDelayRequestQuery query);

    int insert(WorkOrderDelayRequest request);

    /** 将尚未绑定且由当前用户上传的临时附件绑定到延期申请。 */
    int bindAttachments(@Param("orderId") Long orderId, @Param("requestId") Long requestId,
            @Param("uploaderId") Long uploaderId, @Param("ids") List<Long> ids);

    /** 创建申请时设置待审标记，并用工单版本做一次 CAS。 */
    int markDelayPending(@Param("orderId") Long orderId, @Param("version") Integer version,
            @Param("updateBy") String updateBy, @Param("actionTime") Date actionTime);

    /** 审批同意时写入延期截止时间，同时清除待审标记并递增工单版本。 */
    int applyApprovedDelay(@Param("orderId") Long orderId, @Param("version") Integer version,
            @Param("requestedDeadline") Date requestedDeadline, @Param("updateBy") String updateBy,
            @Param("actionTime") Date actionTime);

    /** 审批拒绝时只清除待审标记，不触碰任何有效截止时间。 */
    int clearDelayPending(@Param("orderId") Long orderId, @Param("version") Integer version,
            @Param("updateBy") String updateBy, @Param("actionTime") Date actionTime);

    /** 申请行的 PENDING -> APPROVED/REJECTED 状态 CAS。 */
    int updateDecision(@Param("id") Long id, @Param("requestStatus") String requestStatus,
            @Param("approverId") Long approverId, @Param("approverName") String approverName,
            @Param("approvalComment") String approvalComment, @Param("approvedAt") Date approvedAt,
            @Param("updateTime") Date updateTime);

    /** 完工时取消仍在等待审批的申请，避免主工单结束后留下永久待审记录。 */
    int cancelPendingByOrderId(@Param("orderId") Long orderId, @Param("operatorId") Long operatorId,
            @Param("operatorName") String operatorName, @Param("reason") String reason,
            @Param("actionTime") Date actionTime);

    List<Long> selectAttachmentIdsByRequestId(Long requestId);
}
