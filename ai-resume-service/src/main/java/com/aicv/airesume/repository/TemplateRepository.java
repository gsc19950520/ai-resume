package com.aicv.airesume.repository;

import com.aicv.airesume.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模板数据访问接口
 */
@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    
    /**
     * 查询所有可用模板，按排序权重排序
     */
    List<Template> findAllByOrderBySortWeightDesc();
    
    /**
     * 根据岗位类型查询模板
     */
    List<Template> findByJobTypeOrderBySortWeightDesc(String jobType);
    
    /**
     * 根据是否VIP查询模板
     */
    List<Template> findByIsVipOrderBySortWeightDesc(Integer isVip);
    
    /**
     * 根据岗位类型和VIP状态查询模板
     */
    List<Template> findByJobTypeAndIsVipOrderBySortWeightDesc(String jobType, Integer isVip);
}
