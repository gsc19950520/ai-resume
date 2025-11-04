package com.aicv.airesume.controller;

import com.aicv.airesume.entity.ResumeTemplate;
import com.aicv.airesume.service.ResumeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 简历模板控制器
 */
@RestController
@RequestMapping("/api/resume-template")
public class ResumeTemplateController {

    @Autowired
    private ResumeTemplateService resumeTemplateService;

    /**
     * 保存简历模板关系
     * @param resumeTemplate 简历模板关系
     * @return 保存后的关系
     */
    @PostMapping("/save")
    public ResumeTemplate saveResumeTemplate(@RequestBody ResumeTemplate resumeTemplate) {
        return resumeTemplateService.saveResumeTemplate(resumeTemplate);
    }

    /**
     * 根据简历ID获取模板关系
     * @param resumeId 简历ID
     * @return 简历模板关系
     */
    @GetMapping("/by-resume/{resumeId}")
    public ResumeTemplate getByResumeId(@PathVariable Long resumeId) {
        return resumeTemplateService.getByResumeId(resumeId);
    }

    /**
     * 根据用户ID获取简历模板关系列表
     * @param userId 用户ID
     * @return 关系列表
     */
    @GetMapping("/by-user/{userId}")
    public List<ResumeTemplate> getByUserId(@PathVariable Long userId) {
        return resumeTemplateService.getByUserId(userId);
    }

    /**
     * 更新简历模板关系
     * @param id 关系ID
     * @param resumeTemplate 关系信息
     * @return 更新后的关系
     */
    @PutMapping("/{id}")
    public ResumeTemplate updateResumeTemplate(@PathVariable Long id, @RequestBody ResumeTemplate resumeTemplate) {
        resumeTemplate.setId(id);
        return resumeTemplateService.updateResumeTemplate(resumeTemplate);
    }

    /**
     * 删除简历模板关系
     * @param id 关系ID
     */
    @DeleteMapping("/{id}")
    public void deleteResumeTemplate(@PathVariable Long id) {
        resumeTemplateService.deleteResumeTemplate(id);
    }

    /**
     * 根据简历ID删除模板关系
     * @param resumeId 简历ID
     */
    @DeleteMapping("/by-resume/{resumeId}")
    public void deleteByResumeId(@PathVariable Long resumeId) {
        resumeTemplateService.deleteByResumeId(resumeId);
    }
}