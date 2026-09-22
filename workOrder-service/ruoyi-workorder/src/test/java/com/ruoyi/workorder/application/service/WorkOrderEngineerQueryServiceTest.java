package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.EngineerStatusUpdateRequest;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.application.model.WorkOrderEngineerQuery;
import com.ruoyi.workorder.domain.model.WorkOrderEngineer;
import com.ruoyi.workorder.mapper.WorkOrderEngineerMapper;
import org.junit.Test;

public class WorkOrderEngineerQueryServiceTest
{
    @Test
    public void nonEngineerCannotCreateAStatusRow()
    {
        FakeMapper mapper = new FakeMapper();
        try
        {
            new WorkOrderEngineerQueryService(mapper).updateOwnStatus(request("ON_DUTY", "AVAILABLE"), actor());
            fail("expected ServiceException");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), error.getCode());
        }
        assertEquals(0, mapper.upsertCount);
    }

    @Test
    public void engineerWithActiveOrdersCannotGoOffDuty()
    {
        FakeMapper mapper = new FakeMapper();
        mapper.engineer = engineer(2);
        try
        {
            new WorkOrderEngineerQueryService(mapper).updateOwnStatus(request("OFF_DUTY", "BUSY"), actor());
            fail("expected ServiceException");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), error.getCode());
        }
    }

    @Test
    public void validManualStatusShouldBeStoredAndReturned()
    {
        FakeMapper mapper = new FakeMapper();
        mapper.engineer = engineer(0);
        WorkOrderEngineer result = new WorkOrderEngineerQueryService(mapper)
                .updateOwnStatus(request("ON_DUTY", "AVAILABLE"), actor());

        assertEquals(1, mapper.upsertCount);
        assertEquals("ON_DUTY", result.getDutyStatus());
        assertEquals("AVAILABLE", result.getStatus());
    }

    private EngineerStatusUpdateRequest request(String duty, String work)
    {
        EngineerStatusUpdateRequest request = new EngineerStatusUpdateRequest();
        request.setDutyStatus(duty); request.setWorkStatus(work); return request;
    }

    private WorkOrderActor actor()
    {
        return new WorkOrderActor(9L, "engineer", "工程师", null, 2L, "维修科",
                new HashSet<String>(Arrays.asList("workorder:engineer:status")));
    }

    private WorkOrderEngineer engineer(int load)
    {
        WorkOrderEngineer value = new WorkOrderEngineer();
        value.setId(9L); value.setName("工程师"); value.setDeptId(2L); value.setLoad(load); return value;
    }

    private static class FakeMapper implements WorkOrderEngineerMapper
    {
        private WorkOrderEngineer engineer;
        private int upsertCount;
        @Override public WorkOrderEngineer selectById(Long id) { return engineer; }
        @Override public WorkOrderEngineer selectEligibleById(Long id) { return engineer; }
        @Override public List<WorkOrderEngineer> selectList(WorkOrderEngineerQuery query)
        { return new ArrayList<WorkOrderEngineer>(); }
        @Override public int upsertStatus(Long id, String name, Long deptId, String duty,
                String work, Date changedAt)
        {
            upsertCount++;
            engineer.setDutyStatus(duty); engineer.setStatus(work); engineer.setStatusSource("MANUAL");
            return 1;
        }
    }
}
