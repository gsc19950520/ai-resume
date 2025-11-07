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

    @ManyToOne
    @JoinColumn(name = "session_id", referencedColumnName = "session_id", nullable = false)
    private InterviewSession session;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "user_answer_text", columnDefinition = "TEXT")
    private String userAnswerText;

    @Column(name = "user_answer_audio_url", length = 255)
    private String userAnswerAudioUrl;

    @Column(name = "depth_level", length = 20)
    private String depthLevel;

    @Column(name = "tech_score")
    private Double techScore;

    @Column(name = "logic_score")
    private Double logicScore;

    @Column(name = "clarity_score")
    private Double clarityScore;

    @Column(name = "depth_score")
    private Double depthScore;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "matched_points", columnDefinition = "TEXT")
    private String matchedPoints; // JSON格式存储

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;
    
    // 新增字段 - 动态面试相关
    @Column(name = "answer_duration")
    private Integer answerDuration;
    
    @Column(name = "related_tech_items", columnDefinition = "text")
    private String relatedTechItems;
    
    @Column(name = "related_project_points", columnDefinition = "text")
    private String relatedProjectPoints;
    
    @Column(name = "stop_reason", length = 100)
    private String stopReason;
    
    @Column(name = "persona", length = 50)
    private String persona; // 当前题目使用的面试官语气风格
    
    @Column(name = "ai_feedback_json", columnDefinition = "longtext")
    private String aiFeedbackJson; // AI原始评分和分析结果

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}