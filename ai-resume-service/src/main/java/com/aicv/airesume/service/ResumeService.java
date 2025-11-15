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
     * 获取简历完整数据（包含所有关联信息）
     */
    Map<String, Object> getResumeFullData(Long resumeId);
    
    /**
     * 删除简历
     */
    boolean deleteResume(Long userId, Long resumeId);
    
    /**
     * 导出简历为PDF
     */
    byte[] exportResumeToPdf(Long resumeId);
    
    /**
     * 根据指定模板导出简历为PDF
     * @param resumeId 简历ID
     * @param templateId 模板ID
     * @return PDF字节数组
     */
    byte[] exportResumeToPdf(Long resumeId, String templateId);
    
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
    
    /**
     * 更新简历内容
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param resumeData 简历数据Map，包含所有需要更新的字段
     * @return 更新后的简历对象
     */
    Resume updateResumeContent(Long userId, Long resumeId, Map<String, Object> resumeData);
    
    /**
     * 创建新简历
     * @param userId 用户ID
     * @param resumeData 简历数据Map，包含所有需要设置的字段
     * @return 创建后的简历对象
     */
    Resume createResume(Long userId, Map<String, Object> resumeData);
    
    /**
     * 获取用户最新的简历数据
     * @param userId 用户ID
     * @return 最新简历的完整数据，如果用户没有简历则返回null
     */
    Map<String, Object> getLatestResumeData(Long userId);
    
    /**
     * 创建新简历并保存完整的结构化数据
     * @param userId 用户ID
     * @param resumeData 包含完整结构化信息的简历数据Map
     * @return 创建后的简历对象
     */
    Resume createResumeWithFullData(Long userId, Map<String, Object> resumeData);
}