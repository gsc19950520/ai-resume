package com.aicv.airesume.annotation;

import java.lang.annotation.*;

/**
 * 自定义日志注解，用于标记需要记录日志的方法
 * 可以通过注解属性控制日志记录的行为
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 日志描述
     */
    String description() default "";

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean recordResult() default true;

    /**
     * 是否记录执行时间
     */
    boolean recordExecutionTime() default true;

    /**
     * 是否忽略敏感信息（如密码等）
     */
    boolean ignoreSensitiveInfo() default true;

    /**
     * 敏感参数名称列表，这些参数的值将在日志中被脱敏处理
     */
    String[] sensitiveParams() default {"password", "passwordConfirm", "accessToken", "token"};
}
