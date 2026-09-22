package com.ruoyi.workorder.domain.model;

/**
 * 可以改变工单或追加工单过程记录的领域动作。
 *
 * <p>延期和自动关闭动作仅预留编码，第一版状态机不会开放对应迁移。</p>
 */
public enum WorkOrderAction
{
    SUBMIT,
    CANCEL,
    ASSIGN,
    REASSIGN,
    ACCEPT,
    ARRIVE,
    ASSESS,
    PROGRESS,
    REQUEST_DELAY,
    APPROVE_DELAY,
    FINISH,
    CONFIRM,
    RETURN,
    EVALUATE,
    AUTO_CLOSE
}
