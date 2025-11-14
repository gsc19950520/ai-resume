package com.aicv.airesume.repository;

import com.aicv.airesume.entity.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 简历技能数据访问接口
 */
@Repository
public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {
    
    /**
     * 根据简历ID查询技能列表
     */
    List<ResumeSkill> findByResumeIdOrderByOrderIndexAsc(Long resumeId);
    
    /**
     * 根据简历ID删除技能
     */
    void deleteByResumeId(Long resumeId);
    
    // 注意：原有的findByResumeIdAndCategoryOrderByOrderIndexAsc方法已移除，因为ResumeSkill实体中不存在category属性
}