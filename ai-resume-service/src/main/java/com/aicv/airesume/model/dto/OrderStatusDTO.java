package com.aicv.airesume.model.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 订单状态查询DTO
 * @author AI简历助手
 */
@Data
public class OrderStatusDTO {

    /**
     * 订单编号
     */
    @NotNull(message = "订单编号不能为空")
    private String orderNo;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
