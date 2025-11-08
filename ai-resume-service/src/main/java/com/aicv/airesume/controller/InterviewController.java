package com.aicv.airesume.controller;

import com.aicv.airesume.model.dto.InterviewResponseDTO;
import com.aicv.airesume.model.dto.InterviewReportDTO;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.service.config.DynamicConfigService;
import com.aicv.airesume.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.aicv.airesume.entity.AiTraceLog;
import com.aicv.airesume.repository.AiTraceLogRepository;
import java.util.List;
import java.util.HashMap;
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
    
    @Autowired
    private AiTraceLogRepository aiTraceLogRepository;
    
    @Autowired
    private DynamicConfigService dynamicConfigService;

    /**
     * 获取面试配置
     * @return 面试相关配置信息
     */
    @GetMapping("/get-config")
    public Map<String, Object> getInterviewConfig() {
        try {
            // 获取面试相关配置
            Map<String, Object> config = new HashMap<>();
            
            // 获取面试官风格配置（只返回启用的）
            dynamicConfigService.getInterviewPersonas().ifPresent(personas -> {
                config.put("personas", personas);
            });
            
            // 获取深度级别配置
            dynamicConfigService.getInterviewDepthLevels().ifPresent(depthLevels -> {
                config.put("depthLevels", depthLevels);
            });
            
            // 获取默认会话时长
            config.put("defaultSessionSeconds", dynamicConfigService.getDefaultSessionSeconds());
            
            // 获取默认面试官风格
            config.put("defaultPersona", dynamicConfigService.getDefaultPersona());
            
            return ResponseUtils.success(config);
        } catch (Exception e) {
            log.error("获取面试配置失败", e);
            return ResponseUtils.error("获取面试配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有面试官风格配置（包括禁用的），用于管理功能
     * @return 所有面试官风格配置
     */
    @GetMapping("/admin/get-all-personas")
    public Map<String, Object> getAllInterviewPersonas() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 获取所有面试官风格配置（包括禁用的）
            dynamicConfigService.getAllInterviewPersonas().ifPresent(personas -> {
                result.put("personas", personas);
            });
            
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("获取所有面试官风格配置失败", e);
            return ResponseUtils.error("获取所有面试官风格配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 开始面试 - 支持动态配置
     * @param request 请求参数
     * @return 会话信息和第一个问题
     */
    @PostMapping("/start")
    public Map<String, Object> startInterview(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long resumeId = Long.valueOf(request.get("resumeId").toString());
            
            // 从动态配置获取默认值
            String defaultPersona = dynamicConfigService.getDefaultPersona();
            Integer defaultSessionSeconds = dynamicConfigService.getDefaultSessionSeconds();
            
            // 使用请求参数或默认配置
            String persona = (String) request.getOrDefault("persona", defaultPersona);
            String toneStyle = (String) request.getOrDefault("toneStyle", "neutral"); // 添加语气风格参数
            Integer sessionSeconds = request.get("sessionSeconds") != null ? 
                Integer.valueOf(request.get("sessionSeconds").toString()) : defaultSessionSeconds;

            // 根据语气风格增强面试官角色描述
            String enhancedPersona = enhancePersonaWithTone(persona, toneStyle);
            InterviewResponseDTO result = interviewService.startInterview(userId, resumeId, enhancedPersona, sessionSeconds);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("Start interview failed:", e);
            return ResponseUtils.error("开始面试失败：" + e.getMessage());
        }
    }
    /**
     * 根据语气风格增强面试官角色描述
     */
    private String enhancePersonaWithTone(String basePersona, String toneStyle) {
        switch (toneStyle) {
            case "friendly":
                return basePersona + "，语气友好亲切，鼓励候选人表达。";
            case "challenging":
                return basePersona + "，语气具有挑战性，会深入追问技术细节。";
            case "analytical":
                return basePersona + "，语气分析性强，注重逻辑推理过程。";
            case "encouraging":
                return basePersona + "，语气鼓励性强，给予正面反馈。";
            case "formal":
                return basePersona + "，语气正式专业，严格遵循技术评估标准。";
            case "neutral":
            default:
                return basePersona + "，语气客观中立，关注事实和技术能力。";
        }
    }
    
    /**
     * 获取AI运行日志（调试模式）
     */
    @GetMapping("/trace-logs/{sessionId}")
    public Map<String, Object> getTraceLogs(@PathVariable String sessionId) {
        try {
            List<AiTraceLog> logs = aiTraceLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
            return ResponseUtils.success(logs);
        } catch (Exception e) {
            log.error("获取AI跟踪日志失败", e);
            return ResponseUtils.error("获取AI跟踪日志失败：" + e.getMessage());
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
            String userAnswerText = (String) request.get("userAnswerText");
            Integer answerDuration = Integer.valueOf(request.get("answerDuration").toString());
            // 支持动态更新面试官风格（可选参数）
            String toneStyle = (String) request.getOrDefault("toneStyle", null);

            InterviewResponseDTO result = interviewService.submitAnswer(sessionId, userAnswerText, answerDuration);
            // 如果需要根据toneStyle调整，这里可以扩展逻辑
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("Submit answer failed:", e);
            return ResponseUtils.error("提交答案失败：" + e.getMessage());
        }
    }

    /**
     * 完成面试并生成报告
     * @param request 请求参数
     * @return 面试报告信息
     */
    @PostMapping("/finish")
    public Map<String, Object> finishInterview(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");

            InterviewReportDTO result = interviewService.finishInterview(sessionId);
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
    public Map<String, Object> getInterviewHistory(@RequestParam Long userId) {
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
    public Map<String, Object> calculateSalary(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            return ResponseUtils.success(interviewService.calculateSalary(sessionId));
        } catch (Exception e) {
            log.error("Calculate salary failed:", e);
            return ResponseUtils.error("薪资计算失败：" + e.getMessage());
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
    
    /**
     * 生成第一个面试问题
     * @param request 请求参数（包含resumeId、personaId、industryJobTag）
     * @return 生成的问题
     */
    @PostMapping("/generate-first-question")
    public Map<String, Object> generateFirstQuestion(@RequestBody Map<String, Object> request) {
        try {
            Long resumeId = Long.valueOf(request.get("resumeId").toString());
            String personaId = (String) request.get("personaId");
            String industryJobTag = (String) request.getOrDefault("industryJobTag", "");
            
            Map<String, Object> result = interviewService.generateFirstQuestion(resumeId, personaId, industryJobTag);
            return ResponseUtils.success(result);
        } catch (Exception e) {
            log.error("Generate first question failed:", e);
            return ResponseUtils.error("生成问题失败：" + e.getMessage());
        }
    }

}