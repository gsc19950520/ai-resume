package com.aicv.airesume.service;

import com.aicv.airesume.entity.Template;

import java.util.List;

/**
 * 模板服务接口
 */
public interface TemplateService {
    
    /**
     * 获取所有模板列表
     */
    List<Template> getAllTemplates();
    
    /**
     * 根据岗位类型获取模板
     */
    List<Template> getTemplatesByJobType(String jobType);
    
    /**
     * 获取免费模板
     */
    List<Template> getFreeTemplates();
    
    /**
     * 获取VIP模板
     */
    List<Template> getVipTemplates();
    
    /**
     * 根据ID获取模板
     */
    Template getTemplateById(Long id);
    
    /**
     * 增加模板使用次数
     */
    void addTemplateUseCount(Long templateId);
    
    /**
     * 检查模板使用权限
     */
    boolean checkTemplatePermission(Long userId, Long templateId);
}