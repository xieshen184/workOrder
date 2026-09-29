package com.ruoyi.framework.aspectj;

import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.BusinessStatus;
import com.ruoyi.common.enums.HttpMethod;
import com.ruoyi.common.filter.PropertyPreExcludeFilter;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.domain.SysOperLog;

/**
 * 操作日志切面。
 *
 * <p>请求和响应共用同一套字段排除规则。日志用于审计业务动作，不应成为令牌、联系方式、
 * 地址或工单详情的副本，因此新增业务字段时应优先在注解中继续补充排除项。</p>
 */
@Aspect
@Component
public class LogAspect
{
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /** 所有操作日志默认排除的敏感字段。 */
    public static final String[] EXCLUDE_PROPERTIES = {
            "password", "oldPassword", "newPassword", "confirmPassword",
            "token", "authorization", "secret", "secretKey", "accessKey",
            "credential", "credentialMask", "phone", "phoneNumber", "applicantPhone",
            "address", "location", "description", "possibleCause", "evaluationContent"
    };

    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<Long>("Cost Time");

    @Before(value = "@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog)
    {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult)
    {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e)
    {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception exception,
            Object jsonResult)
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            operLog.setOperIp(IpUtils.getIpAddr());
            operLog.setOperUrl(StringUtils.substring(ServletUtils.getRequest().getRequestURI(), 0, 255));

            if (loginUser != null)
            {
                operLog.setOperName(loginUser.getUsername());
                SysUser currentUser = loginUser.getUser();
                if (StringUtils.isNotNull(currentUser) && StringUtils.isNotNull(currentUser.getDept()))
                {
                    operLog.setDeptName(currentUser.getDept().getDeptName());
                }
            }

            if (exception != null)
            {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                // 异常消息可能包含 SQL 或用户输入，审计日志只持久化异常类型。
                operLog.setErrorMsg(StringUtils.substring(exception.getClass().getName(), 0, 2000));
            }

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);

            Long startTime = TIME_THREADLOCAL.get();
            operLog.setCostTime(startTime == null ? 0L : System.currentTimeMillis() - startTime);
            AsyncManager.me().execute(AsyncFactory.recordOper(operLog));
        }
        catch (Exception logException)
        {
            // 日志组件自身失败时不输出请求值、异常消息和调用栈。
            log.error("Operation log persistence failed, exception={}", logException.getClass().getName());
        }
        finally
        {
            TIME_THREADLOCAL.remove();
        }
    }

    /**
     * 根据 @Log 注解设置审计记录。
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log controllerLog, SysOperLog operLog,
            Object jsonResult) throws Exception
    {
        operLog.setBusinessType(controllerLog.businessType().ordinal());
        operLog.setTitle(controllerLog.title());
        operLog.setOperatorType(controllerLog.operatorType().ordinal());
        if (controllerLog.isSaveRequestData())
        {
            setRequestValue(joinPoint, operLog, controllerLog.excludeParamNames());
        }
        if (controllerLog.isSaveResponseData() && StringUtils.isNotNull(jsonResult))
        {
            operLog.setJsonResult(StringUtils.substring(
                    JSON.toJSONString(jsonResult,
                            excludePropertyPreFilter(controllerLog.excludeParamNames())), 0, 2000));
        }
    }

    private void setRequestValue(JoinPoint joinPoint, SysOperLog operLog, String[] excludeParamNames) throws Exception
    {
        Map<?, ?> paramsMap = ServletUtils.getParamMap(ServletUtils.getRequest());
        String requestMethod = operLog.getRequestMethod();
        if (StringUtils.isEmpty(paramsMap)
                && StringUtils.equalsAny(requestMethod,
                        HttpMethod.PUT.name(), HttpMethod.POST.name(), HttpMethod.DELETE.name()))
        {
            operLog.setOperParam(StringUtils.substring(
                    argsArrayToString(joinPoint.getArgs(), excludeParamNames), 0, 2000));
        }
        else
        {
            operLog.setOperParam(StringUtils.substring(
                    JSON.toJSONString(paramsMap, excludePropertyPreFilter(excludeParamNames)), 0, 2000));
        }
    }

    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames)
    {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null)
        {
            for (Object parameter : paramsArray)
            {
                if (StringUtils.isNotNull(parameter) && !isFilterObject(parameter))
                {
                    try
                    {
                        params.append(JSON.toJSONString(parameter,
                                excludePropertyPreFilter(excludeParamNames))).append(' ');
                    }
                    catch (Exception ignored)
                    {
                        // 单个参数序列化失败不应影响业务请求，也不记录该参数的实际内容。
                    }
                }
            }
        }
        return params.toString().trim();
    }

    /**
     * 合并系统级和接口级敏感字段排除规则。
     */
    public PropertyPreExcludeFilter excludePropertyPreFilter(String[] excludeParamNames)
    {
        return new PropertyPreExcludeFilter().addExcludes(
                ArrayUtils.addAll(EXCLUDE_PROPERTIES, excludeParamNames));
    }

    /**
     * 判断对象是否属于不应序列化进操作日志的基础设施对象。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object object)
    {
        Class<?> clazz = object.getClass();
        if (clazz.isArray())
        {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        }
        else if (Collection.class.isAssignableFrom(clazz))
        {
            Collection collection = (Collection) object;
            for (Object value : collection)
            {
                return value instanceof MultipartFile;
            }
        }
        else if (Map.class.isAssignableFrom(clazz))
        {
            Map map = (Map) object;
            for (Object value : map.entrySet())
            {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return object instanceof MultipartFile || object instanceof HttpServletRequest
                || object instanceof HttpServletResponse || object instanceof BindingResult;
    }
}
