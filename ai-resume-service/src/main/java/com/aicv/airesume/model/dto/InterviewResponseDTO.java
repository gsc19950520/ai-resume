package com.aicv.airesume.model.dto;

import lombok.Data;
import java.util.Map;

/**
 * 面试回答DTO
 */
@Data
public class InterviewResponseDTO {
    private Long id;
    private Long questionId;
    private String sessionId;
    private String answerContent;
    private Long startTime;
    private Long endTime;
    private Map<String, Object> additionalInfo;
    private String question;
    private String questionType;
    private Double score;
    private String feedback;
    private String nextQuestion;
    private String nextQuestionType;
    private Boolean isCompleted;
}
