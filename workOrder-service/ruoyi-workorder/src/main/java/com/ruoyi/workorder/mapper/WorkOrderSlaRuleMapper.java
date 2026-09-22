package com.ruoyi.workorder.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.workorder.domain.model.WorkOrderSlaRule;

public interface WorkOrderSlaRuleMapper
{
    WorkOrderSlaRule selectEffective(@Param("categoryId") Long categoryId,
            @Param("urgencyLevel") Integer urgencyLevel, @Param("effectiveAt") Date effectiveAt);
}
