package com.aicv.airesume.utils;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 微信工具类
 */
@Component
public class WechatUtils {

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 微信登录，获取openId
     */
    public JSONObject getOpenId(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId + "&secret=" + appSecret + "&js_code=" + code + "&grant_type=authorization_code";
        return restTemplate.getForObject(url, JSONObject.class);
    }

    /**
     * 获取微信支付签名
     */
    public String generatePaySign(Map<String, String> params, String key) {
        // 实现微信支付签名生成逻辑
        // 这里简化处理，实际项目中需要按照微信支付文档规范实现
        return "pay_sign";
    }
}