package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import com.ruoyi.workorder.application.model.WorkOrderNotificationDispatchResult;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;
import com.ruoyi.workorder.mapper.WorkOrderNotificationMapper;
import org.junit.Test;

public class WorkOrderNotificationDispatchServiceTest
{
    @Test
    public void shouldClaimSendAndMarkSuccess()
    {
        WorkOrderNotificationTask task = task();
        RecordingAdapter adapter = new RecordingAdapter(false);
        WorkOrderNotificationMapper mapper = mapper(task, null);

        WorkOrderNotificationDispatchResult result = new WorkOrderNotificationDispatchService(
                mapper, Collections.singletonList(adapter)).dispatchDue(new Date(100_000L), 50);

        assertEquals(1, result.getSucceeded()); assertEquals(0, result.getFailed());
        assertEquals(Long.valueOf(1L), adapter.sent.get().getId());
    }

    @Test
    public void shouldRetryAfterOneMinuteOnFirstFailure()
    {
        WorkOrderNotificationTask task = task();
        AtomicReference<Object[]> failure = new AtomicReference<Object[]>();
        WorkOrderNotificationMapper mapper = mapper(task, failure);
        Date now = new Date(200_000L);

        WorkOrderNotificationDispatchResult result = new WorkOrderNotificationDispatchService(
                mapper, Collections.singletonList(new RecordingAdapter(true))).dispatchDue(now, 50);

        assertEquals(1, result.getFailed()); assertEquals("RETRY", failure.get()[2]);
        assertEquals(1, failure.get()[3]);
        assertEquals(now.getTime() + 60_000L, ((Date) failure.get()[4]).getTime());
        assertTrue(String.valueOf(failure.get()[5]).contains("IllegalStateException"));
    }

    private WorkOrderNotificationMapper mapper(WorkOrderNotificationTask task, AtomicReference<Object[]> failure)
    {
        return (WorkOrderNotificationMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { WorkOrderNotificationMapper.class }, (object, method, args) -> {
                    String name = method.getName();
                    if ("selectDueTaskIds".equals(name)) return Collections.singletonList(1L);
                    if ("claimTask".equals(name) || "markSuccess".equals(name)) return 1;
                    if ("selectTaskById".equals(name)) return task;
                    if ("markFailure".equals(name)) { failure.set(args); return 1; }
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
    }

    private WorkOrderNotificationTask task()
    {
        WorkOrderNotificationTask task = new WorkOrderNotificationTask();
        task.setId(1L); task.setChannel("IN_APP"); task.setRetryCount(0); task.setMaxRetry(5); return task;
    }

    private static class RecordingAdapter implements WorkOrderNotificationChannelAdapter
    {
        private final boolean fail;
        private final AtomicReference<WorkOrderNotificationTask> sent = new AtomicReference<WorkOrderNotificationTask>();
        RecordingAdapter(boolean fail) { this.fail = fail; }
        @Override public boolean supports(String channel) { return "IN_APP".equals(channel); }
        @Override public void send(WorkOrderNotificationTask task, Date sentAt)
        {
            if (fail) throw new IllegalStateException("temporary");
            sent.set(task);
        }
    }
}
