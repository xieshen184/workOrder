package com.ruoyi.workorder.application.service;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workorder.application.model.CreateWorkOrderCommand;
import com.ruoyi.workorder.application.model.CreateWorkOrderResult;
import com.ruoyi.workorder.application.model.WorkOrderActor;
import com.ruoyi.workorder.domain.model.WorkOrder;
import com.ruoyi.workorder.domain.model.WorkOrderCategory;
import com.ruoyi.workorder.domain.service.WorkOrderNumberGenerator;
import com.ruoyi.workorder.mapper.WorkOrderCategoryMapper;
import com.ruoyi.workorder.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 工单创建门面。
 *
 * <p>负责身份快照校验、幂等键校验和并发重放收敛；真正的多表写入由
 * {@link WorkOrderCreationService} 在独立事务中完成，确保唯一键冲突时整单回滚。</p>
 */
@Service
public class WorkOrderApplicationService
{
    @Autowired private WorkOrderMapper orderMapper;
    @Autowired private WorkOrderCategoryMapper categoryMapper;
    @Autowired private WorkOrderCreationService creationService;
    private final WorkOrderNumberGenerator numberGenerator = new WorkOrderNumberGenerator();

    public CreateWorkOrderResult create(CreateWorkOrderCommand command, WorkOrderActor actor, String idempotencyKey)
    {
        validateActor(actor);
        validateIdempotencyKey(idempotencyKey);
        WorkOrderCategory category = categoryMapper.selectActiveById(command.getCategoryId());
        if (category == null) throw new ServiceException("故障分类不存在或已停用", HttpStatus.BAD_REQUEST);

        // 同一用户和同一幂等键始终得到同一工单号，数据库唯一键因此能兜住并发重放。
        String orderNo = numberGenerator.generate(actor.getUserId(), idempotencyKey);
        WorkOrder existing = orderMapper.selectByOrderNo(orderNo);
        if (existing != null) return result(existing, true);

        try
        {
            return result(creationService.createNew(command, actor, category, idempotencyKey, orderNo), false);
        }
        catch (DuplicateKeyException duplicate)
        {
            // 并发请求中的失败方在事务回滚后读取成功方结果，对调用方表现为幂等成功。
            existing = orderMapper.selectByOrderNo(orderNo);
            if (existing != null) return result(existing, true);
            throw new ServiceException("工单提交冲突，请刷新后重试", HttpStatus.CONFLICT);
        }
    }

    private CreateWorkOrderResult result(WorkOrder order, boolean replay)
    {
        return new CreateWorkOrderResult(order.getId(), order.getOrderNo(), order.getStatus(), order.getVersion(), replay);
    }

    private void validateActor(WorkOrderActor actor)
    {
        if (actor == null || actor.getUserId() == null) throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        if (blank(actor.getDisplayName())) throw new ServiceException("当前账号缺少姓名，请先完善个人资料", HttpStatus.BAD_REQUEST);
        if (blank(actor.getPhone())) throw new ServiceException("当前账号缺少联系电话，请先完善个人资料", HttpStatus.BAD_REQUEST);
    }

    private void validateIdempotencyKey(String key)
    {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,64}"))
            throw new ServiceException("Idempotency-Key 格式不正确", HttpStatus.BAD_REQUEST);
    }

    private boolean blank(String value) { return value == null || value.trim().length() == 0; }
}
