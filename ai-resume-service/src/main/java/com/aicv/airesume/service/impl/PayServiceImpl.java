package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.PaymentOrder;
import com.aicv.airesume.model.dto.CreatePayOrderDTO;
import com.aicv.airesume.model.vo.PayResultVO;
import com.aicv.airesume.repository.PaymentOrderRepository;
import com.aicv.airesume.service.PayService;
import com.aicv.airesume.service.wechat.WeChatPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 支付服务实现类
 */
@Service
@Slf4j
public class PayServiceImpl implements PayService {

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private WeChatPayService weChatPayService;

    /**
     * 从XML中提取预支付ID
     */
    private static final Pattern PREPAY_ID_PATTERN = Pattern.compile("<prepay_id>([^<]+)</prepay_id>");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResultVO createPayOrder(CreatePayOrderDTO createPayOrderDTO, String ipAddress) {
        // 1. 生成唯一订单号
        String orderNo = generateOrderNo();

        // 2. 创建支付订单
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderNo(orderNo);
        paymentOrder.setUserId(createPayOrderDTO.getUserId());
        paymentOrder.setOpenId(createPayOrderDTO.getOpenId());
        paymentOrder.setTotalFee(createPayOrderDTO.getTotalFee());
        paymentOrder.setPayType(PaymentOrder.PAY_TYPE_WECHAT);
        paymentOrder.setOrderStatus(PaymentOrder.STATUS_PENDING);
        paymentOrder.setBody(createPayOrderDTO.getBody());

        // 3. 保存支付订单
        paymentOrderRepository.save(paymentOrder);

        try {
            // 4. 调用微信支付API创建预支付订单
            String xmlResult = weChatPayService.createPrepayOrder(paymentOrder, ipAddress);
            log.info("微信支付预支付订单返回: {}", xmlResult);

            // 5. 解析预支付ID
            String prepayId = extractPrepayId(xmlResult);
            if (StringUtils.isEmpty(prepayId)) {
                throw new RuntimeException("获取预支付ID失败");
            }

            // 6. 生成JSAPI支付参数
            Map<String, String> jsApiParams = weChatPayService.generateJsApiParams(prepayId);

            // 7. 构造返回结果
            PayResultVO payResultVO = new PayResultVO();
            payResultVO.setOrderNo(orderNo);
            payResultVO.setPrepayId(prepayId);
            payResultVO.setJsApiParams(jsApiParams);
            payResultVO.setOrderStatus(paymentOrder.getOrderStatus());

            return payResultVO;
        } catch (Exception e) {
            log.error("创建预支付订单失败: {}", e.getMessage(), e);
            // 如果创建微信支付订单失败，更新本地订单状态为取消
            paymentOrder.setOrderStatus(PaymentOrder.STATUS_CANCELLED);
            paymentOrderRepository.save(paymentOrder);
            throw new RuntimeException("创建支付订单失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handlePayNotify(String xmlData) {
        log.info("收到微信支付回调: {}", xmlData);

        // 1. 处理微信支付回调
        Map<String, String> notifyData = weChatPayService.handlePayNotify(xmlData);

        // 2. 检查回调状态
        if ("FAIL".equals(notifyData.get("return_code"))) {
            log.error("微信支付回调失败: {}", notifyData.get("return_msg"));
            return weChatPayService.mapToXml(notifyData);
        }

        // 3. 更新订单状态
        String orderNo = notifyData.get("out_trade_no");
        String transactionId = notifyData.get("transaction_id");

        Optional<PaymentOrder> orderOptional = paymentOrderRepository.findByOrderNo(orderNo);
        if (!orderOptional.isPresent()) {
            log.error("订单不存在: {}", orderNo);
            notifyData.put("return_code", "FAIL");
            notifyData.put("return_msg", "订单不存在");
            return weChatPayService.mapToXml(notifyData);
        }

        PaymentOrder paymentOrder = orderOptional.get();
        if (PaymentOrder.STATUS_PAID.equals(paymentOrder.getOrderStatus())) {
            log.info("订单已处理: {}", orderNo);
            return weChatPayService.mapToXml(notifyData);
        }

        // 4. 更新订单状态为已支付
        paymentOrder.setOrderStatus(PaymentOrder.STATUS_PAID);
        paymentOrder.setTradeNo(transactionId);
        paymentOrder.setPayTime(LocalDateTime.now());
        paymentOrderRepository.save(paymentOrder);

        log.info("订单支付成功: {}, 交易号: {}", orderNo, transactionId);

        // 5. 返回成功响应
        return weChatPayService.mapToXml(notifyData);
    }

    @Override
    public PaymentOrder queryOrderStatus(String orderNo) {
        Optional<PaymentOrder> orderOptional = paymentOrderRepository.findByOrderNo(orderNo);
        if (!orderOptional.isPresent()) {
            throw new RuntimeException("订单不存在");
        }
        return orderOptional.get();
    }

    @Override
    public boolean verifyOrderPaid(String orderNo) {
        Optional<PaymentOrder> orderOptional = paymentOrderRepository.findByOrderNo(orderNo);
        return orderOptional.isPresent() && PaymentOrder.STATUS_PAID.equals(orderOptional.get().getOrderStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeOrder(String orderNo) {
        Optional<PaymentOrder> orderOptional = paymentOrderRepository.findByOrderNo(orderNo);
        if (!orderOptional.isPresent()) {
            return false;
        }

        PaymentOrder paymentOrder = orderOptional.get();
        if (PaymentOrder.STATUS_PENDING.equals(paymentOrder.getOrderStatus())) {
            paymentOrder.setOrderStatus(PaymentOrder.STATUS_CANCELLED);
            paymentOrderRepository.save(paymentOrder);
            return true;
        }

        return false;
    }

    @Override
    public Map<String, String> generateJsApiParams(String prepayId) {
        return weChatPayService.generateJsApiParams(prepayId);
    }

    /**
     * 生成唯一订单号
     */
    private String generateOrderNo() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
    }

    /**
     * 从微信支付返回的XML中提取预支付ID
     */
    private String extractPrepayId(String xmlResult) {
        if (StringUtils.isEmpty(xmlResult)) {
            return null;
        }

        Matcher matcher = PREPAY_ID_PATTERN.matcher(xmlResult);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
