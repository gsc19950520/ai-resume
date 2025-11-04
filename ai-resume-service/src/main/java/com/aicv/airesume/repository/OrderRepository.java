package com.aicv.airesume.repository;

import com.aicv.airesume.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 订单数据访问接口
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * 根据订单号查询订单
     */
    Optional<Order> findByOrderNo(String orderNo);
    
    /**
     * 根据用户ID查询订单列表
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据用户ID和状态查询订单列表
     */
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status);
    
    /**
     * 根据微信支付订单号查询订单
     */
    Optional<Order> findByTransactionId(String transactionId);
}
