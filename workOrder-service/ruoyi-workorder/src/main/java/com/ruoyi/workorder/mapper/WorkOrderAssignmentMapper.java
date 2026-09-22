package com.ruoyi.workorder.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.workorder.domain.model.WorkOrderAssignment;

public interface WorkOrderAssignmentMapper
{
    int insert(WorkOrderAssignment assignment);
    int closeActive(@Param("orderId") Long orderId, @Param("endedAt") Date endedAt,
            @Param("reason") String reason);
    int markAccepted(@Param("orderId") Long orderId, @Param("engineerId") Long engineerId,
            @Param("acceptedAt") Date acceptedAt);
}
