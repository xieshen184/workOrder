package com.ruoyi.web.controller.workorder;

import java.util.List;
import java.util.Map;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workorder.application.model.WorkOrderNotificationQuery;
import com.ruoyi.workorder.application.service.WorkOrderNotificationQueryService;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationMessage;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 站内消息及后台发送任务 HTTP 适配器。 */
@RestController
@RequestMapping("/workorder")
public class WorkOrderNotificationController extends BaseController
{
    private final WorkOrderNotificationQueryService notificationService;

    public WorkOrderNotificationController(WorkOrderNotificationQueryService notificationService)
    {
        this.notificationService = notificationService;
    }

    @PreAuthorize("@ss.hasPermi('workorder:notification:list')")
    @GetMapping("/notifications")
    public TableDataInfo messages(WorkOrderNotificationQuery query)
    {
        startPage();
        List<WorkOrderNotificationMessage> rows = notificationService.selectMessages(getUserId(), query);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('workorder:notification:list')")
    @GetMapping("/notifications/unread-count")
    public AjaxResult unreadCount()
    {
        return success(notificationService.unreadCount(getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:notification:read')")
    @PutMapping("/notifications/{id}/read")
    public AjaxResult read(@PathVariable Long id)
    {
        notificationService.markRead(id, getUserId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('workorder:notification:read')")
    @PutMapping("/notifications/read-all")
    public AjaxResult readAll(@RequestBody(required = false) Map<String, String> body)
    {
        String category = body == null ? null : body.get("category");
        return success(notificationService.markAllRead(getUserId(), category));
    }

    @PreAuthorize("@ss.hasPermi('workorder:message:record')")
    @GetMapping("/notification-tasks")
    public TableDataInfo tasks(WorkOrderNotificationQuery query)
    {
        startPage();
        List<WorkOrderNotificationTask> rows = notificationService.selectTasks(query);
        return getDataTable(rows);
    }

    @PreAuthorize("@ss.hasPermi('workorder:message:retry')")
    @Log(title = "通知任务人工重试", businessType = BusinessType.UPDATE)
    @PutMapping("/notification-tasks/{id}/retry")
    public AjaxResult retry(@PathVariable Long id)
    {
        notificationService.retry(id);
        return success();
    }
}
