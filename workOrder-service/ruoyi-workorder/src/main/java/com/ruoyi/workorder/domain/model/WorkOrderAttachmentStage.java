package com.ruoyi.workorder.domain.model;

import java.util.Locale;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

public enum WorkOrderAttachmentStage
{
    SUBMIT(null),
    ARRIVAL("workorder:order:arrive"),
    PROCESS("workorder:order:progress"),
    DELAY("workorder:delay:add"),
    FINISH("workorder:order:finish");

    private final String permission;

    WorkOrderAttachmentStage(String permission) { this.permission = permission; }

    public String getPermission() { return permission; }

    public static WorkOrderAttachmentStage fromUploadValue(String value)
    {
        String normalized = value == null || value.trim().length() == 0
                ? SUBMIT.name() : value.trim().toUpperCase(Locale.ROOT);
        try
        {
            return valueOf(normalized);
        }
        catch (IllegalArgumentException error)
        {
            throw new ServiceException("附件阶段仅支持SUBMIT/ARRIVAL/PROCESS/DELAY/FINISH", HttpStatus.BAD_REQUEST);
        }
    }

    public static WorkOrderAttachmentStage forAction(WorkOrderAction action)
    {
        if (action == WorkOrderAction.ARRIVE) return ARRIVAL;
        if (action == WorkOrderAction.PROGRESS) return PROCESS;
        if (action == WorkOrderAction.FINISH) return FINISH;
        return null;
    }
}
