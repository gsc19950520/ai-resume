package com.aicv.airesume.service;

import com.aicv.airesume.entity.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 简历服务接口
 */
public interface ResumeService {
    
    /**
     * 上传简历
     */
    Resume uploadResume(Long userId, String name, MultipartFile file);
    
    /**
     * 批量上传简历
     */
    List<Resume> batchUploadResume(Long userId, List<MultipartFile> files);
    
    /**
     * 优化简历
     */
    Resume optimizeResume(Long userId, Long resumeId, String targetJob);
    
    /**
     * 批量优化简历
     */
    List<Resume> batchOptimizeResume(Long userId, List<Long> resumeIds, String targetJob);
    
    /**
     * 获取用户简历列表
     */
    List<Resume> getUserResumeList(Long userId);
    
    /**
     * 根据ID获取简历
     */
    Resume getResumeById(Long id);
    
    /**
     * 删除简历
     */
    boolean deleteResume(Long userId, Long resumeId);
    
    /**
     * 导出简历为PDF
     */
    byte[] exportResumeToPdf(Long resumeId);
    
    /**
     * 导出简历为Word
     */
    byte[] exportResumeToWord(Long resumeId);
    
    /**
     * 获取简历AI评分
     */
    Integer getResumeAiScore(Long resumeId);
    
    /**
     * 获取简历AI建议
     */
    String getResumeAiSuggestions(Long resumeId);
    
    /**
     * 检查简历权限
     */
    boolean checkResumePermission(Long userId, Long resumeId);
    
    /**
     * 设置简历模板
     */
    Resume setResumeTemplate(Long userId, Long resumeId, String templateId);
    
    /**
     * 设置简历模板配置
     */
    Resume setResumeTemplateConfig(Long userId, Long resumeId, String templateConfig);
}