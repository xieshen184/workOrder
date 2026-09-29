package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.ruoyi.workorder.application.model.WorkOrderQuery;
import com.ruoyi.workorder.application.model.WorkOrderSlaScanResult;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderActionLog;
import com.ruoyi.workorder.mapper.WorkOrderActionLogMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import org.junit.Test;

public class WorkOrderSlaLifecycleServiceTest
{
    @Test
    public void shouldEnterWarningWindowWithoutChangingBusinessVersion()
    {
        Date now = new Date(10_000_000L);
        WorkOrder order = order("PROCESSING", new Date(now.getTime() + 10 * 60_000L));
        RecordingOrderMapper orders = new RecordingOrderMapper(order);
        RecordingLogMapper logs = new RecordingLogMapper();

        WorkOrderSlaScanResult result = new WorkOrderSlaLifecycleService(orders, logs).refresh(now);

        assertEquals(1, result.getChanged());
        assertEquals("1", orders.warning); assertEquals("0", orders.overdue);
        assertEquals("SLA_WARNING", logs.items.get(0).getActionType());
    }

    @Test
    public void shouldAccumulateFinishOverdueFlag()
    {
        Date now = new Date(20_000_000L);
        WorkOrder order = order("PROCESSING", new Date(now.getTime() - 1));
        RecordingOrderMapper orders = new RecordingOrderMapper(order);

        new WorkOrderSlaLifecycleService(orders, new RecordingLogMapper()).refresh(now);

        assertEquals("0", orders.warning); assertEquals("1", orders.overdue);
        assertEquals("1", orders.finishOverdue);
    }

    @Test
    public void shouldNotifyFinishOverdueEvenWhenEarlierStageWasAlreadyOverdue()
    {
        Date now = new Date(21_000_000L);
        WorkOrder order = order("PROCESSING", new Date(now.getTime() - 1));
        order.setOverdueFlag("1"); order.setResponseOverdue("1"); order.setFinishOverdue("0");
        RecordingLogMapper logs = new RecordingLogMapper();

        new WorkOrderSlaLifecycleService(new RecordingOrderMapper(order), logs).refresh(now);

        assertEquals(1, logs.items.size());
        assertEquals("SLA_OVERDUE", logs.items.get(0).getActionType());
    }

    @Test
    public void shouldWriteStageWarningEvenWhenPreviousWarningFlagWasSet()
    {
        Date now = new Date(22_000_000L);
        WorkOrder order = order("PROCESSING", new Date(now.getTime() + 10 * 60_000L));
        order.setWarningFlag("1");
        RecordingLogMapper logs = new RecordingLogMapper();

        new WorkOrderSlaLifecycleService(new RecordingOrderMapper(order), logs).refresh(now);

        assertEquals(1, logs.items.size());
        assertEquals("SLA_WARNING", logs.items.get(0).getActionType());
    }

    @Test
    public void shouldAutoCloseOnlyOptimisticallyMatchedCompletedOrder()
    {
        Date now = new Date(30_000_000L);
        WorkOrder order = order("COMPLETED", null);
        order.setConfirmedAt(new Date(now.getTime() - 8 * 86_400_000L));
        RecordingOrderMapper orders = new RecordingOrderMapper(order);
        RecordingLogMapper logs = new RecordingLogMapper();

        WorkOrderSlaScanResult result = new WorkOrderSlaLifecycleService(orders, logs).autoClose(now, 7);

        assertEquals(1, result.getAutoClosed());
        assertEquals("AUTO_CLOSE", logs.items.get(0).getActionType());
        assertEquals("CLOSED", logs.items.get(0).getToStatus());
    }

    private WorkOrder order(String status, Date finishDeadline)
    {
        WorkOrder order = new WorkOrder();
        order.setId(1L); order.setOrderNo("WO-1"); order.setStatus(status); order.setVersion(3);
        order.setFinishDeadline(finishDeadline); order.setSlaReminderBeforeMin(15);
        order.setWarningFlag("0"); order.setOverdueFlag("0");
        order.setResponseOverdue("0"); order.setArrivalOverdue("0"); order.setFinishOverdue("0");
        return order;
    }

    private static class RecordingOrderMapper implements WorkOrderMapper
    {
        private final WorkOrder order;
        private String warning;
        private String overdue;
        private String finishOverdue;
        RecordingOrderMapper(WorkOrder order) { this.order = order; }
        @Override public List<WorkOrder> selectSlaOpenOrders()
        { return "PROCESSING".equals(order.getStatus()) ? Collections.singletonList(order) : Collections.emptyList(); }
        @Override public List<WorkOrder> selectAutoCloseCandidates(Date before)
        { return "COMPLETED".equals(order.getStatus()) ? Collections.singletonList(order) : Collections.emptyList(); }
        @Override public int updateSlaFlags(Long id, String status, String warning, String overdue, String response,
                String arrival, String finish, Date time)
        { this.warning = warning; this.overdue = overdue; this.finishOverdue = finish; return 1; }
        @Override public int autoClose(Long id, Integer version, Date closedAt) { return 1; }
        @Override public int insert(WorkOrder value) { return 0; }
        @Override public WorkOrder selectById(Long id) { return order; }
        @Override public WorkOrder selectByIdForUpdate(Long id) { return order; }
        @Override public WorkOrder selectByOrderNo(String orderNo) { return null; }
        @Override public WorkOrder selectScopedById(WorkOrderQuery query) { return null; }
        @Override public List<WorkOrder> selectList(WorkOrderQuery query) { return Collections.emptyList(); }
        @Override public int applyCommand(Long id, String source, String target, Integer version,
                String action, Long engineerId, String engineerName, Date time, String updateBy) { return 0; }
    }

    private static class RecordingLogMapper implements WorkOrderActionLogMapper
    {
        private final List<WorkOrderActionLog> items = new ArrayList<WorkOrderActionLog>();
        @Override public int insert(WorkOrderActionLog log) { items.add(log); return 1; }
        @Override public WorkOrderActionLog selectByIdempotency(Long operatorId, String action, String key) { return null; }
        @Override public List<WorkOrderActionLog> selectByOrderId(Long orderId) { return items; }
    }
}
