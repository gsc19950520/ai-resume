package com.aicv.airesume.service;

import com.aicv.airesume.model.dto.ResumeAnalysisDTO;

/**
 * 简历分析服务接口
 * 用于分析简历内容并生成结构化面试问题
 */
public interface ResumeAnalysisService {

    /**
     * 分析简历并生成面试问题清单
     * @param resumeId 简历ID
     * @param jobType 职位类型（可选）
     * @param analysisDepth 分析深度：basic/intermediate/advanced
     * @return 简历分析结果和面试问题清单
     */
    ResumeAnalysisDTO analyzeResume(Long resumeId, String jobType, String analysisDepth);

}