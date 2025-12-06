package com.aicv.airesume.controller;

import com.aicv.airesume.model.dto.InterviewResponseDTO;
import com.aicv.airesume.model.dto.InterviewReportDTO;
import com.aicv.airesume.model.dto.SubmitAnswerRequestDTO;
import com.aicv.airesume.model.dto.FinishInterviewRequestDTO;
import com.aicv.airesume.model.dto.CalculateSalaryRequestDTO;
import com.aicv.airesume.model.dto.AnalyzeResumeRequestDTO;
import com.aicv.airesume.model.dto.SaveReportRequestDTO;
import com.aicv.airesume.model.dto.UpdateRemainingTimeRequestDTO;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.service.config.DynamicConfigService;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.model.vo.InterviewConfigVO;
import com.aicv.airesume.model.vo.InterviewHistoryItemVO;
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
import com.aicv.airesume.model.vo.ReportChunksVO;
import lombok.extern.slf4j.Slf4j;
import com.aicv.airesume.annotation.Log;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.Arrays;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.aicv.airesume.entity.AiTraceLog;
import com.aicv.airesume.entity.InterviewLog;
import com.aicv.airesume.repository.AiTraceLogRepository;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.aicv.airesume.utils.TokenUtils;
import com.aicv.airesume.utils.GlobalContextUtil;
import com.aicv.airesume.common.constant.ResponseCode;
import com.aicv.airesume.common.exception.BusinessException;

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
    private TokenUtils tokenUtils;

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
            
            // 设置默认会话时长
            config.setDefaultSessionSeconds(dynamicConfigService.getDefaultSessionSeconds());
            
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
            Long userId = GlobalContextUtil.getUserId();
            // 从DTO中获取参数
            Long resumeId = request.getResumeId();
            String persona = request.getPersona();
            Boolean forceNew = request.getForceNew() != null ? request.getForceNew() : false;
            
            // 从动态配置获取默认值（仅用于会话时长）
            Integer defaultSessionSeconds = dynamicConfigService.getDefaultSessionSeconds();
            
            // 使用请求参数或默认配置
            Integer sessionSeconds = request.getSessionSeconds() != null ? 
                request.getSessionSeconds() : defaultSessionSeconds;
            
            // 调用服务层开始面试，直接获取InterviewResponseVO
            Integer jobTypeId = request.getJobTypeId();
            log.info("接收到的参数: userId={}, resumeId={}, jobTypeId={}, forceNew={}", userId, resumeId, jobTypeId, forceNew);
            InterviewResponseVO result = interviewService.startInterview(userId, resumeId, persona, sessionSeconds, jobTypeId, forceNew);
            
            // 直接返回VO对象，无需额外转换
            return BaseResponseVO.success(result);
        } catch (BusinessException e) {
            return BaseResponseVO.error(e.getMessage());
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
        } catch (BusinessException e) {
            return BaseResponseVO.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取AI跟踪日志失败", e);
            return BaseResponseVO.error("获取AI跟踪日志失败：" + e.getMessage());
        }
  }
    
    /**
     * 提交答案并获取下一个问题（流式输出）
     * @param requestDTO 请求参数DTO
     * @return SSE响应流
     */
    @PostMapping(value = "/answer-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter submitAnswerStream(@RequestBody SubmitAnswerRequestDTO requestDTO) {
        try {
            String sessionId = requestDTO.getSessionId();
            String userAnswerText = requestDTO.getUserAnswerText();
            Integer answerDuration = requestDTO.getAnswerDuration();
            // 支持动态更新面试官风格（可选参数）
            String toneStyle = requestDTO.getToneStyle();

            return interviewService.submitAnswerStream(sessionId, userAnswerText, answerDuration, toneStyle);
        } catch (Exception e) {
            log.error("Submit answer failed:", e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    /**
     * 开始生成面试报告
     * @param sessionId 会话ID
     * @return 报告ID
     */
    @GetMapping("/start-report")
    public BaseResponseVO startReportGeneration(@RequestParam String sessionId) {
        String reportId = interviewService.startReportGeneration(sessionId);
        return BaseResponseVO.success(reportId);
    }

    /**
     * 获取报告分片
     * @param reportId 报告ID
     * @param lastIndex 最后接收的分片索引
     * @return 报告分片信息
     */
    @GetMapping("/get-report-chunks")
    public BaseResponseVO getReportChunks(@RequestParam String reportId, @RequestParam int lastIndex) {
        ReportChunksVO result = interviewService.getReportChunks(reportId, lastIndex);
        return BaseResponseVO.success(result);
    }

    /**
     * 获取面试历史列表
     * @return 面试历史列表
     */
    @GetMapping("/history")
    public BaseResponseVO getInterviewHistoryList() {
        try {
            Long userId = GlobalContextUtil.getUserId();
            List<InterviewHistoryVO> histories = interviewService.getInterviewHistory(userId);
            InterviewHistoryListVO result = new InterviewHistoryListVO(histories);
            return BaseResponseVO.success(result);
        } catch (BusinessException e) {
            return BaseResponseVO.error(e.getMessage());
        } catch (Exception e) {
            log.error("Get interview history failed:", e);
            return BaseResponseVO.error("获取面试历史失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查用户是否有进行中的面试
     * @return 如果有进行中的面试，返回面试详情；否则返回null
     */
    @GetMapping("/check-ongoing")
    public BaseResponseVO checkOngoingInterview() {
        try {
            Long userId = GlobalContextUtil.getUserId();
            InterviewSessionVO ongoingInterview = interviewService.checkOngoingInterview(userId);
            return BaseResponseVO.success(ongoingInterview);
        } catch (BusinessException e) {
            return BaseResponseVO.error(e.getMessage());
        } catch (Exception e) {
            log.error("Check ongoing interview failed:", e);
            return BaseResponseVO.error("检查进行中面试失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据面试结果计算薪资范围
     * 基于AI面试评分、技术深度等多维度计算薪资
     */
    @PostMapping("/calculate-salary")
    public BaseResponseVO calculateSalary(@RequestBody CalculateSalaryRequestDTO requestDTO) {
        try {
            String sessionId = requestDTO.getSessionId();
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
    
    /**
     * 获取面试历史记录
     * @param sessionId 会话ID
     * @return 面试历史记录
     */
    @GetMapping("/history/{sessionId}")
    public BaseResponseVO getInterviewHistory(@PathVariable String sessionId) {
        try {
            List<InterviewHistoryItemVO> logs = interviewService.getInterviewHistory(sessionId);
            return BaseResponseVO.success(logs);
        } catch (Exception e) {
            log.error("Get interview history failed:", e);
            return BaseResponseVO.error("获取面试历史失败：" + e.getMessage());
        }
    }
    
    /**
     * 保存面试报告
     * @param request 请求数据
     * @return 保存结果
     */
    @PostMapping("/save-report")
    public BaseResponseVO saveReport(@RequestBody SaveReportRequestDTO request) {
        try {
            String sessionId = request.getSessionId();
            Map<String, Object> reportData = request.getReportData();
            
            interviewService.saveReport(sessionId, reportData);
            return BaseResponseVO.success(null);
        } catch (Exception e) {
            log.error("Save report failed:", e);
            return BaseResponseVO.error("保存报告失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除面试记录
     * @param sessionId 会话ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{sessionId}")
    public BaseResponseVO deleteInterview(@PathVariable String sessionId) {
        try {
            interviewService.deleteInterview(sessionId);
            return BaseResponseVO.success(null);
        } catch (Exception e) {
            log.error("Delete interview failed:", e);
            return BaseResponseVO.error("删除面试记录失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取面试报告详情
     * @param sessionId 会话ID
     * @return 面试报告详情
     */
    @GetMapping("/report/{sessionId}")
    public BaseResponseVO getInterviewReport(@PathVariable String sessionId) {
        try {
            InterviewReportVO report = interviewService.getInterviewReport(sessionId);
            return BaseResponseVO.success(report);
        } catch (Exception e) {
            log.error("Get interview report failed:", e);
            return BaseResponseVO.error("获取面试报告失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新面试剩余时间
     * @param request 请求参数，包含sessionId和remainingTime
     * @return 更新结果
     */
    @PostMapping("/update-remaining-time")
    @Log(recordParams = false, recordResult = false, recordExecutionTime = false)
    public BaseResponseVO updateRemainingTime(@RequestBody UpdateRemainingTimeRequestDTO request) {
        try {
            String sessionId = request.getSessionId();
            Integer remainingTime = request.getSessionTimeRemaining();
            
            if (sessionId == null || remainingTime == null) {
                return BaseResponseVO.error("参数错误：sessionId和remainingTime不能为空");
            }
            
            boolean result = interviewService.updateRemainingTime(sessionId, remainingTime);
            if (result) {
                return BaseResponseVO.success(null);
            } else {
                return BaseResponseVO.error("更新剩余时间失败：会话不存在或更新失败");
            }
        } catch (Exception e) {
            log.error("Update remaining time failed:", e);
            return BaseResponseVO.error("更新剩余时间失败：" + e.getMessage());
        }
    }
}