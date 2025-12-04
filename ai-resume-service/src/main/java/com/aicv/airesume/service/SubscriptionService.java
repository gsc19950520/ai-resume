package com.aicv.airesume.service;

import com.aicv.airesume.model.dto.PayNotifyDTO;

import java.util.List;
import java.util.Map;

/**
 * 订阅服务接口
 */
public interface SubscriptionService {

    /**
     * 创建订单
     */
    Object createOrder(Long userId, String productId, String productType, Integer amount);

    /**
     * 支付订单
     */
    Map<String, Object> payOrder(String orderNo, Long userId);

    /**
     * 处理支付回调
     */
    void handlePayCallback(Map<String, String> notifyData);

    /**
     * 支付回调处理（使用DTO）
     */
    String payNotify(PayNotifyDTO payNotifyDTO);

    /**
     * 查询订单列表
     */
    List<Object> getOrderList(Long userId);

    /**
     * 分页查询订单列表
     */
    List<Object> getOrders(Long userId, int page, int pageSize);

    /**
     * 根据订单号查询订单
     */
    Object getOrderByNo(String orderNo);

    /**
     * 购买会员
     */
    Map<String, Object> buyMembership(Long userId, Integer days);

    /**
     * 购买优化次数包
     */
    Map<String, Object> buyOptimizePackage(Long userId, Integer count);

    /**
     * 购买模板包
     */
    Map<String, Object> buyTemplatePackage(Long userId, Long templateId);
}