package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @PostMapping("/{resumeId}/optimize")
    public Resume optimizeResume(@RequestParam Long userId, @PathVariable Long resumeId, @RequestParam(required = false) String targetJob) {
        return resumeService.optimizeResume(userId, resumeId, targetJob);
    }

    /**
     * 获取用户简历列表
     * @param userId 用户ID
     * @return 简历列表
     */
    @GetMapping("/user/{userId}")
    public List<Resume> getUserResumeList(@PathVariable Long userId) {
        return resumeService.getUserResumeList(userId);
    }

    /**
     * 根据ID获取简历
     * @param id 简历ID
     * @return 简历信息
     */
    @GetMapping("/{id}")
    public Resume getResumeById(@PathVariable Long id) {
        return resumeService.getResumeById(id);
    }

    /**
     * 删除简历
     * @param id 简历ID
     * @param userId 用户ID
     */
    @DeleteMapping("/{id}")
    public boolean deleteResume(@RequestParam Long userId, @PathVariable Long id) {
        return resumeService.deleteResume(userId, id);
    }

    /**
     * 导出为PDF
     * @param resumeId 简历ID
     * @param templateId 模板ID（可选）
     * @return PDF下载链接
     */
    @GetMapping("/export/pdf")
    public byte[] exportToPdf(@RequestParam Long resumeId) {
        return resumeService.exportResumeToPdf(resumeId);
    }

    /**
     * 导出为Word
     * @param resumeId 简历ID
     * @param templateId 模板ID（可选）
     * @return Word下载链接
     */
    @GetMapping("/export/word")
    public byte[] exportToWord(@RequestParam Long resumeId) {
        return resumeService.exportResumeToWord(resumeId);
    }

    /**
     * 获取简历AI评分
     * @param resumeId 简历ID
     * @return 评分结果
     */
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
    @GetMapping("/{resumeId}/ai-suggestion")
    public Map<String, Object> getResumeAiSuggestions(@PathVariable Long resumeId) {
        String suggestions = resumeService.getResumeAiSuggestions(resumeId);
        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", suggestions);
        return result;
    }
}