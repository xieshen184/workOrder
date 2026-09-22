package com.ruoyi.workorder.mapper;

import java.util.List;
import java.util.Date;
import com.ruoyi.workorder.application.model.WorkOrderEngineerQuery;
import com.ruoyi.workorder.domain.model.WorkOrderEngineer;
import org.apache.ibatis.annotations.Param;

public interface WorkOrderEngineerMapper
{
    WorkOrderEngineer selectById(Long id);
    WorkOrderEngineer selectEligibleById(Long id);
    List<WorkOrderEngineer> selectList(WorkOrderEngineerQuery query);
    int upsertStatus(@Param("engineerId") Long engineerId, @Param("engineerName") String engineerName,
            @Param("engineerDeptId") Long engineerDeptId, @Param("dutyStatus") String dutyStatus,
            @Param("workStatus") String workStatus, @Param("changedAt") Date changedAt);
}
