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
    private UserRepository userRepository;

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

    @Override
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取总用户数
        Long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);
        
        // 获取总简历数
        Long totalResumes = resumeRepository.count();
        stats.put("totalResumes", totalResumes);
        
        // 获取已优化简历数 - 临时使用固定值
        Long optimizedResumes = 0L;
        stats.put("optimizedResumes", optimizedResumes);
        
        // 模板总数 - 已移除模板功能，使用固定值
        Long totalTemplates = 0L;
        stats.put("totalTemplates", totalTemplates);
        
        return stats;
    }

    @Override
    public void recordUserAction(Long userId, String action, String data) {
        // 在实际项目中，这里应该记录用户行为到数据库或日志系统
        // 现在只是记录到控制台
        System.out.println("User action recorded: userId=" + userId + ", action=" + action + ", data=" + data);
    }

    @Override
    public Map<String, Object> getUserActionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取用户行为统计 - 临时使用固定值
        Long totalActions = 0L;
        stats.put("totalActions", totalActions);
        
        // 获取今日用户行为统计 - 临时使用固定值
        Long todayActions = 0L;
        stats.put("todayActions", todayActions);
        
        // 获取本周用户行为统计 - 临时使用固定值
        Long weekActions = 0L;
        stats.put("weekActions", weekActions);
        
        // 获取本月用户行为统计 - 临时使用固定值
        Long monthActions = 0L;
        stats.put("monthActions", monthActions);
        
        return stats;
    }

    @Override
    public Map<String, Object> getSystemActionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取系统行为统计 - 临时使用固定值
        Long totalActions = 0L;
        stats.put("totalActions", totalActions);
        
        // 获取今日系统行为统计 - 临时使用固定值
        Long todayActions = 0L;
        stats.put("todayActions", todayActions);
        
        // 获取本周系统行为统计 - 临时使用固定值
        Long weekActions = 0L;
        stats.put("weekActions", weekActions);
        
        // 获取本月系统行为统计 - 临时使用固定值
        Long monthActions = 0L;
        stats.put("monthActions", monthActions);
        
        return stats;
    }
}