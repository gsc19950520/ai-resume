package com.aicv.airesume.repository;

import com.aicv.airesume.entity.AiTraceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI运行日志数据访问接口
 */
@Repository
public interface AiTraceLogRepository extends JpaRepository<AiTraceLog, Long> {

    /**
     * 根据会话ID查询AI运行日志列表
     * @param sessionId 会话ID
     * @return AI运行日志列表
     */
    List<AiTraceLog> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * 根据会话ID和动作类型查询AI运行日志列表
     * @param sessionId 会话ID
     * @param actionType 动作类型
     * @return AI运行日志列表
     */
    List<AiTraceLog> findBySessionIdAndActionTypeOrderByCreatedAtDesc(String sessionId, String actionType);
}