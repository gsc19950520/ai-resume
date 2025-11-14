package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户实体类
 */
@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String openId;

    private String name; // 姓名
    private String email; // 邮箱
    private String phone; // 电话
    private String address; // 地址
    @Column(name = "birth_date")
    private String birthDate; // 出生日期

    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String country;
    private String province;
    private String city;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer remainingOptimizeCount = 0;

    @Column(name = "is_vip", nullable = false, columnDefinition = "tinyint default 0")
    private Boolean vip = false;

    @Column(name = "vip_expire_time")
    private Date vipExpireTime;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;
}