package com.aicv.airesume.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理类
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Map<String, Object> handleException(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        
        // 检查是否是数据库连接相关异常
        if (isDatabaseConnectionException(e)) {
            result.put("message", "数据库正在启动中，请稍后再试");
            result.put("retryable", true);
        } else {
            result.put("message", e.getMessage());
            result.put("retryable", false);
        }
        return result;
    }

    @ExceptionHandler(SQLException.class)
    @ResponseBody
    public Map<String, Object> handleSQLException(SQLException e) {
        Map<String, Object> result = new HashMap<>();
        
        // 检查是否是连接相关的SQLException
        if (isConnectionSQLException(e)) {
            result.put("code", 503); // 服务不可用状态码
            result.put("message", "数据库连接暂时不可用，请稍后再试");
            result.put("retryable", true);
            result.put("errorCode", e.getErrorCode());
        } else {
            result.put("code", 500);
            result.put("message", "数据库操作异常: " + e.getMessage());
            result.put("retryable", false);
        }
        return result;
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public Map<String, Object> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        
        // 检查运行时异常是否包含数据库连接错误
        if (isDatabaseConnectionException(e)) {
            result.put("code", 503);
            result.put("message", "系统正在处理中，请稍后再试");
            result.put("retryable", true);
        } else {
            result.put("message", e.getMessage());
            result.put("retryable", false);
        }
        return result;
    }
    
    /**
     * 检查异常是否与数据库连接相关
     */
    private boolean isDatabaseConnectionException(Throwable e) {
        if (e == null) {
            return false;
        }
        
        String message = e.getMessage();
        if (message == null) {
            message = "";
        }
        
        // 常见的数据库连接异常关键词
        String[] keywords = {
            "Connection refused", 
            "No operations allowed after connection closed",
            "Communications link failure",
            "could not open connection",
            "connection is not valid",
            "SQLNonTransientConnectionException",
            "SQLTransientConnectionException",
            "Connection reset",
            "Connection timed out",
            "database not available",
            "database is starting",
            "HikariPool",
            "Cannot create PoolableConnectionFactory"
        };

        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        
        // 递归检查cause
        return isDatabaseConnectionException(e.getCause());
    }
    
    /**
     * 检查SQLException是否与连接相关
     */
    private boolean isConnectionSQLException(SQLException e) {
        int errorCode = e.getErrorCode();
        // MySQL常见的连接错误代码
        int[] connectionErrorCodes = {0, 1040, 1042, 1043, 1047, 1049, 1053, 1092};
        for (int code : connectionErrorCodes) {
            if (errorCode == code) {
                return true;
            }
        }
        return false;
    }
}