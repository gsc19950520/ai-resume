package com.aicv.airesume.model.dto;

import lombok.Data;

/**
 * 开始面试请求DTO
 */
@Data
public class InterviewStartRequestDTO {
    private Long userId;
    private Long resumeId;
    private String persona;
    private Integer sessionSeconds;
    private String analysisDepth = "intermediate";
    private Integer jobTypeId;
}