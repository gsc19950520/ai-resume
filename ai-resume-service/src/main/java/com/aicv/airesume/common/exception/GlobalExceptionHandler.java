package com.aicv.airesume.common.exception;

import com.aicv.airesume.common.constant.ResponseCode;
import com.aicv.airesume.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLSyntaxErrorException;
import java.util.List;

/**
 * 全局异常处理类
 * 统一处理系统中的各类异常，提供友好的错误响应
 *
 * @author AI Resume Team
 * @date 2023-07-01
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ApiResponse<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {}", e.getMessage(), e);
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数验证异常 (JSON请求体)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = getErrorMessageFromBindingResult(bindingResult);
        log.warn("参数验证异常: {}", errorMessage);
        return ApiResponse.error(ResponseCode.PARAMS_VALID_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理参数绑定异常 (表单请求)
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ApiResponse<?> handleBindException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = getErrorMessageFromBindingResult(bindingResult);
        log.warn("参数绑定异常: {}", errorMessage);
        return ApiResponse.error(ResponseCode.PARAMS_VALID_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ApiResponse<?> handleMissingParamException(MissingServletRequestParameterException e) {
        String message = String.format("缺少必要参数: %s", e.getParameterName());
        log.warn(message);
        return ApiResponse.error(ResponseCode.PARAMS_VALID_ERROR.getCode(), message);
    }

    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ApiResponse<?> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "请求体格式错误或无法解析";
        log.warn("请求体解析异常: {}", e.getMessage());
        return ApiResponse.error(ResponseCode.PARAMS_VALID_ERROR.getCode(), message);
    }

    /**
     * 处理数据库异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public ApiResponse<?> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("数据库完整性异常", e);
        return ApiResponse.error(ResponseCode.DATABASE_ERROR.getCode(), "数据库操作失败，请检查数据完整性");
    }

    /**
     * 处理SQL语法错误异常
     */
    @ExceptionHandler(SQLSyntaxErrorException.class)
    @ResponseBody
    public ApiResponse<?> handleSQLSyntaxErrorException(SQLSyntaxErrorException e) {
        log.error("SQL语法错误异常", e);
        return ApiResponse.error(ResponseCode.DATABASE_ERROR.getCode(), "数据库查询错误");
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public ApiResponse<?> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = String.format("不支持的请求方法: %s，支持的方法: %s", 
                e.getMethod(), String.join(", ", e.getSupportedMethods()));
        log.warn(message);
        return ApiResponse.error(ResponseCode.METHOD_NOT_SUPPORTED.getCode(), message);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseBody
    public ApiResponse<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String message = String.format("请求的资源不存在: %s", e.getRequestURL());
        log.warn(message);
        return ApiResponse.error(ResponseCode.NOT_FOUND.getCode(), message);
    }

    /**
     * 处理未预期的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ApiResponse<?> handleUnexpectedException(Exception e, HttpServletRequest request) {
        log.error("未预期的异常，请求路径: {}", request.getRequestURI(), e);
        return ApiResponse.error(ResponseCode.SYSTEM_ERROR.getCode(), "系统内部错误，请稍后重试");
    }

    /**
     * 从BindingResult中获取错误信息
     */
    private String getErrorMessageFromBindingResult(BindingResult bindingResult) {
        StringBuilder sb = new StringBuilder();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        if (!fieldErrors.isEmpty()) {
            for (int i = 0; i < fieldErrors.size(); i++) {
                FieldError error = fieldErrors.get(i);
                sb.append(String.format("%s: %s", error.getField(), error.getDefaultMessage()));
                if (i < fieldErrors.size() - 1) {
                    sb.append(", ");
                }
            }
        } else {
            sb.append("参数验证失败");
        }
        return sb.toString();
    }
}