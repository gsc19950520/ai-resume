package com.aicv.airesume.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 产品实体类
 */
@Data
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private String type;

    @Column(name = "is_active", nullable = false, columnDefinition = "tinyint default 1")
    private Boolean active = true;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;
}