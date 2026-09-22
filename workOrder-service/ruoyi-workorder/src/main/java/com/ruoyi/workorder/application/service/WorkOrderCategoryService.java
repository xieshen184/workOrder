package com.ruoyi.workorder.application.service;

import java.util.List;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderCategorySaveRequest;
import com.ruoyi.workorder.domain.model.WorkOrderCategory;
import com.ruoyi.workorder.mapper.WorkOrderCategoryMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderCategoryService
{
    private final WorkOrderCategoryMapper categoryMapper;

    public WorkOrderCategoryService(WorkOrderCategoryMapper categoryMapper)
    {
        this.categoryMapper = categoryMapper;
    }

    public List<WorkOrderCategory> selectActiveList()
    {
        return categoryMapper.selectActiveList();
    }

    public List<WorkOrderCategory> selectManageList()
    {
        return categoryMapper.selectManageList();
    }

    public WorkOrderCategory get(Long id)
    {
        WorkOrderCategory category = categoryMapper.selectById(id);
        if (category == null) throw new ServiceException("分类不存在", HttpStatus.NOT_FOUND);
        return category;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrderCategory create(WorkOrderCategorySaveRequest request, String username)
    {
        // 先给出稳定的业务冲突提示；数据库唯一键继续承担并发创建时的最终保护。
        if (categoryMapper.selectByCode(request.getCategoryCode()) != null)
            throw new ServiceException("分类编码已存在", HttpStatus.CONFLICT);
        validateParent(null, request.getParentId());
        WorkOrderCategory category = from(request);
        category.setCreateBy(username);
        try
        {
            categoryMapper.insert(category);
        }
        catch (DuplicateKeyException duplicate)
        {
            throw new ServiceException("分类编码已存在", HttpStatus.CONFLICT);
        }
        return get(category.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrderCategory update(Long id, WorkOrderCategorySaveRequest request, String username)
    {
        WorkOrderCategory existing = get(id);
        // 编码已经进入工单快照和外部筛选条件，创建后保持不可变。
        if (!existing.getCategoryCode().equals(request.getCategoryCode()))
            throw new ServiceException("分类编码创建后不能修改", HttpStatus.CONFLICT);
        validateParent(id, request.getParentId());
        // 父级停用后正常子级将无法形成可选树，因此必须由叶子节点向上依次停用。
        if ("1".equals(request.getStatus()) && categoryMapper.countActiveChildren(id) > 0)
            throw new ServiceException("请先停用下级分类", HttpStatus.CONFLICT);

        WorkOrderCategory category = from(request);
        category.setId(id);
        category.setUpdateBy(username);
        if (categoryMapper.update(category) != 1)
            throw new ServiceException("分类已被删除或发生变化", HttpStatus.CONFLICT);
        return get(id);
    }

    private void validateParent(Long currentId, Long parentId)
    {
        if (currentId != null && currentId.equals(parentId))
            throw new ServiceException("分类不能选择自身作为父分类", HttpStatus.BAD_REQUEST);
        if (parentId == null || parentId == 0) return;
        WorkOrderCategory parent = categoryMapper.selectActiveById(parentId);
        if (parent == null) throw new ServiceException("父分类不存在或已停用", HttpStatus.BAD_REQUEST);

        // 沿父链检查，避免把当前分类挂到自己的后代节点下形成循环。
        for (int depth = 0; currentId != null && parent != null && depth < 64; depth++)
        {
            if (currentId.equals(parent.getId()))
                throw new ServiceException("父分类不能是当前分类的下级", HttpStatus.BAD_REQUEST);
            Long next = parent.getParentId();
            parent = next == null || next == 0 ? null : categoryMapper.selectById(next);
        }
    }

    private WorkOrderCategory from(WorkOrderCategorySaveRequest request)
    {
        // 请求对象不直接进入持久层，集中完成规范化，避免控制器和 Mapper 各自修剪字符串。
        WorkOrderCategory category = new WorkOrderCategory();
        category.setParentId(request.getParentId());
        category.setCategoryCode(request.getCategoryCode().trim());
        category.setCategoryName(request.getCategoryName().trim());
        category.setManagerDeptId(request.getManagerDeptId());
        category.setOrderNum(request.getOrderNum());
        category.setStatus(request.getStatus());
        category.setRemark(trim(request.getRemark()));
        return category;
    }

    private String trim(String value)
    {
        return value == null ? null : value.trim();
    }
}
