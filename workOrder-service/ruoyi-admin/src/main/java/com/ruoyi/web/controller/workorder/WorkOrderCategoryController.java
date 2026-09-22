package com.ruoyi.web.controller.workorder;

import javax.validation.Valid;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.workorder.application.model.WorkOrderCategorySaveRequest;
import com.ruoyi.workorder.application.service.WorkOrderCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PreAuthorize("@ss.hasPermi('workorder:category:list')")
    @GetMapping("/manage")
    public AjaxResult manageList()
    {
        return success(categoryService.selectManageList());
    }

    @PreAuthorize("@ss.hasPermi('workorder:category:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return success(categoryService.get(id));
    }

    @PreAuthorize("@ss.hasPermi('workorder:category:add')")
    @Log(title = "工单分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@Valid @RequestBody WorkOrderCategorySaveRequest request)
    {
        return success(categoryService.create(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('workorder:category:edit')")
    @Log(title = "工单分类", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @Valid @RequestBody WorkOrderCategorySaveRequest request)
    {
        return success(categoryService.update(id, request, getUsername()));
    }
}
