package com.ruoyi.workorder.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.workorder.domain.model.WorkOrderAttachment;

public interface WorkOrderAttachmentMapper
{
    int insert(WorkOrderAttachment attachment);
    WorkOrderAttachment selectById(Long id);
    List<WorkOrderAttachment> selectByOrderId(Long orderId);
    int bindToOrder(@Param("orderId") Long orderId, @Param("uploaderId") Long uploaderId,
            @Param("ids") List<Long> ids);
    int bindToProcess(@Param("orderId") Long orderId, @Param("bizRefId") Long bizRefId,
            @Param("uploaderId") Long uploaderId, @Param("expectedStage") String expectedStage,
            @Param("ids") List<Long> ids);
    int markDeletedIfUnbound(@Param("id") Long id, @Param("uploaderId") Long uploaderId);
}
