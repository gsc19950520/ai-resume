package com.aicv.airesume.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态配置实体类
 * 对应数据库中的dynamic_config表
 */
@Entity
@Table(name = "dynamic_config")
@Data
public class DynamicConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_type", nullable = false)
    private String configType;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "text")
    private String configValue;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}