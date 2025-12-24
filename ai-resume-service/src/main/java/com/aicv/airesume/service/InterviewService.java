package com.aicv.airesume.service;

import com.aicv.airesume.model.vo.InterviewHistoryItemVO;
import com.aicv.airesume.model.vo.InterviewHistoryVO;
import com.aicv.airesume.model.vo.InterviewReportVO;
import com.aicv.airesume.model.vo.InterviewResponseVO;
import com.aicv.airesume.model.vo.InterviewSessionVO;
import com.aicv.airesume.model.vo.ReportChunksVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;

/**
 * 面试服务接口
 */
public interface InterviewService {

    /**
     * 开始面试
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param persona 面试官风格
     * @param sessionSeconds 会话时长
     * @param jobTypeId 职位类型ID
     * @param forceNew 是否强制创建新会话
     * @return 面试响应VO
     */
    InterviewResponseVO startInterview(Long userId, Long resumeId, String persona, Integer sessionSeconds, Integer jobTypeId, Boolean forceNew, String payOrderNo);
    
    /**
     * 完成面试并异步生成报告，返回reportId
     * @param sessionId 会话ID
     * @return reportId和状态
     */
    String startReportGeneration(String sessionId, String lastAnswer);

    /**
     * 获取报告块
     * @param reportId 报告ID
     * @param lastIndex 上一次获取的最后一个块的索引
     * @return 报告块列表和状态
     */
    ReportChunksVO getReportChunks(String reportId, int lastIndex);

    /**
     * 获取用户的面试历史
     * @param userId 用户ID
     * @return 面试历史列表
     */
    List<InterviewHistoryVO> getInterviewHistory(Long userId);
    
    /**
     * 检查用户是否有进行中的面试
     * @param userId 用户ID
     * @return 进行中的面试会话VO，如果没有则返回null
     */
    InterviewSessionVO checkOngoingInterview(Long userId);
    
    /**
     * 获取面试历史记录
     * @param sessionId 会话ID
     * @return 面试历史记录
     */
    List<InterviewHistoryItemVO> getInterviewHistory(String sessionId);
    
    /**
     * 保存面试报告
     * @param sessionId 会话ID
     * @param reportData 报告数据
     */
    void saveReport(String sessionId, Map<String, Object> reportData);

    /**
     * 获取面试详情
     * @param sessionId 会话ID
     * @return 面试详情
     */
    InterviewSessionVO getInterviewDetail(String sessionId);

    /**
     * 获取第一个面试问题（流式输出）
     * @param sessionId 会话ID
     * @return SseEmitter 对象，用于流式输出
     */
    SseEmitter getFirstQuestionStream(String sessionId);

    /**
     * 提交回答并获取下一个问题（流式输出）
     * @param sessionId 会话ID
     * @param userAnswerText 用户回答文本
     * @param answerDuration 回答时长（秒）
     * @return SseEmitter 对象，用于流式输出
     */
    SseEmitter submitAnswerStream(String sessionId, String userAnswerText, Integer answerDuration, String toneStyle);
    
    /**
     * 删除面试记录
     * @param sessionId 会话ID
     */
    void deleteInterview(String sessionId);
    
    /**
     * 更新面试剩余时间
     * @param sessionId 会话ID
     * @param remainingTime 剩余时间（秒）
     * @return 是否更新成功
     */
    boolean updateRemainingTime(String sessionId, Integer remainingTime);
    
    /**
     * 获取面试报告详情
     * @param sessionId 会话ID
     * @return 面试报告VO
     */
    InterviewReportVO getInterviewReport(String sessionId);
    
}