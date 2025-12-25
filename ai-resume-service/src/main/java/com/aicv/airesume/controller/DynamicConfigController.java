package com.aicv.airesume.controller;

import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.service.config.DynamicConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态配置相关接口控制器
 */
@RestController
@RequestMapping("/api/config")
@Slf4j
public class DynamicConfigController {

    @Autowired
    private DynamicConfigService dynamicConfigService;

    /**
     * 获取支付配置
     * @return 支付相关配置信息，包含是否需要支付和支付金额
     */
    @GetMapping("/payment")
    public BaseResponseVO getPaymentConfig() {
        try {
            // 从动态配置中获取支付相关配置
            boolean needPayment = dynamicConfigService.getConfigValue("payment", "need_payment")
                    .map(Boolean::parseBoolean)
                    .orElse(false);
            
            // 从动态配置中获取支付金额（单位：分）
            int amount = dynamicConfigService.getConfigValue("payment", "amount")
                    .map(Integer::parseInt)
                    .orElse(1); // 默认1分（0.01元）
            
            // 构建包含支付配置的Map对象
            Map<String, Object> paymentConfig = new HashMap<>();
            paymentConfig.put("needPayment", needPayment);
            paymentConfig.put("amount", amount);
            
            return BaseResponseVO.success(paymentConfig);
        } catch (Exception e) {
            log.error("获取支付配置失败", e);
            return BaseResponseVO.error("获取支付配置失败：" + e.getMessage());
        }
    }
}
