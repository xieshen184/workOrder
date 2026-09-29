package com.ruoyi.workorder.application.model;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 运营统计的统一筛选条件。
 *
 * <p>日期使用 yyyy-MM-dd，服务层会补齐默认区间并校验先后关系；继承 BaseEntity
 * 是为了让若依的数据范围切面把授权 SQL 放入 params.dataScope。</p>
 */
public class WorkOrderAnalyticsQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String beginTime;
    private String endTime;
    private Long deptId;
    private Long engineerId;
    private Long categoryId;

    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getEngineerId() { return engineerId; }
    public void setEngineerId(Long engineerId) { this.engineerId = engineerId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
