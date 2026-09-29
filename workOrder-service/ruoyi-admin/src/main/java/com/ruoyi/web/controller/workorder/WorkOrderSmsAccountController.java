package com.ruoyi.web.controller.workorder;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workorder.application.model.WorkOrderSmsAccountSettingRequest;
import com.ruoyi.workorder.application.service.WorkOrderSmsAccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 短信账户安全运营 HTTP 适配器。 */
@RestController
@RequestMapping("/workorder/sms-account")
public class WorkOrderSmsAccountController extends BaseController
{
    private final WorkOrderSmsAccountService accountService;

    public WorkOrderSmsAccountController(WorkOrderSmsAccountService accountService)
    {
        this.accountService = accountService;
    }

    @PreAuthorize("@ss.hasPermi('workorder:sms:view')")
    @GetMapping
    public AjaxResult get() { return success(accountService.get()); }

    @PreAuthorize("@ss.hasPermi('workorder:sms:edit')")
    @Log(title = "短信账户设置", businessType = BusinessType.UPDATE)
    @PutMapping("/settings")
    public AjaxResult settings(@RequestBody WorkOrderSmsAccountSettingRequest request)
    {
        return success(accountService.updateSettings(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:sms:refresh')")
    @Log(title = "短信账户余额刷新", businessType = BusinessType.UPDATE)
    @PutMapping("/refresh")
    public AjaxResult refresh() { return success(accountService.refresh()); }
}
