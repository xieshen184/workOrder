package com.ruoyi.workorder.domain.model;

/**
 * 可以改变工单或追加工单过程记录的领域动作。
 *
 * <p>延期审批保持主状态不变，自动关闭负责从已完成进入已关闭；二者都进入统一动作审计。</p>
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
    REJECT_DELAY,
    FINISH,
    CONFIRM,
    RETURN,
    EVALUATE,
    AUTO_CLOSE
}
