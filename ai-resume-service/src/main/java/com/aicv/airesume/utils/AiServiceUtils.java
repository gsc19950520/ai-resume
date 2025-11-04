package com.aicv.airesume.utils;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AI服务工具类
 */
@Component
public class AiServiceUtils {

    @Value("${openai.api-key}")
    private String openaiApiKey;

    @Value("${tongyi.api-key}")
    private String tongyiApiKey;

    @Value("${wenxin.api-key}")
    private String wenxinApiKey;

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
}