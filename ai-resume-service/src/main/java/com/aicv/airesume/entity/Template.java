package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * 简历模板实体类
 */
@Data
@Entity
@Table(name = "template")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Column(nullable = false)
    private String templateUrl;

    @Column(nullable = false)
    private String jobType;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "is_free", nullable = false, columnDefinition = "tinyint default 0")
    private Boolean free = false;

    @Column(name = "is_vip_only", nullable = false, columnDefinition = "tinyint default 0")
    private Boolean vipOnly = false;

    @Column(name = "use_count", nullable = false, columnDefinition = "int default 0")
    private Integer useCount = 0;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}
