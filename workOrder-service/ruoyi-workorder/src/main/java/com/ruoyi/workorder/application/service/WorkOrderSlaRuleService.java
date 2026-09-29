package com.ruoyi.workorder.application.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderSlaRuleQuery;
import com.ruoyi.workorder.application.model.WorkOrderSlaRuleSaveRequest;
import com.ruoyi.workorder.domain.model.WorkOrderCategory;
import com.ruoyi.workorder.domain.model.WorkOrderSlaRule;
import com.ruoyi.workorder.mapper.WorkOrderCategoryMapper;
import com.ruoyi.workorder.mapper.WorkOrderSlaRuleMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SLA 规则管理模块。其小接口隐藏编码不可变、分类有效性、时段冲突和历史快照保护规则。
 */
@Service
public class WorkOrderSlaRuleService
{
    private final WorkOrderSlaRuleMapper ruleMapper;
    private final WorkOrderCategoryMapper categoryMapper;

    public WorkOrderSlaRuleService(WorkOrderSlaRuleMapper ruleMapper, WorkOrderCategoryMapper categoryMapper)
    {
        this.ruleMapper = ruleMapper;
        this.categoryMapper = categoryMapper;
    }

    public List<WorkOrderSlaRule> selectList(WorkOrderSlaRuleQuery query)
    {
        return ruleMapper.selectList(query == null ? new WorkOrderSlaRuleQuery() : query);
    }

    public WorkOrderSlaRule get(Long id)
    {
        WorkOrderSlaRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new ServiceException("SLA规则不存在", HttpStatus.NOT_FOUND);
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrderSlaRule create(WorkOrderSlaRuleSaveRequest request, String username)
    {
        if (ruleMapper.selectByCode(request.getRuleCode()) != null)
            throw new ServiceException("SLA规则编码已存在", HttpStatus.CONFLICT);
        validate(null, request);
        WorkOrderSlaRule rule = from(request);
        rule.setCreateBy(username); rule.setCreateTime(new Date());
        try
        {
            ruleMapper.insert(rule);
        }
        catch (DuplicateKeyException duplicate)
        {
            throw new ServiceException("SLA规则编码已存在", HttpStatus.CONFLICT);
        }
        return get(rule.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrderSlaRule update(Long id, WorkOrderSlaRuleSaveRequest request, String username)
    {
        WorkOrderSlaRule existing = get(id);
        // 规则编码会出现在审计和运营筛选中，创建后保持不可变；历史工单只保留已计算的截止时间。
        if (!existing.getRuleCode().equals(request.getRuleCode()))
            throw new ServiceException("SLA规则编码创建后不能修改", HttpStatus.CONFLICT);
        validate(id, request);
        WorkOrderSlaRule rule = from(request);
        rule.setId(id); rule.setUpdateBy(username); rule.setUpdateTime(new Date());
        if (ruleMapper.update(rule) != 1)
            throw new ServiceException("SLA规则已被删除或发生变化", HttpStatus.CONFLICT);
        return get(id);
    }

    private void validate(Long id, WorkOrderSlaRuleSaveRequest request)
    {
        if (request.getEffectiveTo() != null && !request.getEffectiveTo().after(request.getEffectiveFrom()))
            throw new ServiceException("失效时间必须晚于生效时间", HttpStatus.BAD_REQUEST);
        if (request.getCategoryId() != null)
        {
            WorkOrderCategory category = categoryMapper.selectById(request.getCategoryId());
            if (category == null)
                throw new ServiceException("适用分类不存在", HttpStatus.BAD_REQUEST);
        }
        if (request.getFinishMinutes() != null
                && request.getReminderBeforeMin() > request.getFinishMinutes())
            throw new ServiceException("预警提前量不能大于完成时限", HttpStatus.BAD_REQUEST);
        // 停用规则不参与匹配，也不应占用有效期；重新启用时再执行重叠校验。
        if ("0".equals(request.getStatus())
                && ruleMapper.countOverlapping(id, request.getCategoryId(), request.getUrgencyLevel(),
                        request.getPriority(), request.getEffectiveFrom(), request.getEffectiveTo()) > 0)
            throw new ServiceException("相同范围、紧急度和优先级的规则生效时间不能重叠", HttpStatus.CONFLICT);
    }

    private WorkOrderSlaRule from(WorkOrderSlaRuleSaveRequest request)
    {
        WorkOrderSlaRule rule = new WorkOrderSlaRule();
        rule.setRuleCode(request.getRuleCode().trim()); rule.setRuleName(request.getRuleName().trim());
        rule.setCategoryId(request.getCategoryId()); rule.setUrgencyLevel(request.getUrgencyLevel());
        rule.setPriority(request.getPriority()); rule.setEffectiveFrom(request.getEffectiveFrom());
        rule.setEffectiveTo(request.getEffectiveTo()); rule.setResponseMinutes(request.getResponseMinutes());
        rule.setArrivalMinutes(request.getArrivalMinutes()); rule.setFinishMinutes(request.getFinishMinutes());
        rule.setReminderBeforeMin(request.getReminderBeforeMin());
        rule.setAllowExtension(request.getAllowExtension()); rule.setStatus(request.getStatus());
        rule.setRemark(trim(request.getRemark()));
        return rule;
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
}
