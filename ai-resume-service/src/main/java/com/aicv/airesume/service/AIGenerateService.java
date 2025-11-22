package com.aicv.airesume.service;

import com.aicv.airesume.model.vo.SalaryRangeVO;

import java.util.List;
import java.util.Map;

/**
 * AI生成服务接口
 * 负责动态生成薪资信息和成长建议
 */
public interface AIGenerateService {
    
    /**
     * 基于用户面试表现动态生成薪资范围评估
     * @param sessionId 会话ID
     * @param city 城市
     * @param jobType 职位类型
     * @param aggregatedScores 聚合评分
     * @param userPerformanceData 用户表现数据
     * @return 薪资范围VO对象
     */
    SalaryRangeVO generateSalaryRange(String sessionId, String city, String jobType, 
                                    Map<String, Double> aggregatedScores, Map<String, Object> userPerformanceData);
    
    /**
     * 基于用户面试表现动态生成成长建议
     * @param sessionId 会话ID
     * @param jobType 职位类型
     * @param domain 领域
     * @param aggregatedScores 聚合评分
     * @param weakSkills 技能短板
     * @param userPerformanceData 用户表现数据
     * @return 包含成长建议的Map对象
     */
    Map<String, Object> generateGrowthAdvice(String sessionId, String jobType, String domain,
                                           Map<String, Double> aggregatedScores, List<String> weakSkills,
                                           Map<String, Object> userPerformanceData);
    
    /**
     * 获取职位领域信息
     * @param jobTypeId 职位ID
     * @return 领域信息
     */
    String getJobDomain(Integer jobTypeId);
    
    /**
     * 分析用户技能短板
     * @param sessionId 会话ID
     * @param logs 面试日志数据
     * @return 技能短板列表
     */
    List<String> analyzeWeakSkills(String sessionId, List<Map<String, Object>> logs);
}