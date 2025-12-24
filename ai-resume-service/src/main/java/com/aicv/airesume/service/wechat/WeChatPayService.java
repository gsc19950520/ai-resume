package com.aicv.airesume.service.wechat;

import com.aicv.airesume.config.WeChatPayConfig;
import com.aicv.airesume.entity.PaymentOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 微信支付API服务
 */
@Service
public class WeChatPayService {
    
    private static final Logger logger = LoggerFactory.getLogger(WeChatPayService.class);
    
    @Autowired
    private WeChatPayConfig weChatPayConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 生成微信支付签名
     * @param params 参数Map
     * @param apiKey API密钥
     * @return 签名字符串
     */
    public String generateSign(Map<String, String> params, String apiKey) {
        // 1. 对参数按照key的ASCII码进行升序排序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        
        // 2. 拼接字符串
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String value = params.get(key);
            if (StringUtils.hasLength(key) && StringUtils.hasLength(value) && !"sign".equals(key) && !"key".equals(key)) {
                sb.append(key).append("=").append(value).append("&");
            }
        }
        
        // 3. 添加API密钥并转为大写
        sb.append("key=").append(apiKey);
        String sign = sb.toString();
        
        try {
            // 4. 使用MD5加密
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(sign.getBytes());
            StringBuilder signSb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    signSb.append("0");
                }
                signSb.append(hex);
            }
            return signSb.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            logger.error("MD5加密失败", e);
            throw new RuntimeException("签名生成失败", e);
        }
    }
    
    /**
     * 生成微信支付预支付订单
     * @param paymentOrder 支付订单
     * @param ipAddress 客户端IP地址
     * @return 微信支付返回的XML数据
     */
    public String createPrepayOrder(PaymentOrder paymentOrder, String ipAddress) {
        try {
            // 1. 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mch_id", weChatPayConfig.getMchId());
            params.put("nonce_str", generateNonceStr());
            params.put("body", paymentOrder.getBody());
            params.put("out_trade_no", paymentOrder.getOrderNo());
            params.put("total_fee", String.valueOf(paymentOrder.getTotalFee()));
            params.put("spbill_create_ip", ipAddress);
            params.put("notify_url", weChatPayConfig.getNotifyUrl());
            params.put("trade_type", "JSAPI");
            params.put("openid", paymentOrder.getOpenId());
            
            // 2. 生成签名
            String sign = generateSign(params, weChatPayConfig.getApiKey());
            params.put("sign", sign);
            
            // 3. 转换为XML格式
            String xmlData = mapToXml(params);
            
            // 4. 调用微信支付统一下单接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            HttpEntity<String> entity = new HttpEntity<>(xmlData, headers);
            
            logger.info("调用微信支付统一下单接口，参数: {}", xmlData);
            ResponseEntity<String> response = restTemplate.postForEntity(WeChatPayConfig.UNIFIED_ORDER_URL, entity, String.class);
            logger.info("微信支付统一下单接口返回: {}", response.getBody());
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("创建预支付订单失败", e);
            throw new RuntimeException("创建预支付订单失败", e);
        }
    }
    
    /**
     * 处理微信支付回调
     * @param xmlData 微信支付回调的XML数据
     * @return 处理结果
     */
    public Map<String, String> handlePayNotify(String xmlData) {
        try {
            // 1. 解析XML数据
            Map<String, String> notifyData = xmlToMap(xmlData);
            
            // 2. 验证签名
            String sign = notifyData.get("sign");
            Map<String, String> params = new HashMap<>(notifyData);
            params.remove("sign");
            String verifySign = generateSign(params, weChatPayConfig.getApiKey());
            
            Map<String, String> result = new HashMap<>();
            result.put("return_code", "SUCCESS");
            result.put("return_msg", "OK");
            
            if (!sign.equals(verifySign)) {
                logger.error("微信支付签名验证失败");
                result.put("return_code", "FAIL");
                result.put("return_msg", "签名失败");
                return result;
            }
            
            // 3. 验证返回状态
            if (!"SUCCESS".equals(notifyData.get("return_code"))) {
                logger.error("微信支付失败: {}", notifyData.get("return_msg"));
                result.put("return_code", "FAIL");
                result.put("return_msg", notifyData.get("return_msg"));
                return result;
            }
            
            // 4. 验证业务结果
            if (!"SUCCESS".equals(notifyData.get("result_code"))) {
                logger.error("微信支付业务失败: {}", notifyData.get("err_code_des"));
                result.put("return_code", "FAIL");
                result.put("return_msg", notifyData.get("err_code_des"));
                return result;
            }
            
            return notifyData;
        } catch (Exception e) {
            logger.error("处理微信支付回调失败", e);
            Map<String, String> result = new HashMap<>();
            result.put("return_code", "FAIL");
            result.put("return_msg", "系统错误");
            return result;
        }
    }
    
    /**
     * 查询微信支付订单状态
     * @param orderNo 订单编号
     * @return 微信支付返回的XML数据
     */
    public String queryOrder(String orderNo) {
        try {
            // 1. 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("appid", weChatPayConfig.getAppId());
            params.put("mch_id", weChatPayConfig.getMchId());
            params.put("out_trade_no", orderNo);
            params.put("nonce_str", generateNonceStr());
            
            // 2. 生成签名
            String sign = generateSign(params, weChatPayConfig.getApiKey());
            params.put("sign", sign);
            
            // 3. 转换为XML格式
            String xmlData = mapToXml(params);
            
            // 4. 调用微信支付查询订单接口
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            HttpEntity<String> entity = new HttpEntity<>(xmlData, headers);
            
            logger.info("调用微信支付查询订单接口，参数: {}", xmlData);
            ResponseEntity<String> response = restTemplate.postForEntity(WeChatPayConfig.QUERY_ORDER_URL, entity, String.class);
            logger.info("微信支付查询订单接口返回: {}", response.getBody());
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("查询订单失败", e);
            throw new RuntimeException("查询订单失败", e);
        }
    }
    
    /**
     * 生成微信支付JSAPI需要的参数
     * @param prepayId 预支付ID
     * @return JSAPI参数Map
     */
    public Map<String, String> generateJsApiParams(String prepayId) {
        Map<String, String> params = new HashMap<>();
        params.put("appId", weChatPayConfig.getAppId());
        params.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("nonceStr", generateNonceStr());
        params.put("package", "prepay_id=" + prepayId);
        params.put("signType", "MD5");
        
        // 生成签名
        String sign = generateSign(params, weChatPayConfig.getApiKey());
        params.put("paySign", sign);
        
        return params;
    }
    
    /**
     * 生成随机字符串
     * @return 随机字符串
     */
    private String generateNonceStr() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
    
    /**
     * 将Map转换为XML格式
     * @param map 参数Map
     * @return XML格式字符串
     */
    public String mapToXml(Map<String, String> map) {
        StringBuilder xml = new StringBuilder();
        xml.append("<xml>");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            xml.append("<").append(entry.getKey()).append(">");
            xml.append(entry.getValue());
            xml.append("</").append(entry.getKey()).append(">");
        }
        xml.append("</xml>");
        return xml.toString();
    }
    
    /**
     * 将XML转换为Map
     * @param xml XML格式字符串
     * @return 参数Map
     */
    private Map<String, String> xmlToMap(String xml) {
        // 简单的XML解析，实际项目中可以使用更可靠的XML解析库
        Map<String, String> map = new HashMap<>();
        int start = 0;
        int end = 0;
        
        while ((start = xml.indexOf("<", start)) != -1 && (end = xml.indexOf(">", start)) != -1) {
            String tag = xml.substring(start + 1, end);
            if (tag.startsWith("/")) {
                start = end + 1;
                continue;
            }
            
            int valueStart = end + 1;
            int valueEnd = xml.indexOf("</" + tag + ">", valueStart);
            if (valueEnd == -1) {
                start = end + 1;
                continue;
            }
            
            String value = xml.substring(valueStart, valueEnd);
            map.put(tag, value);
            start = valueEnd + tag.length() + 3;
        }
        
        return map;
    }
}
