package com.aicv.airesume.service;

import com.aicv.airesume.model.dto.InterviewReportDTO;
import com.aicv.airesume.model.dto.InterviewResponseDTO;
import com.aicv.airesume.model.vo.InterviewHistoryVO;
import com.aicv.airesume.model.vo.InterviewSessionVO;
import com.aicv.airesume.model.vo.SalaryRangeVO;
import java.util.List;
import java.util.Map;

/**
 * 面试服务接口
 */
public interface InterviewService {

    /**
     * 开始面试 - 支持动态配置
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param persona 面试官风格（可选）
     * * @param sessionSeconds 会话时长（可选）
     * @param jobTypeId 职位类型ID（可选）
     */
    InterviewResponseDTO startInterview(Long userId, Long resumeId, String persona, Integer sessionSeconds, Integer jobTypeId);

    /**
     * 提交回答 - 支持回答时长
     * @param sessionId 会话ID
     * @param userAnswerText 用户回答文本
     * @param answerDuration 回答时长（秒）
     * @return 评分和下一个问题
     */
    InterviewResponseDTO submitAnswer(String sessionId, String userAnswerText, Integer answerDuration);

    /**
     * 完成面试
     * @param sessionId 会话ID
     * @return 面试报告
     */
    InterviewReportDTO finishInterview(String sessionId);

    /**
     * 获取用户的面试历史
     * @param userId 用户ID
     * @return 面试历史列表
     */
    List<InterviewHistoryVO> getInterviewHistory(Long userId);

    /**
     * 计算薪资范围
     * @param sessionId 会话ID
     * @return 薪资范围
     */
    SalaryRangeVO calculateSalary(String sessionId);

    /**
     * 获取面试详情
     * @param sessionId 会话ID
     * @return 面试详情
     */
    InterviewSessionVO getInterviewDetail(String sessionId);
   
}