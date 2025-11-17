package com.aicv.airesume.model.vo;

import java.util.List;

/**
 * 简历AI建议VO
 */
public class ResumeSuggestionVO {
    
    private List<String> suggestions;
    
    public ResumeSuggestionVO() {
    }
    
    public ResumeSuggestionVO(List<String> suggestions) {
        this.suggestions = suggestions;
    }
    
    public List<String> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}