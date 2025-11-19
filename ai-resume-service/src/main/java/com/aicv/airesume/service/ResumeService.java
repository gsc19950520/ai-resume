package com.aicv.airesume.service;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.model.dto.ResumeDataDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 简历服务接口
 * 优化后的接口，移除了冗余方法，提供统一的服务API
 */
public interface ResumeService {
    
    /**
     * 上传简历
     */
    Resume uploadResume(Long userId, String fileName, MultipartFile file);
    
    Resume setResumeTemplate(Long userId, Long resumeId, String templateId);

    /**
     * 批量上传简历
     */
    List<Resume> batchUploadResume(Long userId, List<MultipartFile> files);
    
    /**
     * 根据用户ID获取简历列表
     */
    List<Resume> getResumeListByUserId(Long userId);
    
    /**
     * 根据简历ID获取简历详情
     */
    Resume getResumeById(Long resumeId);
    
    /**
     * 获取简历完整数据
     */
    Map<String, Object> getResumeFullData(Long resumeId);

    
    /**
     * 创建简历（使用DTO）
     */
    Resume createResumeWithFullData(Long userId, ResumeDataDTO resumeDataDTO);
    
    /**
     * 更新简历（使用DTO）
     */
    Resume updateResumeWithFullData(Long resumeId, ResumeDataDTO resumeDataDTO);
    
    /**
     * 删除简历
     */
    boolean deleteResume(Long userId, Long resumeId);
    
    /**
     * 导出简历为Word
     */
    byte[] exportResumeToWord(Long resumeId);
    
    /**
     * 导出简历为PDF
     */
    byte[] exportResumeToPdf(Long resumeId, String templateId);
    
    /**
     * 优化简历
     */
    Resume optimizeResume(Long userId, Long resumeId, String targetJob);
    
    /**
     * 获取简历AI评分
     */
    Map<String, Object> getResumeAiScore(Long resumeId);
    
    /**
     * 获取简历AI建议
     */
    Map<String, Object> getResumeAiSuggestions(Long resumeId);
    
    /**
     * 检查简历权限
     */
    boolean checkResumePermission(Long userId, Long resumeId);
    
    /**
     * 获取用户最新简历数据
     */
    Map<String, Object> getLatestResumeData(Long userId);
    
}