package com.ruoyi.workorder.domain.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class WorkOrderAttachmentStageTest
{
    @Test public void shouldDefaultLegacyUploadsToSubmit() { assertEquals(WorkOrderAttachmentStage.SUBMIT, WorkOrderAttachmentStage.fromUploadValue(null)); }
    @Test public void shouldNormalizeSupportedStage() { assertEquals(WorkOrderAttachmentStage.ARRIVAL, WorkOrderAttachmentStage.fromUploadValue("arrival")); }
    @Test public void shouldAcceptDelayEvidenceStage() { assertEquals(WorkOrderAttachmentStage.DELAY, WorkOrderAttachmentStage.fromUploadValue("delay")); }
    @Test(expected = com.ruoyi.common.exception.ServiceException.class)
    public void shouldRejectUnsupportedStage() { WorkOrderAttachmentStage.fromUploadValue("ASSESSMENT"); }
}
