package com.ruoyi.workorder.application.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationEvent;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationRecipient;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTemplate;
import com.ruoyi.workorder.mapper.WorkOrderNotificationMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 通知事务发件箱深模块。
 *
 * <p>外部接口只有 {@link #publish(WorkOrderNotificationEvent)}。调用方不需要知道角色收件人、
 * 模板变量、渠道拆分和任务幂等规则；在业务事务中调用时，任务与业务数据一起提交。</p>
 */
@Service
public class WorkOrderNotificationOutboxService
{
    private static final String DISPATCHER_ROLE = "workorder_dispatcher";
    private final WorkOrderNotificationMapper mapper;

    public WorkOrderNotificationOutboxService(WorkOrderNotificationMapper mapper)
    {
        this.mapper = mapper;
    }

    public int publish(WorkOrderNotificationEvent event)
    {
        if (event == null || blank(event.getEventCode()) || blank(event.getOccurrenceKey()))
            throw new IllegalArgumentException("通知事件编码和发生键不能为空");
        List<WorkOrderNotificationTemplate> templates = mapper.selectEnabledTemplates(event.getEventCode());
        if (templates == null || templates.isEmpty()) return 0;
        List<WorkOrderNotificationRecipient> recipients = recipients(event);
        if (recipients.isEmpty()) return 0;

        int created = 0;
        Date now = event.getOccurredAt() == null ? new Date() : event.getOccurredAt();
        for (WorkOrderNotificationTemplate template : templates)
        {
            for (WorkOrderNotificationRecipient recipient : recipients)
            {
                WorkOrderNotificationTask task = task(event, template, recipient, now);
                try
                {
                    created += mapper.insertTask(task);
                }
                catch (DuplicateKeyException ignored)
                {
                    // 业务命令幂等重放或 Quartz 重入时，唯一键保证每个收件人/渠道只保留一条任务。
                }
            }
        }
        return created;
    }

    private List<WorkOrderNotificationRecipient> recipients(WorkOrderNotificationEvent event)
    {
        List<WorkOrderNotificationRecipient> source;
        if (toDispatchers(event.getEventCode()))
        {
            source = mapper.selectRecipientsByRole(DISPATCHER_ROLE);
        }
        else
        {
            Long recipientId = targetUser(event);
            WorkOrderNotificationRecipient recipient = recipientId == null ? null : mapper.selectRecipientById(recipientId);
            source = new ArrayList<WorkOrderNotificationRecipient>();
            if (recipient != null) source.add(recipient);
        }
        Map<Long, WorkOrderNotificationRecipient> unique = new LinkedHashMap<Long, WorkOrderNotificationRecipient>();
        if (source != null)
            for (WorkOrderNotificationRecipient item : source)
                if (item != null && item.getUserId() != null) unique.put(item.getUserId(), item);
        return new ArrayList<WorkOrderNotificationRecipient>(unique.values());
    }

    private Long targetUser(WorkOrderNotificationEvent event)
    {
        if (event.getTargetUserId() != null) return event.getTargetUserId();
        if ("FINISH".equals(event.getEventCode()) || "AUTO_CLOSE".equals(event.getEventCode()))
            return event.getApplicantId();
        return event.getAssigneeId();
    }

    private boolean toDispatchers(String eventCode)
    {
        return "SUBMIT".equals(eventCode) || "CANCEL".equals(eventCode) || "DELAY_REQUEST".equals(eventCode)
                || "SLA_WARNING".equals(eventCode) || "SLA_OVERDUE".equals(eventCode);
    }

    private WorkOrderNotificationTask task(WorkOrderNotificationEvent event,
            WorkOrderNotificationTemplate template, WorkOrderNotificationRecipient recipient, Date now)
    {
        JSONObject routeParams = new JSONObject();
        routeParams.put("id", event.getOrderId());
        WorkOrderNotificationTask task = new WorkOrderNotificationTask();
        task.setEventKey(event.getOccurrenceKey()); task.setEventCode(event.getEventCode());
        task.setCategory(template.getCategory()); task.setChannel(template.getChannel());
        task.setRecipientId(recipient.getUserId()); task.setRecipientName(recipient.displayName());
        // 站内信不复制手机号；只有将来启用短信模板时才冻结发送地址。
        task.setRecipientAddress("SMS".equals(template.getChannel()) ? recipient.getPhoneNumber() : null);
        task.setBusinessType("WORK_ORDER"); task.setBusinessId(event.getOrderId()); task.setOrderNo(event.getOrderNo());
        task.setTitle(render(template.getTitleTemplate(), event));
        task.setContent(render(template.getContentTemplate(), event));
        task.setRoutePath("/pages/order/detail/index"); task.setRouteParams(routeParams.toJSONString());
        task.setStatus("PENDING"); task.setRetryCount(0);
        task.setMaxRetry(template.getMaxRetry() == null ? 5 : template.getMaxRetry());
        task.setNextRetryAt(now); task.setCreateTime(now); task.setUpdateTime(now);
        return task;
    }

    private String render(String template, WorkOrderNotificationEvent event)
    {
        if (template == null) return "";
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("orderNo", value(event.getOrderNo())); values.put("title", value(event.getTitle()));
        values.put("operatorName", value(event.getOperatorName())); values.put("reason", value(event.getReason()));
        values.put("deadline", event.getDeadline() == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(event.getDeadline()));
        String rendered = template;
        for (Map.Entry<String, String> item : values.entrySet())
            rendered = rendered.replace("${" + item.getKey() + "}", item.getValue());
        return rendered;
    }

    private String value(String value) { return value == null ? "" : value; }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
