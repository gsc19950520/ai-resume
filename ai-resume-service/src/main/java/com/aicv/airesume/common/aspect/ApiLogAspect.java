package com.aicv.airesume.common.aspect;

import com.aicv.airesume.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API请求日志拦截器
 * 统一记录所有API请求的详细信息，包括请求参数、响应结果、执行时间等
 *
 * @author AI Resume Team
 * @date 2023-07-01
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    // 定义切点，拦截所有Controller层的方法
    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void apiPointcut() {
    }

    /**
     * 环绕通知，记录API请求的详细信息
     */
    @Around("apiPointcut()")
    public Object aroundApi(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 生成请求ID
        String requestId = UUID.randomUUID().toString().replace("-", "");
        
        // 获取请求信息
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIp(request);
        String userId = getUserId(request);
        
        // 获取类名和方法名
        String className = joinPoint.getTarget().getClass().getSimpleName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();
        String methodName = targetMethod.getName();
        
        // 记录请求开始信息，但跳过update-remaining-time接口
        if (log.isInfoEnabled() && !uri.equals("/api/interview/update-remaining-time")) {
            String params = maskSensitiveParams(joinPoint.getArgs());
            log.info("[API-START] requestId: {}, ip: {}, uri: {}, method: {}, userId: {}, class: {}, method: {}, params: {}",
                    requestId, ip, uri, method, userId, className, methodName, params);
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            // 执行原方法
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            // 记录异常信息
            long endTime = System.currentTimeMillis();
            long timeConsumed = endTime - startTime;
            log.error("[API-ERROR] requestId: {}, uri: {}, method: {}, userId: {}, class: {}, method: {}, time: {}ms",
                    requestId, uri, method, userId, className, methodName, timeConsumed, throwable);
            throw throwable;
        } finally {
            // 记录请求结束信息
            long endTime = System.currentTimeMillis();
            long timeConsumed = endTime - startTime;
            
            // 记录API请求日志（使用LogUtil），但跳过update-remaining-time接口
            if (!uri.equals("/api/interview/update-remaining-time")) {
                LogUtil.logApiRequest(requestId, userId, uri, method, 200, timeConsumed);
                
                if (log.isInfoEnabled()) {
                    // 限制结果日志长度
                    String resultStr = result != null ? result.toString() : "null";
                    if (resultStr.length() > 1000) {
                        resultStr = resultStr.substring(0, 1000) + "...";
                    }
                    log.info("[API-END] requestId: {}, uri: {}, method: {}, time: {}ms, result: {}",
                            requestId, uri, method, timeConsumed, resultStr);
                }
            }
        }
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP，多个IP按照','分割
        if (StringUtils.isNotEmpty(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从请求中获取用户ID
     * 实际项目中可能需要从token、session或自定义header中获取
     */
    private String getUserId(HttpServletRequest request) {
        // 尝试从header获取token中的用户信息
        String userId = request.getHeader("X-User-Id");
        if (StringUtils.isEmpty(userId)) {
            // 如果没有，则返回空字符串或匿名标识
            userId = "anonymous";
        }
        return userId;
    }

    /**
     * 脱敏敏感参数
     */
    private String maskSensitiveParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) {
                        return "null";
                    }
                    String argStr = arg.toString();
                    // 对于JSON格式的字符串，尝试脱敏其中的敏感信息
                    if (argStr.startsWith("{") && argStr.contains(":")) {
                        // 脱敏密码字段
                        argStr = argStr.replaceAll("\\\"password\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"password\\\":\\\"******\\\"");
                        // 脱敏手机号字段
                        argStr = argStr.replaceAll("\\\"phone\\\"\\s*:\\s*\\\"([0-9]{3})[0-9]{4}([0-9]{4})\\\"", "\\\"phone\\\":\\\"$1****$2\\\"");
                        // 脱敏邮箱字段
                        argStr = argStr.replaceAll("\\\"email\\\"\\s*:\\s*\\\"([a-zA-Z0-9._%+-]{2})[a-zA-Z0-9._%+-]*@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\\"", "\\\"email\\\":\\\"$1****@$2\\\"");
                    }
                    // 限制参数长度
                    if (argStr.length() > 500) {
                        argStr = argStr.substring(0, 500) + "...";
                    }
                    return argStr;
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }
}