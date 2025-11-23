package com.aicv.airesume.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.util.Date;

/**
 * 职位类型实体类
 * 对应数据库中的job_type表
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "job_type")
public class JobType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "job_name")
    private String jobName;

    @Column(length = 200)
    private String description;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "create_time", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime = new Date();

    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime = new Date();

}