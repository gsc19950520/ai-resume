package com.aicv.airesume.controller;

import com.aicv.airesume.model.dto.CreatePayOrderDTO;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.model.vo.PayResultVO;
import com.aicv.airesume.entity.PaymentOrder;
import com.aicv.airesume.service.PayService;
import com.aicv.airesume.utils.GlobalContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付相关接口控制器
 */
@RestController
@RequestMapping("/api/pay")
@Slf4j
public class PayController {

    @Autowired
    private PayService payService;

    /**
     * 创建支付订单
     * @param createPayOrderDTO 创建支付订单参数
     * @return 支付结果VO
     */
    @PostMapping("/create-order")
    public BaseResponseVO createPayOrder(@RequestBody CreatePayOrderDTO createPayOrderDTO, HttpServletRequest request) {
        try {
            // 获取客户端IP地址
            String clientIp = getClientIp(request);
            log.info("创建支付订单请求: {}, 客户端IP: {}", createPayOrderDTO, clientIp);
            
            // 创建支付订单
            PayResultVO payResultVO = payService.createPayOrder(createPayOrderDTO, clientIp);
            return BaseResponseVO.success(payResultVO);
        } catch (Exception e) {
            log.error("创建支付订单失败: {}", e.getMessage(), e);
            return BaseResponseVO.error("创建支付订单失败: " + e.getMessage());
        }
    }

    /**
     * 微信支付回调接口
     * @param xmlData 微信支付回调的XML数据
     * @return 处理结果
     */
    @PostMapping("/wechat/notify")
    public String wechatPayNotify(@RequestBody String xmlData) {
        log.info("收到微信支付回调请求");
        return payService.handlePayNotify(xmlData);
    }

    /**
     * 查询订单状态
     * @param orderNo 订单编号
     * @return 订单状态
     */
    @GetMapping("/order-status")
    public BaseResponseVO queryOrderStatus(@RequestParam String orderNo) {
        try {
            log.info("查询订单状态: {}", orderNo);
            
            // 查询订单状态
            PaymentOrder paymentOrder = payService.queryOrderStatus(orderNo);
            return BaseResponseVO.success(paymentOrder);
        } catch (Exception e) {
            log.error("查询订单状态失败: {}", e.getMessage(), e);
            return BaseResponseVO.error("查询订单状态失败: " + e.getMessage());
        }
    }

    /**
     * 验证订单是否已支付
     * @param orderNo 订单编号
     * @return 是否已支付
     */
    @GetMapping("/verify-payment")
    public BaseResponseVO verifyPayment(@RequestParam String orderNo) {
        try {
            log.info("验证订单支付状态: {}", orderNo);
            
            // 验证订单是否已支付
            boolean isPaid = payService.verifyOrderPaid(orderNo);
            return BaseResponseVO.success(isPaid);
        } catch (Exception e) {
            log.error("验证订单支付状态失败: {}", e.getMessage(), e);
            return BaseResponseVO.error("验证订单支付状态失败: " + e.getMessage());
        }
    }

    /**
     * 关闭订单
     * @param orderNo 订单编号
     * @return 是否关闭成功
     */
    @PostMapping("/close-order")
    public BaseResponseVO closeOrder(@RequestParam String orderNo) {
        try {
            log.info("关闭订单: {}", orderNo);
            
            // 关闭订单
            boolean isClosed = payService.closeOrder(orderNo);
            if (isClosed) {
                return BaseResponseVO.success("关闭订单成功");
            } else {
                return BaseResponseVO.error("关闭订单失败");
            }
        } catch (Exception e) {
            log.error("关闭订单失败: {}", e.getMessage(), e);
            return BaseResponseVO.error("关闭订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
