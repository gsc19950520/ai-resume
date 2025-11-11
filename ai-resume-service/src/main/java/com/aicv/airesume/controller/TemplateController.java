package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Template;
import com.aicv.airesume.service.TemplateRendererService;
import com.aicv.airesume.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板控制器
 */
@RestController
@RequestMapping("/api/template")
public class TemplateController {

    @Autowired
    private TemplateService templateService;
    
    @Autowired
    private TemplateRendererService templateRendererService;

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
    public Map<String, Object> getTemplateById(@PathVariable String id) {
        Template template = templateService.getTemplateById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", template);
        return response;
    }

    /**
     * 增加模板使用次数
     * @param templateId 模板ID
     */
    @PostMapping("/{templateId}/use")
    public void addTemplateUseCount(@PathVariable String templateId) {
        templateService.addTemplateUseCount(templateId);
    }

    /**
     * 检查模板使用权限
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 是否有权限
     */
    @GetMapping("/check-permission")
    public Boolean checkTemplatePermission(@RequestParam Long userId, @RequestParam String templateId) {
        return templateService.checkTemplatePermission(userId, templateId);
    }
    
    /**
     * 从Word模板生成HTML内容
     * @param id 模板ID
     * @return HTML模板内容
     */
    @GetMapping("/{id}/generate-html")
    public Map<String, Object> generateHtmlFromWordTemplate(@PathVariable String id) {
        try {
            Template template = templateService.getTemplateById(id);
            if (template == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 404);
                errorResponse.put("message", "模板不存在");
                return errorResponse;
            }
            
            // 使用模板渲染服务从本地Word模板生成HTML
            String htmlContent = templateRendererService.generateHtmlFromLocalWordTemplate(template);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", htmlContent);
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "生成HTML模板失败: " + e.getMessage());
            return errorResponse;
        }
    }
}