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

}