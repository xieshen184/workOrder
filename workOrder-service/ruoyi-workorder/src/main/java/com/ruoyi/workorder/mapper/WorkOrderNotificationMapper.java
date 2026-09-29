package com.ruoyi.workorder.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.ruoyi.workorder.application.model.WorkOrderNotificationQuery;
import com.ruoyi.workorder.application.model.WorkOrderNotificationTemplateQuery;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationMessage;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationRecipient;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTask;
import com.ruoyi.workorder.domain.model.WorkOrderNotificationTemplate;
import org.apache.ibatis.annotations.Param;

public interface WorkOrderNotificationMapper
{
    List<WorkOrderNotificationTemplate> selectEnabledTemplates(String eventCode);
    List<WorkOrderNotificationTemplate> selectTemplates(WorkOrderNotificationTemplateQuery query);
    WorkOrderNotificationTemplate selectTemplateById(Long id);
    int updateTemplate(WorkOrderNotificationTemplate template);
    List<WorkOrderNotificationRecipient> selectRecipientsByRole(String roleKey);
    WorkOrderNotificationRecipient selectRecipientById(Long userId);
    int insertTask(WorkOrderNotificationTask task);
    List<WorkOrderNotificationMessage> selectMessages(@Param("recipientId") Long recipientId,
            @Param("query") WorkOrderNotificationQuery query);
    List<Map<String, Object>> countUnreadByCategory(Long recipientId);
    int markRead(@Param("id") Long id, @Param("recipientId") Long recipientId, @Param("readAt") Date readAt);
    int markAllRead(@Param("recipientId") Long recipientId, @Param("category") String category,
            @Param("readAt") Date readAt);
    List<Long> selectDueTaskIds(@Param("now") Date now, @Param("lockExpiredAt") Date lockExpiredAt,
            @Param("limit") int limit);
    int claimTask(@Param("id") Long id, @Param("workerId") String workerId, @Param("now") Date now,
            @Param("lockExpiredAt") Date lockExpiredAt);
    WorkOrderNotificationTask selectTaskById(Long id);
    int insertMessage(WorkOrderNotificationMessage message);
    int markSuccess(@Param("id") Long id, @Param("workerId") String workerId, @Param("sentAt") Date sentAt);
    int markFailure(@Param("id") Long id, @Param("workerId") String workerId,
            @Param("status") String status, @Param("retryCount") int retryCount,
            @Param("nextRetryAt") Date nextRetryAt, @Param("lastError") String lastError,
            @Param("updateTime") Date updateTime);
    List<WorkOrderNotificationTask> selectTasks(WorkOrderNotificationQuery query);
    int manualRetry(@Param("id") Long id, @Param("now") Date now);
}
