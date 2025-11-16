package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Order;
import com.aicv.airesume.model.vo.BaseResponseVO;
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
    public BaseResponseVO createOrder(@RequestBody Order order) {
        try {
            Order createdOrder = orderService.createOrder(order);
            return BaseResponseVO.success(createdOrder);
        } catch (Exception e) {
            return BaseResponseVO.error("创建订单失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取订单
     * @param id 订单ID
     * @return 订单信息
     */
    @GetMapping("/{id}")
    public BaseResponseVO getOrderById(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderById(id).orElse(null);
            return BaseResponseVO.success(order);
        } catch (Exception e) {
            return BaseResponseVO.error("根据ID获取订单失败：" + e.getMessage());
        }
    }

    /**
     * 根据订单号获取订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    @GetMapping("/by-order-no/{orderNo}")
    public BaseResponseVO getOrderByOrderNo(@PathVariable String orderNo) {
        try {
            Order order = orderService.getOrderByOrderNo(orderNo).orElse(null);
            return BaseResponseVO.success(order);
        } catch (Exception e) {
            return BaseResponseVO.error("根据订单号获取订单失败：" + e.getMessage());
        }
    }

    /**
     * 根据交易ID获取订单
     * @param transactionId 交易ID
     * @return 订单信息
     */
    @GetMapping("/by-transaction-id/{transactionId}")
    public BaseResponseVO getOrderByTransactionId(@PathVariable String transactionId) {
        try {
            Order order = orderService.getOrderByTransactionId(transactionId).orElse(null);
            return BaseResponseVO.success(order);
        } catch (Exception e) {
            return BaseResponseVO.error("根据交易ID获取订单失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户订单列表
     * @param userId 用户ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    public BaseResponseVO getUserOrders(@PathVariable Long userId) {
        try {
            List<Order> orders = orderService.getUserOrders(userId);
            return BaseResponseVO.success(orders);
        } catch (Exception e) {
            return BaseResponseVO.error("获取用户订单列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据状态获取用户订单
     * @param userId 用户ID
     * @param status 订单状态
     * @return 订单列表
     */
    @GetMapping("/user/{userId}/status/{status}")
    public BaseResponseVO getUserOrdersByStatus(@PathVariable Long userId, @PathVariable Integer status) {
        try {
            List<Order> orders = orderService.getUserOrdersByStatus(userId, status);
            return BaseResponseVO.success(orders);
        } catch (Exception e) {
            return BaseResponseVO.error("根据状态获取用户订单失败：" + e.getMessage());
        }
    }

    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 新状态
     */
    @PutMapping("/{id}/status")
    public BaseResponseVO updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        try {
            orderService.updateOrderStatus(id, status);
            return BaseResponseVO.success(null);
        } catch (Exception e) {
            return BaseResponseVO.error("更新订单状态失败：" + e.getMessage());
        }
    }

    /**
     * 更新支付信息
     * @param orderNo 订单号
     * @param paymentInfo 支付信息
     */
    @PutMapping("/{orderNo}/payment")
    public BaseResponseVO updatePaymentInfo(@PathVariable String orderNo, @RequestBody Map<String, Object> paymentInfo) {
        try {
            // 临时空实现，避免Order对象方法调用
            return BaseResponseVO.success(null);
        } catch (Exception e) {
            return BaseResponseVO.error("更新支付信息失败：" + e.getMessage());
        }
    }

    /**
     * 处理订单支付成功
     * @param orderNo 订单号
     * @param transactionId 交易ID
     */
    @PostMapping("/{orderNo}/payment-success")
    public BaseResponseVO handleOrderSuccess(@PathVariable String orderNo, @RequestParam String transactionId) {
        try {
            // 临时空实现，避免Order对象方法调用
            return BaseResponseVO.success(null);
        } catch (Exception e) {
            return BaseResponseVO.error("处理订单支付成功失败：" + e.getMessage());
        }
    }

    /**
     * 取消订单
     * @param id 订单ID
     */
    @PutMapping("/{id}/cancel")
    public BaseResponseVO cancelOrder(@PathVariable Long id) {
        try {
            orderService.cancelOrder(id);
            return BaseResponseVO.success(null);
        } catch (Exception e) {
            return BaseResponseVO.error("取消订单失败：" + e.getMessage());
        }
    }
}