package com.ruoyi.workorder.application;

import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderCommand;
import com.ruoyi.workorder.application.model.WorkOrderCommandResult;

/**
 * Stateful work-order command module interface.
 *
 * <p>Callers provide one immutable command and the authenticated actor. The implementation hides
 * transition rules, optimistic concurrency, idempotency, history, attachment binding and audit
 * persistence behind this seam.</p>
 */
public interface WorkOrderCommandGateway
{
    WorkOrderCommandResult execute(WorkOrderCommand command, WorkOrderActor actor);
}
