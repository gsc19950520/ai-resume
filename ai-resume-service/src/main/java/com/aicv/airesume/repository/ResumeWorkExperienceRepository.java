package com.aicv.airesume.repository;

import com.aicv.airesume.entity.ResumeWorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 简历工作经历数据访问接口
 */
@Repository
public interface ResumeWorkExperienceRepository extends JpaRepository<ResumeWorkExperience, Long> {
    
    /**
     * 根据简历ID查询工作经历列表
     */
    List<ResumeWorkExperience> findByResumeIdOrderByOrderIndexAsc(Long resumeId);
    
    /**
     * 根据简历ID删除工作经历
     */
    void deleteByResumeId(Long resumeId);
}