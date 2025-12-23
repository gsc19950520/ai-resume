package com.aicv.airesume.model.vo;

import java.util.List;
import java.util.Map;

/**
 * 面试报告VO
 */
public class InterviewReportVO {
    
    private String sessionId;
    private String jobType;
    private String domain;
    
    // 评分数据
    private Double totalScore;
    private Double techScore;
    private Double logicScore;
    private Double clarityScore;
    private Double depthScore;
    
    // 详细反馈
    private String overallFeedback;
    private String strengths;
    private String improvements;
    private String techDepthEvaluation;
    private String logicExpressionEvaluation;
    private String communicationEvaluation;
    private String answerDepthEvaluation;
    private String detailedImprovementSuggestions;
    private String createdAt;
    
    // 成长报告数据
    private Map<String, Integer> growthRadar;
    private List<Integer> trendCurve;
    private List<String> recommendedSkills;
    private List<String> longTermPath;
    // 会话记录
    private List<SessionLogVO> sessionLog;
    
    public InterviewReportVO() {}
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getJobType() {
        return jobType;
    }
    
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public Double getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }
    
    public Double getTechScore() {
        return techScore;
    }
    
    public void setTechScore(Double techScore) {
        this.techScore = techScore;
    }
    
    public Double getLogicScore() {
        return logicScore;
    }
    
    public void setLogicScore(Double logicScore) {
        this.logicScore = logicScore;
    }
    
    public Double getClarityScore() {
        return clarityScore;
    }
    
    public void setClarityScore(Double clarityScore) {
        this.clarityScore = clarityScore;
    }
    
    public Double getDepthScore() {
        return depthScore;
    }
    
    public void setDepthScore(Double depthScore) {
        this.depthScore = depthScore;
    }
    
    public String getOverallFeedback() {
        return overallFeedback;
    }
    
    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
    }
    
    public String getStrengths() {
        return strengths;
    }
    
    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }
    
    public String getImprovements() {
        return improvements;
    }
    
    public void setImprovements(String improvements) {
        this.improvements = improvements;
    }
    
    public String getTechDepthEvaluation() {
        return techDepthEvaluation;
    }
    
    public void setTechDepthEvaluation(String techDepthEvaluation) {
        this.techDepthEvaluation = techDepthEvaluation;
    }
    
    public String getLogicExpressionEvaluation() {
        return logicExpressionEvaluation;
    }
    
    public void setLogicExpressionEvaluation(String logicExpressionEvaluation) {
        this.logicExpressionEvaluation = logicExpressionEvaluation;
    }
    
    public String getCommunicationEvaluation() {
        return communicationEvaluation;
    }
    
    public void setCommunicationEvaluation(String communicationEvaluation) {
        this.communicationEvaluation = communicationEvaluation;
    }
    
    public String getAnswerDepthEvaluation() {
        return answerDepthEvaluation;
    }
    
    public void setAnswerDepthEvaluation(String answerDepthEvaluation) {
        this.answerDepthEvaluation = answerDepthEvaluation;
    }
    
    public String getDetailedImprovementSuggestions() {
        return detailedImprovementSuggestions;
    }
    
    public void setDetailedImprovementSuggestions(String detailedImprovementSuggestions) {
        this.detailedImprovementSuggestions = detailedImprovementSuggestions;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public Map<String, Integer> getGrowthRadar() {
        return growthRadar;
    }
    
    public void setGrowthRadar(Map<String, Integer> growthRadar) {
        this.growthRadar = growthRadar;
    }
    
    public List<Integer> getTrendCurve() {
        return trendCurve;
    }
    
    public void setTrendCurve(List<Integer> trendCurve) {
        this.trendCurve = trendCurve;
    }
    
    public List<String> getRecommendedSkills() {
        return recommendedSkills;
    }
    
    public void setRecommendedSkills(List<String> recommendedSkills) {
        this.recommendedSkills = recommendedSkills;
    }
    
    public List<String> getLongTermPath() {
        return longTermPath;
    }
    
    public void setLongTermPath(List<String> longTermPath) {
        this.longTermPath = longTermPath;
    }
    
    
    public List<SessionLogVO> getSessionLog() {
        return sessionLog;
    }
    
    public void setSessionLog(List<SessionLogVO> sessionLog) {
        this.sessionLog = sessionLog;
    }
    
}