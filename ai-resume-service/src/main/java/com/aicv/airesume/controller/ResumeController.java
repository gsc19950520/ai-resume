package com.aicv.airesume.controller;

import com.aicv.airesume.annotation.Log;
import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历控制器
 */
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    /**
     * 上传简历
     * @param userId 用户ID
     * @param file 简历文件
     * @return 简历信息
     */
    @Log(description = "用户上传简历文件", recordParams = true, recordResult = true)
    @PostMapping("/upload")
    public Resume uploadResume(@RequestParam Long userId, @RequestParam String name, @RequestParam MultipartFile file) {
        return resumeService.uploadResume(userId, name, file);
    }

    /**
     * 批量上传简历
     * @param userId 用户ID
     * @param files 简历文件列表
     * @return 简历信息列表
     */
    @Log(description = "用户批量上传简历文件", recordParams = true, recordResult = false)
    @PostMapping("/batch-upload")
    public List<Resume> batchUploadResumes(@RequestParam Long userId, @RequestParam List<MultipartFile> files) {
        return resumeService.batchUploadResume(userId, files);
    }

    /**
     * AI优化简历
     * @param resumeId 简历ID
     * @param jobDescription 职位描述（可选）
     * @return 优化后的简历信息
     */
    @Log(description = "AI优化简历", recordParams = true, recordResult = true)
    @PostMapping("/{resumeId}/optimize")
    public Resume optimizeResume(@RequestParam Long userId, @PathVariable Long resumeId, @RequestParam(required = false) String targetJob) {
        return resumeService.optimizeResume(userId, resumeId, targetJob);
    }

    /**
     * 获取用户简历列表
     * @param userId 用户ID
     * @return 简历列表
     */
    @Log(description = "获取用户简历列表", recordParams = true, recordResult = false)
    @GetMapping("/user/{userId}")
    public List<Resume> getUserResumeList(@PathVariable Long userId) {
        return resumeService.getUserResumeList(userId);
    }
    
    /**
     * 获取用户简历列表（支持查询参数形式）
     * 用于兼容前端调用格式
     * @param userId 用户ID
     * @return 简历列表
     */
    @Log(description = "获取用户简历列表（查询参数形式）", recordParams = true, recordResult = false)
    @GetMapping("/user-resumes")
    public Map<String, Object> getUserResumes(@RequestParam Long userId) {
        List<Resume> resumeList = resumeService.getUserResumeList(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", resumeList);
        return result;
    }

    /**
     * 根据ID获取简历
     * @param id 简历ID
     * @return 简历信息
     */
    @Log(description = "获取简历详情", recordParams = true, recordResult = true)
    @GetMapping("/{id}")
    public Resume getResumeById(@PathVariable Long id) {
        return resumeService.getResumeById(id);
    }
    
    /**
     * 获取简历完整数据（包含所有关联信息）
     * @param resumeId 简历ID
     * @return 完整的简历数据，包含个人信息、联系方式、教育经历、工作经历、项目经历和技能
     * 注意：个人信息和联系方式现在从User表中获取，而不是存储在Resume表中
     */
    @Log(description = "获取简历完整数据", recordParams = true, recordResult = true)
    @GetMapping("/{resumeId}/full-data")
    public Map<String, Object> getResumeFullData(@PathVariable Long resumeId) {
        return resumeService.getResumeFullData(resumeId);
    }

    /**
     * 删除简历
     * @param id 简历ID
     * @param userId 用户ID
     */
    @Log(description = "删除简历", recordParams = true, recordResult = true)
    @DeleteMapping("/{id}")
    public boolean deleteResume(@RequestParam Long userId, @PathVariable Long id) {
        return resumeService.deleteResume(userId, id);
    }

    /**
     * 导出为PDF
     * @param resumeId 简历ID
     * @param response HTTP响应对象
     */
    @Log(description = "导出简历为PDF", recordParams = true, recordResult = false, recordExecutionTime = true)
    @GetMapping("/export/pdf")
    public void exportToPdf(@RequestParam Long resumeId, HttpServletResponse response) {
        try {
            // 默认使用第一个模板
            byte[] pdfBytes = resumeService.exportResumeToPdf(resumeId);
            
            // 设置响应头
            response.setContentType("application/pdf");
            response.setContentLength(pdfBytes.length);
            response.setHeader("Content-Disposition", "attachment; filename=resume_" + resumeId + ".pdf");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            // 写入响应体
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(pdfBytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("导出PDF失败", e);
        }
    }
    
    /**
     * 根据模板导出为PDF
     * @param resumeId 简历ID
     * @param templateId 模板ID
     * @param response HTTP响应对象
     */
    @Log(description = "根据模板导出简历为PDF", recordParams = true, recordResult = false, recordExecutionTime = true)
    @GetMapping("/template/{templateId}/generate-pdf")
    public void generatePdfByTemplate(@RequestParam Long resumeId, @PathVariable String templateId, HttpServletResponse response) {
        try {
            byte[] pdfBytes = resumeService.exportResumeToPdf(resumeId, templateId);
            
            // 设置响应头
            response.setContentType("application/pdf");
            response.setContentLength(pdfBytes.length);
            response.setHeader("Content-Disposition", "attachment; filename=resume_" + resumeId + "_" + templateId + ".pdf");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            // 写入响应体
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(pdfBytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("根据模板导出PDF失败", e);
        }
    }

    /**
     * 导出为Word
     * @param resumeId 简历ID
     * @param templateId 模板ID（可选）
     * @return Word下载链接
     */
    @Log(description = "导出简历为Word", recordParams = true, recordResult = false, recordExecutionTime = true)
    @GetMapping("/export/word")
    public byte[] exportToWord(@RequestParam Long resumeId) {
        return resumeService.exportResumeToWord(resumeId);
    }

    /**
     * 获取简历AI评分
     * @param resumeId 简历ID
     * @return 评分结果
     */
    @Log(description = "获取简历AI评分", recordParams = true, recordResult = true)
    @GetMapping("/{resumeId}/ai-score")
    public Map<String, Object> getResumeAiScore(@PathVariable Long resumeId) {
        Integer score = resumeService.getResumeAiScore(resumeId);
        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        return result;
    }

    /**
     * 获取简历AI优化建议
     * @param resumeId 简历ID
     * @return 优化建议
     */
    @Log(description = "获取简历AI优化建议", recordParams = true, recordResult = true)
    @GetMapping("/{resumeId}/ai-suggestion")
    public Map<String, Object> getResumeAiSuggestions(@PathVariable Long resumeId) {
        String suggestions = resumeService.getResumeAiSuggestions(resumeId);
        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", suggestions);
        return result;
    }
    
    /**
     * 设置简历模板
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param templateId 模板ID
     * @return 更新后的简历信息
     */
    @Log(description = "设置简历模板", recordParams = true, recordResult = true)
    @PostMapping("/{resumeId}/template")
    public Resume setResumeTemplate(@RequestParam Long userId, @PathVariable Long resumeId, @RequestParam String templateId) {
        return resumeService.setResumeTemplate(userId, resumeId, templateId);
    }
    
    /**
     * 设置简历模板配置
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param templateConfig 模板配置信息（JSON格式）
     * @return 更新后的简历信息
     */
    @Log(description = "设置简历模板配置", recordParams = true, recordResult = true)
    @PostMapping("/{resumeId}/template-config")
    public Resume setResumeTemplateConfig(@RequestParam Long userId, @PathVariable Long resumeId, @RequestBody String templateConfig) {
        return resumeService.setResumeTemplateConfig(userId, resumeId, templateConfig);
    }
    
    /**
     * 更新简历内容（结构化格式）
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param resumeData 简历数据（包含完整的结构化信息）
     * @return 更新结果
     * 注意：个人信息和联系方式现在从User表中获取，不再通过此接口更新
     * 此接口仅更新Resume表中可修改的字段：期望薪资、到岗时间、职位名称、jobTypeId等
     */
    @Log(description = "更新简历内容（结构化）", recordParams = true, recordResult = true)
    @PostMapping("/{resumeId}/structured-content")
    public ResponseEntity<?> updateResumeStructuredContent(@RequestParam Long userId, @PathVariable Long resumeId, @RequestBody Map<String, Object> resumeData) {
        try {
            Resume updatedResume = resumeService.updateResumeContent(userId, resumeId, resumeData);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", updatedResume);
            result.put("message", "简历内容更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 更新简历内容（兼容格式）
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param resumeData 简历数据，包含所有需要更新的字段
     * @return 更新后的简历信息
     * 注意：个人信息和联系方式现在从User表中获取，不再通过此接口更新
     * 此接口仅更新Resume表中可修改的字段：期望薪资、到岗时间、职位名称、jobTypeId等
     */
    @Log(description = "更新简历内容", recordParams = true, recordResult = true)
    @PostMapping("/{resumeId}/content")
    public ResponseEntity<?> updateResumeContent(@RequestParam Long userId, @PathVariable Long resumeId, @RequestBody Map<String, Object> resumeData) {
        try {
            // 参数验证
            if (resumeData == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 400);
                errorResponse.put("message", "简历数据不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 更新简历内容
            Resume updatedResume = resumeService.updateResumeContent(userId, resumeId, resumeData);
            
            // 构造返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updatedResume);
            response.put("message", "简历内容更新成功");
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "更新简历内容失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 创建新简历
     */
    @PostMapping("/create")
    public ResponseEntity<Resume> createResume(@RequestParam Long userId, @RequestBody Map<String, Object> resumeData) {
        Resume createdResume = resumeService.createResume(userId, resumeData);
        return ResponseEntity.ok(createdResume);
    }
    
    /**
     * 获取用户最新的简历数据
     */
    @GetMapping("/getLatest")
    public ResponseEntity<Map<String, Object>> getLatestResumeData(@RequestParam Long userId) {
        Map<String, Object> latestResumeData = resumeService.getLatestResumeData(userId);
        if (latestResumeData != null) {
            return ResponseEntity.ok(latestResumeData);
        } else {
            return ResponseEntity.noContent().build();
        }
    }
    
    /**
     * 保存或更新简历内容（结构化格式）
     * 如果简历不存在，则创建新简历
     * @param userId 用户ID
     * @param resumeId 简历ID（可选，如果不存在则创建新简历）
     * @param resumeData 简历数据（包含完整的结构化信息）
     * @return 保存或更新结果
     */
    @Log(description = "保存或更新简历内容（结构化）", recordParams = true, recordResult = true)
    @PostMapping("/new/structured-content")
    public ResponseEntity<?> saveOrUpdateResumeStructuredContent(@RequestParam Long userId, @RequestParam(required = false) Long resumeId, @RequestBody Map<String, Object> resumeData) {
        try {
            // 如果简历ID存在，则更新现有简历
            // 如果简历ID不存在，则创建新简历
            Resume resume;
            if (resumeId != null) {
                resume = resumeService.updateResumeContent(userId, resumeId, resumeData);
            } else {
                // 创建新简历，传入完整的结构化数据
                resume = resumeService.createResumeWithFullData(userId, resumeData);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", resume);
            result.put("message", resumeId != null ? "简历内容更新成功" : "简历创建成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}