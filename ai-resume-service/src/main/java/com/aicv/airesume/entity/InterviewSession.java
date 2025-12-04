package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 面试会话实体类
 */
@Data
@Entity
@Table(name = "interview_session")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "job_type_id", nullable = false, length = 100)
    private Integer jobTypeId;

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // pending, in_progress, completed, canceled



    // 新增字段 - 动态面试相关
    @Column(name = "persona", length = 50, columnDefinition = "varchar(50) default 'friendly'")
    private String persona = "friendly"; // 面试官风格：friendly, neutral, challenging
    
    @Column(name = "session_seconds", columnDefinition = "int default 900")
    private Integer sessionSeconds = 600; // 会话总时长（秒）
    
    @Column(name = "session_time_remaining", columnDefinition = "int default 900")
    private Integer sessionTimeRemaining = 600; // 剩余时间（秒）
    
    @Column(name = "stop_reason", length = 100)
    private String stopReason; // 停止原因
    
    @CreationTimestamp
    @Column(name = "start_time", nullable = false, updatable = false)
    private LocalDateTime startTime; // 开始时间
    
    @Column(name = "end_time")
    private LocalDateTime endTime; // 结束时间
    
    @Column(name = "tech_items", columnDefinition = "text")
    private String techItems;
    
    @Column(name = "project_points", columnDefinition = "text")
    private String projectPoints;
    
    @Column(name = "interview_state", columnDefinition = "text")
    private String interviewState;
    
    @Column(name = "ai_question_seed")
    private Integer aiQuestionSeed;
    
    @Column(name = "adaptive_level", length = 20, columnDefinition = "varchar(20) default 'auto'")
    private String adaptiveLevel = "auto"; // 题目深度模式：auto/fixed
    
    @Column(name = "question_count", columnDefinition = "int default 0")
    private Integer questionCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}