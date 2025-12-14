package com.aicv.airesume.repository;

import com.aicv.airesume.entity.GrowthReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI成长报告数据访问接口
 */
@Repository
public interface GrowthReportRepository extends JpaRepository<GrowthReport, Long> {

    /**
     * 根据用户ID获取成长报告列表（按生成时间倒序）
     * @param userId 用户ID
     * @return 成长报告列表
     */
    List<GrowthReport> findByUserIdOrderByGeneratedAtDesc(Long userId);

    /**
     * 获取用户最新的成长报告
     * @param userId 用户ID
     * @return 最新的成长报告
     */
    Optional<GrowthReport> findFirstByUserIdOrderByGeneratedAtDesc(Long userId);

    /**
     * 根据用户ID和生成时间范围获取成长报告
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 成长报告列表
     */
    List<GrowthReport> findByUserIdAndGeneratedAtBetween(Long userId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
}
