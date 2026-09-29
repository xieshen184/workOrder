package com.ruoyi.workorder.application.service;

import java.util.Date;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;

/** 渠道接缝。当前生产适配器为站内信；短信供应商可在不改调度逻辑的情况下接入。 */
public interface WorkOrderNotificationChannelAdapter
{
    boolean supports(String channel);
    void send(WorkOrderNotificationTask task, Date sentAt);
}
