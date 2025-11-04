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
     * 查询所有可用模板，按使用次数排序
     */
    List<Template> findAllByOrderByUseCountDesc();
    
    /**
     * 根据岗位类型查询模板
     */
    List<Template> findByJobTypeOrderByUseCountDesc(String jobType);
    
    /**
     * 根据是否VIP查询模板
     */
    List<Template> findByVipOnlyOrderByUseCountDesc(Boolean vipOnly);
    
    /**
     * 根据岗位类型和VIP状态查询模板
     */
    List<Template> findByJobTypeAndVipOnlyOrderByUseCountDesc(String jobType, Boolean vipOnly);
}
