package com.ruoyi.web.controller.workorder;

import java.util.List;
import javax.validation.Valid;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workorder.application.model.WorkOrderSlaRuleQuery;
import com.ruoyi.workorder.application.model.WorkOrderSlaRuleSaveRequest;
import com.ruoyi.workorder.application.service.WorkOrderSlaRuleService;
import com.ruoyi.workorder.domain.model.WorkOrderSlaRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workorder/sla-rules")
public class WorkOrderSlaRuleController extends BaseController
{
    private final WorkOrderSlaRuleService ruleService;

    public WorkOrderSlaRuleController(WorkOrderSlaRuleService ruleService)
    {
        this.ruleService = ruleService;
    }

    @PreAuthorize("@ss.hasPermi('workorder:sla:list')")
    @GetMapping
    public TableDataInfo list(WorkOrderSlaRuleQuery query)
    {
        startPage();
        List<WorkOrderSlaRule> rows = ruleService.selectList(query);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('workorder:sla:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(ruleService.get(id)); }

    @PreAuthorize("@ss.hasPermi('workorder:sla:add')")
    @Log(title = "SLA规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@Valid @RequestBody WorkOrderSlaRuleSaveRequest request)
    {
        return success(ruleService.create(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:sla:edit')")
    @Log(title = "SLA规则", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @Valid @RequestBody WorkOrderSlaRuleSaveRequest request)
    {
        return success(ruleService.update(id, request, getUsername()));
    }
}
