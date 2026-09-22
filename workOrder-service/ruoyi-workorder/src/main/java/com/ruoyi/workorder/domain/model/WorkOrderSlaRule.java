package com.ruoyi.workorder.domain.model;

import java.util.Date;

/** Active SLA rule selected and snapshotted when a work order is submitted. */
public class WorkOrderSlaRule
{
    private Long id;
    private Integer responseMinutes;
    private Integer arrivalMinutes;
    private Integer finishMinutes;

    public Date responseDeadline(Date submittedAt) { return plusMinutes(submittedAt, responseMinutes); }
    public Date arrivalDeadline(Date submittedAt) { return plusMinutes(submittedAt, arrivalMinutes); }
    public Date finishDeadline(Date submittedAt) { return plusMinutes(submittedAt, finishMinutes); }

    private Date plusMinutes(Date source, Integer minutes)
    {
        return minutes == null ? null : new Date(source.getTime() + minutes.longValue() * 60_000L);
    }

    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Integer getResponseMinutes() { return responseMinutes; } public void setResponseMinutes(Integer v) { responseMinutes = v; }
    public Integer getArrivalMinutes() { return arrivalMinutes; } public void setArrivalMinutes(Integer v) { arrivalMinutes = v; }
    public Integer getFinishMinutes() { return finishMinutes; } public void setFinishMinutes(Integer v) { finishMinutes = v; }
}
