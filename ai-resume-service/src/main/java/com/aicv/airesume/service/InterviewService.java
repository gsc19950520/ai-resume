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
     * @param sessionSeconds 会话时长（可选）
     * @return 面试会话信息和第一个问题
     */
    InterviewResponseDTO startInterview(Long userId, Long resumeId, String persona, Integer sessionSeconds);

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
    
    /**
     * 从简历提取技术项和项目点
     * @param resumeContent 简历内容
     * @return 包含技术项和项目点的对象
     */
    Map<String, Object> extractTechItemsAndProjectPoints(String resumeContent);
    
    /**
     * 动态生成下一个面试问题
     * @param techItems 技术项列表
     * @param projectPoints 项目点列表
     * @param interviewState 面试状态
     * @param sessionTimeRemaining 剩余时间（秒）
     * @param persona 面试官风格
     * @return 包含下一个问题的对象
     */
    Map<String, Object> generateNextQuestion(List<String> techItems, List<Map<String, Object>> projectPoints, Map<String, Object> interviewState, Integer sessionTimeRemaining, String persona);

    /**
     * 生成第一个面试问题
     * @param resumeId 简历ID
     * @param personaId 面试官风格ID
     * @param industryJobTag 行业职位标签
     * @return 包含问题、深度级别等信息的对象
     */
    Map<String, Object> generateFirstQuestion(Long resumeId, String personaId, String industryJobTag);

}