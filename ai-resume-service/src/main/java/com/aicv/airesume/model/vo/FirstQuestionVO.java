package com.aicv.airesume.model.vo;

/**
 * 生成第一个问题VO
 */
public class FirstQuestionVO {
    
    private String question;
    private String questionType;
    private String difficulty;
    private String category;
    
    public FirstQuestionVO() {}
    
    public FirstQuestionVO(String question, String questionType, String difficulty, String category) {
        this.question = question;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.category = category;
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
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
}