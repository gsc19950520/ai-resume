package com.aicv.airesume.model.dto;

import lombok.Data;

/**
 * 面试问题DTO
 */
@Data
public class InterviewQuestionDTO {
    private Long id;
    private String questionContent;
    private String questionType;
    private Integer orderIndex;
    private Long sessionId;
    private String category;
}
