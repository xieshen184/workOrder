package com.ruoyi.workorder.application.service;

import java.util.List;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderQuery;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderEvaluation;
import com.ruoyi.workorder.domain.service.WorkOrderAllowedActionResolver;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import com.ruoyi.workorder.mapper.WorkOrderAttachmentMapper;
import com.ruoyi.workorder.mapper.WorkOrderEvaluationMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 统一工单查询和可见性校验。
 * 报修人、维修人按本人字段过滤；后台查询额外复用若依部门数据范围切面。
 */
@Service
public class WorkOrderQueryService
{
    @Autowired private WorkOrderMapper orderMapper;
    @Autowired private WorkOrderAttachmentMapper attachmentMapper;
    @Autowired private WorkOrderActionLogMapper actionLogMapper;
    @Autowired private WorkOrderEvaluationMapper evaluationMapper;
    private final WorkOrderAllowedActionResolver actionResolver = new WorkOrderAllowedActionResolver();

    public List<WorkOrder> selectMyList(WorkOrderQuery query, Long applicantId)
    {
        query.setApplicantId(applicantId);
        query.setCurrentAssigneeId(null);
        return orderMapper.selectList(query);
    }

    public List<WorkOrder> selectAssignedList(WorkOrderQuery query, Long assigneeId)
    {
        query.setApplicantId(null);
        query.setCurrentAssigneeId(assigneeId);
        return orderMapper.selectList(query);
    }

    @DataScope(deptAlias = "d", userAlias = "u", permission = "workorder:order:list")
    public List<WorkOrder> selectAdminList(WorkOrderQuery query)
    {
        return orderMapper.selectList(query);
    }

    @DataScope(deptAlias = "d", userAlias = "u", permission = "workorder:order:list")
    public WorkOrder selectScopedById(WorkOrderQuery query)
    {
        return orderMapper.selectScopedById(query);
    }

    @DataScope(deptAlias = "d", userAlias = "u", permission = "workorder:evaluation:query")
    public WorkOrder selectEvaluationScopedById(WorkOrderQuery query)
    {
        return orderMapper.selectScopedById(query);
    }

    public WorkOrder getVisibleDetail(Long id, WorkOrderActor actor)
    {
        WorkOrder order = requireVisibleOrder(id, actor);

        // 详情只暴露受鉴权下载入口，绝不返回私有存储 objectKey。
        order.setAttachments(attachmentMapper.selectByOrderId(id));
        if (order.getAttachments() != null)
        {
            order.getAttachments().forEach(item -> item.setDownloadUrl("/workorder/attachments/" + item.getId()));
        }
        order.setTimeline(actionLogMapper.selectByOrderId(id));
        if (canViewEvaluation(order, actor)) order.setEvaluation(evaluationMapper.selectByOrderId(id));
        order.setAllowedActions(actionResolver.resolve(order, actor));
        return order;
    }

    public void assertVisible(Long orderId, WorkOrderActor actor)
    {
        // 命令入口只做可见性判断，避免为每次状态写操作加载附件、时间线和评价。
        requireVisibleOrder(orderId, actor);
    }

    public WorkOrderEvaluation getVisibleEvaluation(Long orderId, WorkOrderActor actor)
    {
        WorkOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new ServiceException("工单不存在", HttpStatus.NOT_FOUND);
        if (!actor.getUserId().equals(order.getApplicantId()))
        {
            if (!actor.hasPermission("workorder:evaluation:query"))
                throw new ServiceException("无权查看该工单评价", HttpStatus.FORBIDDEN);
            WorkOrderQuery query = new WorkOrderQuery();
            query.setId(orderId);
            if (selectEvaluationScopedByIdThroughProxy(query) == null)
                throw new ServiceException("无权查看该工单评价", HttpStatus.FORBIDDEN);
        }
        return evaluationMapper.selectByOrderId(orderId);
    }

    private WorkOrder requireVisibleOrder(Long id, WorkOrderActor actor)
    {
        WorkOrder order = orderMapper.selectById(id);
        if (order == null) throw new ServiceException("工单不存在", HttpStatus.NOT_FOUND);

        // 本人/当前处理人直接可见；后台用户必须同时具备查询权限并命中 DataScope。
        boolean direct = actor.getUserId().equals(order.getApplicantId())
                || actor.getUserId().equals(order.getCurrentAssigneeId());
        if (direct) return order;
        if (!actor.hasPermission("workorder:order:list"))
            throw new ServiceException("无权访问该工单", HttpStatus.FORBIDDEN);
        WorkOrderQuery query = new WorkOrderQuery();
        query.setId(id);
        order = selectScopedByIdThroughProxy(query);
        if (order == null) throw new ServiceException("无权访问该工单", HttpStatus.FORBIDDEN);
        return order;
    }

    private boolean canViewEvaluation(WorkOrder order, WorkOrderActor actor)
    {
        if (actor.getUserId().equals(order.getApplicantId())) return true;
        if (!actor.hasPermission("workorder:evaluation:query")) return false;
        WorkOrderQuery query = new WorkOrderQuery();
        query.setId(order.getId());
        return selectEvaluationScopedByIdThroughProxy(query) != null;
    }

    private WorkOrder selectScopedByIdThroughProxy(WorkOrderQuery query)
    {
        // 自调用不会触发 AOP，必须通过当前代理执行 @DataScope。
        return com.ruoyi.common.utils.spring.SpringUtils.getAopProxy(this).selectScopedById(query);
    }

    private WorkOrder selectEvaluationScopedByIdThroughProxy(WorkOrderQuery query)
    {
        return com.ruoyi.common.utils.spring.SpringUtils.getAopProxy(this).selectEvaluationScopedById(query);
    }
}
