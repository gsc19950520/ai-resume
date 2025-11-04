package com.aicv.airesume.repository;

import com.aicv.airesume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 简历数据访问接口
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    
    /**
     * 根据用户ID查询简历列表
     */
    List<Resume> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据用户ID和状态查询简历列表
     */
    List<Resume> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status);
    
    /**
     * 根据ID列表查询简历
     */
    List<Resume> findByIdIn(List<Long> ids);
    
    /**
     * 根据用户ID统计简历数量
     */
    long countByUserId(Long userId);
}
