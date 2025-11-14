package com.aicv.airesume.repository;

import com.aicv.airesume.entity.ResumeProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 简历项目经历数据访问接口
 */
@Repository
public interface ResumeProjectRepository extends JpaRepository<ResumeProject, Long> {
    
    /**
     * 根据简历ID查询项目经历列表
     */
    List<ResumeProject> findByResumeIdOrderByOrderIndexAsc(Long resumeId);
    
    /**
     * 根据简历ID删除项目经历
     */
    void deleteByResumeId(Long resumeId);
}