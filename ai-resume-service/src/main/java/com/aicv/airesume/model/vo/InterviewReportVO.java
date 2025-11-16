package com.aicv.airesume.model.vo;

/**
 * 面试报告VO
 */
public class InterviewReportVO {
    
    private String sessionId;
    private Double totalScore;
    private String overallFeedback;
    private String strengths;
    private String improvements;
    private String createdAt;
    
    public InterviewReportVO() {}
    
    public InterviewReportVO(String sessionId, Double totalScore, String overallFeedback, 
                           String strengths, String improvements, String createdAt) {
        this.sessionId = sessionId;
        this.totalScore = totalScore;
        this.overallFeedback = overallFeedback;
        this.strengths = strengths;
        this.improvements = improvements;
        this.createdAt = createdAt;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public Double getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
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
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}