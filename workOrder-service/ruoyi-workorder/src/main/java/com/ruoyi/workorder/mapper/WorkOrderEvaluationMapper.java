package com.ruoyi.workorder.mapper;

import com.ruoyi.workorder.domain.model.WorkOrderEvaluation;

public interface WorkOrderEvaluationMapper
{
    int insert(WorkOrderEvaluation evaluation);
    WorkOrderEvaluation selectByOrderId(Long orderId);
}
