package com.aicv.airesume.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 支付订单VO
 * 用于返回订单信息给前端
 * @author AI简历助手
 */
@Data
public class PaymentOrderVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 面试配置ID
     */
    private Long interviewConfigId;

    /**
     * 支付金额（单位：分）
     */
    private Integer totalFee;

    /**
     * 订单描述
     */
    private String body;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 微信支付交易号
     */
    private String transactionId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;
}
