package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * 简历实体类
 */
@Data
@Entity
@Table(name = "resume")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private String jobType;

    private String originalContent;
    @Column(name = "optimized_content")
    private String optimizedContent;
    @Column(name = "ai_score")
    private Integer aiScore;
    private String aiSuggestion;

    @Column(name = "download_url_pdf")
    private String downloadUrlPdf;

    @Column(name = "download_url_word")
    private String downloadUrlWord;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "status", nullable = false, columnDefinition = "int default 0")
    private Integer status = 0; // 0: 上传成功, 1: 优化中, 2: 优化成功, 3: 优化失败

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}
