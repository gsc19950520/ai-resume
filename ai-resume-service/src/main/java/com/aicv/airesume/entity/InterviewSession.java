package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

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

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "job_type", nullable = false, length = 100)
    private String jobType;

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // pending, in_progress, completed, canceled

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "tech_score")
    private Double techScore;

    @Column(name = "logic_score")
    private Double logicScore;

    @Column(name = "clarity_score")
    private Double clarityScore;

    @Column(name = "depth_score")
    private Double depthScore;

    @Column(name = "ai_estimated_years", length = 20)
    private String aiEstimatedYears;

    @Column(name = "ai_salary_range", length = 20)
    private String aiSalaryRange;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "report_url", length = 255)
    private String reportUrl;

    @Column(name = "max_depth_per_point")
    private Integer maxDepthPerPoint;

    @Column(name = "max_followups")
    private Integer maxFollowups;

    @Column(name = "time_limit_secs")
    private Integer timeLimitSecs;

    @Column(name = "actual_duration_secs")
    private Integer actualDurationSecs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InterviewLog> interviewLogs;

}