package com.ruoyi.web.controller.workorder;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.workorder.application.model.WorkOrderAnalyticsQuery;
import com.ruoyi.workorder.application.service.WorkOrderAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PC 运营驾驶舱与维修绩效 HTTP 适配器。 */
@RestController
@RequestMapping("/workorder")
public class WorkOrderAnalyticsController extends BaseController
{
    private final WorkOrderAnalyticsService analyticsService;

    public WorkOrderAnalyticsController(WorkOrderAnalyticsService analyticsService)
    {
        this.analyticsService = analyticsService;
    }

    @PreAuthorize("@ss.hasPermi('workorder:dashboard:view')")
    @GetMapping("/dashboard")
    public AjaxResult dashboard(WorkOrderAnalyticsQuery query)
    {
        return success(analyticsService.dashboard(query));
    }

    @PreAuthorize("@ss.hasPermi('workorder:performance:view')")
    @GetMapping("/performance")
    public AjaxResult performance(WorkOrderAnalyticsQuery query)
    {
        return success(analyticsService.performance(query));
    }
}
