package com.aicv.airesume.model.vo;

import java.util.List;
import java.util.Map;

/**
 * 面试配置VO
 */
public class InterviewConfigVO {
    
    private List<Map<String, Object>> personas;
    private List<Map<String, Object>> depthLevels;
    private Integer defaultSessionSeconds;
    private String defaultPersona;
    
    public InterviewConfigVO() {}
    
    public InterviewConfigVO(List<Map<String, Object>> personas, List<Map<String, Object>> depthLevels, 
                           Integer defaultSessionSeconds, String defaultPersona) {
        this.personas = personas;
        this.depthLevels = depthLevels;
        this.defaultSessionSeconds = defaultSessionSeconds;
        this.defaultPersona = defaultPersona;
    }
    
    public List<Map<String, Object>> getPersonas() {
        return personas;
    }
    
    public void setPersonas(List<Map<String, Object>> personas) {
        this.personas = personas;
    }
    
    public List<Map<String, Object>> getDepthLevels() {
        return depthLevels;
    }
    
    public void setDepthLevels(List<Map<String, Object>> depthLevels) {
        this.depthLevels = depthLevels;
    }
    
    public Integer getDefaultSessionSeconds() {
        return defaultSessionSeconds;
    }
    
    public void setDefaultSessionSeconds(Integer defaultSessionSeconds) {
        this.defaultSessionSeconds = defaultSessionSeconds;
    }
    
    public String getDefaultPersona() {
        return defaultPersona;
    }
    
    public void setDefaultPersona(String defaultPersona) {
        this.defaultPersona = defaultPersona;
    }
}