package com.aicv.airesume.repository;

import com.aicv.airesume.entity.InterviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 面试报告数据访问接口
 */
@Repository
public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {

    /**
     * 根据会话ID查询面试报告
     * @param sessionId 会话ID
     * @return 面试报告
     */
    Optional<InterviewReport> findBySessionId(String sessionId);
}