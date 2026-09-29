package com.ruoyi.quartz.task;

import java.util.Date;
import com.ruoyi.workorder.application.model.WorkOrderSlaScanResult;
import com.ruoyi.workorder.application.service.WorkOrderSlaLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Quartz 白名单任务入口：全部业务判断由 SLA 生命周期模块完成。 */
@Component("workOrderSlaTask")
public class WorkOrderSlaTask
{
    private static final Logger log = LoggerFactory.getLogger(WorkOrderSlaTask.class);
    private final WorkOrderSlaLifecycleService lifecycleService;

    @Value("${workorder.auto-close-days:7}")
    private int autoCloseDays;

    public WorkOrderSlaTask(WorkOrderSlaLifecycleService lifecycleService) { this.lifecycleService = lifecycleService; }

    public void scan()
    {
        WorkOrderSlaScanResult result = lifecycleService.refresh(new Date());
        log.info("工单SLA扫描完成：检查{}条，更新{}条", result.getInspected(), result.getChanged());
    }

    public void autoClose()
    {
        WorkOrderSlaScanResult result = lifecycleService.autoClose(new Date(), autoCloseDays);
        log.info("工单自动关闭完成：检查{}条，关闭{}条", result.getInspected(), result.getAutoClosed());
    }
}
