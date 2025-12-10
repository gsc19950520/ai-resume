package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 面试日志实体类
 */
@Data
@Entity
@Table(name = "interview_log")
public class InterviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false, length = 100)
    private String questionId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "question_text", nullable = true, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "user_answer_text", columnDefinition = "TEXT")
    private String userAnswerText;

    @Column(name = "depth_level", length = 20)
    private String depthLevel;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;
    
    // 新增字段 - 动态面试相关
    @Column(name = "answer_duration")
    private Integer answerDuration;
    
    @Column(name = "expected_key_points", columnDefinition = "text")
    private String expectedKeyPoints; // 期望的关键点，JSON格式存储

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}