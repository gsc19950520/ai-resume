package com.aicv.airesume.model.vo;

/**
 * AI跟踪日志VO
 */
public class AiTraceLogVO {
    
    private String sessionId;
    private String logLevel;
    private String message;
    private String createdAt;
    
    public AiTraceLogVO() {}
    
    public AiTraceLogVO(String sessionId, String logLevel, String message, String createdAt) {
        this.sessionId = sessionId;
        this.logLevel = logLevel;
        this.message = message;
        this.createdAt = createdAt;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}