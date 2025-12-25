package com.aicv.airesume.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置类
 */
@Configuration
@Data
public class WeChatPayConfig {
    
    /**
     * 微信小程序appid
     */
    @Value("${wechat.app-id}")
    private String appId;
    
    /**
     * 微信小程序appsecret
     */
    @Value("${wechat.app-secret}")
    private String appSecret;
    
    /**
     * 微信支付商户号
     */
    @Value("${wechat.pay.mch-id}")
    private String mchId;
    
    /**
     * 微信支付API密钥
     */
    @Value("${wechat.pay.api-key}")
    private String apiKey;
    
    /**
     * 微信支付回调地址
     */
    @Value("${wechat.pay.notify-url}")
    private String notifyUrl;
    
    /**
     * 微信支付统一下单API
     */
    public static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    
    /**
     * 微信支付查询订单API
     */
    public static final String QUERY_ORDER_URL = "https://api.mch.weixin.qq.com/pay/orderquery";
    
    /**
     * 微信支付关闭订单API
     */
    public static final String CLOSE_ORDER_URL = "https://api.mch.weixin.qq.com/pay/closeorder";
    
    /**
     * 微信支付签名类型
     */
    public static final String SIGN_TYPE = "MD5";
}
