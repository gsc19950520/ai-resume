package com.aicv.airesume.model.vo;

/**
 * 薪资信息VO
 */
public class SalaryInfoVO {
    
    private Double minSalary;
    private Double maxSalary;
    private String currency;
    private String level;
    private String reason;
    
    // Getters and Setters
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