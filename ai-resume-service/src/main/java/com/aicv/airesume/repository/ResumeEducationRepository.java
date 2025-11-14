package com.aicv.airesume.repository;

import com.aicv.airesume.entity.ResumeEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 简历教育经历数据访问接口
 */
@Repository
public interface ResumeEducationRepository extends JpaRepository<ResumeEducation, Long> {
    
    /**
     * 根据简历ID查询教育经历列表
     */
    List<ResumeEducation> findByResumeIdOrderByOrderIndexAsc(Long resumeId);
    
    /**
     * 根据简历ID删除教育经历
     */
    void deleteByResumeId(Long resumeId);
}