package com.aicv.airesume.service;

import java.util.Map;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取用户统计信息
     * @param userId 用户ID
     * @return 统计信息Map
     */
    Map<String, Object> getUserStatistics(Long userId);

    /**
     * 获取系统统计信息
     * @return 统计信息Map
     */
    Map<String, Object> getSystemStatistics();

    /**
     * 记录用户行为
     * @param userId 用户ID
     * @param action 行为类型
     * @param data 相关数据
     */
    void recordUserAction(Long userId, String action, String data);
}