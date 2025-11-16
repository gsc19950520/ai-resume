package com.aicv.airesume.model.dto;

import lombok.Data;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * 面试报告DTO
 */
@Data
public class InterviewReportDTO {
    private Long reportId;
    private String sessionId;
    private String overallFeedback;
    private Map<String, Double> scores;
    private String strengths;
    private String areasForImprovement;
    private String improvements;
    private Double finalScore;
    private Double totalScore;
    private LocalDateTime createdAt;
}
