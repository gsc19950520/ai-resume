package com.aicv.airesume.model.vo;

/**
 * 面试提交答案响应VO
 */
public class InterviewAnswerVO {
    
    private String sessionId;
    private String question;
    private String questionType;
    private Double score;
    private String feedback;
    private String nextQuestion;
    private String nextQuestionType;
    private Boolean isCompleted;
    
    public InterviewAnswerVO() {}
    
    public InterviewAnswerVO(String sessionId, String question, String questionType, Double score, 
                           String feedback, String nextQuestion, String nextQuestionType, Boolean isCompleted) {
        this.sessionId = sessionId;
        this.question = question;
        this.questionType = questionType;
        this.score = score;
        this.feedback = feedback;
        this.nextQuestion = nextQuestion;
        this.nextQuestionType = nextQuestionType;
        this.isCompleted = isCompleted;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public String getNextQuestion() {
        return nextQuestion;
    }
    
    public void setNextQuestion(String nextQuestion) {
        this.nextQuestion = nextQuestion;
    }
    
    public String getNextQuestionType() {
        return nextQuestionType;
    }
    
    
    public void setNextQuestionType(String nextQuestionType) {
        this.nextQuestionType = nextQuestionType;
    }
    
    public Boolean getIsCompleted() {
        return isCompleted;
    }
    
    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }
}