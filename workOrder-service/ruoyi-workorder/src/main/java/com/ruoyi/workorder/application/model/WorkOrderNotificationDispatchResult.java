package com.ruoyi.workorder.application.model;

public class WorkOrderNotificationDispatchResult
{
    private final int due;
    private final int succeeded;
    private final int failed;

    public WorkOrderNotificationDispatchResult(int due, int succeeded, int failed)
    {
        this.due = due; this.succeeded = succeeded; this.failed = failed;
    }

    public int getDue() { return due; }
    public int getSucceeded() { return succeeded; }
    public int getFailed() { return failed; }
}
