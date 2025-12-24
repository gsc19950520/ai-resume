package com.aicv.airesume.service;

import com.aicv.airesume.entity.PaymentOrder;
import com.aicv.airesume.model.vo.PayResultVO;
import com.aicv.airesume.model.dto.CreatePayOrderDTO;
import java.util.Map;

/**
 * 支付服务接口
 */
public interface PayService {

    /**
     * 创建支付订单
     * @param createPayOrderDTO 创建支付订单参数
     * @param ipAddress 客户端IP地址
     * @return 支付结果VO
     */
    PayResultVO createPayOrder(CreatePayOrderDTO createPayOrderDTO, String ipAddress);

    /**
     * 处理微信支付回调
     * @param xmlData 微信支付回调的XML数据
     * @return 处理结果
     */
    String handlePayNotify(String xmlData);

    /**
     * 查询订单状态
     * @param orderNo 订单编号
     * @return 支付订单信息
     */
    PaymentOrder queryOrderStatus(String orderNo);

    /**
     * 验证订单是否已支付
     * @param orderNo 订单编号
     * @return 是否已支付
     */
    boolean verifyOrderPaid(String orderNo);

    /**
     * 关闭订单
     * @param orderNo 订单编号
     * @return 是否关闭成功
     */
    boolean closeOrder(String orderNo);

    /**
     * 生成微信支付JSAPI参数
     * @param prepayId 预支付ID
     * @return JSAPI参数
     */
    Map<String, String> generateJsApiParams(String prepayId);
}
