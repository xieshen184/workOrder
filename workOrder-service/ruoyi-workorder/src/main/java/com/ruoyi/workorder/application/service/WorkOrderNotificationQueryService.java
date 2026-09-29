package com.ruoyi.workorder.application.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.WorkOrderNotificationQuery;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationMessage;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;
import com.ruoyi.workorder.mapper.WorkOrderNotificationMapper;
import org.springframework.stereotype.Service;

/** 用户消息和后台发送记录的统一查询/状态接口。 */
@Service
public class WorkOrderNotificationQueryService
{
    private final WorkOrderNotificationMapper mapper;

    public WorkOrderNotificationQueryService(WorkOrderNotificationMapper mapper) { this.mapper = mapper; }

    public List<WorkOrderNotificationMessage> selectMessages(Long userId, WorkOrderNotificationQuery query)
    {
        requireUser(userId); validateMessageQuery(query);
        return mapper.selectMessages(userId, query == null ? new WorkOrderNotificationQuery() : query);
    }

    public Map<String, Integer> unreadCount(Long userId)
    {
        requireUser(userId);
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        result.put("total", 0); result.put("workOrder", 0); result.put("approval", 0); result.put("system", 0);
        List<Map<String, Object>> rows = mapper.countUnreadByCategory(userId);
        if (rows == null) return result;
        for (Map<String, Object> row : rows)
        {
            String category = text(row, "category");
            int count = number(row, "total");
            if ("WORK_ORDER".equals(category)) result.put("workOrder", count);
            else if ("APPROVAL".equals(category)) result.put("approval", count);
            else if ("SYSTEM".equals(category)) result.put("system", count);
            result.put("total", result.get("total") + count);
        }
        return result;
    }

    public void markRead(Long id, Long userId)
    {
        requireUser(userId);
        if (id == null) throw new ServiceException("缺少消息编号", HttpStatus.BAD_REQUEST);
        // 幂等接口：消息已读、消息不存在或不属于本人时都不泄漏额外信息。
        mapper.markRead(id, userId, new Date());
    }

    public int markAllRead(Long userId, String category)
    {
        requireUser(userId); validateCategory(category);
        return mapper.markAllRead(userId, emptyToNull(category), new Date());
    }

    @DataScope(deptAlias = "d", userAlias = "u", permission = "workorder:message:record")
    public List<WorkOrderNotificationTask> selectTasks(WorkOrderNotificationQuery query)
    {
        List<WorkOrderNotificationTask> rows = mapper.selectTasks(query == null ? new WorkOrderNotificationQuery() : query);
        // 发送记录页面只返回脱敏地址；实际发送地址永不经管理接口明文回显。
        for (WorkOrderNotificationTask row : rows) row.setRecipientAddress(mask(row.getRecipientAddress()));
        return rows;
    }

    public void retry(Long id)
    {
        if (id == null) throw new ServiceException("缺少通知任务编号", HttpStatus.BAD_REQUEST);
        if (mapper.manualRetry(id, new Date()) != 1)
            throw new ServiceException("只有重试中或已终止的任务可以人工重试", HttpStatus.CONFLICT);
    }

    private void validateMessageQuery(WorkOrderNotificationQuery query)
    {
        if (query == null) return;
        validateCategory(query.getCategory());
        if (!blank(query.getReadStatus()) && !"0".equals(query.getReadStatus()) && !"1".equals(query.getReadStatus()))
            throw new ServiceException("消息已读状态不正确", HttpStatus.BAD_REQUEST);
    }

    private void validateCategory(String category)
    {
        if (blank(category)) return;
        if (!"WORK_ORDER".equals(category) && !"APPROVAL".equals(category) && !"SYSTEM".equals(category))
            throw new ServiceException("消息分类不正确", HttpStatus.BAD_REQUEST);
    }

    private void requireUser(Long userId)
    {
        if (userId == null) throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
    }

    private String text(Map<String, Object> row, String key)
    {
        Object value = row.get(key);
        if (value == null) value = row.get(key.toUpperCase());
        return value == null ? null : String.valueOf(value);
    }
    private int number(Map<String, Object> row, String key)
    {
        Object value = row.get(key); if (value == null) value = row.get(key.toUpperCase());
        return value instanceof Number ? ((Number) value).intValue() : value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }
    private String mask(String address)
    {
        if (blank(address)) return null;
        if (address.length() <= 4) return "****";
        return address.substring(0, Math.min(3, address.length())) + "****" + address.substring(address.length() - 2);
    }
    private String emptyToNull(String value) { return blank(value) ? null : value; }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
