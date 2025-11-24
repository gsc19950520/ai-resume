package com.aicv.airesume.model.dto;

import lombok.Data;

/**
 * 提交答案请求DTO
 */
@Data
public class SubmitAnswerRequestDTO {
    private String sessionId;
    private String userAnswerText;
    private Integer answerDuration;
    private String toneStyle;
}