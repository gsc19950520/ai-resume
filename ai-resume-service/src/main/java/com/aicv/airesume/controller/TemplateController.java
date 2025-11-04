package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Template;
import com.aicv.airesume.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板控制器
 */
@RestController
@RequestMapping("/api/template")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    /**
     * 获取所有模板
     * @return 模板列表
     */
    @GetMapping("/all")
    public List<Template> getAllTemplates() {
        return templateService.getAllTemplates();
    }

    /**
     * 根据岗位类型获取模板
     * @param jobType 岗位类型
     * @return 模板列表
     */
    @GetMapping("/job-type/{jobType}")
    public List<Template> getTemplatesByJobType(@PathVariable String jobType) {
        return templateService.getTemplatesByJobType(jobType);
    }

    /**
     * 获取免费模板
     * @return 免费模板列表
     */
    @GetMapping("/free")
    public List<Template> getFreeTemplates() {
        return templateService.getFreeTemplates();
    }

    /**
     * 获取VIP模板
     * @return VIP模板列表
     */
    @GetMapping("/vip")
    public List<Template> getVipTemplates() {
        return templateService.getVipTemplates();
    }

    /**
     * 根据ID获取模板
     * @param id 模板ID
     * @return 模板信息
     */
    @GetMapping("/{id}")
    public Template getTemplateById(@PathVariable Long id) {
        return templateService.getTemplateById(id);
    }

    /**
     * 增加模板使用次数
     * @param templateId 模板ID
     */
    @PostMapping("/{templateId}/use")
    public void addTemplateUseCount(@PathVariable Long templateId) {
        templateService.addTemplateUseCount(templateId);
    }

    /**
     * 检查模板使用权限
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 是否有权限
     */
    @GetMapping("/check-permission")
    public Boolean checkTemplatePermission(@RequestParam Long userId, @RequestParam Long templateId) {
        return templateService.checkTemplatePermission(userId, templateId);
    }
}