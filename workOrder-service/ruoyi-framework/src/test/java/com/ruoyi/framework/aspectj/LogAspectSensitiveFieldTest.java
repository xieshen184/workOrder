package com.ruoyi.framework.aspectj;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import com.alibaba.fastjson2.JSON;

/**
 * 验证操作日志的请求和响应序列化都会排除敏感字段。
 */
public class LogAspectSensitiveFieldTest
{
    @Test
    public void shouldExcludeSensitiveFieldsFromSerializedLogPayload()
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "WO-001");
        payload.put("phoneNumber", "13800000000");
        payload.put("token", "private-token");
        payload.put("description", "sensitive work order details");

        String json = JSON.toJSONString(payload, new LogAspect().excludePropertyPreFilter(new String[0]));

        Assert.assertTrue(json.contains("title"));
        Assert.assertFalse(json.contains("13800000000"));
        Assert.assertFalse(json.contains("private-token"));
        Assert.assertFalse(json.contains("sensitive work order details"));
    }
}
