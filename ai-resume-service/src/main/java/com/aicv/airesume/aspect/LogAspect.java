package com.aicv.airesume.aspect;

import com.aicv.airesume.annotation.Log;
import com.alibaba.fastjson.JSON;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * 日志切面类，用于记录Controller方法的请求和响应信息
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 定义切点，拦截所有Controller方法
     */
    @Pointcut("execution(* com.aicv.airesume.controller.*.*(..)) || @annotation(com.aicv.airesume.annotation.Log)")
    public void logPointCut() {}

    /**
     * 前置通知，在方法执行前记录请求信息
     */
    @Before("logPointCut()")
    public void doBefore(JoinPoint joinPoint) {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        
        HttpServletRequest request = attributes.getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 跳过update-remaining-time接口的日志
        if (request.getRequestURI().equals("/api/interview/update-remaining-time")) {
            return;
        }
        
        // 获取方法上的Log注解
        Log logAnnotation = method.getAnnotation(Log.class);
        
        // 构建请求日志信息
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("[REQUEST] ")
                .append("URL: ").append(request.getRequestURL().toString()).append(" | ")
                .append("Method: ").append(request.getMethod()).append(" | ")
                .append("Controller: ").append(signature.getDeclaringTypeName()).append(" | ")
                .append("Action: ").append(method.getName());
        
        // 如果有Log注解，添加描述信息
        if (logAnnotation != null && !logAnnotation.description().isEmpty()) {
            logBuilder.append(" | Description: ").append(logAnnotation.description());
        }
        
        // 判断是否需要记录请求参数
        boolean recordParams = true;
        if (logAnnotation != null) {
            recordParams = logAnnotation.recordParams();
        }
        
        // 记录请求参数
        if (recordParams) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                Parameter[] parameters = method.getParameters();
                Map<String, Object> paramMap = new HashMap<>();
                
                // 获取敏感参数列表
                Set<String> sensitiveParams = new HashSet<>();
                if (logAnnotation != null && logAnnotation.ignoreSensitiveInfo()) {
                    sensitiveParams.addAll(Arrays.asList(logAnnotation.sensitiveParams()));
                }
                
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    // 跳过不能序列化的参数类型
                    if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse || arg instanceof MultipartFile) {
                        continue;
                    }
                    
                    String paramName = i < parameters.length ? parameters[i].getName() : "param" + i;
                    
                    // 对敏感参数进行脱敏处理
                    if (sensitiveParams.contains(paramName.toLowerCase())) {
                        paramMap.put(paramName, "******");
                    } else {
                        paramMap.put(paramName, arg);
                    }
                }
                
                if (!paramMap.isEmpty()) {
                    logBuilder.append(" | Parameters: ").append(JSON.toJSONString(paramMap));
                }
            }
        }
        
        logger.info(logBuilder.toString());
    }

    /**
     * 环绕通知，记录方法执行时间和响应结果
     */
    @Around("logPointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        String errorMsg = null;
        
        // 跳过update-remaining-time接口的日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if (request.getRequestURI().equals("/api/interview/update-remaining-time")) {
                return joinPoint.proceed();
            }
        }
        
        // 获取方法信息和Log注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 构建响应日志信息
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("[RESPONSE] ")
                    .append("Controller: ").append(joinPoint.getSignature().getDeclaringTypeName()).append(" | ")
                    .append("Action: ").append(joinPoint.getSignature().getName());
            
            // 判断是否需要记录执行时间
            boolean recordExecutionTime = true;
            if (logAnnotation != null) {
                recordExecutionTime = logAnnotation.recordExecutionTime();
            }
            
            if (recordExecutionTime) {
                logBuilder.append(" | Execution Time: ").append(executionTime).append("ms");
            }
            
            // 判断是否需要记录响应结果
            boolean recordResult = true;
            if (logAnnotation != null) {
                recordResult = logAnnotation.recordResult();
            }
            
            // 记录响应结果（如果没有异常且需要记录）
            if (errorMsg == null && recordResult) {
                // 限制响应结果的长度，避免日志过大
                String resultStr = JSON.toJSONString(result);
                if (resultStr.length() > 1000) {
                    resultStr = resultStr.substring(0, 1000) + "...[truncated]";
                }
                logBuilder.append(" | Result: ").append(resultStr);
            } else if (errorMsg != null) {
                // 记录异常信息
                logBuilder.append(" | Error: ").append(errorMsg);
            }
            
            logger.info(logBuilder.toString());
        }
    }

    /**
     * 异常通知，记录方法执行异常信息
     */
    @AfterThrowing(pointcut = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable e) {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        
        HttpServletRequest request = attributes.getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取方法上的Log注解
        Log logAnnotation = method.getAnnotation(Log.class);
        
        // 构建异常日志信息
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("[EXCEPTION] ")
                .append("URL: ").append(request.getRequestURL().toString()).append(" | ")
                .append("Method: ").append(request.getMethod()).append(" | ")
                .append("Controller: ").append(joinPoint.getSignature().getDeclaringTypeName()).append(" | ")
                .append("Action: ").append(joinPoint.getSignature().getName());
        
        // 如果有Log注解，添加描述信息
        if (logAnnotation != null && !logAnnotation.description().isEmpty()) {
            logBuilder.append(" | Description: ").append(logAnnotation.description());
        }
        
        // 记录异常信息
        logBuilder.append(" | Exception: ").append(e.getMessage())
                .append(" | Exception Type: ").append(e.getClass().getName());
        
        logger.error(logBuilder.toString(), e);
    }
}
