package com.ruoyi.web.controller.workorder;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.workorder.application.service.WorkOrderCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workorder/categories")
public class WorkOrderCategoryController extends BaseController
{
    @Autowired private WorkOrderCategoryService categoryService;

    @PreAuthorize("@ss.hasAnyPermi('workorder:order:add,workorder:category:list')")
    @GetMapping
    public AjaxResult list()
    {
        return success(categoryService.selectActiveList());
    }
}
