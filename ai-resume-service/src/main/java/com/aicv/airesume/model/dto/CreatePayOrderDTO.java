package com.aicv.airesume.model.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 创建支付订单请求DTO
 * @author AI简历助手
 */
@Data
public class CreatePayOrderDTO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 面试配置ID
     */
    @NotNull(message = "面试配置ID不能为空")
    private Long interviewConfigId;

    /**
     * 支付金额（单位：分）
     */
    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于0")
    private Integer totalFee;

    /**
     * 订单描述
     */
    private String body;
    
    /**
     * 用户OpenID（微信小程序支付需要）
     */
    @NotNull(message = "OpenID不能为空")
    private String openId;
}
