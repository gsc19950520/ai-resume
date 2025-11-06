package com.aicv.airesume.controller;

import com.aicv.airesume.entity.InterviewSession;
import com.aicv.airesume.entity.InterviewLog;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 面试相关接口控制器
 */
@RestController
@RequestMapping("/api/interview")
@Slf4j
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    /**
     * 开始面试
     * @param request 请求参数
     * @return 会话信息和第一个问题
     */
    @PostMapping("/start")
    public Map<String, Object> startInterview(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            Long resumeId = Long.valueOf(request.get("resumeId").toString());
            String jobType = (String) request.get("jobType");
            String city = (String) request.get("city");
            Map<String, Object> sessionParams = (Map<String, Object>) request.get("sessionParams");

            Map<String, Object> result = interviewService.startInterview(userId, resumeId, jobType, city, sessionParams);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("Start interview failed:", e);
            return ResponseUtils.error("开始面试失败：" + e.getMessage());
        }
    }

    /**
     * 提交答案并获取下一个问题
     * @param request 请求参数
     * @return 下一个问题、评分和反馈
     */
    @PostMapping("/answer")
    public Map<String, Object> submitAnswer(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            String questionId = (String) request.get("questionId");
            String userAnswerText = (String) request.get("userAnswerText");
            String userAnswerAudioUrl = (String) request.get("userAnswerAudioUrl");

            Map<String, Object> result = interviewService.submitAnswer(sessionId, questionId, userAnswerText, userAnswerAudioUrl);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("Submit answer failed:", e);
            return ResponseUtils.error("提交答案失败：" + e.getMessage());
        }
    }

    /**
     * 完成面试并生成报告
     * @param request 请求参数
     * @return 聚合评分、薪资信息和报告URL
     */
    @PostMapping("/finish")
    public Map<String, Object> finishInterview(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");

            Map<String, Object> result = interviewService.finishInterview(sessionId);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("Finish interview failed:", e);
            return ResponseUtils.error("完成面试失败：" + e.getMessage());
        }
    }

    /**
     * 获取面试历史列表
     * @param userId 用户ID
     * @return 面试历史列表
     */
    @GetMapping("/history")
    public Map<String, Object> getInterviewHistory(@RequestParam String userId) {
        try {
            return ResponseUtils.success(interviewService.getInterviewHistory(userId));
        } catch (Exception e) {
            log.error("Get interview history failed:", e);
            return ResponseUtils.error("获取面试历史失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据面试结果计算薪资范围
     * 基于AI面试评分、技术深度等多维度计算薪资
     */
    @PostMapping("/calculate-salary")
    public Map<String, Object> calculateSalary(@RequestBody SalaryCalculateRequest request) {
        try {
            Map<String, Double> aggregatedScores = new HashMap<>();
            
            // 确保总评分存在
            if (!request.getAggregatedScores().containsKey("total")) {
                // 计算总分
                double tech = request.getAggregatedScores().getOrDefault("tech", 0.0);
                double logic = request.getAggregatedScores().getOrDefault("logic", 0.0);
                double clarity = request.getAggregatedScores().getOrDefault("clarity", 0.0);
                double depth = request.getAggregatedScores().getOrDefault("depth", 0.0);
                double total = (tech + logic + clarity + depth) / 4;
                aggregatedScores.put("total", total);
            } else {
                aggregatedScores.putAll(request.getAggregatedScores());
            }
            
            // 调用薪资计算方法
            Map<String, Object> salaryInfo = interviewService.calculateSalary(
                request.getCity(), 
                request.getJobType(), 
                aggregatedScores
            );
            
            return ResponseUtils.success(salaryInfo);
        } catch (Exception e) {
            log.error("Calculate salary failed:", e);
            return ResponseUtils.error("薪资计算失败：" + e.getMessage());
        }
    }
    
    // 薪资计算请求参数类
    public static class SalaryCalculateRequest {
        private String city;
        private String jobType;
        private Map<String, Double> aggregatedScores;
        
        // Getters and Setters
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getJobType() {
            return jobType;
        }
        
        public void setJobType(String jobType) {
            this.jobType = jobType;
        }
        
        public Map<String, Double> getAggregatedScores() {
            return aggregatedScores;
        }
        
        public void setAggregatedScores(Map<String, Double> aggregatedScores) {
            this.aggregatedScores = aggregatedScores;
        }
    }

    /**
     * 获取面试详情
     * @param sessionId 会话ID
     * @return 面试详情信息
     */
    @GetMapping("/detail/{sessionId}")
    public Map<String, Object> getInterviewDetail(@PathVariable String sessionId) {
        try {
            return ResponseUtils.success(interviewService.getInterviewDetail(sessionId));
        } catch (Exception e) {
            log.error("Get interview detail failed:", e);
            return ResponseUtils.error("获取面试详情失败：" + e.getMessage());
        }
    }

}