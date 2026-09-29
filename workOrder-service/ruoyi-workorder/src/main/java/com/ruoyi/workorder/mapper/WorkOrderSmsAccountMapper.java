package com.ruoyi.workorder.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import com.ruoyi.workorder.domain.model.WorkOrderSmsAccount;
import org.apache.ibatis.annotations.Param;

/** 短信账户快照和运营设置持久化接口。 */
public interface WorkOrderSmsAccountMapper
{
    WorkOrderSmsAccount selectAccount();
    Map<String, Object> selectSendStatistics();
    int updateSettings(@Param("warningThreshold") BigDecimal warningThreshold,
            @Param("retryEnabled") String retryEnabled, @Param("updateBy") String updateBy,
            @Param("updateTime") Date updateTime);
}
