package com.aicv.airesume.model.vo;

import lombok.Data;
import java.util.Map;

/**
 * 支付结果VO
 * 用于返回给前端小程序的支付参数
 * @author AI简历助手
 */
@Data
public class PayResultVO {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 微信支付时间戳
     */
    private String timeStamp;

    /**
     * 微信支付随机字符串
     */
    private String nonceStr;

    /**
     * 微信支付数据包
     */
    private String packageStr;

    /**
     * 微信支付签名方式
     */
    private String signType;

    /**
     * 微信支付签名
     */
    private String paySign;

    /**
     * 订单状态
     */
    private String orderStatus;
    
    /**
     * 预支付ID
     */
    private String prepayId;
    
    /**
     * JSAPI支付参数
     */
    private Map<String, String> jsApiParams;
}
