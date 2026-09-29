package com.ruoyi.workorder.application.service;

import java.util.Date;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationMessage;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;
import com.ruoyi.workorder.mapper.WorkOrderNotificationMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/** 站内信渠道适配器：一条成功任务生成一条收件人可见消息。 */
@Component
public class InAppWorkOrderNotificationAdapter implements WorkOrderNotificationChannelAdapter
{
    private final WorkOrderNotificationMapper mapper;

    public InAppWorkOrderNotificationAdapter(WorkOrderNotificationMapper mapper) { this.mapper = mapper; }

    @Override public boolean supports(String channel) { return "IN_APP".equals(channel); }

    @Override
    public void send(WorkOrderNotificationTask task, Date sentAt)
    {
        WorkOrderNotificationMessage message = new WorkOrderNotificationMessage();
        message.setTaskId(task.getId()); message.setRecipientId(task.getRecipientId());
        message.setCategory(task.getCategory()); message.setEventCode(task.getEventCode());
        message.setTitle(task.getTitle()); message.setContent(task.getContent());
        message.setBusinessType(task.getBusinessType()); message.setBusinessId(task.getBusinessId());
        message.setOrderNo(task.getOrderNo()); message.setRoutePath(task.getRoutePath());
        message.setRouteParams(task.getRouteParams()); message.setReadFlag("0"); message.setCreateTime(sentAt);
        try { mapper.insertMessage(message); }
        catch (DuplicateKeyException ignored)
        {
            // 进程在“消息落库后、任务置成功前”中断时会重试；task_id 唯一键把重试变成成功重放。
        }
    }
}
