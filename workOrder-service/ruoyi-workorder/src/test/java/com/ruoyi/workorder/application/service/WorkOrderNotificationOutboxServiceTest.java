package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationEvent;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationRecipient;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTemplate;
import com.ruoyi.workorder.mapper.WorkOrderNotificationMapper;
import org.junit.Test;

public class WorkOrderNotificationOutboxServiceTest
{
    @Test
    public void shouldResolveRecipientRenderTemplateAndFreezeTask()
    {
        AtomicReference<WorkOrderNotificationTask> inserted = new AtomicReference<WorkOrderNotificationTask>();
        WorkOrderNotificationTemplate template = template();
        WorkOrderNotificationRecipient recipient = new WorkOrderNotificationRecipient();
        recipient.setUserId(9L); recipient.setNickName("维修人员"); recipient.setPhoneNumber("13800000000");
        WorkOrderNotificationMapper mapper = proxy((method, args) -> {
            if ("selectEnabledTemplates".equals(method)) return Collections.singletonList(template);
            if ("selectRecipientById".equals(method)) return recipient;
            if ("insertTask".equals(method)) { inserted.set((WorkOrderNotificationTask) args[0]); return 1; }
            return defaultValue(method);
        });
        WorkOrder order = new WorkOrder(); order.setId(1L); order.setOrderNo("WO-1");
        order.setTitle("空调报修"); order.setApplicantId(2L); order.setCurrentAssigneeId(9L);

        int created = new WorkOrderNotificationOutboxService(mapper).publish(
                new WorkOrderNotificationEvent("ASSIGN", "event-1", order, 9L,
                        "调度员", null, null, new Date(1000L)));

        assertEquals(1, created);
        assertEquals(Long.valueOf(9L), inserted.get().getRecipientId());
        assertEquals("工单WO-1", inserted.get().getTitle());
        assertTrue(inserted.get().getContent().contains("调度员"));
        assertEquals("PENDING", inserted.get().getStatus());
        assertEquals(null, inserted.get().getRecipientAddress());
    }

    private WorkOrderNotificationTemplate template()
    {
        WorkOrderNotificationTemplate value = new WorkOrderNotificationTemplate();
        value.setEventCode("ASSIGN"); value.setCategory("WORK_ORDER"); value.setChannel("IN_APP");
        value.setTitleTemplate("工单${orderNo}"); value.setContentTemplate("${title}由${operatorName}派单");
        value.setMaxRetry(5); return value;
    }

    private interface Call { Object invoke(String method, Object[] args); }
    private WorkOrderNotificationMapper proxy(Call call)
    {
        return (WorkOrderNotificationMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { WorkOrderNotificationMapper.class },
                (object, method, args) -> call.invoke(method.getName(), args));
    }
    private Object defaultValue(String method) { return method.startsWith("select") ? Collections.emptyList() : 0; }
}
