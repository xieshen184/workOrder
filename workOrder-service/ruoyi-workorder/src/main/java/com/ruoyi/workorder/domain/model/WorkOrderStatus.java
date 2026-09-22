package com.ruoyi.workorder.domain.model;

/**
 * 工单主状态。
 *
 * <p>超时、预警和延期审批中属于正交标记，不扩展为主状态。</p>
 */
public enum WorkOrderStatus
{
    DRAFT("DRAFT", "草稿", false),
    WAIT_ASSIGN("WAIT_ASSIGN", "待派单", false),
    WAIT_ACCEPT("WAIT_ACCEPT", "待接单", false),
    ACCEPTED("ACCEPTED", "已接单", false),
    PROCESSING("PROCESSING", "处理中", false),
    WAIT_CONFIRM("WAIT_CONFIRM", "待确认", false),
    COMPLETED("COMPLETED", "已完成", false),
    CLOSED("CLOSED", "已关闭", true),
    CANCELLED("CANCELLED", "已取消", true);

    private final String code;
    private final String description;
    private final boolean terminal;

    WorkOrderStatus(String code, String description, boolean terminal)
    {
        this.code = code;
        this.description = description;
        this.terminal = terminal;
    }

    public String getCode()
    {
        return code;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isTerminal()
    {
        return terminal;
    }

    public static WorkOrderStatus fromCode(String code)
    {
        if (code != null)
        {
            for (WorkOrderStatus status : values())
            {
                if (status.code.equals(code))
                {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("未知工单状态: " + code);
    }
}
