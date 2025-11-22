package com.aicv.airesume.controller;

import com.aicv.airesume.model.dto.InterviewResponseDTO;
import com.aicv.airesume.model.dto.InterviewReportDTO;
import com.aicv.airesume.model.dto.ResumeAnalysisDTO;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.service.ResumeAnalysisService;
import com.aicv.airesume.service.config.DynamicConfigService;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.model.vo.InterviewConfigVO;
import com.aicv.airesume.model.vo.InterviewPersonasVO;
import com.aicv.airesume.model.vo.InterviewStartVO;
import com.aicv.airesume.model.dto.InterviewStartRequestDTO;
import com.aicv.airesume.model.vo.InterviewAnswerVO;
import com.aicv.airesume.model.vo.InterviewReportVO;
import com.aicv.airesume.model.vo.InterviewHistoryListVO;
import com.aicv.airesume.model.vo.FirstQuestionVO;
import com.aicv.airesume.model.vo.AiTraceLogVO;
import com.aicv.airesume.model.vo.InterviewHistoryVO;
import com.aicv.airesume.model.vo.InterviewSessionVO;
import com.aicv.airesume.model.vo.SalaryRangeVO;
import com.aicv.airesume.model.vo.InterviewSalaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.aicv.airesume.entity.AiTraceLog;
import com.aicv.airesume.repository.AiTraceLogRepository;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    /**
     * 获取面试配置
     * @return 面试相关配置信息
     */
    @GetMapping("/get-config")
    public BaseResponseVO getInterviewConfig() {
        try {
            // 获取面试相关配置
            InterviewConfigVO config = new InterviewConfigVO();
            
            // 获取面试官风格配置（只返回启用的）
            dynamicConfigService.getInterviewPersonas().ifPresent(config::setPersonas);
            
            // 获取深度级别配置
            dynamicConfigService.getInterviewDepthLevels().ifPresent(config::setDepthLevels);
            
            // 获取默认会话时长
            config.setDefaultSessionSeconds(dynamicConfigService.getDefaultSessionSeconds());
            
            // 获取默认面试官风格
            config.setDefaultPersona(dynamicConfigService.getDefaultPersona());
            
            return BaseResponseVO.success(config);
        } catch (Exception e) {
            log.error("获取面试配置失败", e);
            return BaseResponseVO.error("获取面试配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有面试官风格配置（包括禁用的），用于管理功能
     * @return 所有面试官风格配置
     */
    @GetMapping("/admin/get-all-personas")
    public BaseResponseVO getAllInterviewPersonas() {
        try {
            // 获取所有面试官风格配置（包括禁用的）
            InterviewPersonasVO result = new InterviewPersonasVO();
            dynamicConfigService.getAllInterviewPersonas().ifPresent(result::setPersonas);
            
            return BaseResponseVO.success(result);
        } catch (Exception e) {
            log.error("获取所有面试官风格配置失败", e);
            return BaseResponseVO.error("获取所有面试官风格配置失败：" + e.getMessage());
        }
    }
    

    @PostMapping("/start")
    public BaseResponseVO startInterview(@RequestBody InterviewStartRequestDTO request) {
        try {
            // 从DTO中获取参数
            Long userId = request.getUserId();
            Long resumeId = request.getResumeId();
            String persona = request.getPersona();
            
            // 从动态配置获取默认值（仅用于会话时长）
            Integer defaultSessionSeconds = dynamicConfigService.getDefaultSessionSeconds();
            
            // 使用请求参数或默认配置
            Integer sessionSeconds = request.getSessionSeconds() != null ? 
                request.getSessionSeconds() : defaultSessionSeconds;
            
            // 创建综合响应对象
            Map<String, Object> responseData = new HashMap<>();
            
            // 调用服务层开始面试
            Integer jobTypeId = request.getJobTypeId();
            log.info("接收到的jobTypeId: {}", jobTypeId);
            InterviewResponseDTO result = interviewService.startInterview(userId, resumeId, persona, sessionSeconds, jobTypeId);
            
            // 转换为VO对象并添加到响应中
            InterviewStartVO vo = new InterviewStartVO();
            vo.setSessionId(result.getSessionId());
            vo.setQuestion(result.getQuestion());
            vo.setQuestionType(result.getQuestionType());
            vo.setPersona(persona);
            vo.setSessionSeconds(sessionSeconds);
            
            responseData.put("interviewInfo", vo);
            
            return BaseResponseVO.success(responseData);
        } catch (Exception e) {
            log.error("Start interview failed:", e);
            return BaseResponseVO.error("开始面试失败：" + e.getMessage());
        }
    }

    
    /**
     * 分析简历并生成结构化面试问题清单
     * @param request 请求参数（包含resumeId、jobType、analysisDepth）
     * @return 简历分析结果和面试问题清单
     */
    @PostMapping("/analyze-resume")
    public BaseResponseVO analyzeResume(@RequestBody Map<String, Object> request) {
        try {
            Long resumeId = Long.valueOf(request.get("resumeId").toString());
            String jobType = (String) request.getOrDefault("jobType", "");
            String analysisDepth = (String) request.getOrDefault("analysisDepth", "intermediate");
            
            log.info("开始分析简历，resumeId: {}, jobType: {}, analysisDepth: {}", resumeId, jobType, analysisDepth);
            
            // 调用简历分析服务
            ResumeAnalysisDTO analysisResult = resumeAnalysisService.analyzeResume(resumeId, jobType, analysisDepth);
            
            log.info("简历分析完成，analysisId: {}", analysisResult.getAnalysisId());
            
            return BaseResponseVO.success(analysisResult);
            
        } catch (Exception e) {
            log.error("简历分析失败: {}", e.getMessage(), e);
            return BaseResponseVO.error("简历分析失败：" + e.getMessage());
        }
    }

    /**
     * 获取AI运行日志（调试模式）
     */
    @GetMapping("/trace-logs/{sessionId}")
    public BaseResponseVO getTraceLogs(@PathVariable String sessionId) {
        try {
            List<AiTraceLog> logs = aiTraceLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
            
            // 转换为VO对象列表
            List<AiTraceLogVO> voList = logs.stream()
                .map(log -> new AiTraceLogVO(
                    log.getSessionId(),
                    log.getActionType(),
                    log.getPromptInput(),
                    log.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
            
            return BaseResponseVO.success(voList);
        } catch (Exception e) {
            log.error("获取AI跟踪日志失败", e);
            return BaseResponseVO.error("获取AI跟踪日志失败：" + e.getMessage());
        }
  }
  
  /**
     * 提交答案并获取下一个问题
     * @param request 请求参数
     * @return 下一个问题、评分和反馈
     */
    @PostMapping("/answer")
    public BaseResponseVO submitAnswer(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            String userAnswerText = (String) request.get("userAnswerText");
            Integer answerDuration = Integer.valueOf(request.get("answerDuration").toString());
            // 支持动态更新面试官风格（可选参数）
            String toneStyle = (String) request.getOrDefault("toneStyle", null);

            InterviewResponseDTO result = interviewService.submitAnswer(sessionId, userAnswerText, answerDuration);
            
            // 转换为VO对象
            InterviewAnswerVO vo = new InterviewAnswerVO();
            vo.setSessionId(result.getSessionId());
            vo.setQuestion(result.getQuestion());
            vo.setQuestionType(result.getQuestionType());
            vo.setScore(result.getScore());
            vo.setFeedback(result.getFeedback());
            vo.setNextQuestion(result.getNextQuestion());
            vo.setNextQuestionType(result.getNextQuestionType());
            vo.setIsCompleted(result.getIsCompleted());
            
            // 如果需要根据toneStyle调整，这里可以扩展逻辑
            return BaseResponseVO.success(vo);
        } catch (Exception e) {
            log.error("Submit answer failed:", e);
            return BaseResponseVO.error("提交答案失败：" + e.getMessage());
        }
    }

    /**
     * 完成面试并生成报告
     * @param request 请求参数
     * @return 面试报告信息
     */
    @PostMapping("/finish")
    public BaseResponseVO finishInterview(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");

            InterviewReportDTO result = interviewService.finishInterview(sessionId);
            
            // 转换为VO对象
            InterviewReportVO vo = new InterviewReportVO();
            vo.setSessionId(result.getSessionId());
            vo.setTotalScore(result.getTotalScore());
            vo.setOverallFeedback(result.getOverallFeedback());
            vo.setStrengths(result.getStrengths());
            vo.setImprovements(result.getImprovements());
            vo.setCreatedAt(result.getCreatedAt() != null ? result.getCreatedAt().toString() : null);
            
            return BaseResponseVO.success(vo);
        } catch (Exception e) {
            log.error("Finish interview failed:", e);
            return BaseResponseVO.error("完成面试失败：" + e.getMessage());
        }
    }

    /**
     * 获取面试历史列表
     * @param userId 用户ID
     * @return 面试历史列表
     */
    @GetMapping("/history")
    public BaseResponseVO getInterviewHistory(@RequestParam Long userId) {
        try {
            List<InterviewHistoryVO> histories = interviewService.getInterviewHistory(userId);
            InterviewHistoryListVO result = new InterviewHistoryListVO(histories);
            return BaseResponseVO.success(result);
        } catch (Exception e) {
            log.error("Get interview history failed:", e);
            return BaseResponseVO.error("获取面试历史失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据面试结果计算薪资范围
     * 基于AI面试评分、技术深度等多维度计算薪资
     */
    @PostMapping("/calculate-salary")
    public BaseResponseVO calculateSalary(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            SalaryRangeVO salaryRange = interviewService.calculateSalary(sessionId);
            
            // 转换为VO对象
            InterviewSalaryVO vo = new InterviewSalaryVO();
            vo.setMinSalary(salaryRange.getMinSalary().doubleValue());
            vo.setMaxSalary(salaryRange.getMaxSalary().doubleValue());
            vo.setCurrency(salaryRange.getCurrency());
            vo.setLevel(salaryRange.getLevel());
            vo.setReason(salaryRange.getReason());
            
            return BaseResponseVO.success(vo);
        } catch (Exception e) {
            log.error("Calculate salary failed:", e);
            return BaseResponseVO.error("薪资计算失败：" + e.getMessage());
        }
    }

    /**
     * 获取面试详情
     * @param sessionId 会话ID
     * @return 面试详情信息
     */
    @GetMapping("/detail/{sessionId}")
    public BaseResponseVO getInterviewDetail(@PathVariable String sessionId) {
        try {
            InterviewSessionVO detail = interviewService.getInterviewDetail(sessionId);
            return BaseResponseVO.success(detail);
        } catch (Exception e) {
            log.error("Get interview detail failed:", e);
            return BaseResponseVO.error("获取面试详情失败：" + e.getMessage());
        }
    }

}