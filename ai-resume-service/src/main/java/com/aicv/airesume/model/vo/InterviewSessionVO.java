package com.aicv.airesume.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.aicv.airesume.entity.InterviewLog;

/**
 * 面试会话VO
 */
@Data
public class InterviewSessionVO {
    private Long id;
    private String title;
    private String description;
    private Long startTime;
    private Long endTime;
    private String status;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private List<InterviewHistoryVO> relatedSessions;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    private String stopReason;
    private List<InterviewLog> logs;
    private String persona;
    private Integer sessionSeconds;
    private Integer sessionTimeRemaining;
    private Double totalScore;
    private Integer interviewDuration;
    private Boolean hasQuestion;
    private String currentQuestion;
}
