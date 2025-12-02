package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 面试报告实体类
 */
@Data
@Entity
@Table(name = "interview_report")
public class InterviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "overall_feedback", columnDefinition = "text")
    private String overallFeedback;

    @Column(name = "strengths", columnDefinition = "text")
    private String strengths;

    @Column(name = "improvements", columnDefinition = "text")
    private String improvements;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}