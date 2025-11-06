package com.aicv.airesume.service;

import java.util.List;
import java.util.Map;

/**
 * 面试服务接口
 */
public interface InterviewService {

    /**
     * 开始面试
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param jobType 工作类型
     * @param city 城市
     * @param sessionParams 会话参数
     * @return 包含sessionId和firstQuestion的Map
     */
    Map<String, Object> startInterview(String userId, Long resumeId, String jobType, String city, Map<String, Object> sessionParams);

    /**
     * 提交答案并获取下一个问题
     * @param sessionId 会话ID
     * @param questionId 问题ID
     * @param userAnswerText 用户文字答案
     * @param userAnswerAudioUrl 用户音频答案URL
     * @return 包含nextQuestion、perQuestionScore、feedback、stopFlag的Map
     */
    Map<String, Object> submitAnswer(String sessionId, String questionId, String userAnswerText, String userAnswerAudioUrl);

    /**
     * 完成面试并生成报告
     * @param sessionId 会话ID
     * @return 包含aggregatedScores、salaryInfo、reportUrl的Map
     */
    Map<String, Object> finishInterview(String sessionId);

    /**
     * 获取用户面试历史
     * @param userId 用户ID
     * @return 面试历史列表
     */
    List<Map<String, Object>> getInterviewHistory(String userId);
    
    /**
     * 根据面试评分计算薪资范围
     * @param city 城市
     * @param jobType 职位类型
     * @param aggregatedScores 各维度评分
     * @return 薪资信息
     */
    Map<String, Object> calculateSalary(String city, String jobType, Map<String, Double> aggregatedScores);

    /**
     * 获取面试详情
     * @param sessionId 会话ID
     * @return 面试详情信息
     */
    Map<String, Object> getInterviewDetail(String sessionId);

}