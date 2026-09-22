package com.ruoyi.workorder.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.WorkOrderCommandGateway;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderCommand;
import com.ruoyi.workorder.application.model.WorkOrderCommandResult;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderCommandService implements WorkOrderCommandGateway
{
    private final WorkOrderActionLogMapper actionLogMapper;
    private final WorkOrderCommandTransaction transaction;
    private final WorkOrderQueryService queryService;

    public WorkOrderCommandService(WorkOrderActionLogMapper actionLogMapper,
            WorkOrderCommandTransaction transaction, WorkOrderQueryService queryService)
    {
        this.actionLogMapper = actionLogMapper;
        this.transaction = transaction;
        this.queryService = queryService;
    }

    @Override
    public WorkOrderCommandResult execute(WorkOrderCommand command, WorkOrderActor actor)
    {
        validate(command, actor);
        // 可见性必须先于幂等查询，防止操作者通过历史 key 重放已不可见工单的结果。
        queryService.assertVisible(command.getOrderId(), actor);
        WorkOrderActionLog existing = find(command, actor);
        if (existing != null) return replay(existing, command);
        try
        {
            return transaction.execute(command, actor);
        }
        catch (DuplicateKeyException race)
        {
            // The unique action key is the final guard for requests racing on different DB sessions.
            existing = find(command, actor);
            if (existing != null) return replay(existing, command);
            throw race;
        }
    }

    private WorkOrderActionLog find(WorkOrderCommand command, WorkOrderActor actor)
    {
        return actionLogMapper.selectByIdempotency(actor.getUserId(), command.getAction().name(),
                command.getIdempotencyKey());
    }

    private void validate(WorkOrderCommand command, WorkOrderActor actor)
    {
        if (command == null || command.getOrderId() == null || command.getAction() == null)
            throw new ServiceException("工单命令不完整", HttpStatus.BAD_REQUEST);
        if (actor == null || actor.getUserId() == null)
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        String key = command.getIdempotencyKey();
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,64}"))
            throw new ServiceException("Idempotency-Key 格式不正确", HttpStatus.BAD_REQUEST);
    }

    static WorkOrderCommandResult replay(WorkOrderActionLog log, WorkOrderCommand command)
    {
        JSONObject ext = JSON.parseObject(log.getExtJson());
        String fingerprint = ext == null ? null : ext.getString("requestFingerprint");
        if (!log.getOrderId().equals(command.getOrderId()) || !hash(command).equals(fingerprint))
            throw new ServiceException("Idempotency-Key 已用于不同请求", HttpStatus.CONFLICT);
        return new WorkOrderCommandResult(log.getOrderId(), ext.getString("orderNo"), log.getToStatus(),
                ext.getInteger("version"), true);
    }

    static String hash(WorkOrderCommand command)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(command.fingerprintSource().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        }
        catch (Exception error)
        {
            throw new IllegalStateException("无法计算命令摘要", error);
        }
    }
}
