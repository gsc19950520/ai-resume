package com.aicv.airesume.controller;

import com.aicv.airesume.model.dto.InterviewResponseDTO;
import com.aicv.airesume.model.dto.InterviewReportDTO;
import com.aicv.airesume.model.dto.ResumeAnalysisDTO;
import com.aicv.airesume.model.dto.SubmitAnswerRequestDTO;
import com.aicv.airesume.model.dto.FinishInterviewRequestDTO;
import com.aicv.airesume.model.dto.CalculateSalaryRequestDTO;
import com.aicv.airesume.model.dto.AnalyzeResumeRequestDTO;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.service.ResumeAnalysisService;
import com.aicv.airesume.service.config.DynamicConfigService;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.model.vo.InterviewConfigVO;
import com.aicv.airesume.model.vo.InterviewPersonasVO;
import com.aicv.airesume.model.vo.InterviewStartVO;
import com.aicv.airesume.model.vo.InterviewResponseVO;
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
import com.aicv.airesume.model.vo.SalaryInfoVO;
import com.aicv.airesume.model.vo.GrowthAdviceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.Arrays;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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
            
            // 调用服务层开始面试，直接获取InterviewResponseVO
            Integer jobTypeId = request.getJobTypeId();
            log.info("接收到的jobTypeId: {}", jobTypeId);
            InterviewResponseVO result = interviewService.startInterview(userId, resumeId, persona, sessionSeconds, jobTypeId);
            
            // 直接返回VO对象，无需额外转换
            return BaseResponseVO.success(result);
        } catch (Exception e) {
            log.error("Start interview failed:", e);
            return BaseResponseVO.error("开始面试失败：" + e.getMessage());
        }
    }


    
    /**
     * 获取第一个面试问题（流式输出）
     * @param sessionId 会话ID
     * @return SSE响应流
     */
    @GetMapping(value = "/get-first-question-stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getFirstQuestionStream(@PathVariable String sessionId) {
        try {
            return interviewService.getFirstQuestionStream(sessionId);
        } catch (Exception e) {
            log.error("获取第一个面试问题失败", e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    
    /**
     * 分析简历并生成结构化面试问题清单
     * @param request 请求参数DTO
     * @return 简历分析结果和面试问题清单
     */
    @PostMapping("/analyze-resume")
    public BaseResponseVO analyzeResume(@RequestBody AnalyzeResumeRequestDTO request) {
        try {
            Long resumeId = request.getResumeId();
            String jobType = request.getJobType();
            String analysisDepth = request.getAnalysisDepth();
            
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
     * 提交答案并获取下一个问题（流式输出）
     * @param request 请求参数DTO
     * @return SSE响应流
     */
    @PostMapping(value = "/answer-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter submitAnswerStream(@RequestBody SubmitAnswerRequestDTO request) {
        try {
            String sessionId = request.getSessionId();
            String userAnswerText = request.getUserAnswerText();
            Integer answerDuration = request.getAnswerDuration();
            // 支持动态更新面试官风格（可选参数）
            String toneStyle = request.getToneStyle();

            return interviewService.submitAnswerStream(sessionId, userAnswerText, answerDuration, toneStyle);
        } catch (Exception e) {
            log.error("Submit answer failed:", e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    /**
     * 完成面试并生成报告
     * @param sessionId 会话ID
     * @return 面试报告信息
     */
    @GetMapping(value = "/finish", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter finishInterview(@RequestParam String sessionId) {
        try {
            // 使用流式方式调用服务，只返回DeepSeek的结果
            return interviewService.finishInterviewStream(sessionId);
        } catch (Exception e) {
            log.error("完成面试失败", e);
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event().name("error").data("生成面试报告失败: " + e.getMessage()));
                errorEmitter.completeWithError(e);
            } catch (IOException ex) {
                log.error("发送错误信息失败", ex);
            }
            return errorEmitter;
        }
    }
    
    /**
     * 完成面试并生成报告（旧版非流式接口，保留兼容性）
     * @param request 请求参数DTO
     * @return 面试报告信息
     */
    @PostMapping("/finish/non-stream")
    public BaseResponseVO finishInterviewNonStream(@RequestBody FinishInterviewRequestDTO request) {
        try {
            String sessionId = request.getSessionId();

            InterviewReportDTO result = interviewService.finishInterview(sessionId);
            
            // 转换为VO对象
            InterviewReportVO vo = new InterviewReportVO();
            vo.setSessionId(result.getSessionId());
            vo.setTotalScore(result.getTotalScore());
            vo.setOverallFeedback(result.getOverallFeedback());
            vo.setStrengths(result.getStrengths());
            vo.setImprovements(result.getImprovements());
            vo.setCreatedAt(result.getCreatedAt() != null ? result.getCreatedAt().toString() : null);
            
            // 设置职位类型和领域（示例数据）
            vo.setJobType("前端工程师");
            vo.setDomain("软件工程");
            
            // 设置评分数据
            Map<String, Double> scores = result.getScores();
            vo.setTechScore(scores.getOrDefault("tech", 0.0));
            vo.setLogicScore(scores.getOrDefault("logic", 0.0));
            vo.setClarityScore(scores.getOrDefault("clarity", 0.0));
            vo.setDepthScore(scores.getOrDefault("depth", 0.0));
            
            // 设置成长报告数据（示例数据）
            Map<String, Integer> growthRadar = new HashMap<>();
            growthRadar.put("专业技能", 75);
            growthRadar.put("逻辑思维", 68);
            growthRadar.put("沟通表达", 80);
            growthRadar.put("创新潜力", 60);
            vo.setGrowthRadar(growthRadar);
            
            List<Integer> trendCurve = Arrays.asList(65, 70, 73, 78);
            vo.setTrendCurve(trendCurve);
            
            List<String> recommendedSkills = Arrays.asList("前端框架优化", "性能调优", "架构设计");
            vo.setRecommendedSkills(recommendedSkills);
            
            List<String> longTermPath = Arrays.asList("高级工程师", "技术架构师", "技术专家");
            vo.setLongTermPath(longTermPath);
            
            // 设置薪资数据（示例数据）
            SalaryInfoVO salaryInfo = new SalaryInfoVO();
            salaryInfo.setMinSalary(15000.0);
            salaryInfo.setMaxSalary(25000.0);
            salaryInfo.setCurrency("CNY");
            salaryInfo.setLevel("中级");
            salaryInfo.setReason("基于您的技术能力和面试表现，我们给出的薪资范围是15k-25k");
            vo.setSalaryInfo(salaryInfo);
            
            // 设置成长建议（示例数据）
            GrowthAdviceVO growthAdvice = new GrowthAdviceVO();
            growthAdvice.setShortTerm("短期建议：加强前端框架源码学习，提升性能优化能力");
            growthAdvice.setMidTerm("中期建议：学习架构设计，参与大型项目开发");
            growthAdvice.setLongTerm("长期建议：培养技术领导力，成为领域专家");
            vo.setGrowthAdvice(growthAdvice);
            
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
    public BaseResponseVO calculateSalary(@RequestBody CalculateSalaryRequestDTO request) {
        try {
            String sessionId = request.getSessionId();
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