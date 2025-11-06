package com.aicv.airesume.repository;

import com.aicv.airesume.entity.InterviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 面试日志数据访问接口
 */
@Repository
public interface InterviewLogRepository extends JpaRepository<InterviewLog, Long> {

    /**
     * 根据问题ID查找日志
     * @param questionId 问题ID
     * @return 日志对象
     */
    Optional<InterviewLog> findByQuestionId(String questionId);

    /**
     * 根据会话ID查找日志列表，按轮次升序
     * @param sessionId 会话ID
     * @return 日志列表
     */
    List<InterviewLog> findBySession_SessionIdOrderByRoundNumberAsc(String sessionId);

    /**
     * 根据会话ID和轮次查找日志
     * @param sessionId 会话ID
     * @param roundNumber 轮次
     * @return 日志对象
     */
    Optional<InterviewLog> findBySession_SessionIdAndRoundNumber(String sessionId, Integer roundNumber);

    /**
     * 删除会话相关的所有日志
     * @param sessionId 会话ID
     */
    void deleteBySession_SessionId(String sessionId);

}