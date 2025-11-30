package com.aicv.airesume.model.vo;

/**
 * 会话记录VO
 */
public class SessionLogVO {
    
    private String question;
    private String answer;
    private String feedback;
    private Double score;
    
    // Getters and Setters
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
}