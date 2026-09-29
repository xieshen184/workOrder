package com.ruoyi.web.controller.workorder;

import java.util.List;
import javax.validation.Valid;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderDelayDecisionRequest;
import com.ruoyi.workorder.application.model.WorkOrderDelayRequestCreateRequest;
import com.ruoyi.workorder.application.model.WorkOrderDelayRequestQuery;
import com.ruoyi.workorder.application.service.WorkOrderDelayService;
import com.ruoyi.workorder.domain.model.WorkOrderDelayRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * F01 延期申请 HTTP 适配层。
 *
 * <p>Controller 只负责路由、权限、参数校验和 RuoYi 响应包装；延期状态机、行锁、
 * 附件绑定及审计日志均由 {@link WorkOrderDelayService} 统一完成。</p>
 */
@RestController
@RequestMapping("/workorder")
public class WorkOrderDelayController extends BaseController
{
    private final WorkOrderDelayService delayService;
    private final WorkOrderActorFactory actorFactory;

    public WorkOrderDelayController(WorkOrderDelayService delayService, WorkOrderActorFactory actorFactory)
    {
        this.delayService = delayService;
        this.actorFactory = actorFactory;
    }

    @PreAuthorize("@ss.hasPermi('workorder:delay:add')")
    @Log(title = "工单延期申请", businessType = BusinessType.INSERT)
    @PostMapping("/orders/{id}/delay-requests")
    public AjaxResult create(@PathVariable Long id,
            @Valid @RequestBody WorkOrderDelayRequestCreateRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey)
    {
        return success(delayService.create(id, request, actorFactory.current(), idempotencyKey));
    }

    @PreAuthorize("@ss.hasAnyPermi('workorder:delay:add,workorder:delay:query,workorder:delay:list')")
    @GetMapping("/orders/{id}/delay-requests/latest")
    public AjaxResult latest(@PathVariable Long id)
    {
        return success(delayService.getLatest(id, actorFactory.current()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:delay:list')")
    @GetMapping("/admin/delay-requests")
    public TableDataInfo list(WorkOrderDelayRequestQuery query)
    {
        startPage();
        List<WorkOrderDelayRequest> rows = delayService.selectList(query);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('workorder:delay:approve')")
    @Log(title = "同意工单延期", businessType = BusinessType.UPDATE)
    @PostMapping("/delay-requests/{id}/approve")
    public AjaxResult approve(@PathVariable Long id,
            @Valid @RequestBody(required = false) WorkOrderDelayDecisionRequest request)
    {
        return success(delayService.approve(id, request, actorFactory.current()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:delay:approve')")
    @Log(title = "拒绝工单延期", businessType = BusinessType.UPDATE)
    @PostMapping("/delay-requests/{id}/reject")
    public AjaxResult reject(@PathVariable Long id,
            @Valid @RequestBody(required = false) WorkOrderDelayDecisionRequest request)
    {
        return success(delayService.reject(id, request, actorFactory.current()));
    }
}
