package com.ruoyi.workorder.application.model;

/** Quartz 任务可记录的扫描摘要，不向调用方暴露逐单实现细节。 */
public class WorkOrderSlaScanResult
{
    private final int inspected;
    private final int changed;
    private final int autoClosed;

    public WorkOrderSlaScanResult(int inspected, int changed, int autoClosed)
    {
        this.inspected = inspected;
        this.changed = changed;
        this.autoClosed = autoClosed;
    }

    public int getInspected() { return inspected; }
    public int getChanged() { return changed; }
    public int getAutoClosed() { return autoClosed; }
}
