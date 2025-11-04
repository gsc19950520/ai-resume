package com.aicv.airesume.service;

import com.aicv.airesume.entity.ResumeTemplate;
import java.util.List;

/**
 * 简历模板服务接口
 */
public interface ResumeTemplateService {

    /**
     * 保存简历模板关系
     * @param resumeTemplate 简历模板关系对象
     * @return 保存后的简历模板关系对象
     */
    ResumeTemplate saveResumeTemplate(ResumeTemplate resumeTemplate);

    /**
     * 根据简历ID获取模板关系
     * @param resumeId 简历ID
     * @return 简历模板关系对象
     */
    ResumeTemplate getByResumeId(Long resumeId);

    /**
     * 根据用户ID获取简历模板关系列表
     * @param userId 用户ID
     * @return 简历模板关系列表
     */
    List<ResumeTemplate> getByUserId(Long userId);

    /**
     * 更新简历模板关系
     * @param resumeTemplate 简历模板关系对象
     * @return 更新后的简历模板关系对象
     */
    ResumeTemplate updateResumeTemplate(ResumeTemplate resumeTemplate);

    /**
     * 删除简历模板关系
     * @param id 关系ID
     */
    void deleteResumeTemplate(Long id);

    /**
     * 根据简历ID删除模板关系
     * @param resumeId 简历ID
     */
    void deleteByResumeId(Long resumeId);
}