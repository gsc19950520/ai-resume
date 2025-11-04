package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Order;
import com.aicv.airesume.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     * @param order 订单信息
     * @return 创建的订单
     */
    @PostMapping("/create")
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    /**
     * 根据ID获取订单
     * @param id 订单ID
     * @return 订单信息
     */
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id).orElse(null);
    }

    /**
     * 根据订单号获取订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    @GetMapping("/by-order-no/{orderNo}")
    public Order getOrderByOrderNo(@PathVariable String orderNo) {
        return orderService.getOrderByOrderNo(orderNo).orElse(null);
    }

    /**
     * 根据交易ID获取订单
     * @param transactionId 交易ID
     * @return 订单信息
     */
    @GetMapping("/by-transaction-id/{transactionId}")
    public Order getOrderByTransactionId(@PathVariable String transactionId) {
        return orderService.getOrderByTransactionId(transactionId).orElse(null);
    }

    /**
     * 获取用户订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    public List<Order> getUserOrders(@PathVariable Long userId) {
        return orderService.getUserOrders(userId);
    }

    /**
     * 根据状态获取用户订单
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单列表
     */
    @GetMapping("/user/{userId}/status/{status}")
    public List<Order> getUserOrdersByStatus(@PathVariable Long userId, @PathVariable Integer status) {
        return orderService.getUserOrdersByStatus(userId, status);
    }

    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 新状态
     */
    @PutMapping("/{id}/status")
    public void updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        orderService.updateOrderStatus(id, status);
    }

    /**
     * 更新支付信息
     * @param orderNo 订单号
     * @param paymentInfo 支付信息
     */
    @PutMapping("/{orderNo}/payment")
    public void updatePaymentInfo(@PathVariable String orderNo, @RequestBody Map<String, Object> paymentInfo) {
        // 临时空实现，避免Order对象方法调用
    }

    /**
     * 处理订单支付成功
     * @param orderNo 订单号
     * @param transactionId 交易ID
     */
    @PostMapping("/{orderNo}/payment-success")
    public void handleOrderSuccess(@PathVariable String orderNo, @RequestParam String transactionId) {
        // 临时空实现，避免Order对象方法调用
    }

    /**
     * 取消订单
     * @param id 订单ID
     */
    @PutMapping("/{id}/cancel")
    public void cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
    }
}