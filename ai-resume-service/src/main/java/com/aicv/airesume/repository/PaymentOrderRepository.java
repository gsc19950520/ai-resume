package com.aicv.airesume.repository;

import com.aicv.airesume.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 支付订单数据访问接口
 */
@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    
    /**
     * 根据订单编号查询订单
     * @param orderNo 订单编号
     * @return 支付订单对象
     */
    Optional<PaymentOrder> findByOrderNo(String orderNo);
    
    /**
     * 根据用户ID和订单状态查询订单列表
     * @param userId 用户ID
     * @param orderStatus 订单状态
     * @return 支付订单列表
     */
    List<PaymentOrder> findByUserIdAndOrderStatus(Long userId, String orderStatus);
    
    /**
     * 根据微信交易号查询订单
     * @param tradeNo 微信交易号
     * @return 支付订单对象
     */
    Optional<PaymentOrder> findByTradeNo(String tradeNo);
    
    /**
     * 根据openId和订单状态查询订单列表
     * @param openId 微信openId
     * @param orderStatus 订单状态
     * @return 支付订单列表
     */
    List<PaymentOrder> findByOpenIdAndOrderStatus(String openId, String orderStatus);
}
