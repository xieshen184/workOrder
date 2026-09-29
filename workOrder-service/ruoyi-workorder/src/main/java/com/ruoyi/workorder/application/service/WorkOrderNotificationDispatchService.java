package com.ruoyi.workorder.application.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.ruoyi.workorder.application.model.WorkOrderNotificationDispatchResult;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;
import com.ruoyi.workorder.mapper.WorkOrderNotificationMapper;
import org.springframework.stereotype.Service;

/**
 * 通知任务派送深模块。调用者只提供当前时间和批量大小，模块负责领取、陈旧锁恢复、渠道选择和退避。
 */
@Service
public class WorkOrderNotificationDispatchService
{
    private static final int[] RETRY_MINUTES = { 1, 5, 15, 60 };
    private final WorkOrderNotificationMapper mapper;
    private final List<WorkOrderNotificationChannelAdapter> adapters;

    public WorkOrderNotificationDispatchService(WorkOrderNotificationMapper mapper,
            List<WorkOrderNotificationChannelAdapter> adapters)
    {
        this.mapper = mapper; this.adapters = adapters;
    }

    public WorkOrderNotificationDispatchResult dispatchDue(Date now, int batchSize)
    {
        if (batchSize < 1 || batchSize > 200) throw new IllegalArgumentException("通知批量大小必须在1到200之间");
        Date lockExpiredAt = new Date(now.getTime() - 5 * 60_000L);
        List<Long> ids = mapper.selectDueTaskIds(now, lockExpiredAt, batchSize);
        String workerId = UUID.randomUUID().toString();
        int succeeded = 0, failed = 0;
        for (Long id : ids)
        {
            if (mapper.claimTask(id, workerId, now, lockExpiredAt) != 1) continue;
            WorkOrderNotificationTask task = mapper.selectTaskById(id);
            try
            {
                adapter(task.getChannel()).send(task, now);
                if (mapper.markSuccess(id, workerId, now) == 1) succeeded++;
            }
            catch (Exception error)
            {
                failed++;
                fail(task, workerId, now, error);
            }
        }
        return new WorkOrderNotificationDispatchResult(ids.size(), succeeded, failed);
    }

    private WorkOrderNotificationChannelAdapter adapter(String channel)
    {
        for (WorkOrderNotificationChannelAdapter adapter : adapters)
            if (adapter.supports(channel)) return adapter;
        throw new IllegalStateException("未配置通知渠道适配器：" + channel);
    }

    private void fail(WorkOrderNotificationTask task, String workerId, Date now, Exception error)
    {
        int retryCount = value(task.getRetryCount()) + 1;
        int maxRetry = task.getMaxRetry() == null ? 5 : task.getMaxRetry();
        boolean dead = retryCount >= maxRetry;
        Date next = dead ? now : new Date(now.getTime() + delayMinutes(retryCount) * 60_000L);
        mapper.markFailure(task.getId(), workerId, dead ? "DEAD" : "RETRY", retryCount,
                next, safeMessage(error), now);
    }

    private int delayMinutes(int retryCount)
    {
        int index = Math.max(0, Math.min(retryCount - 1, RETRY_MINUTES.length - 1));
        return RETRY_MINUTES[index];
    }

    private String safeMessage(Exception error)
    {
        // 只记录异常类型和短消息，避免第三方异常把密钥、完整请求或手机号写入数据库。
        String message = error.getMessage() == null ? "" : error.getMessage().replaceAll("[\\r\\n]", " ");
        if (message.length() > 300) message = message.substring(0, 300);
        return error.getClass().getSimpleName() + (message.isEmpty() ? "" : ": " + message);
    }

    private int value(Integer value) { return value == null ? 0 : value; }
}
