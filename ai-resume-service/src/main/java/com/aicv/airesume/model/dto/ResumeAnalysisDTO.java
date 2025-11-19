package com.aicv.airesume.model.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 简历分析结果DTO
 */
@Data
public class ResumeAnalysisDTO {

    /**
     * 分析ID
     */
    private String analysisId;

    /**
     * 简历ID
     */
    private Long resumeId;

    /**
     * 候选人基本信息
     */
    private CandidateInfo candidateInfo;

    /**
     * 技术栈分析
     */
    private TechnicalAnalysis technicalAnalysis;

    /**
     * 项目经验分析
     */
    private ProjectAnalysis projectAnalysis;

    /**
     * 业务能力分析
     */
    private BusinessAnalysis businessAnalysis;

    /**
     * 经验级别评估
     */
    private String experienceLevel;

    /**
     * 综合评分（0-100）
     */
    private Integer overallScore;

    /**
     * 优势分析
     */
    private List<String> strengths;

    /**
     * 待提升项
     */
    private List<String> improvements;

    /**
     * 面试问题清单
     */
    private InterviewQuestions interviewQuestions;

    /**
     * 分析完成时间
     */
    private String analysisTime;

    /**
     * 候选人基本信息内部类
     */
    @Data
    public static class CandidateInfo {
        private String name;
        private String jobTitle;
        private Integer workYears;
        private String educationLevel;
        private String expectedSalary;
        private String selfEvaluation;
    }

    /**
     * 技术栈分析内部类
     */
    @Data
    public static class TechnicalAnalysis {
        private List<TechSkill> primarySkills;
        private List<TechSkill> secondarySkills;
        private List<String> skillGaps;
        private String technicalLevel;
        private Map<String, Integer> skillProficiency;
    }

    /**
     * 技能信息内部类
     */
    @Data
    public static class TechSkill {
        private String name;
        private String category;
        private String proficiency;
        private Integer yearsOfExperience;
        private Boolean isCoreSkill;
    }

    /**
     * 项目经验分析内部类
     */
    @Data
    public static class ProjectAnalysis {
        private Integer totalProjects;
        private List<ProjectInfo> keyProjects;
        private String projectComplexity;
        private List<String> projectTypes;
        private Map<String, Integer> techUsageFrequency;
    }

    /**
     * 项目信息内部类
     */
    @Data
    public static class ProjectInfo {
        private String name;
        private String description;
        private String duration;
        private String role;
        private List<String> technologies;
        private String complexity;
        private String businessImpact;
    }

    /**
     * 业务能力分析内部类
     */
    @Data
    public static class BusinessAnalysis {
        private List<String> domainKnowledge;
        private List<String> softSkills;
        private String leadershipExperience;
        private String communicationSkills;
        private String problemSolvingAbility;
    }

    /**
     * 面试问题清单内部类
     */
    @Data
    public static class InterviewQuestions {
        private List<QuestionDetail> technicalQuestions;
        private List<QuestionDetail> behavioralQuestions;
        private List<QuestionDetail> situationalQuestions;
        private List<QuestionDetail> projectQuestions;
        private List<QuestionDetail> cultureFitQuestions;
    }

    /**
     * 问题详情内部类
     */
    @Data
    public static class QuestionDetail {
        private String question;
        private String category;
        private String difficulty;
        private String purpose;
        private List<String> expectedPoints;
        private String followUpQuestions;
    }
}