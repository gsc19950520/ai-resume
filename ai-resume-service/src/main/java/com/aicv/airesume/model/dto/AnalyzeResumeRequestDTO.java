package com.aicv.airesume.model.dto;

import lombok.Data;

/**
 * 分析简历请求DTO
 */
@Data
public class AnalyzeResumeRequestDTO {
    private Long resumeId;
    private String jobType = "";
    private String analysisDepth = "intermediate";
}