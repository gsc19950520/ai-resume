package com.aicv.airesume.controller;

import com.aicv.airesume.annotation.Log;
import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.model.dto.CreateResumeDTO;
import com.aicv.airesume.model.dto.EducationDTO;
import com.aicv.airesume.model.dto.PersonalInfoDTO;
import com.aicv.airesume.model.dto.ProjectDTO;
import com.aicv.airesume.model.dto.ResumeDataDTO;
import com.aicv.airesume.model.dto.SkillWithLevelDTO;
import com.aicv.airesume.model.dto.UpdateResumeDTO;
import com.aicv.airesume.model.dto.WorkExperienceDTO;
import com.aicv.airesume.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.Valid;


/**
 * 简历控制器
 * 优化后的控制器，合并了冗余接口，提供统一的API入口
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
     * @param targetJob 目标职位（可选）
     * @return 优化后的简历信息
     */
    @Log(description = "AI优化简历", recordParams = true, recordResult = true)
    @PostMapping("/{resumeId}/optimize")
    public Resume optimizeResume(@RequestParam Long userId, @PathVariable Long resumeId, @RequestParam(required = false) String targetJob) {
        return resumeService.optimizeResume(userId, resumeId, targetJob);
    }

    /**
     * 获取用户简历列表
     * 统一接口，使用查询参数传递userId
     * @param userId 用户ID（查询参数）
     * @return 简历列表，统一包装在Map中返回
     */
    @Log(description = "获取用户简历列表", recordParams = true, recordResult = false)
    @GetMapping("/user")
    public Map<String, Object> getUserResumeList(@RequestParam Long userId) {
        List<Resume> resumeList = resumeService.getResumeListByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", resumeList);
        return result;
    }

    /**
     * 获取简历详情
     * 统一接口，合并了基础信息和完整数据的获取
     * @param resumeId 简历ID
     * @param fullData 是否返回完整数据（默认false）
     * @return 简历信息
     */
    @Log(description = "获取简历详情", recordParams = true, recordResult = true)
    @GetMapping("/{resumeId}")
    public Object getResume(@PathVariable Long resumeId, @RequestParam(required = false, defaultValue = "false") boolean fullData) {
        if (fullData) {
            // 返回完整数据，包含所有关联信息
            return resumeService.getResumeFullData(resumeId);
        } else {
            // 返回基础简历信息
            return resumeService.getResumeById(resumeId);
        }
    }

    /**
     * 删除简历
     * @param resumeId 简历ID
     * @param userId 用户ID
     */
    @Log(description = "删除简历", recordParams = true, recordResult = true)
    @DeleteMapping("/{resumeId}")
    public boolean deleteResume(@RequestParam Long userId, @PathVariable Long resumeId) {
        return resumeService.deleteResume(userId, resumeId);
    }

    /**
     * 导出为PDF
     * 统一接口，合并了默认模板和指定模板的导出功能
     * @param resumeId 简历ID
     * @param templateId 模板ID（可选）
     * @param response HTTP响应对象
     */
    @Log(description = "导出简历为PDF", recordParams = true, recordResult = false, recordExecutionTime = true)
    @GetMapping("/export/pdf")
    public void exportToPdf(@RequestParam Long resumeId, 
                           @RequestParam(required = false) String templateId, 
                           HttpServletResponse response) {
        try {
            byte[] pdfBytes;
            String fileName;
            
            // 根据是否提供模板ID选择不同的导出方法
            if (templateId != null && !templateId.isEmpty()) {
                pdfBytes = resumeService.exportResumeToPdf(resumeId, templateId);
                fileName = "resume_" + resumeId + "_" + templateId + ".pdf";
            } else {
                throw new RuntimeException("templateId不可为空");
            }
            
            // 设置响应头
            response.setContentType("application/pdf");
            response.setContentLength(pdfBytes.length);
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
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
     * 导出为Word
     * @param resumeId 简历ID
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
     * 创建新简历（结构化格式）
     * @param userId 用户ID
     * @param resumeData 简历数据（包含完整的结构化信息）
     * @return 创建结果
     */
    @Log(description = "创建新简历（结构化）", recordParams = true, recordResult = true)
    @PostMapping("/")
    public ResponseEntity<?> createResume(@Valid @RequestBody CreateResumeDTO createResumeDTO) {
        try {
            // 从DTO中提取用户ID和简历数据
            Long userId = createResumeDTO.getUserId();
            ResumeDataDTO resumeDataDTO = createResumeDTO.getResumeData();
            
            // 直接传递DTO对象给服务层，不再转换为Map
            Resume resume = resumeService.createResumeWithFullData(userId, resumeDataDTO);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", resume);
            result.put("message", "简历创建成功");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("code", 500);
            errorResponse.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 更新简历内容（结构化格式）
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param resumeData 简历数据（包含完整的结构化信息）
     * @return 更新结果
     */
    @Log(description = "更新简历内容（结构化）", recordParams = true, recordResult = true)
    @PutMapping("/{resumeId}")
    public ResponseEntity<?> updateResume(@PathVariable Long resumeId, 
                                         @Valid @RequestBody UpdateResumeDTO updateResumeDTO) {
        try {
            // 参数验证
            if (resumeId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("code", 400);
                errorResponse.put("message", "简历ID不能为空");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 验证用户权限
            if (!resumeService.checkResumePermission(updateResumeDTO.getUserId(), resumeId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("code", 403);
                errorResponse.put("message", "无权限操作该简历");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }
            
            // 更新现有简历
            Resume updatedResume = resumeService.updateResumeWithFullData(resumeId, updateResumeDTO.getData());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", updatedResume);
            result.put("message", "简历更新成功");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("code", 500);
            errorResponse.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 获取用户最新的简历数据
     * @param userId 用户ID
     * @return 最新简历的完整数据
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
}