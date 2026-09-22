package com.ruoyi.web.controller.workorder;

import java.util.List;
import javax.validation.Valid;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workorder.application.model.CreateWorkOrderCommand;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderQuery;
import com.ruoyi.workorder.application.WorkOrderCommandGateway;
import com.ruoyi.workorder.application.model.AssessmentRequest;
import com.ruoyi.workorder.application.model.AssignmentRequest;
import com.ruoyi.workorder.application.model.CancellationRequest;
import com.ruoyi.workorder.application.model.EvaluationRequest;
import com.ruoyi.workorder.application.model.ProcessRequest;
import com.ruoyi.workorder.application.model.ReturnRequest;
import com.ruoyi.workorder.application.model.VersionedRequest;
import com.ruoyi.workorder.application.model.WorkOrderCommand;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.application.service.WorkOrderApplicationService;
import com.ruoyi.workorder.application.service.WorkOrderQueryService;
import com.ruoyi.workorder.domain.model.WorkOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workorder/orders")
public class WorkOrderController extends BaseController
{
    @Autowired private WorkOrderApplicationService applicationService;
    @Autowired private WorkOrderQueryService queryService;
    @Autowired private WorkOrderActorFactory actorFactory;
    @Autowired private WorkOrderCommandGateway commandGateway;

    @PreAuthorize("@ss.hasPermi('workorder:order:add')")
    @Log(title = "工单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@Valid @RequestBody CreateWorkOrderCommand command,
            @RequestHeader("Idempotency-Key") String idempotencyKey)
    {
        return success(applicationService.create(command, actorFactory.current(), idempotencyKey));
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:query')")
    @GetMapping("/my")
    public TableDataInfo my(WorkOrderQuery query)
    {
        startPage();
        List<WorkOrder> rows = queryService.selectMyList(query, getUserId());
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        WorkOrderActor actor = actorFactory.current();
        return success(queryService.getVisibleDetail(id, actor));
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:assign')")
    @Log(title = "工单派单", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/assign")
    public AjaxResult assign(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return AjaxResult.success(commandGateway.execute(WorkOrderCommand.assignment(id,
                WorkOrderAction.ASSIGN, key, request.getVersion(), request.getEngineerId(), request.getReason()),
                actorFactory.current()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:reassign')")
    @Log(title = "工单改派", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reassign")
    public AjaxResult reassign(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return AjaxResult.success(commandGateway.execute(WorkOrderCommand.assignment(id,
                WorkOrderAction.REASSIGN, key, request.getVersion(), request.getEngineerId(), request.getReason()),
                actorFactory.current()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:accept')")
    @Log(title = "工单接单", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/accept")
    public AjaxResult accept(@PathVariable Long id, @Valid @RequestBody VersionedRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return executeSimple(id, WorkOrderAction.ACCEPT, key, request.getVersion());
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:arrive')")
    @Log(title = "工单到场", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/arrive")
    public AjaxResult arrive(@PathVariable Long id, @Valid @RequestBody ProcessRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return executeProcess(id, WorkOrderAction.ARRIVE, key, request);
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:assess')")
    @Log(title = "工单评估", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/assessment")
    public AjaxResult assessment(@PathVariable Long id, @Valid @RequestBody AssessmentRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        WorkOrderCommand command = WorkOrderCommand.assessment(id, key, request.getVersion(), request.getContent(),
                request.getRequiresParts(), request.getPartsDescription(), request.getAssessedHours(),
                request.getAssessedUrgency(), request.getAssessedScope(), request.getRequiresExtension());
        return AjaxResult.success(commandGateway.execute(command, actorFactory.current()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:progress')")
    @Log(title = "工单进度", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/progress")
    public AjaxResult progress(@PathVariable Long id, @Valid @RequestBody ProcessRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return executeProcess(id, WorkOrderAction.PROGRESS, key, request);
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:finish')")
    @Log(title = "工单完工", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/finish")
    public AjaxResult finish(@PathVariable Long id, @Valid @RequestBody ProcessRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return executeProcess(id, WorkOrderAction.FINISH, key, request);
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:cancel')")
    @Log(title = "工单取消", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id, @Valid @RequestBody CancellationRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return AjaxResult.success(commandGateway.execute(WorkOrderCommand.reasoned(id,
                WorkOrderAction.CANCEL, key, request.getVersion(), request.getReason()), actorFactory.current()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:confirm')")
    @Log(title = "工单确认完工", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable Long id, @Valid @RequestBody VersionedRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return executeSimple(id, WorkOrderAction.CONFIRM, key, request.getVersion());
    }

    @PreAuthorize("@ss.hasPermi('workorder:order:return')")
    @Log(title = "工单退回返工", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/return")
    public AjaxResult returnForRework(@PathVariable Long id, @Valid @RequestBody ReturnRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        return AjaxResult.success(commandGateway.execute(WorkOrderCommand.reasoned(id,
                WorkOrderAction.RETURN, key, request.getVersion(), request.getReason()), actorFactory.current()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:evaluation:add')")
    @Log(title = "工单评价", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/evaluation")
    public AjaxResult evaluate(@PathVariable Long id, @Valid @RequestBody EvaluationRequest request,
            @RequestHeader("Idempotency-Key") String key)
    {
        WorkOrderCommand command = WorkOrderCommand.evaluation(id, key, request.getVersion(),
                request.getOverallScore(), request.getResponseScore(), request.getQualityScore(),
                request.getAttitudeScore(), request.getEvaluationContent());
        return AjaxResult.success(commandGateway.execute(command, actorFactory.current()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:evaluation:query')")
    @GetMapping("/{id}/evaluation")
    public AjaxResult getEvaluation(@PathVariable Long id)
    {
        return success(queryService.getVisibleEvaluation(id, actorFactory.current()));
    }

    private AjaxResult executeSimple(Long id, WorkOrderAction action, String key, Integer version)
    {
        return AjaxResult.success(commandGateway.execute(WorkOrderCommand.simple(id, action, key, version),
                actorFactory.current()));
    }

    private AjaxResult executeProcess(Long id, WorkOrderAction action, String key, ProcessRequest request)
    {
        WorkOrderCommand command = WorkOrderCommand.process(id, action, key, request.getVersion(),
                request.getContent(), request.getAttachmentIds());
        return AjaxResult.success(commandGateway.execute(command, actorFactory.current()));
    }
}
