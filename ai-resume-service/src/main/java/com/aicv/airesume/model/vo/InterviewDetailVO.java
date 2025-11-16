package com.aicv.airesume.model.vo;

/**
 * 面试详情VO
 */
public class InterviewDetailVO {
    
    private String sessionId;
    private String status;
    private Double totalScore;
    private String createdAt;
    private String finishedAt;
    private Integer questionCount;
    private Integer answeredCount;
    
    public InterviewDetailVO() {}
    
    public InterviewDetailVO(String sessionId, String status, Double totalScore, String createdAt, 
                           String finishedAt, Integer questionCount, Integer answeredCount) {
        this.sessionId = sessionId;
        this.status = status;
        this.totalScore = totalScore;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
        this.questionCount = questionCount;
        this.answeredCount = answeredCount;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Double getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getFinishedAt() {
        return finishedAt;
    }
    
    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }
    
    public Integer getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
    
    public Integer getAnsweredCount() {
        return answeredCount;
    }
    
    public void setAnsweredCount(Integer answeredCount) {
        this.answeredCount = answeredCount;
    }
}