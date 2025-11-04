package com.aicv.airesume.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 简历模板实体类
 */
@Data
@Entity
@Table(name = "resume_template")
public class ResumeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "template_file_url")
    private String templateFileUrl;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer usageCount = 0;

    @Column(name = "is_premium", nullable = false, columnDefinition = "tinyint default 0")
    private Boolean premium = false;

    @Column(nullable = false, columnDefinition = "tinyint default 1")
    private Boolean active = true;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;
}