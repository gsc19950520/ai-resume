package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * 简历项目经历实体类
 */
@Data
@Entity
@Table(name = "resume_project")
public class ResumeProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "project_name")
    private String projectName; // 项目名称
    
    @Column(name = "start_date")
    private String startDate; // 开始日期
    
    @Column(name = "end_date")
    private String endDate; // 结束日期
    
    @Column(columnDefinition = "text")
    private String description; // 项目描述
    
    private Integer orderIndex; // 排序索引
    
    private String role; // 项目角色
    
    @Column(name = "tech_stack")
    private String techStack; // 技术栈

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}