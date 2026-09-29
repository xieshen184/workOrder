package com.ruoyi.workorder.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderDelayDecisionRequest;
import com.ruoyi.workorder.application.model.WorkOrderDelayRequestCreateRequest;
import com.ruoyi.workorder.application.model.WorkOrderDelayRequestQuery;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderAction;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.domain.model.WorkOrderDelayRequest;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationEvent;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import com.ruoyi.workorder.mapper.WorkOrderDelayRequestMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F01 延期申请应用服务。
 *
 * <p>延期申请不是普通 CRUD：申请会占用工单的待审槽位，审批同意才改变有效完成截止时间，
 * 拒绝则只释放待审槽位。所有写操作都在本服务的事务中完成，并通过工单行锁、申请状态
 * CAS 和版本条件共同保证并发请求最多一次生效。</p>
 */
@Service
public class WorkOrderDelayService
{
    private static final String PROCESSING = "PROCESSING";
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final WorkOrderDelayRequestMapper delayRequestMapper;
    private final WorkOrderMapper orderMapper;
    private final WorkOrderActionLogMapper actionLogMapper;
    private final WorkOrderQueryService queryService;
    @Autowired private WorkOrderNotificationOutboxService notificationOutbox;

    public WorkOrderDelayService(WorkOrderDelayRequestMapper delayRequestMapper, WorkOrderMapper orderMapper,
            WorkOrderActionLogMapper actionLogMapper, WorkOrderQueryService queryService)
    {
        this.delayRequestMapper = delayRequestMapper;
        this.orderMapper = orderMapper;
        this.actionLogMapper = actionLogMapper;
        this.queryService = queryService;
    }

    /**
     * 创建延期申请。
     *
     * <p>先做可见性校验，再锁定工单重新读取所有业务状态。这样既不会向无权用户泄露
     * 工单是否存在，也不会使用锁前读取的旧版本进行写入。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderDelayRequest create(Long orderId, WorkOrderDelayRequestCreateRequest request,
            WorkOrderActor actor, String idempotencyKey)
    {
        assertActor(actor, "workorder:delay:add");
        validateCreateRequest(orderId, request);
        validateIdempotencyKey(idempotencyKey);
        List<Long> attachmentIds = normalizeAttachmentIds(request.getAttachmentIds());
        String fingerprint = requestFingerprint(orderId, request, attachmentIds);
        queryService.assertVisible(orderId, actor);

        WorkOrderDelayRequest replay = replayCreate(orderId, actor, idempotencyKey, fingerprint);
        if (replay != null) return replay;

        WorkOrder order = requireOrderForUpdate(orderId);
        // 并发请求可能在等待工单行锁期间已经完成，锁后必须再次检查幂等日志。
        replay = replayCreate(orderId, actor, idempotencyKey, fingerprint);
        if (replay != null) return replay;
        assertCreateContext(order, request, actor);

        Date originalDeadline = effectiveDeadline(order);
        Date now = new Date();

        // 工单行已被锁定，版本条件和 delay_pending_flag 条件是第二层并发保护。
        if (delayRequestMapper.markDelayPending(orderId, request.getVersion(), actor.getUsername(), now) != 1)
            throw conflict("WO_VERSION_CONFLICT: 工单状态已变化，请刷新后重试");

        WorkOrderDelayRequest delay = new WorkOrderDelayRequest();
        delay.setOrderId(order.getId());
        delay.setOrderNo(order.getOrderNo());
        delay.setOrderTitle(order.getTitle());
        delay.setApplicantId(actor.getUserId());
        delay.setApplicantName(actor.getDisplayName());
        delay.setOriginalDeadline(originalDeadline);
        delay.setRequestedDeadline(request.getRequestedDeadline());
        delay.setReason(trim(request.getReason()));
        delay.setRequestStatus(PENDING);
        delay.setCreateTime(now);
        delay.setUpdateTime(now);
        if (delayRequestMapper.insert(delay) != 1 || delay.getId() == null)
            throw conflict("延期申请保存失败，请稍后重试");

        bindAttachments(order, delay, actor, attachmentIds);
        writeActionLog(order, delay, WorkOrderAction.REQUEST_DELAY, actor, now, delay.getReason(),
                idempotencyKey, fingerprint);
        publishDelay("DELAY_REQUEST", idempotencyKey, order, null, actor, delay, delay.getReason(), now);
        return reloadOr(delay, attachmentIds);
    }

    /** 查询当前用户可见工单的最新延期申请。 */
    public WorkOrderDelayRequest getLatest(Long orderId, WorkOrderActor actor)
    {
        assertActor(actor, null);
        if (orderId == null) throw badRequest("缺少工单编号");
        // latest 接口允许多种延期权限，但数据可见性仍统一由工单查询服务判定。
        queryService.assertVisible(orderId, actor);
        return delayRequestMapper.selectLatestByOrderId(orderId);
    }

    /** 管理端分页查询延期申请；数据范围和审批权限由 Controller/平台权限层控制。 */
    @DataScope(deptAlias = "d", userAlias = "u", permission = "workorder:delay:list")
    public List<WorkOrderDelayRequest> selectList(WorkOrderDelayRequestQuery query)
    {
        return delayRequestMapper.selectList(query == null ? new WorkOrderDelayRequestQuery() : query);
    }

    /**
     * 同意延期。
     *
     * <p>审批人必须先经过 {@link WorkOrderQueryService#assertVisible(Long, WorkOrderActor)}，
     * 然后锁申请和工单；申请状态 CAS、工单版本和待审标志全部成功后才写审批日志。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderDelayRequest approve(Long requestId, WorkOrderDelayDecisionRequest decision,
            WorkOrderActor actor)
    {
        assertActor(actor, "workorder:delay:approve");
        WorkOrderDelayRequest visible = requireDelayRequest(requestId, false);
        queryService.assertVisible(visible.getOrderId(), actor);

        // 与工单命令统一采用“先锁工单、再锁延期申请”的顺序，避免完工与审批并发时反向加锁。
        WorkOrder order = requireOrderForUpdate(visible.getOrderId());
        WorkOrderDelayRequest delay = requireDelayRequestForUpdate(requestId);
        assertSameOrder(visible, delay);
        assertPending(delay);
        assertApprovalContext(order, delay);

        Date now = new Date();
        if (delayRequestMapper.applyApprovedDelay(order.getId(), order.getVersion(),
                delay.getRequestedDeadline(), actor.getUsername(), now) != 1)
            throw conflict("WO_DELAY_CONFLICT: 工单已被其他操作更新，请刷新后重试");

        String comment = normalizeDecisionReason(decision, false);
        if (delayRequestMapper.updateDecision(delay.getId(), APPROVED, actor.getUserId(),
                actor.getDisplayName(), comment, now, now) != 1)
            throw conflict("WO_DELAY_CONFLICT: 延期申请已被其他审批操作处理");

        delay.setRequestStatus(APPROVED);
        delay.setApproverId(actor.getUserId());
        delay.setApproverName(actor.getDisplayName());
        delay.setApprovalComment(comment);
        delay.setApprovedAt(now);
        delay.setUpdateTime(now);
        writeActionLog(order, delay, WorkOrderAction.APPROVE_DELAY, actor, now, comment,
                idempotencyKey(WorkOrderAction.APPROVE_DELAY, delay.getId()), null);
        publishDelay("DELAY_APPROVED", idempotencyKey(WorkOrderAction.APPROVE_DELAY, delay.getId()),
                order, delay.getApplicantId(), actor, delay, comment, now);
        return reloadOr(delay, null);
    }

    /**
     * 拒绝延期。
     *
     * <p>拒绝路径绝不写 extension_deadline；即使工单已被其它动作清掉待审标记，也只更新
     * 申请状态，避免为了拒绝而覆盖工单截止时间。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrderDelayRequest reject(Long requestId, WorkOrderDelayDecisionRequest decision,
            WorkOrderActor actor)
    {
        assertActor(actor, "workorder:delay:approve");
        WorkOrderDelayRequest visible = requireDelayRequest(requestId, false);
        queryService.assertVisible(visible.getOrderId(), actor);

        WorkOrder order = requireOrderForUpdate(visible.getOrderId());
        WorkOrderDelayRequest delay = requireDelayRequestForUpdate(requestId);
        assertSameOrder(visible, delay);
        assertPending(delay);
        String comment = normalizeDecisionReason(decision, true);
        Date now = new Date();

        // FINISH 等命令可能已经清掉标志；此时拒绝仍可安全落申请状态，不重新写截止时间。
        if ("1".equals(order.getDelayPendingFlag())
                && delayRequestMapper.clearDelayPending(order.getId(), order.getVersion(),
                        actor.getUsername(), now) != 1)
            throw conflict("WO_DELAY_CONFLICT: 工单已被其他操作更新，请刷新后重试");

        if (delayRequestMapper.updateDecision(delay.getId(), REJECTED, actor.getUserId(),
                actor.getDisplayName(), comment, now, now) != 1)
            throw conflict("WO_DELAY_CONFLICT: 延期申请已被其他审批操作处理");

        delay.setRequestStatus(REJECTED);
        delay.setApproverId(actor.getUserId());
        delay.setApproverName(actor.getDisplayName());
        delay.setApprovalComment(comment);
        delay.setApprovedAt(now);
        delay.setUpdateTime(now);
        writeActionLog(order, delay, WorkOrderAction.REJECT_DELAY, actor, now, comment,
                idempotencyKey(WorkOrderAction.REJECT_DELAY, delay.getId()), null);
        publishDelay("DELAY_REJECTED", idempotencyKey(WorkOrderAction.REJECT_DELAY, delay.getId()),
                order, delay.getApplicantId(), actor, delay, comment, now);
        return reloadOr(delay, null);
    }

    private void publishDelay(String eventCode, String operationKey, WorkOrder order, Long targetUserId,
            WorkOrderActor actor, WorkOrderDelayRequest delay, String reason, Date now)
    {
        if (notificationOutbox == null) return;
        notificationOutbox.publish(new WorkOrderNotificationEvent(eventCode,
                "delay:" + delay.getId() + ":" + operationKey, order, targetUserId,
                actor.getDisplayName(), reason, delay.getRequestedDeadline(), now));
    }

    private void assertCreateContext(WorkOrder order, WorkOrderDelayRequestCreateRequest request,
            WorkOrderActor actor)
    {
        if (!PROCESSING.equals(order.getStatus()))
            throw conflict("WO_STATE_CONFLICT: 只有处理中的工单可以申请延期");
        if (!actor.getUserId().equals(order.getCurrentAssigneeId()))
            throw forbidden("WO_FORBIDDEN: 仅当前处理人可以申请延期");
        if (!extensionAllowed(order.getSlaAllowExtension()))
            throw conflict("WO_SLA_CONFLICT: 当前 SLA 不允许延期");
        if (request.getVersion() == null || !request.getVersion().equals(order.getVersion()))
            throw conflict("WO_VERSION_CONFLICT: 工单版本已变化，请刷新后重试");
        if ("1".equals(order.getDelayPendingFlag())
                || delayRequestMapper.selectPendingByOrderId(order.getId()) != null)
            throw conflict("WO_DELAY_CONFLICT: 该工单已有待审批延期申请");

        Date currentDeadline = effectiveDeadline(order);
        if (currentDeadline == null)
            throw conflict("WO_SLA_CONFLICT: 工单缺少有效的完成截止时间");
        if (!request.getRequestedDeadline().after(currentDeadline))
            throw badRequest("申请延期后的截止时间必须晚于当前有效完成截止时间");
    }

    private void assertApprovalContext(WorkOrder order, WorkOrderDelayRequest delay)
    {
        if (!PROCESSING.equals(order.getStatus()))
            throw conflict("WO_STATE_CONFLICT: 只有处理中的工单可以审批延期");
        if (!"1".equals(order.getDelayPendingFlag()))
            throw conflict("WO_DELAY_CONFLICT: 工单当前没有待审批延期");
        Date currentDeadline = effectiveDeadline(order);
        if (currentDeadline == null || delay.getRequestedDeadline() == null
                || !delay.getRequestedDeadline().after(currentDeadline))
            throw conflict("WO_DELAY_CONFLICT: 延期申请已不再晚于当前有效完成截止时间");
        if (delay.getOriginalDeadline() != null && !sameTime(delay.getOriginalDeadline(), currentDeadline))
            throw conflict("WO_DELAY_CONFLICT: 延期申请基于旧的完成截止时间");
    }

    private void validateCreateRequest(Long orderId, WorkOrderDelayRequestCreateRequest request)
    {
        if (orderId == null) throw badRequest("缺少工单编号");
        if (request == null) throw badRequest("延期申请参数不能为空");
        if (request.getRequestedDeadline() == null) throw badRequest("缺少申请延期后的完成截止时间");
        if (request.getVersion() == null || request.getVersion() < 0) throw badRequest("工单版本不正确");
        String reason = trim(request.getReason());
        if (reason == null) throw badRequest("延期原因不能为空");
        if (reason.length() > 500) throw badRequest("延期原因不能超过500个字符");
    }

    private String normalizeDecisionReason(WorkOrderDelayDecisionRequest decision, boolean required)
    {
        String reason = decision == null ? null : trim(decision.effectiveReason());
        if (required && reason == null) throw badRequest("拒绝原因不能为空");
        if (reason != null && reason.length() > 500) throw badRequest("审批意见不能超过500个字符");
        return reason;
    }

    private List<Long> normalizeAttachmentIds(List<Long> ids)
    {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        if (ids.size() > 10) throw badRequest("单次最多绑定10个附件");
        Set<Long> distinct = new LinkedHashSet<Long>();
        for (Long id : ids)
        {
            if (id == null || id <= 0) throw badRequest("附件编号不正确");
            if (!distinct.add(id)) throw badRequest("附件编号不能重复");
        }
        return new ArrayList<Long>(distinct);
    }

    private void bindAttachments(WorkOrder order, WorkOrderDelayRequest delay, WorkOrderActor actor,
            List<Long> attachmentIds)
    {
        if (attachmentIds.isEmpty()) return;
        int bound = delayRequestMapper.bindAttachments(order.getId(), delay.getId(), actor.getUserId(), attachmentIds);
        if (bound != attachmentIds.size())
            throw badRequest("部分附件不属于当前用户、已绑定、已删除或不是临时附件");
        delay.setAttachmentIds(attachmentIds);
    }

    private void writeActionLog(WorkOrder order, WorkOrderDelayRequest delay, WorkOrderAction action,
            WorkOrderActor actor, Date now, String comment, String idempotencyKey, String fingerprint)
    {
        JSONObject ext = new JSONObject();
        ext.put("delayRequestId", delay.getId());
        ext.put("originalDeadline", delay.getOriginalDeadline());
        ext.put("requestedDeadline", delay.getRequestedDeadline());
        ext.put("requestStatus", delay.getRequestStatus());
        if (fingerprint != null) ext.put("requestFingerprint", fingerprint);

        WorkOrderActionLog log = new WorkOrderActionLog();
        log.setOrderId(order.getId());
        log.setActionType(action.name());
        log.setFromStatus(order.getStatus());
        log.setToStatus(order.getStatus());
        log.setOperatorId(actor.getUserId());
        log.setOperatorName(actor.getDisplayName());
        log.setOperatorRole(actor.primaryRole());
        log.setActionContent(actionContent(action, delay, comment));
        log.setExtJson(ext.toJSONString());
        log.setIdempotencyKey(idempotencyKey);
        log.setActionTime(now);
        try
        {
            if (actionLogMapper.insert(log) != 1)
                throw conflict("延期操作日志写入失败，请稍后重试");
        }
        catch (DuplicateKeyException duplicate)
        {
            throw conflict("Idempotency-Key 已被其他延期操作使用");
        }
    }

    private String actionContent(WorkOrderAction action, WorkOrderDelayRequest delay, String comment)
    {
        if (action == WorkOrderAction.REQUEST_DELAY)
            return "申请延期至" + delay.getRequestedDeadline() + "：" + delay.getReason();
        if (action == WorkOrderAction.APPROVE_DELAY)
            return "同意延期至" + delay.getRequestedDeadline() + suffix(comment);
        return "拒绝延期申请" + suffix(comment);
    }

    private String idempotencyKey(WorkOrderAction action, Long requestId)
    {
        return "delay-" + action.name().toLowerCase(Locale.ROOT) + "-" + requestId;
    }

    private WorkOrderDelayRequest replayCreate(Long orderId, WorkOrderActor actor,
            String idempotencyKey, String fingerprint)
    {
        WorkOrderActionLog log = actionLogMapper.selectByIdempotency(actor.getUserId(),
                WorkOrderAction.REQUEST_DELAY.name(), idempotencyKey);
        if (log == null) return null;
        JSONObject ext = JSONObject.parseObject(log.getExtJson());
        String storedFingerprint = ext == null ? null : ext.getString("requestFingerprint");
        if (!orderId.equals(log.getOrderId()) || !fingerprint.equals(storedFingerprint))
            throw conflict("Idempotency-Key 已用于不同的延期申请");
        Long requestId = ext.getLong("delayRequestId");
        WorkOrderDelayRequest replay = requestId == null ? null : delayRequestMapper.selectById(requestId);
        if (replay == null)
            throw conflict("延期申请幂等记录不完整，请联系管理员");
        return replay;
    }

    private void validateIdempotencyKey(String key)
    {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,64}"))
            throw badRequest("Idempotency-Key 格式不正确");
    }

    private String requestFingerprint(Long orderId, WorkOrderDelayRequestCreateRequest request,
            List<Long> attachmentIds)
    {
        try
        {
            List<Long> sorted = new ArrayList<Long>(attachmentIds);
            Collections.sort(sorted);
            String source = orderId + "|" + request.getRequestedDeadline().getTime() + "|"
                    + trim(request.getReason()) + "|" + sorted + "|" + request.getVersion();
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (byte item : digest) value.append(String.format("%02x", item & 0xff));
            return value.toString();
        }
        catch (Exception error)
        {
            throw new IllegalStateException("无法计算延期申请摘要", error);
        }
    }

    private WorkOrderDelayRequest reloadOr(WorkOrderDelayRequest fallback, List<Long> attachmentIds)
    {
        WorkOrderDelayRequest reloaded = fallback.getId() == null ? null : delayRequestMapper.selectById(fallback.getId());
        if (reloaded == null) reloaded = fallback;
        if (reloaded.getAttachmentIds() == null && attachmentIds != null)
            reloaded.setAttachmentIds(attachmentIds);
        return reloaded;
    }

    private WorkOrder requireOrderForUpdate(Long orderId)
    {
        WorkOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) throw new ServiceException("工单不存在", HttpStatus.NOT_FOUND);
        return order;
    }

    private WorkOrderDelayRequest requireDelayRequest(Long requestId, boolean lock)
    {
        if (requestId == null) throw badRequest("缺少延期申请编号");
        WorkOrderDelayRequest request = lock
                ? delayRequestMapper.selectByIdForUpdate(requestId)
                : delayRequestMapper.selectById(requestId);
        if (request == null) throw new ServiceException("延期申请不存在", HttpStatus.NOT_FOUND);
        return request;
    }

    private WorkOrderDelayRequest requireDelayRequestForUpdate(Long requestId)
    {
        return requireDelayRequest(requestId, true);
    }

    private void assertPending(WorkOrderDelayRequest request)
    {
        if (!PENDING.equals(request.getRequestStatus()))
            throw conflict("WO_DELAY_CONFLICT: 延期申请已被其他审批操作处理");
    }

    private void assertSameOrder(WorkOrderDelayRequest visible, WorkOrderDelayRequest locked)
    {
        if (visible.getOrderId() == null || !visible.getOrderId().equals(locked.getOrderId()))
            throw conflict("WO_DELAY_CONFLICT: 延期申请关联工单已变化");
    }

    private Date effectiveDeadline(WorkOrder order)
    {
        return order.getExtensionDeadline() != null ? order.getExtensionDeadline() : order.getFinishDeadline();
    }

    private boolean extensionAllowed(String value)
    {
        return "1".equals(value) || "Y".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value);
    }

    private boolean sameTime(Date left, Date right)
    {
        return left != null && right != null && left.getTime() == right.getTime();
    }

    private String trim(String value)
    {
        if (value == null) return null;
        String result = value.trim();
        return result.length() == 0 ? null : result;
    }

    private String suffix(String value)
    {
        return value == null ? "" : "：" + value;
    }

    private void assertActor(WorkOrderActor actor, String permission)
    {
        if (actor == null || actor.getUserId() == null)
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        if (permission != null && !actor.hasPermission(permission))
            throw forbidden("WO_FORBIDDEN: 无权执行延期操作");
    }

    private ServiceException badRequest(String message)
    {
        return new ServiceException(message, HttpStatus.BAD_REQUEST);
    }

    private ServiceException forbidden(String message)
    {
        return new ServiceException(message, HttpStatus.FORBIDDEN);
    }

    private ServiceException conflict(String message)
    {
        return new ServiceException(message, HttpStatus.CONFLICT);
    }
}
