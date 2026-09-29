package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderNotificationTemplateSaveRequest;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTemplate;
import com.ruoyi.workorder.mapper.WorkOrderNotificationMapper;
import org.junit.Test;

public class WorkOrderNotificationTemplateServiceTest
{
    @Test
    public void shouldRejectUnknownVariableBeforePreview()
    {
        WorkOrderNotificationTemplateService service = new WorkOrderNotificationTemplateService(proxy((method, args) -> null));
        WorkOrderNotificationTemplateSaveRequest request = request();
        request.setContentTemplate("未知变量${secret}");

        try { service.preview(request); }
        catch (ServiceException error)
        {
            assertTrue(error.getMessage().contains("未知模板变量"));
            return;
        }
        throw new AssertionError("未知变量必须被拒绝");
    }

    @Test
    public void shouldKeepRegisteredEventAndUpdateEditableFields()
    {
        AtomicReference<WorkOrderNotificationTemplate> updated = new AtomicReference<WorkOrderNotificationTemplate>();
        WorkOrderNotificationTemplate current = new WorkOrderNotificationTemplate();
        current.setId(1L); current.setEventCode("ASSIGN"); current.setChannel("IN_APP"); current.setCategory("WORK_ORDER");
        WorkOrderNotificationMapper mapper = proxy((method, args) -> {
            if ("selectTemplateById".equals(method)) return current;
            if ("updateTemplate".equals(method)) { updated.set((WorkOrderNotificationTemplate) args[0]); return 1; }
            return defaultValue(method);
        });

        WorkOrderNotificationTemplate result = new WorkOrderNotificationTemplateService(mapper).update(1L, request(), "admin");

        assertEquals("ASSIGN", result.getEventCode());
        assertEquals("工单${orderNo}", updated.get().getTitleTemplate());
        assertEquals("admin", updated.get().getUpdateBy());
    }

    private WorkOrderNotificationTemplateSaveRequest request()
    {
        WorkOrderNotificationTemplateSaveRequest value = new WorkOrderNotificationTemplateSaveRequest();
        value.setEventCode("ASSIGN"); value.setChannel("IN_APP"); value.setTemplateName("派单模板");
        value.setTitleTemplate("工单${orderNo}"); value.setContentTemplate("${title}已由${operatorName}派单");
        value.setEnabledFlag("1"); value.setMaxRetry(5); return value;
    }
    private interface Call { Object invoke(String method, Object[] args); }
    private WorkOrderNotificationMapper proxy(Call call)
    {
        return (WorkOrderNotificationMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WorkOrderNotificationMapper.class}, (object, method, args) -> call.invoke(method.getName(), args));
    }
    private Object defaultValue(String method) { return method.startsWith("select") ? Collections.emptyList() : 0; }
}
