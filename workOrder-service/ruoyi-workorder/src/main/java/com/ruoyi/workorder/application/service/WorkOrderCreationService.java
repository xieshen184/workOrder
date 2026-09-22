package com.ruoyi.workorder.application.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.CreateWorkOrderCommand;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.domain.model.WorkOrderCategory;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;
import com.ruoyi.workorder.domain.model.WorkOrderSlaRule;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import com.ruoyi.workorder.mapper.WorkOrderAttachmentMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import com.ruoyi.workorder.mapper.WorkOrderSlaRuleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在单个数据库事务内创建工单、绑定预上传附件并写入首条审计日志。 */
@Service
public class WorkOrderCreationService
{
    @Autowired private WorkOrderMapper orderMapper;
    @Autowired private WorkOrderSlaRuleMapper slaRuleMapper;
    @Autowired private WorkOrderAttachmentMapper attachmentMapper;
    @Autowired private WorkOrderActionLogMapper actionLogMapper;

    /**
     * 创建全新工单。调用方必须先完成参数、身份、分类和幂等校验。
     * 附件绑定数量必须与请求中的去重 ID 数一致，否则回滚工单和日志，避免半绑定数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder createNew(CreateWorkOrderCommand command, WorkOrderActor actor,
            WorkOrderCategory category, String idempotencyKey, String orderNo)
    {
        Date now = new Date();
        WorkOrderSlaRule slaRule = slaRuleMapper.selectEffective(category.getId(), command.getUrgencyLevel(), now);
        if (slaRule == null)
        {
            throw new ServiceException("当前分类和紧急程度未配置生效的SLA规则", HttpStatus.CONFLICT);
        }
        WorkOrder order = new WorkOrder();
        order.setOrderNo(orderNo);
        order.setTitle(trimToNull(command.getTitle()) == null ? category.getCategoryName() + "报修" : command.getTitle().trim());
        order.setCategoryId(category.getId());
        order.setCategoryCode(category.getCategoryCode());
        order.setCategoryName(category.getCategoryName());
        order.setSourceType(command.getSourceType() == null ? 1 : command.getSourceType());
        order.setApplicantId(actor.getUserId());
        order.setApplicantName(actor.getDisplayName());
        order.setApplicantPhone(actor.getPhone());
        order.setApplicantDeptId(actor.getDeptId());
        order.setApplicantDeptName(actor.getDeptName());
        order.setLocation(command.getLocation().trim());
        order.setUrgencyLevel(command.getUrgencyLevel());
        order.setImpactScope(command.getImpactScope());
        order.setDescription(command.getDescription().trim());
        order.setPossibleCause(trimToNull(command.getPossibleCause()));
        order.setStatus(WorkOrderStatus.WAIT_ASSIGN.getCode());
        // SLA is selected once at submission; later rule edits must not move historical deadlines.
        order.setSlaRuleId(slaRule.getId());
        order.setResponseDeadline(slaRule.responseDeadline(now));
        order.setArrivalDeadline(slaRule.arrivalDeadline(now));
        order.setFinishDeadline(slaRule.finishDeadline(now));
        order.setSubmittedAt(now);
        order.setVersion(0);
        order.setCreateBy(actor.getUsername());
        order.setCreateTime(now);
        orderMapper.insert(order);

        // 去重后再比对更新行数，既允许客户端误传重复 ID，也不放过越权或已绑定附件。
        List<Long> attachmentIds = uniqueIds(command.getAttachmentIds());
        if (!attachmentIds.isEmpty())
        {
            int bound = attachmentMapper.bindToOrder(order.getId(), actor.getUserId(), attachmentIds);
            if (bound != attachmentIds.size())
            {
                throw new ServiceException("部分附件不存在、已绑定或不属于当前用户", HttpStatus.BAD_REQUEST);
            }
        }

        // 提交动作和工单在同一事务内落库，时间线不会出现“有工单、无首日志”。
        WorkOrderActionLog log = new WorkOrderActionLog();
        log.setOrderId(order.getId());
        log.setActionType("SUBMIT");
        log.setFromStatus(WorkOrderStatus.DRAFT.getCode());
        log.setToStatus(WorkOrderStatus.WAIT_ASSIGN.getCode());
        log.setOperatorId(actor.getUserId());
        log.setOperatorName(actor.getDisplayName());
        log.setOperatorRole(actor.primaryRole());
        log.setActionContent("提交工单");
        log.setExtJson("{\"orderNo\":\"" + orderNo + "\"}");
        log.setIdempotencyKey(idempotencyKey);
        log.setActionTime(now);
        actionLogMapper.insert(log);
        return order;
    }

    private List<Long> uniqueIds(List<Long> ids)
    {
        if (ids == null || ids.isEmpty()) return new ArrayList<Long>();
        LinkedHashSet<Long> values = new LinkedHashSet<Long>();
        for (Long id : ids) if (id != null) values.add(id);
        return new ArrayList<Long>(values);
    }

    private String trimToNull(String value)
    {
        if (value == null || value.trim().length() == 0) return null;
        return value.trim();
    }
}
