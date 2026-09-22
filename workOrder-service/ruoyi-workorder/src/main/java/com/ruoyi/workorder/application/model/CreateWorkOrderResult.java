package com.ruoyi.workorder.application.model;

public class CreateWorkOrderResult
{
    private final Long id;
    private final String orderNo;
    private final String status;
    private final Integer version;
    private final boolean idempotentReplay;

    public CreateWorkOrderResult(Long id, String orderNo, String status, Integer version, boolean idempotentReplay)
    {
        this.id = id;
        this.orderNo = orderNo;
        this.status = status;
        this.version = version;
        this.idempotentReplay = idempotentReplay;
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public String getStatus() { return status; }
    public Integer getVersion() { return version; }
    public boolean isIdempotentReplay() { return idempotentReplay; }
}
