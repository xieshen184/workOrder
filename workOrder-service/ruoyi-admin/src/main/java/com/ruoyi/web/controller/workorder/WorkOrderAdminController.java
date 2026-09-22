package com.ruoyi.web.controller.workorder;

import java.util.List;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.workorder.application.service.WorkOrderEngineerQueryService;
import com.ruoyi.workorder.application.model.WorkOrderQuery;
import com.ruoyi.workorder.application.model.WorkOrderEngineerQuery;
import com.ruoyi.workorder.application.service.WorkOrderQueryService;
import com.ruoyi.workorder.domain.model.WorkOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workorder/admin")
public class WorkOrderAdminController extends BaseController
{
    @Autowired private WorkOrderQueryService queryService;
    @Autowired private WorkOrderEngineerQueryService engineerQueryService;

    @PreAuthorize("@ss.hasPermi('workorder:order:list')")
    @GetMapping("/orders")
    public TableDataInfo list(WorkOrderQuery query)
    {
        startPage();
        List<WorkOrder> rows = queryService.selectAdminList(query);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('workorder:engineer:list')")
    @GetMapping("/engineers")
    public AjaxResult engineers(WorkOrderEngineerQuery query)
    {
        return AjaxResult.success(engineerQueryService.selectList(query));
    }
}
