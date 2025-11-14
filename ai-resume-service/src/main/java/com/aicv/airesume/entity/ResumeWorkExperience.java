package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * 简历工作经历实体类
 */
@Data
@Entity
@Table(name = "resume_work_experience")
public class ResumeWorkExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "company_name")
    private String companyName; // 公司名称
    
    @Column(name = "position_name")
    private String positionName; // 职位名称
    
    @Column(name = "start_date")
    private String startDate; // 开始日期
    
    @Column(name = "end_date")
    private String endDate; // 结束日期
    
    @Column(columnDefinition = "text")
    private String description; // 工作描述
    private Integer orderIndex; // 排序索引

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}