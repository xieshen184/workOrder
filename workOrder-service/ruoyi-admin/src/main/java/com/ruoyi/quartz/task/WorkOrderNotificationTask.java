package com.ruoyi.quartz.task;

import java.util.Date;
import com.ruoyi.workorder.application.model.WorkOrderNotificationDispatchResult;
import com.ruoyi.workorder.application.service.WorkOrderNotificationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Quartz 白名单任务入口；领取、重试和渠道逻辑全部由通知派送模块负责。 */
@Component("workOrderNotificationTask")
public class WorkOrderNotificationTask
{
    private static final Logger log = LoggerFactory.getLogger(WorkOrderNotificationTask.class);
    private final WorkOrderNotificationDispatchService dispatchService;

    @Value("${workorder.notification.batch-size:50}")
    private int batchSize;

    public WorkOrderNotificationTask(WorkOrderNotificationDispatchService dispatchService)
    {
        this.dispatchService = dispatchService;
    }

    public void dispatch()
    {
        WorkOrderNotificationDispatchResult result = dispatchService.dispatchDue(new Date(), batchSize);
        log.info("工单通知任务派送完成：到期{}条，成功{}条，失败{}条",
                result.getDue(), result.getSucceeded(), result.getFailed());
    }
}
