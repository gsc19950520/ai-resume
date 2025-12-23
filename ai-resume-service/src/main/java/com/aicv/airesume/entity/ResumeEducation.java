package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * 简历教育经历实体类
 */
@Data
@Entity
@Table(name = "resume_education")
public class ResumeEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    private String school; // 学校名称
    private String degree; // 学位
    private String major; // 专业
    
    @Column(name = "start_date")
    private String startDate; // 开始日期
    
    @Column(name = "end_date")
    private String endDate; // 结束日期
    
    // 手动添加setter方法以解决编译问题
    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    private Integer orderIndex; // 排序索引

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}