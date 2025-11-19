package com.aicv.airesume.service;

import com.aicv.airesume.model.dto.ResumeAnalysisDTO;
import com.aicv.airesume.model.dto.QuestionAnalysisDTO;
import java.util.Map;

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

    /**
     * 分析简历内容文本
     * @param resumeContent 简历内容文本
     * @param jobType 职位类型（可选）
     * @param analysisDepth 分析深度
     * @return 简历分析结果
     */
    ResumeAnalysisDTO analyzeResumeContent(String resumeContent, String jobType, String analysisDepth);

    /**
     * 根据职位类型获取专业问题模板
     * @param jobType 职位类型
     * @param experienceLevel 经验级别
     * @return 专业问题模板
     */
    Map<String, Object> getProfessionalQuestions(String jobType, String experienceLevel);

    /**
     * 分析技术栈深度
     * @param skills 技能列表
     * @param projects 项目经验
     * @return 技术栈分析结果
     */
    Map<String, Object> analyzeTechnicalDepth(Map<String, Integer> skills, String projects);

    /**
     * 生成行为面试问题
     * @param workExperience 工作经历
     * @param projects 项目经验
     * @return 行为面试问题清单
     */
    QuestionAnalysisDTO generateBehavioralQuestions(String workExperience, String projects);

    /**
     * 分析项目复杂度
     * @param projectDescriptions 项目描述列表
     * @return 项目复杂度分析
     */
    Map<String, Object> analyzeProjectComplexity(String[] projectDescriptions);

    /**
     * 评估候选人的经验级别
     * @param workYears 工作年限
     * @param projectCount 项目数量
     * @param skills 技能列表
     * @return 经验级别评估
     */
    String assessExperienceLevel(Integer workYears, Integer projectCount, Map<String, Integer> skills);
}