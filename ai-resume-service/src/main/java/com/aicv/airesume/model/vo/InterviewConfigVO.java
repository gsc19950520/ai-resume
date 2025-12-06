package com.aicv.airesume.model.vo;

import java.util.List;
import java.util.Map;

/**
 * 面试配置VO
 */
public class InterviewConfigVO {
    
    private List<Map<String, Object>> personas;
    private Integer defaultSessionSeconds;
    
    public InterviewConfigVO() {}
    
    public InterviewConfigVO(List<Map<String, Object>> personas) {
        this.personas = personas;
    }
    
    public List<Map<String, Object>> getPersonas() {
        return personas;
    }
    
    public void setPersonas(List<Map<String, Object>> personas) {
        this.personas = personas;
    }
    
    public Integer getDefaultSessionSeconds() {
        return defaultSessionSeconds;
    }
    
    public void setDefaultSessionSeconds(Integer defaultSessionSeconds) {
        this.defaultSessionSeconds = defaultSessionSeconds;
    }
}