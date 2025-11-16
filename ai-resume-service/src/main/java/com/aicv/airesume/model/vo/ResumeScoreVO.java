package com.aicv.airesume.model.vo;

/**
 * 简历AI评分VO
 */
public class ResumeScoreVO {
    
    private Integer score;
    
    public ResumeScoreVO() {
    }
    
    public ResumeScoreVO(Integer score) {
        this.score = score;
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
}