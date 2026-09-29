package com.ruoyi.workorder.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.workorder.application.model.WorkOrderSlaRuleQuery;
import com.ruoyi.workorder.domain.model.WorkOrderSlaRule;

public interface WorkOrderSlaRuleMapper
{
    WorkOrderSlaRule selectEffective(@Param("categoryId") Long categoryId,
            @Param("urgencyLevel") Integer urgencyLevel, @Param("effectiveAt") Date effectiveAt);
    WorkOrderSlaRule selectById(Long id);
    WorkOrderSlaRule selectByCode(String ruleCode);
    List<WorkOrderSlaRule> selectList(WorkOrderSlaRuleQuery query);
    int countOverlapping(@Param("excludeId") Long excludeId, @Param("categoryId") Long categoryId,
            @Param("urgencyLevel") Integer urgencyLevel, @Param("priority") Integer priority,
            @Param("effectiveFrom") Date effectiveFrom, @Param("effectiveTo") Date effectiveTo);
    int insert(WorkOrderSlaRule rule);
    int update(WorkOrderSlaRule rule);
}
