package com.aicv.airesume.controller;

import com.aicv.airesume.service.SubscriptionService;
import com.aicv.airesume.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订阅控制器
 */
@RestController
@RequestMapping("/api/subscribe")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private TokenUtils tokenUtils;

    /**
     * 购买会员
     */
    @PostMapping("/buy/membership")
    public Object buyMembership(
            @RequestBody Map<String, Integer> request,
            @RequestHeader("Authorization") String token) {
        
        Long userId = tokenUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) {
            throw new RuntimeException("Token无效");
        }
        
        Integer days = request.get("days");
        if (days == null) {
            throw new RuntimeException("天数不能为空");
        }
        
        return subscriptionService.buyMembership(userId, days);
    }

    /**
     * 购买优化次数包
     */
    @PostMapping("/buy/optimize")
    public Object buyOptimizePackage(
            @RequestBody Map<String, Integer> request,
            @RequestHeader("Authorization") String token) {
        
        Long userId = tokenUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) {
            throw new RuntimeException("Token无效");
        }
        
        Integer count = request.get("count");
        if (count == null) {
            throw new RuntimeException("次数不能为空");
        }
        
        return subscriptionService.buyOptimizePackage(userId, count);
    }

    /**
     * 购买模板包
     */
    @PostMapping("/buy/template")
    public Object buyTemplatePackage(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String token) {
        
        Long userId = tokenUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) {
            throw new RuntimeException("Token无效");
        }
        
        Long templateId = request.get("templateId");
        if (templateId == null) {
            throw new RuntimeException("模板ID不能为空");
        }
        
        return subscriptionService.buyTemplatePackage(userId, templateId);
    }

    /**
     * 获取订单列表
     */
    @GetMapping("/orders")
    public Object getOrders(@RequestHeader("Authorization") String token) {
        
        Long userId = tokenUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        if (userId == null) {
            throw new RuntimeException("Token无效");
        }
        
        return subscriptionService.getOrderList(userId);
    }

    /**
     * 支付回调
     */
    @PostMapping("/pay/notify")
    public String payNotify(@RequestBody Map<String, String> notifyData) {
        try {
            subscriptionService.handlePayCallback(notifyData);
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        } catch (Exception e) {
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[" + e.getMessage() + "]]></return_msg></xml>";
        }
    }
}