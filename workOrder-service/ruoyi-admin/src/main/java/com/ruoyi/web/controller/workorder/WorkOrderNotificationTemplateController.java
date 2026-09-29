package com.ruoyi.web.controller.workorder;

import java.util.List;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workorder.application.model.WorkOrderNotificationTemplateQuery;
import com.ruoyi.workorder.application.model.WorkOrderNotificationTemplateSaveRequest;
import com.ruoyi.workorder.application.service.WorkOrderNotificationTemplateService;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 消息模板配置 HTTP 适配器。 */
@RestController
@RequestMapping("/workorder/notification-templates")
public class WorkOrderNotificationTemplateController extends BaseController
{
    private final WorkOrderNotificationTemplateService templateService;

    public WorkOrderNotificationTemplateController(WorkOrderNotificationTemplateService templateService)
    {
        this.templateService = templateService;
    }

    @PreAuthorize("@ss.hasPermi('workorder:message:template')")
    @GetMapping
    public TableDataInfo list(WorkOrderNotificationTemplateQuery query)
    {
        startPage();
        List<WorkOrderNotificationTemplate> rows = templateService.list(query);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('workorder:message:template')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(templateService.get(id)); }

    @PreAuthorize("@ss.hasPermi('workorder:message:template')")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody WorkOrderNotificationTemplateSaveRequest request)
    {
        return success(templateService.preview(request));
    }

    @PreAuthorize("@ss.hasPermi('workorder:message:template')")
    @Log(title = "消息模板", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @RequestBody WorkOrderNotificationTemplateSaveRequest request)
    {
        return success(templateService.update(id, request, getUsername()));
    }
}
