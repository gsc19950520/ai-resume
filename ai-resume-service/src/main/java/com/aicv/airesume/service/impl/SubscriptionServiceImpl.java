package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Order;
import com.aicv.airesume.model.dto.PayNotifyDTO;
import com.aicv.airesume.repository.OrderRepository;
import com.aicv.airesume.service.SubscriptionService;
import com.aicv.airesume.service.UserService;
import com.aicv.airesume.utils.WechatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 订阅服务实现类
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WechatUtils wechatUtils;

    @Override
    public Order createOrder(Long userId, String productId, String productType, Integer amount) {
        return null;
    }

    @Override
    public Map<String, Object> payOrder(String orderNo, Long userId) {
        return new HashMap<>();
    }

    @Transactional
    @Override
    public void handlePayCallback(Map<String, String> notifyData) {
    }

    @Override
    public String payNotify(PayNotifyDTO payNotifyDTO) {
        try {
            // 将DTO转换为Map供现有方法使用
            Map<String, String> notifyData = convertPayNotifyDTOToMap(payNotifyDTO);
            handlePayCallback(notifyData);
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        } catch (Exception e) {
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[" + e.getMessage() + "]]></return_msg></xml>";
        }
    }

    /**
     * 将PayNotifyDTO转换为Map
     */
    private Map<String, String> convertPayNotifyDTOToMap(PayNotifyDTO dto) {
        Map<String, String> map = new HashMap<>();
        if (dto.getReturnCode() != null) map.put("return_code", dto.getReturnCode());
        if (dto.getAppid() != null) map.put("appid", dto.getAppid());
        if (dto.getMchId() != null) map.put("mch_id", dto.getMchId());
        if (dto.getNonceStr() != null) map.put("nonce_str", dto.getNonceStr());
        if (dto.getSign() != null) map.put("sign", dto.getSign());
        if (dto.getResultCode() != null) map.put("result_code", dto.getResultCode());
        if (dto.getOpenid() != null) map.put("openid", dto.getOpenid());
        if (dto.getIsSubscribe() != null) map.put("is_subscribe", dto.getIsSubscribe());
        if (dto.getTradeType() != null) map.put("trade_type", dto.getTradeType());
        if (dto.getBankType() != null) map.put("bank_type", dto.getBankType());
        if (dto.getTotalFee() != null) map.put("total_fee", dto.getTotalFee().toString());
        if (dto.getFeeType() != null) map.put("fee_type", dto.getFeeType());
        if (dto.getTransactionId() != null) map.put("transaction_id", dto.getTransactionId());
        if (dto.getOutTradeNo() != null) map.put("out_trade_no", dto.getOutTradeNo());
        if (dto.getTimeEnd() != null) map.put("time_end", dto.getTimeEnd());
        return map;
    }

    @Override
    public List<Order> getOrderList(Long userId) {
        return new ArrayList<>();
    }

    @Override
    public Order getOrderByNo(String orderNo) {
        return null;
    }

    @Override
    public Map<String, Object> buyMembership(Long userId, Integer days) {
        String orderNo = generateOrderNo();
        Integer amount = calculateMembershipPrice(days);
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        result.put("payParams", new HashMap<>());
        return result;
    }

    @Override
    public Map<String, Object> buyOptimizePackage(Long userId, Integer count) {
        String orderNo = generateOrderNo();
        Integer amount = calculateOptimizePrice(count);
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        result.put("payParams", new HashMap<>());
        return result;
    }

    @Override
    public Map<String, Object> buyTemplatePackage(Long userId, Long templateId) {
        String orderNo = generateOrderNo();
        Integer amount = 299; // 模板包价格299元
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        result.put("payParams", new HashMap<>());
        return result;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 6);
        return timestamp + random;
    }

    /**
     * 获取产品名称
     */
    private String getProductName(String productType, String productId) {
        return "AI简历优化服务";
    }

    /**
     * 处理产品交付
     */
    private void handleProductDelivery(Order order) {
    }

    /**
     * 计算会员价格
     */
    private Integer calculateMembershipPrice(Integer days) {
        if (days == 30) {
            return 199; // 月卡199元
        } else if (days == 90) {
            return 499; // 季卡499元
        } else if (days == 365) {
            return 1299; // 年卡1299元
        }
        return 199;
    }

    /**
     * 计算优化次数价格
     */
    private Integer calculateOptimizePrice(Integer count) {
        if (count == 10) {
            return 99; // 10次99元
        } else if (count == 30) {
            return 249; // 30次249元
        } else if (count == 100) {
            return 699; // 100次699元
        }
        return 99;
    }
}