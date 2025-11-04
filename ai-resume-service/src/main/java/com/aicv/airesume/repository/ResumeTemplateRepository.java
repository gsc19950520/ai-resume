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

    /**
     * 根据是否活跃查询模板列表并按更新时间降序排序
     */
    List<ResumeTemplate> findByActiveOrderByUpdateTimeDesc(Boolean active);
    
    /**
     * 查询所有模板并按更新时间降序排序
     */
    List<ResumeTemplate> findAllByOrderByUpdateTimeDesc();
}