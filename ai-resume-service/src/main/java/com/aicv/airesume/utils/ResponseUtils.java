package com.aicv.airesume.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 响应工具类，用于统一API返回格式
 */
public class ResponseUtils {

    private static final String CODE = "code";
    private static final String MESSAGE = "message";
    private static final String DATA = "data";

    /**
     * 成功响应
     * @param data 响应数据
     * @return Map格式的响应
     */
    public static Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put(CODE, 200);
        result.put(MESSAGE, "success");
        if (data != null) {
            result.put(DATA, data);
        }
        return result;
    }

    /**
     * 成功响应，无数据
     * @return Map格式的响应
     */
    public static Map<String, Object> success() {
        return success(null);
    }

    /**
     * 错误响应
     * @param message 错误消息
     * @return Map格式的响应
     */
    public static Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put(CODE, 500);
        result.put(MESSAGE, message);
        return result;
    }

    /**
     * 错误响应，带自定义错误码
     * @param code 错误码
     * @param message 错误消息
     * @return Map格式的响应
     */
    public static Map<String, Object> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put(CODE, code);
        result.put(MESSAGE, message);
        return result;
    }

}