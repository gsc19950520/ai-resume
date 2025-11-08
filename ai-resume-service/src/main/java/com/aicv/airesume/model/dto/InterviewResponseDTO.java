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
}
