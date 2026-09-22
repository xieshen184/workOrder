package com.ruoyi.workorder.application.service;

import java.util.Date;
import java.util.List;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderCommand;
import com.ruoyi.workorder.application.model.WorkOrderCommandResult;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.domain.model.WorkOrderAssignment;
import com.ruoyi.workorder.domain.model.WorkOrderAttachmentStage;
import com.ruoyi.workorder.domain.model.WorkOrderEngineer;
import com.ruoyi.workorder.domain.model.WorkOrderEvaluation;
import com.ruoyi.workorder.domain.model.WorkOrderProcessRecord;
import com.ruoyi.workorder.domain.model.WorkOrderStatus;
import com.ruoyi.workorder.domain.service.WorkOrderCommandPolicy;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import com.ruoyi.workorder.mapper.WorkOrderAssignmentMapper;
import com.ruoyi.workorder.mapper.WorkOrderAttachmentMapper;
import com.ruoyi.workorder.mapper.WorkOrderEngineerMapper;
import com.ruoyi.workorder.mapper.WorkOrderEvaluationMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import com.ruoyi.workorder.mapper.WorkOrderProcessRecordMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Internal transactional implementation behind {@code WorkOrderCommandGateway}. */
@Service
public class WorkOrderCommandTransaction
{
    private final WorkOrderMapper orderMapper;
    private final WorkOrderActionLogMapper actionLogMapper;
    private final WorkOrderAssignmentMapper assignmentMapper;
    private final WorkOrderProcessRecordMapper processRecordMapper;
    private final WorkOrderAttachmentMapper attachmentMapper;
    private final WorkOrderEngineerMapper engineerMapper;
    private final WorkOrderEvaluationMapper evaluationMapper;
    private final WorkOrderCommandPolicy policy = new WorkOrderCommandPolicy();

    public WorkOrderCommandTransaction(WorkOrderMapper orderMapper, WorkOrderActionLogMapper actionLogMapper,
            WorkOrderAssignmentMapper assignmentMapper, WorkOrderProcessRecordMapper processRecordMapper,
            WorkOrderAttachmentMapper attachmentMapper, WorkOrderEngineerMapper engineerMapper,
            WorkOrderEvaluationMapper evaluationMapper)
    {
        this.orderMapper = orderMapper;
        this.actionLogMapper = actionLogMapper;
        this.assignmentMapper = assignmentMapper;
        this.processRecordMapper = processRecordMapper;
        this.attachmentMapper = attachmentMapper;
        this.engineerMapper = engineerMapper;
        this.evaluationMapper = evaluationMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkOrderCommandResult execute(WorkOrderCommand command, WorkOrderActor actor)
    {
        WorkOrder order = orderMapper.selectByIdForUpdate(command.getOrderId());
        if (order == null) throw new ServiceException("工单不存在", HttpStatus.NOT_FOUND);

        // Re-check after the row lock: a concurrent first request may have committed while this one waited.
        WorkOrderActionLog replay = actionLogMapper.selectByIdempotency(actor.getUserId(),
                command.getAction().name(), command.getIdempotencyKey());
        if (replay != null) return WorkOrderCommandService.replay(replay, command);

        // 行锁串行化同一工单；相同 key 已在上方重放，不同 key 的重复评价返回明确冲突。
        if (command.getAction() == WorkOrderAction.EVALUATE)
        {
            policy.assertActor(order, command.getAction(), actor);
            if (evaluationMapper.selectByOrderId(order.getId()) != null)
                throw new ServiceException("WO_EVALUATION_CONFLICT: 该工单已评价", HttpStatus.CONFLICT);
        }

        WorkOrderStatus target = policy.target(order, command, actor);
        validatePayload(command);
        Date now = new Date();
        WorkOrderEngineer engineer = resolveEngineer(command);

        int changed = orderMapper.applyCommand(order.getId(), order.getStatus(), target.getCode(),
                command.getVersion(), command.getAction().name(), engineer == null ? null : engineer.getId(),
                engineer == null ? null : engineer.getName(), now, actor.getUsername());
        if (changed != 1)
            throw new ServiceException("WO_VERSION_CONFLICT: 工单已被其他请求更新", HttpStatus.CONFLICT);

        if (command.getAction() == WorkOrderAction.ASSIGN || command.getAction() == WorkOrderAction.REASSIGN)
            writeAssignment(order, command, actor, engineer, now);
        else if (command.getAction() == WorkOrderAction.ACCEPT)
        {
            // 主状态和派单历史必须一起变化；缺失或重复的生效派单均视为数据冲突并整单回滚。
            int accepted = assignmentMapper.markAccepted(order.getId(), actor.getUserId(), now);
            if (accepted != 1)
                throw new ServiceException("工单当前派单记录异常，请刷新后重试", HttpStatus.CONFLICT);
        }

        WorkOrderProcessRecord process = writeProcess(order, command, actor, now);
        bindAttachments(order, command, actor, process);
        WorkOrderEvaluation evaluation = writeEvaluation(order, command, actor, now);

        int newVersion = command.getVersion() + 1;
        WorkOrderActionLog log = actionLog(order, command, actor, target, now, newVersion, process, evaluation);
        actionLogMapper.insert(log);
        return new WorkOrderCommandResult(order.getId(), order.getOrderNo(), target.getCode(), newVersion, false);
    }

    private WorkOrderEngineer resolveEngineer(WorkOrderCommand command)
    {
        if (command.getAction() != WorkOrderAction.ASSIGN && command.getAction() != WorkOrderAction.REASSIGN)
            return null;
        WorkOrderEngineer engineer = engineerMapper.selectEligibleById(command.getEngineerId());
        if (engineer == null)
            throw new ServiceException("所选用户不是有效的工单维修人员", HttpStatus.BAD_REQUEST);
        return engineer;
    }

    private void writeAssignment(WorkOrder order, WorkOrderCommand command, WorkOrderActor actor,
            WorkOrderEngineer engineer, Date now)
    {
        if (order.getSlaRuleId() == null || order.getSubmittedAt() == null || order.getResponseDeadline() == null)
            throw new ServiceException("工单缺少提交时的响应SLA快照", HttpStatus.CONFLICT);
        long responseMillis = order.getResponseDeadline().getTime() - order.getSubmittedAt().getTime();
        if (responseMillis <= 0 || responseMillis % 60_000L != 0 || responseMillis / 60_000L > Integer.MAX_VALUE)
            throw new ServiceException("工单响应SLA快照不完整", HttpStatus.CONFLICT);
        int responseMinutes = (int) (responseMillis / 60_000L);
        if (command.getAction() == WorkOrderAction.REASSIGN)
        {
            // 未接单和已接单的记录都可能被改派；必须且只能结束一条当前派单。
            int closed = assignmentMapper.closeActive(order.getId(), now, trim(command.getReason()));
            if (closed != 1)
                throw new ServiceException("工单当前派单记录异常，无法改派", HttpStatus.CONFLICT);
        }
        WorkOrderAssignment row = new WorkOrderAssignment();
        row.setOrderId(order.getId()); row.setEngineerId(engineer.getId()); row.setEngineerName(engineer.getName());
        row.setEngineerDeptId(engineer.getDeptId()); row.setAssignedBy(actor.getUserId());
        row.setAssignedByName(actor.getDisplayName()); row.setAssignedAt(now);
        row.setResponseMinutes(responseMinutes); row.setResponseDeadline(order.getResponseDeadline());
        row.setAssignmentStatus("ACTIVE"); row.setCreateTime(now);
        assignmentMapper.insert(row);
    }

    private WorkOrderProcessRecord writeProcess(WorkOrder order, WorkOrderCommand command,
            WorkOrderActor actor, Date now)
    {
        WorkOrderAction action = command.getAction();
        if (action != WorkOrderAction.ARRIVE && action != WorkOrderAction.ASSESS
                && action != WorkOrderAction.PROGRESS && action != WorkOrderAction.FINISH) return null;
        WorkOrderProcessRecord row = new WorkOrderProcessRecord();
        row.setOrderId(order.getId()); row.setRecordStage(recordStage(action));
        row.setEngineerId(actor.getUserId()); row.setEngineerName(actor.getDisplayName());
        String content = trim(command.getContent());
        row.setContent(content == null ? "" : content); row.setOccurredAt(now); row.setCreateTime(now);
        if (action == WorkOrderAction.ASSESS)
        {
            row.setRequiresParts(flag(command.getRequiresParts()));
            row.setPartsDescription(trim(command.getPartsDescription()));
            row.setAssessedHours(command.getAssessedHours()); row.setAssessedUrgency(command.getAssessedUrgency());
            row.setAssessedScope(command.getAssessedScope()); row.setRequiresExtension(flag(command.getRequiresExtension()));
        }
        processRecordMapper.insert(row);
        return row;
    }

    private void bindAttachments(WorkOrder order, WorkOrderCommand command, WorkOrderActor actor,
            WorkOrderProcessRecord process)
    {
        List<Long> ids = command.getAttachmentIds();
        if (ids.isEmpty()) return;
        WorkOrderAttachmentStage stage = WorkOrderAttachmentStage.forAction(command.getAction());
        if (stage == null || process == null) throw new ServiceException("该动作不支持附件", HttpStatus.BAD_REQUEST);
        int bound = attachmentMapper.bindToProcess(order.getId(), process.getId(), actor.getUserId(), stage.name(), ids);
        if (bound != ids.size())
            throw new ServiceException("部分附件不属于当前用户、已绑定、已删除或业务阶段不匹配", HttpStatus.BAD_REQUEST);
    }

    private void validatePayload(WorkOrderCommand command)
    {
        WorkOrderAction action = command.getAction();
        if ((action == WorkOrderAction.ASSIGN || action == WorkOrderAction.REASSIGN) && command.getEngineerId() == null)
            throw new ServiceException("请选择维修人员", HttpStatus.BAD_REQUEST);
        if (action == WorkOrderAction.REASSIGN && blank(command.getReason()))
            throw new ServiceException("改派原因不能为空", HttpStatus.BAD_REQUEST);
        if (action == WorkOrderAction.RETURN && blank(command.getReason()))
            throw new ServiceException("退回原因不能为空", HttpStatus.BAD_REQUEST);
        if ((action == WorkOrderAction.CANCEL || action == WorkOrderAction.RETURN)
                && length(command.getReason()) > 500)
            throw new ServiceException("原因不能超过500个字符", HttpStatus.BAD_REQUEST);
        if ((action == WorkOrderAction.ARRIVE || action == WorkOrderAction.FINISH) && command.getAttachmentIds().isEmpty())
            throw new ServiceException(action == WorkOrderAction.ARRIVE ? "到场照片不能为空" : "完工照片不能为空", HttpStatus.BAD_REQUEST);
        if ((action == WorkOrderAction.ARRIVE || action == WorkOrderAction.FINISH) && blank(command.getContent()))
            throw new ServiceException("处理内容不能为空", HttpStatus.BAD_REQUEST);
        if (action == WorkOrderAction.PROGRESS && blank(command.getContent()) && command.getAttachmentIds().isEmpty())
            throw new ServiceException("进度内容和附件至少填写一项", HttpStatus.BAD_REQUEST);
        if (action == WorkOrderAction.ASSESS && Boolean.TRUE.equals(command.getRequiresParts())
                && blank(command.getPartsDescription()))
            throw new ServiceException("需要零部件时必须填写零部件说明", HttpStatus.BAD_REQUEST);
        if (action == WorkOrderAction.EVALUATE)
        {
            if (!score(command.getOverallScore()))
                throw new ServiceException("总体评分必须为1到5", HttpStatus.BAD_REQUEST);
            if (!optionalScore(command.getResponseScore()) || !optionalScore(command.getQualityScore())
                    || !optionalScore(command.getAttitudeScore()))
                throw new ServiceException("评价分项评分必须为1到5", HttpStatus.BAD_REQUEST);
            if (length(command.getContent()) > 500)
                throw new ServiceException("评价内容不能超过500个字符", HttpStatus.BAD_REQUEST);
        }
    }

    private WorkOrderEvaluation writeEvaluation(WorkOrder order, WorkOrderCommand command,
            WorkOrderActor actor, Date now)
    {
        if (command.getAction() != WorkOrderAction.EVALUATE) return null;
        WorkOrderEvaluation row = new WorkOrderEvaluation();
        row.setOrderId(order.getId()); row.setEvaluatorId(actor.getUserId());
        row.setOverallScore(command.getOverallScore()); row.setResponseScore(command.getResponseScore());
        row.setQualityScore(command.getQualityScore()); row.setAttitudeScore(command.getAttitudeScore());
        row.setEvaluationContent(trim(command.getContent())); row.setEvaluatedAt(now); row.setCreateTime(now);
        try
        {
            // 状态更新、唯一评价和动作日志处于同一事务；唯一键是并发重复评价的最后防线。
            evaluationMapper.insert(row);
        }
        catch (DuplicateKeyException duplicate)
        {
            throw new ServiceException("WO_EVALUATION_CONFLICT: 该工单已评价", HttpStatus.CONFLICT);
        }
        return row;
    }

    private WorkOrderActionLog actionLog(WorkOrder order, WorkOrderCommand command, WorkOrderActor actor,
            WorkOrderStatus target, Date now, int version, WorkOrderProcessRecord process,
            WorkOrderEvaluation evaluation)
    {
        JSONObject ext = new JSONObject();
        ext.put("orderNo", order.getOrderNo()); ext.put("version", version);
        ext.put("requestFingerprint", WorkOrderCommandService.hash(command));
        if (command.getEngineerId() != null) ext.put("engineerId", command.getEngineerId());
        if (process != null) ext.put("processRecordId", process.getId());
        if (evaluation != null) ext.put("evaluationId", evaluation.getId());
        WorkOrderActionLog log = new WorkOrderActionLog();
        log.setOrderId(order.getId()); log.setActionType(command.getAction().name());
        log.setFromStatus(order.getStatus()); log.setToStatus(target.getCode());
        log.setOperatorId(actor.getUserId()); log.setOperatorName(actor.getDisplayName());
        log.setOperatorRole(actor.primaryRole()); log.setActionContent(actionContent(command));
        log.setExtJson(ext.toJSONString()); log.setIdempotencyKey(command.getIdempotencyKey()); log.setActionTime(now);
        return log;
    }

    private String actionContent(WorkOrderCommand command)
    {
        if (command.getAction() == WorkOrderAction.ASSIGN) return "派单" + suffix(command.getReason());
        if (command.getAction() == WorkOrderAction.REASSIGN) return "改派" + suffix(command.getReason());
        if (command.getAction() == WorkOrderAction.ACCEPT) return "接单";
        if (command.getAction() == WorkOrderAction.CANCEL) return "取消工单" + suffix(command.getReason());
        if (command.getAction() == WorkOrderAction.CONFIRM) return "确认完工";
        if (command.getAction() == WorkOrderAction.RETURN) return "退回返工" + suffix(command.getReason());
        if (command.getAction() == WorkOrderAction.EVALUATE)
            return "提交评价（总体" + command.getOverallScore() + "分）" + suffix(command.getContent());
        return trim(command.getContent());
    }
    private String suffix(String value) { return blank(value) ? "" : "：" + value.trim(); }
    private String recordStage(WorkOrderAction action) { return action == WorkOrderAction.ARRIVE ? "ARRIVAL" : action == WorkOrderAction.ASSESS ? "ASSESSMENT" : action.name(); }
    private String flag(Boolean value) { return value == null ? null : (value ? "1" : "0"); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.trim().length() == 0; }
    private int length(String value) { return value == null ? 0 : value.trim().length(); }
    private boolean score(Integer value) { return value != null && value >= 1 && value <= 5; }
    private boolean optionalScore(Integer value) { return value == null || score(value); }
}
