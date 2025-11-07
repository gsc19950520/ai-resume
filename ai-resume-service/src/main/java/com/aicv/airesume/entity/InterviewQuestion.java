package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 面试问题实体类
 * 对应数据库中的interview_question表，实现AI职业问题库功能
 */
@Data
@Entity
@Table(name = "interview_question")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "expected_key_points", columnDefinition = "TEXT")
    private String expectedKeyPoints; // JSON格式存储期望的关键点

    @Column(name = "job_type_id")
    private Long jobTypeId;

    @Column(name = "skill_tag", length = 100)
    private String skillTag;

    @Column(name = "depth_level", length = 20)
    private String depthLevel; // 问题深度：usage/implementation/principle/optimization

    @Column(name = "persona", length = 50)
    private String persona; // 语言风格标签：friendly/formal/challenging

    @Column(name = "ai_generated", columnDefinition = "boolean default true")
    private Boolean aiGenerated = true;

    @Column(name = "usage_count", columnDefinition = "int default 0")
    private Integer usageCount = 0;

    @Column(name = "avg_score", columnDefinition = "float default 0")
    private Double avgScore = 0.00;

    @Column(name = "similarity_hash", length = 64)
    private String similarityHash;

    // 关联job_type表
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_type_id", insertable = false, updatable = false)
    private JobType jobType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}