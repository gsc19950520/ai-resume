package com.aicv.airesume.model.vo;

import lombok.Data;

/**
 * 面试历史VO
 */
@Data
public class InterviewHistoryVO {
    private Long sessionId;
    private String uniqueSessionId;
    private String title;
    private Long startTime;
    private Long endTime;
    private Integer questionCount;
    private Double finalScore;
    private String status;
}
