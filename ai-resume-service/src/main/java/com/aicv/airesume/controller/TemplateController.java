package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Template;

import com.aicv.airesume.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

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
            
            // Word转HTML功能已移除，因为不再需要后端渲染
            String htmlContent = "";
            
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
    
    /**
     * 生成PDF并下载
     * @param id 模板ID
     * @return PDF文件的字节数组响应
     */
    @GetMapping("/{id}/generate-pdf")
    public ResponseEntity<byte[]> generatePdfFromTemplate(@PathVariable String id) {
        try {
            Template template = templateService.getTemplateById(id);
            if (template == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // Word转HTML和HTML转PDF功能已移除，因为不再需要后端渲染
            byte[] pdfBytes = new byte[0]; // 返回空数组，实际应用中可能需要返回错误信息
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "resume.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 为前端模板生成PDF
     * @param id 模板ID
     * @return PDF文件的字节数组响应
     */
    @GetMapping("/{id}/generate-pdf-from-frontend")
    public ResponseEntity<byte[]> generatePdfFromFrontendTemplate(@PathVariable String id) {
        try {
            // 为前端硬编码模板生成PDF
            // 创建一个基本的HTML结构，包含前端模板的样式和内容
            String basicHtml = "<!DOCTYPE html>"
                    + "<html lang=\"zh-CN\">"
                    + "<head>"
                    + "<meta charset=\"UTF-8\">"
                    + "<title>个人简历</title>"
                    + "<style>"
                    + "body { font-family: 'Microsoft YaHei', SimSun, Arial, sans-serif; margin: 0; padding: 0; background: #fff; }"
                    + ".template-one-container { width: 210mm; min-height: 297mm; display: flex; background: #fff; padding: 20px; box-sizing: border-box; }"
                    + ".left-panel { width: 60mm; background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; padding: 20px; box-sizing: border-box; }"
                    + ".right-panel { flex: 1; padding: 20px; box-sizing: border-box; }"
                    + ".profile-section { text-align: center; margin-bottom: 20px; }"
                    + ".avatar-frame { width: 100px; height: 100px; border-radius: 50%; overflow: hidden; margin: 0 auto 10px; border: 3px solid rgba(255,255,255,0.8); }"
                    + ".avatar { width: 100%; height: 100%; object-fit: cover; }"
                    + ".name { font-size: 20px; font-weight: bold; margin-bottom: 5px; }"
                    + ".title { font-size: 14px; opacity: 0.9; margin-bottom: 10px; }"
                    + ".contact-section { margin-bottom: 20px; }"
                    + ".section-title { font-size: 14px; font-weight: bold; margin-bottom: 10px; padding-bottom: 5px; border-bottom: 1px solid rgba(255,255,255,0.3); }"
                    + ".contact-item { font-size: 12px; margin-bottom: 8px; display: flex; align-items: center; }"
                    + ".skill-section { margin-bottom: 20px; }"
                    + ".skill-item { margin-bottom: 10px; }"
                    + ".skill-name { font-size: 12px; margin-bottom: 3px; }"
                    + ".skill-bar { height: 6px; background: rgba(255,255,255,0.2); border-radius: 3px; overflow: hidden; }"
                    + ".skill-fill { height: 100%; background: white; }"
                    + ".section { margin-bottom: 20px; }"
                    + ".section-header { font-size: 16px; font-weight: bold; color: #333; margin-bottom: 15px; padding-bottom: 5px; border-bottom: 2px solid #4facfe; }"
                    + ".experience-item { margin-bottom: 15px; }"
                    + ".experience-header { font-weight: bold; margin-bottom: 5px; }"
                    + ".experience-date { color: #666; font-size: 12px; margin-bottom: 5px; }"
                    + ".experience-description { font-size: 12px; line-height: 1.5; color: #555; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class=\"template-one-container\">"
                    + "<div class=\"left-panel\">"
                    + "<div class=\"profile-section\">"
                    + "<div class=\"avatar-frame\">"
                    + "<img class=\"avatar\" src=\"https://via.placeholder.com/100\" alt=\"头像\" />"
                    + "</div>"
                    + "<div class=\"name\">张三</div>"
                    + "<div class=\"title\">前端工程师</div>"
                    + "</div>"
                    + "<div class=\"contact-section\">"
                    + "<div class=\"section-title\">联系方式</div>"
                    + "<div class=\"contact-item\">电话: 138-0000-0000</div>"
                    + "<div class=\"contact-item\">邮箱: example@domain.com</div>"
                    + "<div class=\"contact-item\">地址: 北京市朝阳区</div>"
                    + "</div>"
                    + "<div class=\"skill-section\">"
                    + "<div class=\"section-title\">技能</div>"
                    + "<div class=\"skill-item\">"
                    + "<div class=\"skill-name\">HTML/CSS</div>"
                    + "<div class=\"skill-bar\"><div class=\"skill-fill\" style=\"width:90%;\"></div></div>"
                    + "</div>"
                    + "<div class=\"skill-item\">"
                    + "<div class=\"skill-name\">JavaScript</div>"
                    + "<div class=\"skill-bar\"><div class=\"skill-fill\" style=\"width:85%;\"></div></div>"
                    + "</div>"
                    + "<div class=\"skill-item\">"
                    + "<div class=\"skill-name\">React</div>"
                    + "<div class=\"skill-bar\"><div class=\"skill-fill\" style=\"width:80%;\"></div></div>"
                    + "</div>"
                    + "<div class=\"skill-item\">"
                    + "<div class=\"skill-name\">Sketch</div>"
                    + "<div class=\"skill-bar\"><div class=\"skill-fill\" style=\"width:85%;\"></div></div>"
                    + "</div>"
                    + "<div class=\"skill-item\">"
                    + "<div class=\"skill-name\">Figma</div>"
                    + "<div class=\"skill-bar\"><div class=\"skill-fill\" style=\"width:80%;\"></div></div>"
                    + "</div>"
                    + "</div>"
                    + "</div>"
                    + "<div class=\"right-panel\">"
                    + "<div class=\"section\">"
                    + "<div class=\"section-header\">个人简介</div>"
                    + "<p>具有5年前端开发经验，精通HTML5、CSS3、JavaScript，熟练使用React、Vue等主流前端框架，有丰富的移动端开发经验。</p>"
                    + "</div>"
                    + "<div class=\"section\">"
                    + "<div class=\"section-header\">工作经历</div>"
                    + "<div class=\"experience-item\">"
                    + "<div class=\"experience-header\">高级前端工程师 - ABC科技有限公司</div>"
                    + "<div class=\"experience-date\">2020年3月 - 至今</div>"
                    + "<div class=\"experience-description\">负责公司核心产品的前端架构设计和开发，优化前端性能，提升用户体验。</div>"
                    + "</div>"
                    + "<div class=\"experience-item\">"
                    + "<div class=\"experience-header\">前端工程师 - XYZ互联网公司</div>"
                    + "<div class=\"experience-date\">2018年1月 - 2020年2月</div>"
                    + "<div class=\"experience-description\">参与多个Web项目的前端开发，编写高质量的代码，确保项目按时交付。</div>"
                    + "</div>"
                    + "</div>"
                    + "<div class=\"section\">"
                    + "<div class=\"section-header\">教育背景</div>"
                    + "<div class=\"experience-item\">"
                    + "<div class=\"experience-header\">计算机科学与技术 - 北京大学</div>"
                    + "<div class=\"experience-date\">2014年9月 - 2018年6月</div>"
                    + "<div class=\"experience-description\">本科，GPA 3.8/4.0</div>"
                    + "</div>"
                    + "</div>"
                    + "<div class=\"section\">"
                    + "<div class=\"section-header\">项目经验</div>"
                    + "<div class=\"experience-item\">"
                    + "<div class=\"experience-header\">企业管理系统重构</div>"
                    + "<div class=\"experience-date\">2021年1月 - 2021年6月</div>"
                    + "<div class=\"experience-description\">使用React+TypeScript重构企业管理系统，提升系统性能和用户体验。</div>"
                    + "</div>"
                    + "</div>"
                    + "</div>"
                    + "</div>"
                    + "</body>"
                    + "</html>";
            
            // HTML转PDF功能已移除，因为不再需要后端渲染
            byte[] pdfBytes = new byte[0]; // 返回空数组，实际应用中可能需要返回错误信息
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "resume.pdf");
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}