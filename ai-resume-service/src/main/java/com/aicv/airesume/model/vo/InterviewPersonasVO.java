package com.aicv.airesume.model.vo;

import java.util.List;
import java.util.Map;

/**
 * 面试官风格列表VO
 */
public class InterviewPersonasVO {
    
    private List<Map<String, Object>> personas;
    
    public InterviewPersonasVO() {}
    
    public InterviewPersonasVO(List<Map<String, Object>> personas) {
        this.personas = personas;
    }
    
    public List<Map<String, Object>> getPersonas() {
        return personas;
    }
    
    public void setPersonas(List<Map<String, Object>> personas) {
        this.personas = personas;
    }
}