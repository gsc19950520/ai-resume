package com.aicv.airesume.service;

import com.aicv.airesume.entity.Resume;
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
     * @param userId 用户ID
     * @param name 简历名称
     * @param file 简历文件
     * @return 简历信息
     */
    Resume uploadResume(Long userId, String name, MultipartFile file);
    
    /**
     * 批量上传简历
     * @param userId 用户ID
     * @param files 简历文件列表
     * @return 简历信息列表
     */
    List<Resume> batchUploadResume(Long userId, List<MultipartFile> files);
    
    /**
     * AI优化简历
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param targetJob 目标职位（可选）
     * @return 优化后的简历信息
     */
    Resume optimizeResume(Long userId, Long resumeId, String targetJob);
    
    /**
     * 获取用户简历列表
     * @param userId 用户ID
     * @return 简历列表
     */
    List<Resume> getUserResumeList(Long userId);
    
    /**
     * 根据ID获取简历
     * @param resumeId 简历ID
     * @return 简历信息
     */
    Resume getResumeById(Long resumeId);
    
    /**
     * 获取简历完整数据（包含所有关联信息）
     * @param resumeId 简历ID
     * @return 完整的简历数据，包含个人信息、联系方式、教育经历、工作经历、项目经历和技能
     */
    Map<String, Object> getResumeFullData(Long resumeId);
    
    /**
     * 删除简历
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @return 是否删除成功
     */
    boolean deleteResume(Long userId, Long resumeId);
    
    /**
     * 导出简历为PDF
     * @param resumeId 简历ID
     * @param templateId 模板ID（可选）
     * @return PDF字节数组
     * 注意：如果templateId为空，使用默认模板
     */
    byte[] exportResumeToPdf(Long resumeId, String templateId);
    
    /**
     * 导出简历为Word
     * @param resumeId 简历ID
     * @return Word字节数组
     */
    byte[] exportResumeToWord(Long resumeId);
    
    /**
     * 获取简历AI评分
     * @param resumeId 简历ID
     * @return 评分结果
     */
    Integer getResumeAiScore(Long resumeId);
    
    /**
     * 获取简历AI优化建议
     * @param resumeId 简历ID
     * @return 优化建议
     */
    String getResumeAiSuggestions(Long resumeId);
    
    /**
     * 设置简历模板
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param templateId 模板ID
     * @return 更新后的简历信息
     */
    Resume setResumeTemplate(Long userId, Long resumeId, String templateId);
    
    /**
     * 设置简历模板配置
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param templateConfig 模板配置信息（JSON格式）
     * @return 更新后的简历信息
     */
    Resume setResumeTemplateConfig(Long userId, Long resumeId, String templateConfig);
    
    /**
     * 更新简历内容
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param resumeData 简历数据，包含所有需要更新的字段
     * @return 更新后的简历信息
     */
    Resume updateResumeContent(Long userId, Long resumeId, Map<String, Object> resumeData);
    
    /**
     * 获取用户最新的简历数据
     * @param userId 用户ID
     * @return 最新简历的完整数据
     */
    Map<String, Object> getLatestResumeData(Long userId);
    
    /**
     * 创建包含完整结构化数据的新简历
     * @param userId 用户ID
     * @param resumeData 包含完整结构化数据的简历信息
     * @return 创建的简历信息
     */
    Resume createResumeWithFullData(Long userId, Map<String, Object> resumeData);
    
    /**
     * 检查用户对简历的操作权限
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @return 是否有权限操作
     */
    boolean checkResumePermission(Long userId, Long resumeId);
    
    /**
     * 更新简历完整数据
     * @param resumeId 简历ID
     * @param resumeData 包含完整结构化数据的简历信息
     * @return 更新后的简历信息
     */
    Resume updateResumeWithFullData(Long resumeId, Map<String, Object> resumeData);
}