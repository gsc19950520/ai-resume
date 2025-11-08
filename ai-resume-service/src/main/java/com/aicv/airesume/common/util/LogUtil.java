package com.aicv.airesume.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 日志增强工具类
 * 提供更灵活、更规范的日志记录功能
 *
 * @author AI Resume Team
 * @date 2023-07-01
 */
@Slf4j
public class LogUtil {

    /**
     * 记录方法开始执行的日志
     *
     * @param className 类名
     * @param methodName 方法名
     * @param args 参数数组
     */
    public static void logMethodStart(String className, String methodName, Object[] args) {
        if (log.isDebugEnabled()) {
            log.debug("[{}.{}] start, args: {}", className, methodName, args != null ? Arrays.toString(args) : "[]");
        }
    }

    /**
     * 记录方法结束执行的日志
     *
     * @param className 类名
     * @param methodName 方法名
     * @param result 方法执行结果
     */
    public static void logMethodEnd(String className, String methodName, Object result) {
        if (log.isDebugEnabled()) {
            // 限制结果字符串长度，避免日志过大
            String resultStr = result != null ? result.toString() : "null";
            if (resultStr.length() > 500) {
                resultStr = resultStr.substring(0, 500) + "...";
            }
            log.debug("[{}.{}] end, result: {}", className, methodName, resultStr);
        }
    }

    /**
     * 记录方法执行异常的日志
     *
     * @param className 类名
     * @param methodName 方法名
     * @param e 异常对象
     */
    public static void logMethodError(String className, String methodName, Exception e) {
        log.error("[{}.{}] error", className, methodName, e);
    }

    /**
     * 记录耗时操作的日志
     *
     * @param operation 操作描述
     * @param task 要执行的任务
     * @param <T> 任务返回类型
     * @return 任务执行结果
     */
    public static <T> T logTimeConsuming(String operation, Supplier<T> task) {
        if (log.isInfoEnabled()) {
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                return task.get();
            } finally {
                watch.stop();
                log.info("{} completed in {} ms", operation, watch.getTotalTimeMillis());
            }
        }
        return task.get();
    }

    /**
     * 记录耗时操作的日志（无返回值）
     *
     * @param operation 操作描述
     * @param task 要执行的任务
     */
    public static void logTimeConsuming(String operation, Runnable task) {
        if (log.isInfoEnabled()) {
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                task.run();
            } finally {
                watch.stop();
                log.info("{} completed in {} ms", operation, watch.getTotalTimeMillis());
            }
        } else {
            task.run();
        }
    }

    /**
     * 记录业务操作日志
     *
     * @param userId 用户ID
     * @param operation 操作类型
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param remark 备注信息
     */
    public static void logBusinessOperation(String userId, String operation, String targetType, String targetId, String remark) {
        log.info("[Business] userId: {}, operation: {}, targetType: {}, targetId: {}, remark: {}",
                userId, operation, targetType, targetId, remark);
    }

    /**
     * 安全地记录敏感信息日志
     * 对敏感信息进行脱敏处理
     *
     * @param sensitiveInfo 敏感信息
     * @param sensitiveType 敏感信息类型（phone, email, idcard等）
     * @return 脱敏后的日志内容
     */
    public static String maskSensitiveInfo(String sensitiveInfo, String sensitiveType) {
        if (StringUtils.isEmpty(sensitiveInfo)) {
            return sensitiveInfo;
        }

        switch (sensitiveType) {
            case "phone":
                // 手机号脱敏，保留前3后4
                if (sensitiveInfo.length() > 7) {
                    return sensitiveInfo.substring(0, 3) + "****" + sensitiveInfo.substring(sensitiveInfo.length() - 4);
                }
                break;
            case "email":
                // 邮箱脱敏，保留前2和域名
                int atIndex = sensitiveInfo.indexOf('@');
                if (atIndex > 2) {
                    return sensitiveInfo.substring(0, 2) + "****" + sensitiveInfo.substring(atIndex);
                }
                break;
            case "idcard":
                // 身份证号脱敏，保留前6后4
                if (sensitiveInfo.length() > 10) {
                    return sensitiveInfo.substring(0, 6) + "********" + sensitiveInfo.substring(sensitiveInfo.length() - 4);
                }
                break;
            case "password":
                // 密码完全脱敏
                return "******";
            case "bankcard":
                // 银行卡号脱敏，保留前4后4
                if (sensitiveInfo.length() > 8) {
                    return sensitiveInfo.substring(0, 4) + " **** **** " + sensitiveInfo.substring(sensitiveInfo.length() - 4);
                }
                break;
            default:
                // 默认脱敏，保留前3后3
                if (sensitiveInfo.length() > 6) {
                    return sensitiveInfo.substring(0, 3) + "****" + sensitiveInfo.substring(sensitiveInfo.length() - 3);
                }
                break;
        }
        return sensitiveInfo;
    }

    /**
     * 记录API调用日志
     *
     * @param requestId 请求ID
     * @param userId 用户ID
     * @param uri 请求URI
     * @param method 请求方法
     * @param statusCode 响应状态码
     * @param timeConsumed 耗时（毫秒）
     */
    public static void logApiRequest(String requestId, String userId, String uri, String method, int statusCode, long timeConsumed) {
        log.info("[API] requestId: {}, userId: {}, uri: {}, method: {}, statusCode: {}, time: {}ms",
                requestId, userId, uri, method, statusCode, timeConsumed);
    }

    /**
     * 记录外部服务调用日志
     *
     * @param serviceName 服务名称
     * @param operation 操作名称
     * @param success 是否成功
     * @param timeConsumed 耗时（毫秒）
     * @param errorMsg 错误消息（如果失败）
     */
    public static void logExternalServiceCall(String serviceName, String operation, boolean success, long timeConsumed, String errorMsg) {
        if (success) {
            log.info("[ExternalService] service: {}, operation: {}, success: {}, time: {}ms",
                    serviceName, operation, success, timeConsumed);
        } else {
            log.warn("[ExternalService] service: {}, operation: {}, success: {}, time: {}ms, error: {}",
                    serviceName, operation, success, timeConsumed, errorMsg);
        }
    }
}