package com.aicv.airesume.controller;

import com.aicv.airesume.model.dto.SubmitAnswerRequestDTO;
import com.aicv.airesume.model.dto.CalculateSalaryRequestDTO;
import com.aicv.airesume.model.dto.SaveReportRequestDTO;
import com.aicv.airesume.model.dto.UpdateRemainingTimeRequestDTO;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.service.config.DynamicConfigService;
import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.model.vo.InterviewConfigVO;
import com.aicv.airesume.model.vo.InterviewHistoryItemVO;
import com.aicv.airesume.model.vo.InterviewPersonasVO;
import com.aicv.airesume.model.vo.InterviewResponseVO;
import com.aicv.airesume.model.dto.InterviewStartRequestDTO;
import com.aicv.airesume.model.vo.InterviewReportVO;
import com.aicv.airesume.model.vo.InterviewHistoryListVO;
import com.aicv.airesume.model.vo.InterviewHistoryVO;
import com.aicv.airesume.model.vo.InterviewSessionVO;
import com.aicv.airesume.model.vo.SalaryRangeVO;
import com.aicv.airesume.model.vo.InterviewSalaryVO;
import com.aicv.airesume.model.vo.ReportChunksVO;
import lombok.extern.slf4j.Slf4j;
import com.aicv.airesume.annotation.Log;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.aicv.airesume.model.dto.StartReportRequest;
import java.util.List;
import java.util.Map;
import com.aicv.airesume.utils.TokenUtils;
import com.aicv.airesume.utils.GlobalContextUtil;
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
    private DynamicConfigService dynamicConfigService;

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
     * @param request 请求参数DTO
     * @return 报告ID
     */
    @PostMapping("/start-report")
    public BaseResponseVO startReportGeneration(@RequestBody StartReportRequest request) {
        // 验证参数
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            return BaseResponseVO.error("缺少必要参数: sessionId");
        }
        String reportId = interviewService.startReportGeneration(request.getSessionId(), request.getLastAnswer());
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