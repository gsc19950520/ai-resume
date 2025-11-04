package com.aicv.airesume.repository;

import com.aicv.airesume.entity.ResumeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 简历模板数据访问接口
 */
@Repository
public interface ResumeTemplateRepository extends JpaRepository<ResumeTemplate, Long> {

    Optional<ResumeTemplate> findByResumeId(Long resumeId);
    
    List<ResumeTemplate> findByUserIdOrderByUpdateTimeDesc(Long userId);
}