package com.aicv.airesume.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.util.Date;

/**
 * 薪资信息实体类
 * 对应数据库中的salary_info表
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "salary_info")
public class SalaryInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type_id", nullable = false)
    private Long jobTypeId;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(name = "salary_range", nullable = false, length = 50)
    private String salaryRange;

    @Column(length = 50)
    private String experience;

    @Column(nullable = false, columnDefinition = "int default 80")
    private Integer confidence = 80;

    @Column(name = "salary_level", length = 50)
    private String salaryLevel;

    @Column(name = "trend_change", length = 20)
    private String trendChange;

    @Column(name = "create_time", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime = new Date();

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime = new Date();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_type_id", insertable = false, updatable = false)
    private JobType jobType;
}
