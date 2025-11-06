package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

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
    private String matchedPoints;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "answer_duration_secs")
    private Integer answerDurationSecs;

}