package com.aicv.airesume.utils;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 重试工具类，用于处理数据库冷启动问题和临时连接失败
 */
@Component
public class RetryUtils {

    /**
     * 执行可能需要重试的操作
     * @param callable 待执行的操作
     * @param maxRetries 最大重试次数
     * @param retryDelay 重试间隔（毫秒）
     * @return 操作结果
     * @throws Exception 所有重试都失败后的异常
     */
    public <T> T executeWithRetry(Callable<T> callable, int maxRetries, long retryDelay) throws Exception {
        int retries = 0;
        Exception lastException = null;

        while (retries <= maxRetries) {
            try {
                return callable.call();
            } catch (SQLException | RuntimeException e) {
                lastException = e;
                retries++;
                
                // 检查是否是连接相关的异常
                if (isConnectionException(e) && retries <= maxRetries) {
                    System.err.println("数据库连接异常，准备重试 (" + retries + "/" + maxRetries + "): " + e.getMessage());
                    Thread.sleep(retryDelay);
                    // 每次重试后增加延迟时间
                    retryDelay *= 2;
                } else {
                    throw e;
                }
            }
        }

        throw lastException;
    }

    /**
     * 检查异常是否与数据库连接相关
     */
    private boolean isConnectionException(Throwable e) {
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
            "Connection timed out"
        };

        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        
        // 检查异常类型
        if (e instanceof SQLException) {
            int errorCode = ((SQLException) e).getErrorCode();
            // MySQL常见的连接错误代码
            int[] connectionErrorCodes = {0, 1040, 1042, 1043, 1047, 1049, 1053, 1092};
            for (int code : connectionErrorCodes) {
                if (errorCode == code) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 默认重试配置的操作执行方法（支持Callable）
     */
    public <T> T executeWithDefaultRetry(Callable<T> callable) throws Exception {
        // 默认重试3次，初始延迟1秒，指数退避
        return executeWithRetry(callable, 3, 1000);
    }
    
    /**
     * 默认重试配置的操作执行方法（支持Supplier）
     */
    public <T> T executeWithDefaultRetrySupplier(Supplier<T> supplier) {
        try {
            // 将Supplier转换为Callable
            Callable<T> callable = supplier::get;
            return executeWithRetry(callable, 3, 1000);
        } catch (Exception e) {
            // 重新抛出为运行时异常，简化调用方代码
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("执行重试操作失败", e);
        }
    }

    /**
     * 用于方法级别的重试注解示例
     * 注意：使用此注解需要在应用主类上添加@EnableRetry注解
     */
    @Retryable(
        value = {SQLException.class, RuntimeException.class},
        maxAttempts = 4, // 1次原始尝试 + 3次重试
        backoff = @Backoff(delay = 1000, multiplier = 2) // 1秒后第一次重试，然后2秒，然后4秒
    )
    public void exampleRetryableMethod() {
        // 此方法在遇到指定异常时会自动重试
    }
}