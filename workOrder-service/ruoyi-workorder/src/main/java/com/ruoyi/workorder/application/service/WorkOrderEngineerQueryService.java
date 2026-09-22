package com.ruoyi.workorder.application.service;

import java.util.List;
import java.util.Date;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.EngineerStatusUpdateRequest;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderEngineerQuery;
import com.ruoyi.workorder.domain.model.WorkOrderEngineer;
import com.ruoyi.workorder.mapper.WorkOrderEngineerMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderEngineerQueryService
{
    private final WorkOrderEngineerMapper engineerMapper;
    public WorkOrderEngineerQueryService(WorkOrderEngineerMapper engineerMapper) { this.engineerMapper = engineerMapper; }
    public List<WorkOrderEngineer> selectList(WorkOrderEngineerQuery query)
    {
        return engineerMapper.selectList(query == null ? new WorkOrderEngineerQuery() : query);
    }

    public WorkOrderEngineer updateOwnStatus(EngineerStatusUpdateRequest request, WorkOrderActor actor)
    {
        WorkOrderEngineer engineer = engineerMapper.selectById(actor.getUserId());
        if (engineer == null)
            throw new ServiceException("当前账号不是有效的维修人员", HttpStatus.FORBIDDEN);
        if ("OFF_DUTY".equals(request.getDutyStatus()) && engineer.getLoad() != null && engineer.getLoad() > 0)
            throw new ServiceException("仍有在办工单，不能切换为离线", HttpStatus.CONFLICT);

        Date now = new Date();
        engineerMapper.upsertStatus(actor.getUserId(), actor.getDisplayName(), actor.getDeptId(),
                request.getDutyStatus(), request.getWorkStatus(), now);
        return engineerMapper.selectById(actor.getUserId());
    }
}
