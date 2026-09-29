package com.ruoyi.framework.web.exception;

import java.lang.reflect.Proxy;
import javax.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Test;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * 验证系统异常不会把内部异常消息返回客户端。
 */
public class GlobalExceptionHandlerTest
{
    @Test
    public void shouldHideRuntimeExceptionMessage()
    {
        HttpServletRequest request = request("/workorder/orders/1");
        RuntimeException exception = new RuntimeException("jdbc password=private-value");

        AjaxResult result = new GlobalExceptionHandler().handleRuntimeException(exception, request);

        Assert.assertEquals("系统异常，请联系管理员", result.get(AjaxResult.MSG_TAG));
        Assert.assertFalse(result.toString().contains("private-value"));
    }

    private HttpServletRequest request(String uri)
    {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> "getRequestURI".equals(method.getName()) ? uri : null);
    }
}
