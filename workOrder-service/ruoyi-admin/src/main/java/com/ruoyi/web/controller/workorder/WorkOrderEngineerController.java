package com.ruoyi.web.controller.workorder;

import java.util.List;
import javax.validation.Valid;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workorder.application.model.EngineerStatusUpdateRequest;
import com.ruoyi.workorder.application.model.WorkOrderQuery;
import com.ruoyi.workorder.application.service.WorkOrderEngineerQueryService;
import com.ruoyi.workorder.application.service.WorkOrderQueryService;
import com.ruoyi.workorder.domain.model.WorkOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workorder/engineer")
public class WorkOrderEngineerController extends BaseController
{
    @Autowired private WorkOrderQueryService queryService;
    @Autowired private WorkOrderEngineerQueryService engineerService;
    @Autowired private WorkOrderActorFactory actorFactory;

    @PreAuthorize("@ss.hasPermi('workorder:order:assigned:list')")
    @GetMapping("/orders")
    public TableDataInfo list(WorkOrderQuery query)
    {
        startPage();
        List<WorkOrder> rows = queryService.selectAssignedList(query, getUserId());
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('workorder:engineer:status')")
    @Log(title = "维修人员状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public AjaxResult updateStatus(@Valid @RequestBody EngineerStatusUpdateRequest request)
    {
        return success(engineerService.updateOwnStatus(request, actorFactory.current()));
    }
}
