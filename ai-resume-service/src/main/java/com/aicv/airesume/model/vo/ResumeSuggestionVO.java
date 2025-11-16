package com.aicv.airesume.model.vo;

/**
 * 简历AI建议VO
 */
public class ResumeSuggestionVO {
    
    private String suggestions;
    
    public ResumeSuggestionVO() {
    }
    
    public ResumeSuggestionVO(String suggestions) {
        this.suggestions = suggestions;
    }
    
    public String getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }
}