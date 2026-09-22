package com.ruoyi.workorder.domain.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class WorkOrderAttachmentStageTest
{
    @Test public void shouldDefaultLegacyUploadsToSubmit() { assertEquals(WorkOrderAttachmentStage.SUBMIT, WorkOrderAttachmentStage.fromUploadValue(null)); }
    @Test public void shouldNormalizeSupportedStage() { assertEquals(WorkOrderAttachmentStage.ARRIVAL, WorkOrderAttachmentStage.fromUploadValue("arrival")); }
    @Test(expected = com.ruoyi.common.exception.ServiceException.class)
    public void shouldRejectUnsupportedStage() { WorkOrderAttachmentStage.fromUploadValue("ASSESSMENT"); }
}
