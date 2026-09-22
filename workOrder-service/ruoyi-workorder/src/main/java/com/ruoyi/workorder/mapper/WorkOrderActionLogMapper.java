package com.ruoyi.workorder.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;

public interface WorkOrderActionLogMapper
{
    int insert(WorkOrderActionLog log);
    WorkOrderActionLog selectByIdempotency(@Param("operatorId") Long operatorId,
            @Param("actionType") String actionType, @Param("idempotencyKey") String idempotencyKey);
    List<WorkOrderActionLog> selectByOrderId(Long orderId);
}
