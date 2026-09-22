package com.ruoyi.workorder.mapper;

import java.util.List;
import com.ruoyi.workorder.domain.model.WorkOrderCategory;

public interface WorkOrderCategoryMapper
{
    WorkOrderCategory selectById(Long id);
    WorkOrderCategory selectActiveById(Long id);
    WorkOrderCategory selectByCode(String code);
    List<WorkOrderCategory> selectActiveList();
    List<WorkOrderCategory> selectManageList();
    int countActiveChildren(Long id);
    int insert(WorkOrderCategory category);
    int update(WorkOrderCategory category);
}
