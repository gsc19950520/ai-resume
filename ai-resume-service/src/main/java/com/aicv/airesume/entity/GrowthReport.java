package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI成长报告实体类
 */
@Data
@Entity
@Table(name = "growth_report")
public class GrowthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 概览信息
    @Column(name = "user_name", length = 100)
    private String userName;
    
    @Column(name = "latest_job_name", length = 200)
    private String latestJobName;
    
    @Column(name = "time_range", length = 100)
    private String timeRange;
    
    @Column(name = "first_total_score")
    private Double firstTotalScore;
    
    @Column(name = "latest_total_score")
    private Double latestTotalScore;
    
    @Column(name = "average_score")
    private Double averageScore;
    
    @Column(name = "improvement_rate")
    private Double improvementRate;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "interview_count", nullable = false)
    private Integer interviewCount;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;
}
