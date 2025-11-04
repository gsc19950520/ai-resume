package com.aicv.airesume.service;

/**
 * AI服务接口
 */
public interface AiService {

    /**
     * AI优化简历
     * @param resumeContent 原始简历内容
     * @param jobDescription 职位描述（可选）
     * @return 优化后的简历内容
     */
    String optimizeResume(String resumeContent, String jobDescription);

    /**
     * 获取简历评分
     * @param resumeContent 简历内容
     * @return 评分（0-100）
     */
    Integer getResumeScore(String resumeContent);

    /**
     * 获取简历优化建议
     * @param resumeContent 简历内容
     * @return 优化建议文本
     */
    String getResumeSuggestions(String resumeContent);
}