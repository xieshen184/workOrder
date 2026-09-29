package com.ruoyi.workorder.mapper;

import java.util.List;
import java.util.Date;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.workorder.application.model.WorkOrderQuery;
import com.ruoyi.workorder.domain.model.WorkOrder;

public interface WorkOrderMapper
{
    int insert(WorkOrder order);
    WorkOrder selectById(Long id);
    WorkOrder selectByIdForUpdate(Long id);
    WorkOrder selectByOrderNo(String orderNo);
    WorkOrder selectScopedById(WorkOrderQuery query);
    List<WorkOrder> selectList(WorkOrderQuery query);
    List<WorkOrder> selectSlaOpenOrders();
    List<WorkOrder> selectAutoCloseCandidates(@Param("confirmedBefore") Date confirmedBefore);
    int applyCommand(@Param("id") Long id, @Param("sourceStatus") String sourceStatus,
            @Param("targetStatus") String targetStatus, @Param("version") Integer version,
            @Param("actionType") String actionType, @Param("engineerId") Long engineerId,
            @Param("engineerName") String engineerName, @Param("actionTime") Date actionTime,
            @Param("updateBy") String updateBy);
    int updateSlaFlags(@Param("id") Long id, @Param("status") String status,
            @Param("warningFlag") String warningFlag,
            @Param("overdueFlag") String overdueFlag, @Param("responseOverdue") String responseOverdue,
            @Param("arrivalOverdue") String arrivalOverdue, @Param("finishOverdue") String finishOverdue,
            @Param("updateTime") Date updateTime);
    int autoClose(@Param("id") Long id, @Param("version") Integer version,
            @Param("closedAt") Date closedAt);
}
