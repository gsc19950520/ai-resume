package com.aicv.airesume.utils;

/**
 * 全局上下文工具类，使用ThreadLocal存储当前请求的用户信息
 */
public class GlobalContextUtil {

    private static final ThreadLocal<Long> USER_ID_CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_CONTEXT.set(userId);
    }

    /**
     * 获取当前线程的用户ID
     * @return 用户ID
     */
    public static Long getUserId() {
        return USER_ID_CONTEXT.get();
    }

    /**
     * 清除当前线程的用户ID
     */
    public static void clearUserId() {
        USER_ID_CONTEXT.remove();
    }
}