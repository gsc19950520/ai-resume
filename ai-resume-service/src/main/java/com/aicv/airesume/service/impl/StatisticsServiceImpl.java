package com.aicv.airesume.service.impl;

import com.aicv.airesume.repository.ResumeRepository;
import com.aicv.airesume.repository.UserRepository;
import com.aicv.airesume.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.aicv.airesume.utils.RetryUtils;

/**
 * 统计服务实现类
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private RetryUtils retryUtils;

    @Override
    public Map<String, Object> getUserStatistics(Long userId) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取用户的实际简历数量
            Long resumeCount = resumeRepository.countByUserId(userId);
            stats.put("resumeCount", resumeCount);
            
            // 获取用户已优化简历数量 - 临时使用固定值
            Long optimizedCount = 0L;
            stats.put("optimizedCount", optimizedCount);
            
            // 检查用户是否为VIP - 临时使用固定值
            Boolean isVip = false;
            stats.put("isVip", isVip);
            
            return stats;
        });
    }
}