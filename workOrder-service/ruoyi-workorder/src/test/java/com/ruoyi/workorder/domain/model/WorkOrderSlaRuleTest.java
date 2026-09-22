package com.ruoyi.workorder.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Date;
import org.junit.Test;

public class WorkOrderSlaRuleTest
{
    @Test
    public void shouldCalculateSubmissionSnapshotDeadlines()
    {
        WorkOrderSlaRule rule = new WorkOrderSlaRule();
        rule.setResponseMinutes(15); rule.setArrivalMinutes(45); rule.setFinishMinutes(null);
        Date submitted = new Date(1_000L);
        assertEquals(new Date(901_000L), rule.responseDeadline(submitted));
        assertEquals(new Date(2_701_000L), rule.arrivalDeadline(submitted));
        assertNull(rule.finishDeadline(submitted));
    }
}
