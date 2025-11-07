package com.aicv.airesume.repository;

import com.aicv.airesume.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 面试会话数据访问接口
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    /**
     * 根据会话ID查找会话
     * @param sessionId 会话ID
     * @return 会话对象
     */
    Optional<InterviewSession> findBySessionId(String sessionId);

    /**
     * 根据用户ID查找会话列表，按创建时间倒序
     * @param userId 用户ID
     * @return 会话列表
     */
    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据用户ID和状态查找会话列表
     * @param userId 用户ID
     * @param status 状态
     * @return 会话列表
     */
    List<InterviewSession> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

}