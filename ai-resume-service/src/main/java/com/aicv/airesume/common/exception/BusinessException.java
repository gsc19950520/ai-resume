package com.aicv.airesume.common.exception;

import com.aicv.airesume.common.constant.ResponseCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于封装业务层面的异常信息，包含错误码和错误消息
 *
 * @author AI Resume Team
 * @date 2023-07-01
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String message;

    /**
     * 使用响应码枚举构造异常
     *
     * @param responseCode 响应码枚举
     */
    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
    }

    /**
     * 使用自定义错误码和错误消息构造异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 使用响应码枚举和自定义消息构造异常
     *
     * @param responseCode 响应码枚举
     * @param message      自定义错误消息
     */
    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
        this.message = message;
    }

    /**
     * 使用响应码枚举、自定义消息和异常原因构造异常
     *
     * @param responseCode 响应码枚举
     * @param message      自定义错误消息
     * @param cause        异常原因
     */
    public BusinessException(ResponseCode responseCode, String message, Throwable cause) {
        super(message, cause);
        this.code = responseCode.getCode();
        this.message = message;
    }
}