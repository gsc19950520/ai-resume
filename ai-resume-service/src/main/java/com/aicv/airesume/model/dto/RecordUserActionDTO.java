package com.aicv.airesume.model.dto;

/**
 * 用户行为记录DTO
 */
public class RecordUserActionDTO {
    
    private Long userId;
    private String action;
    private String details;
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
}