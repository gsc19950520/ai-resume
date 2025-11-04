package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * 订单实体类
 */
@Data
@Entity
@Table(name = "ai_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNo;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String productType;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer status = 0; // 0: 待支付, 1: 支付成功, 2: 支付失败, 3: 已取消

    private String transactionId;
    private String payType;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false)
    private Date createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private Date updateTime;

    @Column(name = "pay_time")
    private Date payTime;

    /**
     * 支付信息内部类
     */
    @Data
    public static class PaymentInfo {
        private String transactionId;
        private String payType;
        private Date payTime;
    }
}
