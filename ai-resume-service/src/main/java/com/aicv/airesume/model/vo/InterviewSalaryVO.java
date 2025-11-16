package com.aicv.airesume.model.vo;

/**
 * 薪资范围VO
 */
public class InterviewSalaryVO {
    
    private Double minSalary;
    private Double maxSalary;
    private String currency;
    private String level;
    private String reason;
    
    public InterviewSalaryVO() {}
    
    public InterviewSalaryVO(Double minSalary, Double maxSalary, String currency, String level, String reason) {
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.currency = currency;
        this.level = level;
        this.reason = reason;
    }
    
    public Double getMinSalary() {
        return minSalary;
    }
    
    public void setMinSalary(Double minSalary) {
        this.minSalary = minSalary;
    }
    
    public Double getMaxSalary() {
        return maxSalary;
    }
    
    public void setMaxSalary(Double maxSalary) {
        this.maxSalary = maxSalary;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}