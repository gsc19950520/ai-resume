package com.aicv.airesume.service;

import com.aicv.airesume.entity.Order;

import java.util.List;
import java.util.Optional;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     */
    Order createOrder(Order order);
    
    /**
     * 根据ID查询订单
     */
    Optional<Order> getOrderById(Long id);
    
    /**
     * 根据订单号查询订单
     */
    Optional<Order> getOrderByOrderNo(String orderNo);
    
    /**
     * 根据微信支付订单号查询订单
     */
    Optional<Order> getOrderByTransactionId(String transactionId);
    
    /**
     * 根据用户ID查询订单列表
     */
    List<Order> getUserOrders(Long userId);
    
    /**
     * 根据用户ID和状态查询订单
     */
    List<Order> getUserOrdersByStatus(Long userId, Integer status);
    
    /**
     * 更新订单状态
     */
    Order updateOrderStatus(Long orderId, Integer status);
    
    /**
     * 更新订单支付信息
     */
    Order updatePaymentInfo(Long orderId, String transactionId, String paymentTime);
    
    /**
     * 处理订单支付成功
     */
    void handleOrderSuccess(Order order);
    
    /**
     * 取消订单
     */
    Order cancelOrder(Long orderId);
}