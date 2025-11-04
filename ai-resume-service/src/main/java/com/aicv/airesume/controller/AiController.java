package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * AI服务控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * AI优化简历
     * @param request 请求参数，包含简历内容
     * @return 优化结果
     */
    @PostMapping("/optimize")
    public Map<String, String> optimizeResume(@RequestBody Map<String, String> request) {
        String resumeContent = request.get("resumeContent");
        String jobDescription = request.get("jobDescription");
        String optimizedContent = aiService.optimizeResume(resumeContent, jobDescription);
        Map<String, String> result = new java.util.HashMap<>();
        result.put("optimizedContent", optimizedContent);
        return result;
    }

    /**
     * 获取简历评分
     * @param request 请求参数，包含简历内容
     * @return 评分结果
     */
    @PostMapping("/score")
    public Map<String, Object> getResumeScore(@RequestBody Map<String, String> request) {
        String resumeContent = request.get("resumeContent");
        Integer score = aiService.getResumeScore(resumeContent);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("score", score);
        return result;
    }

    /**
     * 获取优化建议
     * @param request 请求参数，包含简历内容
     * @return 优化建议
     */
    @PostMapping("/suggestions")
    public Map<String, String> getResumeSuggestions(@RequestBody Map<String, String> request) {
        String resumeContent = request.get("resumeContent");
        String suggestions = aiService.getResumeSuggestions(resumeContent);
        Map<String, String> result = new java.util.HashMap<>();
        result.put("suggestions", suggestions);
        return result;
    }

    /**
     * AI优化简历文本
     * @param request 请求参数，包含原始文本
     * @return 优化后的文本
     */
    @PostMapping("/optimize-text")
    public Map<String, String> optimizeText(@RequestBody Map<String, String> request) {
        // 使用optimizeResume方法代替不存在的optimizeText
        String originalText = request.get("text");
        String optimizedText = aiService.optimizeResume(originalText, null);
        Map<String, String> result = new java.util.HashMap<>();
        result.put("original", originalText);
        result.put("optimized", optimizedText);
        return result;
    }

    /**
     * AI生成简历内容
     * @param request 请求参数，包含职位、技能等信息
     * @return 生成的内容
     */
    @PostMapping("/generate-content")
    public Map<String, String> generateContent(@RequestBody Map<String, Object> request) {
        // 临时返回空结果，因为AiService接口中没有定义这个方法
        Map<String, String> result = new java.util.HashMap<>();
        result.put("generatedContent", "");
        return result;
    }
}