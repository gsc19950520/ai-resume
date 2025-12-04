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
    @Column(name = "job_type_id")
    private Long jobTypeId;
    private String jobTitle; // 职位名称
    @Column(name = "template_id")
    private String templateId;
    // 期望薪资
    @Column(name = "expected_salary")
    private String expectedSalary;
    // 到岗时间
    @Column(name = "start_time")
    private String startTime;
    private String selfEvaluation; // 自我评价
    private String interests; // 兴趣爱好

    @Column(nullable = false)
    private String originalFilename;


    @Column(name = "status", nullable = false, columnDefinition = "int default 0")
    private Integer status = 0; // 0: 上传/创建成功, 1: 优化中, 2: 优化成功, 3: 优化失败

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
    
    // 技术项，存储JSON格式
    @Column(name = "tech_items", columnDefinition = "text")
    private String techItems;
    
    // 项目点，存储JSON格式
    @Column(name = "project_points", columnDefinition = "text")
    private String projectPoints;
    
    // 最后提取时间
    @Column(name = "last_extracted_time")
    private Date lastExtractedTime;
    
    // 手动添加getter方法以解决编译问题
    public Long getId() {
        return id;
    }
    
    public Long getUserId() {
        return userId;
    }
}
