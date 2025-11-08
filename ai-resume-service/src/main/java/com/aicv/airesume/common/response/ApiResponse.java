package com.aicv.airesume.common.response;

import com.aicv.airesume.common.constant.ResponseCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * API响应封装类
 * 统一所有API接口的返回格式
 *
 * @param <T> 响应数据类型
 * @author AI Resume Team
 * @date 2023-07-01
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码：200表示成功，其他表示失败
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * 构造器私有，推荐使用静态工厂方法
     */
    private ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造器私有，推荐使用静态工厂方法
     */
    private ApiResponse(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 创建成功响应
     *
     * @param <T> 响应数据类型
     * @param data 响应数据
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), data);
    }

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 响应数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage());
    }

    /**
     * 创建成功响应（自定义消息）
     *
     * @param <T> 响应数据类型
     * @param message 自定义成功消息
     * @param data 响应数据
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 创建错误响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @return 错误响应对象
     */
    public static ApiResponse<?> error(Integer code, String message) {
        return new ApiResponse<>(code, message);
    }

    /**
     * 使用响应码枚举创建错误响应
     *
     * @param responseCode 响应码枚举
     * @return 错误响应对象
     */
    public static ApiResponse<?> error(ResponseCode responseCode) {
        return new ApiResponse<>(responseCode.getCode(), responseCode.getMessage());
    }

    /**
     * 使用响应码枚举和自定义消息创建错误响应
     *
     * @param responseCode 响应码枚举
     * @param message 自定义错误消息
     * @return 错误响应对象
     */
    public static ApiResponse<?> error(ResponseCode responseCode, String message) {
        return new ApiResponse<>(responseCode.getCode(), message);
    }

    /**
     * 判断响应是否成功
     *
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return ResponseCode.SUCCESS.getCode().equals(this.code);
    }
}