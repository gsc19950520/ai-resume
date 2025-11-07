package com.aicv.airesume.model.dto;

import lombok.Data;
import java.util.Map;

/**
 * 面试报告DTO
 */
@Data
public class InterviewReportDTO {
    private Long reportId;
    private Long sessionId;
    private String overallFeedback;
    private Map<String, Double> scores;
    private String strengths;
    private String areasForImprovement;
    private Double finalScore;
}
