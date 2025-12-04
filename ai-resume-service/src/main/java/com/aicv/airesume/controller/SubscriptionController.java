package com.aicv.airesume.controller;

import com.aicv.airesume.model.dto.BuyMembershipDTO;
import com.aicv.airesume.model.dto.BuyOptimizePackageDTO;
import com.aicv.airesume.model.dto.BuyTemplatePackageDTO;
import com.aicv.airesume.model.dto.PayNotifyDTO;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.service.SubscriptionService;
import com.aicv.airesume.utils.GlobalContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.aicv.airesume.common.constant.ResponseCode;
import com.aicv.airesume.common.exception.BusinessException;

import java.util.Map;

/**
 * 订阅控制器
 */
@RestController
@RequestMapping("/api/subscribe")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;



    /**
     * 购买会员
     */
    @PostMapping("/buy/membership")
    public BaseResponseVO buyMembership(
            @RequestBody BuyMembershipDTO buyMembershipDTO) {
        
        Long userId = GlobalContextUtil.getUserId();
        
        Integer days = buyMembershipDTO.getDays();
        if (days == null) {
            throw new RuntimeException("天数不能为空");
        }
        
        Object result = subscriptionService.buyMembership(userId, days);
        return BaseResponseVO.success(result);
    }

    /**
     * 购买优化次数包
     */
    @PostMapping("/buy/optimize-package")
    public BaseResponseVO buyOptimizePackage(
            @RequestBody BuyOptimizePackageDTO buyOptimizePackageDTO) {
        
        Long userId = GlobalContextUtil.getUserId();
        
        Integer count = buyOptimizePackageDTO.getCount();
        if (count == null) {
            throw new RuntimeException("数量不能为空");
        }
        
        Object result = subscriptionService.buyOptimizePackage(userId, count);
        return BaseResponseVO.success(result);
    }

    /**
     * 购买模板包
     */
    @PostMapping("/buy/template-package")
    public BaseResponseVO buyTemplatePackage(
            @RequestBody BuyTemplatePackageDTO buyTemplatePackageDTO) {
        
        Long userId = GlobalContextUtil.getUserId();
        
        Long templateId = buyTemplatePackageDTO.getTemplateId();
        if (templateId == null) {
            throw new RuntimeException("模板ID不能为空");
        }
        
        Object result = subscriptionService.buyTemplatePackage(userId, templateId);
        return BaseResponseVO.success(result);
    }

    /**
     * 获取订单列表
     */
    @GetMapping("/orders")
    public BaseResponseVO getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = GlobalContextUtil.getUserId();
        
        Object result = subscriptionService.getOrders(userId, page, size);
        return BaseResponseVO.success(result);
    }

    /**
     * 支付回调
     */
    @PostMapping("/pay/notify")
    public String payNotify(@RequestBody PayNotifyDTO payNotifyDTO) {
        return subscriptionService.payNotify(payNotifyDTO);
    }
}