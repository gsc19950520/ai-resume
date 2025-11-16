package com.aicv.airesume.model.vo;

/**
 * 面试开始响应VO
 */
public class InterviewStartVO {
    
    private String sessionId;
    private String question;
    private String questionType;
    private String persona;
    private Integer sessionSeconds;
    
    public InterviewStartVO() {}
    
    public InterviewStartVO(String sessionId, String question, String questionType, String persona, Integer sessionSeconds) {
        this.sessionId = sessionId;
        this.question = question;
        this.questionType = questionType;
        this.persona = persona;
        this.sessionSeconds = sessionSeconds;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public String getPersona() {
        return persona;
    }
    
    public void setPersona(String persona) {
        this.persona = persona;
    }
    
    public Integer getSessionSeconds() {
        return sessionSeconds;
    }
    
    public void setSessionSeconds(Integer sessionSeconds) {
        this.sessionSeconds = sessionSeconds;
    }
}