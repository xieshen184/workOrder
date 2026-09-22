package com.ruoyi.workorder.application.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.List;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderCategorySaveRequest;
import com.ruoyi.workorder.domain.model.WorkOrderCategory;
import com.ruoyi.workorder.mapper.WorkOrderCategoryMapper;
import org.junit.Test;

public class WorkOrderCategoryServiceTest
{
    @Test
    public void categoryCodeShouldRemainImmutableAfterCreation()
    {
        FakeMapper mapper = new FakeMapper();
        mapper.current = category(1L, "OLD_CODE", "0", 0L);
        try
        {
            new WorkOrderCategoryService(mapper).update(1L, request("NEW_CODE", "0", 0L), "admin");
            fail("expected ServiceException");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), error.getCode());
        }
        assertEquals(0, mapper.updateCount);
    }

    @Test
    public void parentWithActiveChildrenCannotBeDisabled()
    {
        FakeMapper mapper = new FakeMapper();
        mapper.current = category(1L, "EQUIPMENT", "0", 0L);
        mapper.activeChildren = 1;
        try
        {
            new WorkOrderCategoryService(mapper).update(1L, request("EQUIPMENT", "1", 0L), "admin");
            fail("expected ServiceException");
        }
        catch (ServiceException error)
        {
            assertEquals(Integer.valueOf(HttpStatus.CONFLICT), error.getCode());
        }
    }

    @Test
    public void referencedLeafCanBeDisabledWithoutDeletingHistory()
    {
        FakeMapper mapper = new FakeMapper();
        mapper.current = category(2L, "DEVICE", "0", 0L);
        mapper.current.setReferenceCount(12);
        WorkOrderCategory result = new WorkOrderCategoryService(mapper)
                .update(2L, request("DEVICE", "1", 0L), "admin");

        assertEquals(1, mapper.updateCount);
        assertEquals("1", result.getStatus());
        assertEquals(Integer.valueOf(12), result.getReferenceCount());
    }

    private WorkOrderCategorySaveRequest request(String code, String status, Long parentId)
    {
        WorkOrderCategorySaveRequest request = new WorkOrderCategorySaveRequest();
        request.setParentId(parentId); request.setCategoryCode(code); request.setCategoryName("分类");
        request.setOrderNum(1); request.setStatus(status); return request;
    }

    private WorkOrderCategory category(Long id, String code, String status, Long parentId)
    {
        WorkOrderCategory value = new WorkOrderCategory();
        value.setId(id); value.setCategoryCode(code); value.setCategoryName("分类");
        value.setStatus(status); value.setParentId(parentId); value.setReferenceCount(0); return value;
    }

    private static class FakeMapper implements WorkOrderCategoryMapper
    {
        private WorkOrderCategory current;
        private int activeChildren;
        private int updateCount;
        @Override public WorkOrderCategory selectById(Long id) { return current; }
        @Override public WorkOrderCategory selectActiveById(Long id)
        { return current != null && "0".equals(current.getStatus()) ? current : null; }
        @Override public WorkOrderCategory selectByCode(String code) { return null; }
        @Override public List<WorkOrderCategory> selectActiveList() { return new ArrayList<WorkOrderCategory>(); }
        @Override public List<WorkOrderCategory> selectManageList() { return new ArrayList<WorkOrderCategory>(); }
        @Override public int countActiveChildren(Long id) { return activeChildren; }
        @Override public int insert(WorkOrderCategory category) { current = category; category.setId(1L); return 1; }
        @Override public int update(WorkOrderCategory category)
        {
            updateCount++;
            category.setReferenceCount(current.getReferenceCount());
            current = category;
            return 1;
        }
    }
}
