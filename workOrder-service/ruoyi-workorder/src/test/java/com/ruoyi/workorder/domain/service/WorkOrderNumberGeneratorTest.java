package com.ruoyi.workorder.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class WorkOrderNumberGeneratorTest
{
    private final WorkOrderNumberGenerator generator = new WorkOrderNumberGenerator();

    @Test
    public void shouldGenerateStableFixedLengthOrderNumber()
    {
        String first = generator.generate(9L, "wo-stable-request-001");
        String replay = generator.generate(9L, "wo-stable-request-001");
        assertEquals(first, replay);
        assertEquals(32, first.length());
        assertEquals("WO", first.substring(0, 2));
    }

    @Test
    public void shouldSeparateUsersAndRequests()
    {
        String original = generator.generate(9L, "wo-stable-request-001");
        assertFalse(original.equals(generator.generate(10L, "wo-stable-request-001")));
        assertFalse(original.equals(generator.generate(9L, "wo-stable-request-002")));
    }
}
