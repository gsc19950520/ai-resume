package com.aicv.airesume.common.constant;

import lombok.Getter;

/**
 * 响应码常量类
 * 定义系统中统一的响应码和对应的错误消息
 *
 * @author AI Resume Team
 * @date 2023-07-01
 */
@Getter
public enum ResponseCode {
    
    // 成功响应
    SUCCESS(200, "操作成功"),
    
    // 通用错误码
    SYSTEM_ERROR(500, "系统内部错误"),
    PARAMS_VALID_ERROR(400, "参数校验失败"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_SUPPORTED(405, "请求方法不支持"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    TIMEOUT(408, "请求超时"),
    
    // 业务错误码 (1000-1999)
    BUSINESS_ERROR(1000, "业务处理失败"),
    RESUME_PARSE_ERROR(1001, "简历解析失败"),
    AI_GENERATION_ERROR(1002, "AI内容生成失败"),
    FILE_UPLOAD_ERROR(1003, "文件上传失败"),
    FILE_TYPE_ERROR(1004, "不支持的文件类型"),
    FILE_SIZE_ERROR(1005, "文件大小超过限制"),
    
    // 面试相关错误码 (2000-2999)
    INTERVIEW_NOT_FOUND(2001, "面试记录不存在"),
    INTERVIEW_HAS_ENDED(2002, "面试已结束"),
    INTERVIEW_NOT_STARTED(2003, "面试未开始"),
    
    // 用户相关错误码 (3000-3999)
    USER_NOT_FOUND(3001, "用户不存在"),
    USERNAME_EXIST(3002, "用户名已存在"),
    PHONE_EXIST(3003, "手机号已被注册"),
    EMAIL_EXIST(3004, "邮箱已被注册"),
    LOGIN_FAILED(3005, "登录失败，用户名或密码错误"),
    
    // 数据库错误码 (4000-4999)
    DATABASE_ERROR(4001, "数据库操作失败"),
    DATA_INTEGRITY_VIOLATION(4002, "数据完整性约束冲突"),
    
    // AI服务错误码 (5000-5999)
    AI_SERVICE_TIMEOUT(5001, "AI服务超时"),
    AI_SERVICE_ERROR(5002, "AI服务异常"),
    AI_API_LIMIT_ERROR(5003, "AI API调用次数超限"),
    
    // 文件存储错误码 (6000-6999)
    OSS_UPLOAD_ERROR(6001, "OSS文件上传失败"),
    OSS_DOWNLOAD_ERROR(6002, "OSS文件下载失败"),
    OSS_DELETE_ERROR(6003, "OSS文件删除失败"),
    
    // 微信相关错误码 (7000-7999)
    WECHAT_LOGIN_ERROR(7001, "微信登录失败"),
    WECHAT_CODE_ERROR(7002, "微信授权码无效"),
    
    // 限流相关错误码 (8000-8999)
    RATE_LIMIT_EXCEEDED(8001, "请求过于频繁，请稍后再试"),
    
    // 免费次数限制错误码 (9000-9999)
    FREE_COUNT_EXCEEDED(9001, "免费次数已用完，请升级套餐"),
    
    ;

    private final Integer code;
    private final String message;

    ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}