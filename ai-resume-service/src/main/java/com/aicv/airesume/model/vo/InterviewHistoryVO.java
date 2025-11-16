package com.aicv.airesume.model.vo;

/**
 * 面试历史VO
 */
public class InterviewHistoryVO {
    private Long sessionId;
    private String uniqueSessionId;
    private String title;
    private Long startTime;
    private Long endTime;
    private Integer questionCount;
    private Double finalScore;
    private String status;

    public InterviewHistoryVO() {}

    public InterviewHistoryVO(Long sessionId, String uniqueSessionId, String title, Long startTime, Long endTime, Integer questionCount, Double finalScore, String status) {
        this.sessionId = sessionId;
        this.uniqueSessionId = uniqueSessionId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.questionCount = questionCount;
        this.finalScore = finalScore;
        this.status = status;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getUniqueSessionId() {
        return uniqueSessionId;
    }

    public void setUniqueSessionId(String uniqueSessionId) {
        this.uniqueSessionId = uniqueSessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
