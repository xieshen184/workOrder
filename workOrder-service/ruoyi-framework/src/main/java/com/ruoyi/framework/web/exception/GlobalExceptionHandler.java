package com.ruoyi.framework.web.exception;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.DemoModeException;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.html.EscapeUtil;

/**
 * 全局异常转换器。
 *
 * <p>业务异常保留可理解的业务提示；系统异常统一返回通用文案，日志仅记录请求路径和
 * 异常类型，避免把 SQL、密钥、请求值或内部调用栈通过接口和生产日志暴露出去。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request)
    {
        log.warn("Access denied, uri={}, exception={}", request.getRequestURI(), e.getClass().getSimpleName());
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
            HttpServletRequest request)
    {
        log.warn("Request method not supported, uri={}, method={}", request.getRequestURI(), e.getMethod());
        return AjaxResult.error("请求方式不支持");
    }

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request)
    {
        // ServiceException 的消息由业务代码定义，可以安全地向当前用户展示；日志不重复记录原文。
        log.warn("Business request rejected, uri={}, code={}, exception={}",
                request.getRequestURI(), e.getCode(), e.getClass().getSimpleName());
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request)
    {
        log.error("Required path variable missing, uri={}, variable={}",
                request.getRequestURI(), safeName(e.getVariableName()));
        return AjaxResult.error("请求路径缺少必要参数");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
            HttpServletRequest request)
    {
        String parameterName = safeName(e.getName());
        log.warn("Request parameter type mismatch, uri={}, parameter={}, exception={}",
                request.getRequestURI(), parameterName, e.getClass().getSimpleName());
        // 不回显用户提交值和 Java 内部类型，避免敏感数据与实现细节泄露。
        return AjaxResult.error("请求参数格式不正确：" + parameterName);
    }

    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        log.error("Unhandled runtime exception, uri={}, exception={}",
                request.getRequestURI(), e.getClass().getName());
        return AjaxResult.error("系统异常，请联系管理员");
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request)
    {
        log.error("Unhandled system exception, uri={}, exception={}",
                request.getRequestURI(), e.getClass().getName());
        return AjaxResult.error("系统异常，请联系管理员");
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e)
    {
        log.warn("Request binding validation failed, exception={}", e.getClass().getSimpleName());
        return AjaxResult.error(firstValidationMessage(e.getFieldError()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        log.warn("Request body validation failed, exception={}", e.getClass().getSimpleName());
        return AjaxResult.error(firstValidationMessage(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(DemoModeException.class)
    public AjaxResult handleDemoModeException(DemoModeException e)
    {
        return AjaxResult.error("演示模式，不允许操作");
    }

    private String firstValidationMessage(FieldError fieldError)
    {
        if (fieldError == null || !StringUtils.isNotEmpty(fieldError.getDefaultMessage()))
        {
            return "请求参数校验失败";
        }
        return fieldError.getDefaultMessage();
    }

    private String safeName(String name)
    {
        return StringUtils.isEmpty(name) ? "unknown" : EscapeUtil.clean(name);
    }
}
