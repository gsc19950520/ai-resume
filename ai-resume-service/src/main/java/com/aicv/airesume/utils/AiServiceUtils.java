package com.aicv.airesume.utils;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

/**
 * AI服务工具类
 */
@Component
public class AiServiceUtils {

    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 使用OpenAI优化简历
     */
    public String optimizeResumeWithOpenAI(String originalContent, String jobType) {
        // 实现OpenAI API调用逻辑
        // 这里简化处理，实际项目中需要按照OpenAI API文档规范实现
        return "优化后的简历内容 (OpenAI)";
    }

    /**
     * 使用通义千问优化简历
     */
    public String optimizeResumeWithTongyi(String originalContent, String jobType) {
        // 实现通义千问API调用逻辑
        return "优化后的简历内容 (通义千问)";
    }

    /**
     * 使用文心一言优化简历
     */
    public String optimizeResumeWithWenxin(String originalContent, String jobType) {
        // 实现文心一言API调用逻辑
        return "优化后的简历内容 (文心一言)";
    }

    /**
     * 获取AI评分
     */
    public Integer getAiScore(String resumeContent, String jobType) {
        // 实现评分逻辑
        // 这里返回模拟分数，实际项目中需要通过AI评估
        return 85 + (int)(Math.random() * 10);
    }

    /**
     * 获取AI建议
     */
    public String getAiSuggestion(String resumeContent, String jobType) {
        // 实现建议生成逻辑
        Map<String, String> suggestions = new HashMap<>();
        suggestions.put("技术岗", "建议突出您的技术栈和项目经验，使用具体的数据成果来展示您的能力。");
        suggestions.put("市场岗", "建议强调您的市场分析能力和营销活动经验，突出您对市场趋势的把握。");
        suggestions.put("设计岗", "建议添加作品集链接，详细描述设计思路和使用的设计工具。");
        suggestions.put("销售岗", "建议量化您的销售业绩，使用具体的数字和百分比来展示您的成就。");
        suggestions.put("管理岗", "建议突出您的团队管理经验和项目交付能力，强调领导力和决策能力。");
        
        return suggestions.getOrDefault(jobType, "您的简历整体不错，建议进一步完善个人技能和项目经验的描述。");
    }
    
    /**
     * 计算文本的语义哈希
     * @param text 需要计算哈希的文本
     * @return 语义哈希值（MD5格式）
     */
    public String getSemanticHash(String text) {
        try {
            // 实际项目中，这里应该调用AI embedding接口获取语义向量
            // 然后对向量进行哈希计算
            // 这里使用简单的MD5实现作为模拟
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            // 如果计算失败，返回文本本身的哈希作为备选
            return text.hashCode() + "_fallback";
        }
    }

    /**
     * 调用DeepSeek API
     * @param prompt 提示词
     * @return API响应内容
     */
    public String callDeepSeekApi(String prompt) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepseekApiKey);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat"); // 使用DeepSeek聊天模型
            
            // 构建消息数组
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            
            requestBody.put("messages", new Object[]{message});
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<JSONObject> response = restTemplate.exchange(
                    deepseekApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    JSONObject.class
            );

            // 解析响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject body = response.getBody();
                if (body.containsKey("choices") && !body.getJSONArray("choices").isEmpty()) {
                    JSONObject choice = body.getJSONArray("choices").getJSONObject(0);
                    if (choice.containsKey("message")) {
                        return choice.getJSONObject("message").getString("content");
                    }
                }
            }

            // 如果响应不符合预期，返回空字符串
            return "";
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("Error calling DeepSeek API: " + e.getMessage());
            // 返回空字符串，让调用方处理错误情况
            return "";
        }
    }
}