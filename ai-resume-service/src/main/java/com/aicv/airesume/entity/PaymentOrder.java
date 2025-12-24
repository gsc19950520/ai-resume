package com.aicv.airesume.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付订单实体类
 * 对应数据库中的payment_order表
 */
@Entity
@Table(name = "payment_order")
@Data
public class PaymentOrder {
    
    /**
     * 订单状态：待支付
     */
    public static final String STATUS_PENDING = "pending";
    
    /**
     * 订单状态：已支付
     */
    public static final String STATUS_PAID = "paid";
    
    /**
     * 订单状态：已取消
     */
    public static final String STATUS_CANCELLED = "cancelled";
    
    /**
     * 订单状态：已过期
     */
    public static final String STATUS_EXPIRED = "expired";
    
    /**
     * 支付类型：微信支付
     */
    public static final String PAY_TYPE_WECHAT = "wechat";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 订单编号
     */
    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 微信openId
     */
    @Column(name = "open_id", nullable = false)
    private String openId;
    
    /**
     * 总金额（单位：分）
     */
    @Column(name = "total_fee", nullable = false)
    private Integer totalFee;
    
    /**
     * 支付类型
     */
    @Column(name = "pay_type", nullable = false)
    private String payType;
    
    /**
     * 订单状态
     */
    @Column(name = "order_status", nullable = false)
    private String orderStatus;
    
    /**
     * 微信支付交易号
     */
    @Column(name = "trade_no")
    private String tradeNo;
    
    /**
     * 商品描述
     */
    @Column(name = "body")
    private String body;
    
    /**
     * 通知地址
     */
    @Column(name = "notify_url")
    private String notifyUrl;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 支付时间
     */
    @Column(name = "pay_time")
    private LocalDateTime payTime;
    
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
