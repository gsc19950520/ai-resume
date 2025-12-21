package com.aicv.airesume.service;

import com.aicv.airesume.entity.GrowthReport;
import com.aicv.airesume.model.vo.GrowthReportVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;

/**
 * AI成长报告服务接口
 */
public interface GrowthReportService {

    /**
     * 生成用户的AI成长报告
     * @param userId 用户ID
     * @return 生成的成长报告VO
     */
    GrowthReportVO generateGrowthReport(Long userId);

    /**
     * 获取用户最新的成长报告
     * @param userId 用户ID
     * @return 最新的成长报告VO
     */
    Optional<GrowthReportVO> getLatestGrowthReport(Long userId);

    /**
     * 获取用户的所有成长报告
     * @param userId 用户ID
     * @return 成长报告VO列表
     */
    List<GrowthReportVO> getAllGrowthReports(Long userId);

    /**
     * 根据报告ID获取成长报告
     * @param reportId 报告ID
     * @return 成长报告VO
     */
    Optional<GrowthReportVO> getGrowthReportById(Long reportId);

    /**
 * 删除成长报告
 * @param reportId 报告ID
 * @param userId 用户ID
 * @return 是否删除成功
 */
boolean deleteGrowthReport(Long reportId, Long userId);

/**
     * 异步流式生成用户的AI成长报告
     * @param userId 用户ID
     * @param emitter SSE发射器，用于流式输出结果
     * @return 报告ID
     */
    String generateGrowthReportStream(Long userId, SseEmitter emitter);
    
    /**
     * 获取或生成成长报告的主方法，包含条件判断逻辑
     * 1. 检查用户是否有至少两次面试记录
     * 2. 检查是否有新的面试记录
     * 3. 如果没有新记录且有报告，则直接返回现有报告
     * 4. 如果没有新记录且没有报告，则生成新报告
     * 5. 如果有新记录，则重新生成报告并替换原数据
     * @param userId 用户ID
     * @param emitter SSE发射器，用于流式输出结果
     * @return 报告ID
     */
    String getOrGenerateGrowthReport(Long userId, SseEmitter emitter);
}
