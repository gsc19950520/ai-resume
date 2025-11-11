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
    @Column(name = "id", nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Column(nullable = false)
    private String templateUrl;

    @Column(name = "word_template_url")
    private String wordTemplateUrl;

    @Column(name = "html_template_content", columnDefinition = "text")
    private String htmlTemplateContent;

    @Column(name = "template_type", nullable = false, columnDefinition = "varchar(20) default 'fixed'")
    private String templateType = "fixed";

    @Column(nullable = false)
    private String jobType;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "is_free", nullable = false, columnDefinition = "tinyint default 0")
    private Boolean free = false;

    @Column(name = "vip_only", nullable = false, columnDefinition = "tinyint default 0")
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
