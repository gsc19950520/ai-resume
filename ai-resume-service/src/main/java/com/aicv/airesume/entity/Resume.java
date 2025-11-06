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

    // 简历与模板的一对一关联
    @ManyToOne
    @JoinColumn(name = "template_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_resume_template"))
    private Template template;
    
    // 直接访问template_id字段，用于设置和获取
    @Transient
    private Long templateId;
    
    // 模板配置信息，用于存储简历特定的模板配置
    @Column(name = "template_config", columnDefinition = "text")
    private String templateConfig;
    
    // 获取templateId
    public Long getTemplateId() {
        if (templateId != null) {
            return templateId;
        }
        return template != null ? template.getId() : null;
    }
    
    // 设置templateId
    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    @Column(name = "status", nullable = false, columnDefinition = "int default 0")
    private Integer status = 0; // 0: 上传成功, 1: 优化中, 2: 优化成功, 3: 优化失败

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}
