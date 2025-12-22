package com.aicv.airesume.service;

import com.aicv.airesume.entity.GrowthReport;
import com.aicv.airesume.model.vo.GrowthReportVO;

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
}
