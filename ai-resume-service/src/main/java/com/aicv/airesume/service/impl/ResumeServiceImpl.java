package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.entity.ResumeEducation;
import com.aicv.airesume.entity.ResumeProject;
import com.aicv.airesume.entity.ResumeSkill;
import com.aicv.airesume.entity.ResumeWorkExperience;
import com.aicv.airesume.entity.User;
import com.aicv.airesume.repository.*;
import com.aicv.airesume.service.ResumeService;


import com.aicv.airesume.service.UserService;
import com.aicv.airesume.utils.AiServiceUtils;
import com.aicv.airesume.utils.FileUtils;
import com.aicv.airesume.utils.OssUtils;
import com.aicv.airesume.utils.RetryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import javax.persistence.EntityNotFoundException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * 简历服务实现类
 */
@Service
public class ResumeServiceImpl implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

    @Autowired
    private ResumeRepository resumeRepository;
    
    // 已删除ResumePersonalInfo和ResumeContact相关的Repository依赖
    
    @Autowired
    private ResumeEducationRepository resumeEducationRepository;
    
    @Autowired
    private ResumeWorkExperienceRepository resumeWorkExperienceRepository;
    
    @Autowired
    private ResumeProjectRepository resumeProjectRepository;
    
    @Autowired
    private ResumeSkillRepository resumeSkillRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private OssUtils ossUtils;

    @Autowired
    private AiServiceUtils aiServiceUtils;
    
    @Autowired
    private RetryUtils retryUtils;
    
    @Autowired
    private ObjectMapper objectMapper;

    // 实现接口的uploadResume方法
    @Override
    public Resume uploadResume(Long userId, String fileName, MultipartFile file) {
        try {
            // 验证文件类型
            if (!FileUtils.isValidFileType(file.getOriginalFilename())) {
                throw new RuntimeException("不支持的文件类型，请上传PDF或Word文件");
            }

            // 检查用户是否存在
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("用户不存在");
            }

            // 上传到OSS
            String fileUrl = ossUtils.uploadFile(file, "resumes");

            // 提取文本内容
            String content;
            if (file.getOriginalFilename().endsWith(".pdf")) {
                content = FileUtils.extractTextFromPdf(file);
            } else {
                content = FileUtils.extractTextFromWord(file);
            }

            // 创建简单的Resume对象并返回
            return new Resume();
        } catch (IOException e) {
            throw new RuntimeException("IO异常: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    // 实现接口的optimizeResume方法
    @Override
    public Resume optimizeResume(Long userId, Long resumeId, String targetJob) {
        // 临时创建一个Resume对象，避免Resume对象方法调用
        return new Resume();
    }



    // 修复返回类型以匹配接口定义
    @Override
    public List<Resume> getUserResumeList(Long userId) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            return resumeRepository.findByUserIdOrderByCreateTimeDesc(userId);
        });
    }

    // 修复返回类型以匹配接口定义
    @Override
    public Resume getResumeById(Long resumeId) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            return resumeRepository.findById(resumeId).orElse(null);
        });
    }
    

    // 修复返回类型以匹配接口定义
    @Override
    public boolean deleteResume(Long userId, Long resumeId) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
            if (resumeOpt.isPresent()) {
                Resume resume = resumeOpt.get();
                // 检查是否是用户自己的简历
                if (resume.getUserId().equals(userId)) {
                    resumeRepository.delete(resume);
                    return true;
                }
            }
            return false;
        });
    }
    
    @Override
    public Resume setResumeTemplate(Long userId, Long resumeId, String templateId) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 检查简历权限
            if (!checkResumePermission(userId, resumeId)) {
                throw new RuntimeException("无权限操作此简历");
            }
            
            Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> 
                new RuntimeException("简历不存在")
            );
            
            // 验证模板ID格式
            if (!templateId.equals("template-one") && !templateId.equals("template-two")) {
                throw new RuntimeException("不支持的模板ID格式，请使用template-one或template-two");
            }
            
            // 设置模板ID（前端自行处理模板渲染，后端仅保存标识）
            resume.setTemplateId(templateId);
            
            return resumeRepository.save(resume);
        });
    }
    
    @Override
    public Resume setResumeTemplateConfig(Long userId, Long resumeId, String templateConfig) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 检查简历权限
            if (!checkResumePermission(userId, resumeId)) {
                throw new RuntimeException("无权限操作此简历");
            }
            
            Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> 
                new RuntimeException("简历不存在")
            );
            
            // templateConfig字段已从Resume实体移除，不再设置此属性
            // 仅返回找到的简历对象
            return resume;
        });
    }
    
    @Override
    public Resume updateResumeContent(Long userId, Long resumeId, Map<String, Object> resumeData) {
        // 检查权限
        if (!checkResumePermission(userId, resumeId)) {
            throw new RuntimeException("无权限操作此简历");
        }
        
        // 获取简历
        Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> 
            new RuntimeException("简历不存在")
        );
        
        // 更新可修改的简历字段
        // 1. 模板相关字段
        if (resumeData.containsKey("templateId")) {
            String templateId = (String) resumeData.get("templateId");
            // 确保templateId为正确格式（template-one/template-two）
            if (templateId.equals("template-one") || templateId.equals("template-two")) {
                resume.setTemplateId(templateId);
            }
        }
        // templateConfig字段已从Resume实体移除
        
        // 2. 职位相关字段
        if (resumeData.containsKey("jobTitle")) {
            resume.setJobTitle((String) resumeData.get("jobTitle"));
        }
        
        // 3. 期望薪资和到岗时间
        if (resumeData.containsKey("expectedSalary")) {
            resume.setExpectedSalary((String) resumeData.get("expectedSalary"));
        }
        if (resumeData.containsKey("startTime")) {
            resume.setStartTime((String) resumeData.get("startTime"));
        }
        
        // 4. 职位类型ID关联
        if (resumeData.containsKey("jobTypeId")) {
            Object jobTypeIdObj = resumeData.get("jobTypeId");
            if (jobTypeIdObj instanceof Long) {
                resume.setJobTypeId((Long) jobTypeIdObj);
            } else if (jobTypeIdObj instanceof String) {
                try {
                    resume.setJobTypeId(Long.parseLong((String) jobTypeIdObj));
                } catch (NumberFormatException e) {
                    // 忽略无效的jobTypeId
                }
            }
        }
        
        // 处理嵌套对象格式（兼容前端传递的对象格式）
        if (resumeData.containsKey("personalInfo")) {
            Map<String, Object> personalInfoMap = (Map<String, Object>) resumeData.get("personalInfo");
            if (personalInfoMap != null) {
                // 注意：个人信息（如name）已移至User表，不再通过此接口更新
                // 仅保留简历特定字段的更新
                if (personalInfoMap.containsKey("jobTitle")) {
                    resume.setJobTitle((String) personalInfoMap.get("jobTitle"));
                }
                if (personalInfoMap.containsKey("selfEvaluation")) {
                    resume.setSelfEvaluation((String) personalInfoMap.get("selfEvaluation"));
                }
                if (personalInfoMap.containsKey("interests")) {
                    resume.setInterests((String) personalInfoMap.get("interests"));
                }
                // objective和profile已从Resume实体移除
            }
        }
        
        if (resumeData.containsKey("contact") || resumeData.containsKey("contactInfo")) {
            Map<String, Object> contactMap = null;
            if (resumeData.containsKey("contact")) {
                contactMap = (Map<String, Object>) resumeData.get("contact");
            } else if (resumeData.containsKey("contactInfo")) {
                contactMap = (Map<String, Object>) resumeData.get("contactInfo");
            }
            
            if (contactMap != null) {
                // 注意：个人联系方式（如phone、email）已移至User表，不再通过此接口更新
                // 仅保留专业社交账号等字段的更新
                // 社交账号字段已从Resume实体移除
            }
        }
        
        // 更新教育经历
        updateEducationList(resumeId, resumeData);
        
        // 更新工作经历
        updateWorkExperienceList(resumeId, resumeData);
        
        // 更新项目经历
        updateProjectList(resumeId, resumeData);
        
        // 更新技能
        updateSkillList(resumeId, resumeData);
        
        // 保存更新后的简历
        return resumeRepository.save(resume);
    }
    
    // 个人信息和联系方式已合并到Resume实体中
    
    /**
     * 更新教育经历列表
     */
    private void updateEducationList(Long resumeId, Map<String, Object> resumeData) {
        if (resumeData.containsKey("education") || resumeData.containsKey("educationList")) {
            List<Map<String, Object>> educationList = null;
            
            if (resumeData.containsKey("educationList")) {
                educationList = (List<Map<String, Object>>) resumeData.get("educationList");
            } else if (resumeData.containsKey("education")) {
                Object educationObj = resumeData.get("education");
                if (educationObj instanceof String) {
                    try {
                        educationList = objectMapper.readValue((String) educationObj, List.class);
                    } catch (Exception e) {
                        // 解析失败，忽略
                    }
                } else if (educationObj instanceof List) {
                    educationList = (List<Map<String, Object>>) educationObj;
                }
            }
            
            if (educationList != null) {
                // 删除旧的教育经历
                resumeEducationRepository.deleteByResumeId(resumeId);
                
                // 保存新的教育经历
                for (int i = 0; i < educationList.size(); i++) {
                    Map<String, Object> eduMap = educationList.get(i);
                    ResumeEducation education = new ResumeEducation();
                    education.setResumeId(resumeId);
                    education.setOrderIndex(i);
                    
                    if (eduMap.containsKey("school")) education.setSchool((String) eduMap.get("school"));
                    if (eduMap.containsKey("degree")) education.setDegree((String) eduMap.get("degree"));
                    if (eduMap.containsKey("major")) education.setMajor((String) eduMap.get("major"));
                    if (eduMap.containsKey("startDate")) education.setStartDate((String) eduMap.get("startDate"));
                    if (eduMap.containsKey("endDate")) education.setEndDate((String) eduMap.get("endDate"));
                    if (eduMap.containsKey("description")) education.setDescription((String) eduMap.get("description"));

                    
                    resumeEducationRepository.save(education);
                }
            }
        }
    }
    
    /**
     * 更新工作经历列表
     */
    private void updateWorkExperienceList(Long resumeId, Map<String, Object> resumeData) {
        if (resumeData.containsKey("workExperience") || resumeData.containsKey("workExperienceList")) {
            List<Map<String, Object>> workExperienceList = null;
            
            if (resumeData.containsKey("workExperienceList")) {
                workExperienceList = (List<Map<String, Object>>) resumeData.get("workExperienceList");
            } else if (resumeData.containsKey("workExperience")) {
                Object workExpObj = resumeData.get("workExperience");
                if (workExpObj instanceof String) {
                    try {
                        workExperienceList = objectMapper.readValue((String) workExpObj, List.class);
                    } catch (Exception e) {
                        // 解析失败，忽略
                    }
                } else if (workExpObj instanceof List) {
                    workExperienceList = (List<Map<String, Object>>) workExpObj;
                }
            }
            
            if (workExperienceList != null) {
                // 删除旧的工作经历
                resumeWorkExperienceRepository.deleteByResumeId(resumeId);
                
                // 保存新的工作经历
                for (int i = 0; i < workExperienceList.size(); i++) {
                    Map<String, Object> workMap = workExperienceList.get(i);
                    ResumeWorkExperience workExperience = new ResumeWorkExperience();
                    workExperience.setResumeId(resumeId);
                    workExperience.setOrderIndex(i);
                    
                    if (workMap.containsKey("companyName")) workExperience.setCompanyName((String) workMap.get("companyName"));
                    if (workMap.containsKey("positionName")) workExperience.setPositionName((String) workMap.get("positionName"));
                    if (workMap.containsKey("startDate")) workExperience.setStartDate((String) workMap.get("startDate"));
                    if (workMap.containsKey("endDate")) workExperience.setEndDate((String) workMap.get("endDate"));
                    if (workMap.containsKey("description")) workExperience.setDescription((String) workMap.get("description"));

                    
                    resumeWorkExperienceRepository.save(workExperience);
                }
            }
        }
    }
    
    /**
     * 更新项目经历列表
     */
    private void updateProjectList(Long resumeId, Map<String, Object> resumeData) {
        if (resumeData.containsKey("projects") || resumeData.containsKey("projectExperienceList")) {
            List<Map<String, Object>> projectList = null;
            
            if (resumeData.containsKey("projectExperienceList")) {
                projectList = (List<Map<String, Object>>) resumeData.get("projectExperienceList");
            } else if (resumeData.containsKey("projects")) {
                Object projectObj = resumeData.get("projects");
                if (projectObj instanceof String) {
                    try {
                        projectList = objectMapper.readValue((String) projectObj, List.class);
                    } catch (Exception e) {
                        // 解析失败，忽略
                    }
                } else if (projectObj instanceof List) {
                    projectList = (List<Map<String, Object>>) projectObj;
                }
            }
            
            if (projectList != null) {
                // 删除旧的项目经历
                resumeProjectRepository.deleteByResumeId(resumeId);
                
                // 保存新的项目经历
                for (int i = 0; i < projectList.size(); i++) {
                    Map<String, Object> projectMap = projectList.get(i);
                    ResumeProject project = new ResumeProject();
                    project.setResumeId(resumeId);
                    project.setOrderIndex(i);
                    
                    if (projectMap.containsKey("projectName")) project.setProjectName((String) projectMap.get("projectName"));
                    if (projectMap.containsKey("startDate")) project.setStartDate((String) projectMap.get("startDate"));
                    if (projectMap.containsKey("endDate")) project.setEndDate((String) projectMap.get("endDate"));
                    if (projectMap.containsKey("description")) project.setDescription((String) projectMap.get("description"));

                    
                    resumeProjectRepository.save(project);
                }
            }
        }
    }
    
    /**
     * 更新技能列表
     */
    private void updateSkillList(Long resumeId, Map<String, Object> resumeData) {
        // 处理skillsWithLevel
        if (resumeData.containsKey("skillsWithLevel")) {
            Object skillsWithLevelObj = resumeData.get("skillsWithLevel");
            List<Map<String, Object>> skillsWithLevelList = null;
            
            if (skillsWithLevelObj instanceof String) {
                try {
                    skillsWithLevelList = objectMapper.readValue((String) skillsWithLevelObj, List.class);
                } catch (Exception e) {
                    // 解析失败，忽略
                }
            } else if (skillsWithLevelObj instanceof List) {
                skillsWithLevelList = (List<Map<String, Object>>) skillsWithLevelObj;
            }
            
            if (skillsWithLevelList != null) {
                // 删除旧的技能
                resumeSkillRepository.deleteByResumeId(resumeId);
                
                // 保存新的技能
                for (int i = 0; i < skillsWithLevelList.size(); i++) {
                    Map<String, Object> skillMap = skillsWithLevelList.get(i);
                    ResumeSkill skill = new ResumeSkill();
                    skill.setResumeId(resumeId);
                    skill.setOrderIndex(i);
                    
                    if (skillMap.containsKey("name")) skill.setName((String) skillMap.get("name"));
                    if (skillMap.containsKey("level")) {
                        Object levelObj = skillMap.get("level");
                        if (levelObj instanceof Integer) {
                            skill.setLevel((Integer) levelObj);
                        } else if (levelObj instanceof String) {
                            try {
                                skill.setLevel(Integer.parseInt((String) levelObj));
                            } catch (NumberFormatException e) {
                                // 解析失败，设置默认值
                                skill.setLevel(3);
                            }
                        }
                    }
                    
                    resumeSkillRepository.save(skill);
                }
            }
        } else if (resumeData.containsKey("skills")) {
            // 处理简单的skills数组
            Object skillsObj = resumeData.get("skills");
            List<String> simpleSkillsList = null;
            
            if (skillsObj instanceof String) {
                try {
                    simpleSkillsList = objectMapper.readValue((String) skillsObj, List.class);
                } catch (Exception e) {
                    // 解析失败，尝试其他格式
                }
            } else if (skillsObj instanceof List) {
                List<?> list = (List<?>) skillsObj;
                if (!list.isEmpty() && list.get(0) instanceof String) {
                    simpleSkillsList = (List<String>) list;
                }
            }
            
            if (simpleSkillsList != null) {
                // 删除旧的技能
                resumeSkillRepository.deleteByResumeId(resumeId);
                
                // 保存新的技能
                for (int i = 0; i < simpleSkillsList.size(); i++) {
                    ResumeSkill skill = new ResumeSkill();
                    skill.setResumeId(resumeId);
                    skill.setName(simpleSkillsList.get(i));
                    skill.setLevel(3); // 默认中等水平
                    skill.setOrderIndex(i);
                    
                    resumeSkillRepository.save(skill);
                }
            }
        }
    }


    

    
    // 添加缺失的exportResumeToWord方法（单参数版本）
    @Override
    public byte[] exportResumeToWord(Long resumeId) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                Resume resume = resumeRepository.findById(resumeId)
                        .orElseThrow(() -> new RuntimeException("简历不存在"));
                
                // PDF和Word导出功能已移除，因为不再需要后端渲染
                // 返回空的byte数组作为默认值
                return new byte[0];
            });
        } catch (Exception e) {
            throw new RuntimeException("导出Word文档失败", e);
        }
    }
    
    // PDF生成需要的注入依赖
    @Autowired
    private TemplateEngine templateEngine;
    
    // 实现exportResumeToPdf方法，使用Thymeleaf和Flying Saucer生成PDF
    @Override
    public byte[] exportResumeToPdf(Long resumeId) {
        // 默认使用第一个模板
        return exportResumeToPdf(resumeId, "template-one");
    }
    
    /**
     * 根据指定模板导出简历为PDF
     * @param resumeId 简历ID
     * @param templateId 模板ID
     * @return PDF字节数组
     */
    public byte[] exportResumeToPdf(Long resumeId, String templateId) {
        // 创建最终变量副本，用于lambda表达式中引用
        final Long finalResumeId = resumeId;

        // 处理默认模板ID
        final String finalTemplateId = (templateId == null || templateId.isEmpty()) ? "template-one" : templateId;
        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                // 1. 获取简历数据
                Resume resume = resumeRepository.findById(finalResumeId)
                        .orElseThrow(() -> new RuntimeException("简历不存在"));
                
                // 2. 获取完整的简历数据，包括关联的所有信息
                Map<String, Object> resumeData = getResumeFullData(finalResumeId);
                
                // 3. 创建Thymeleaf上下文
                Context context = new Context();
                context.setVariable("resume", resumeData);
                context.setVariable("templateId", finalTemplateId);
                
                // 4. 渲染HTML
                String htmlContent = templateEngine.process("resume_template", context);
                
                // 5. 设置中文字体支持（可选，需要确保服务器上有中文字体）
                // 这里使用了默认字体，实际部署时可能需要配置中文字体路径
                
                // 6. 使用Flying Saucer将HTML转换为PDF
                ITextRenderer renderer = new ITextRenderer();
                // 设置基础URL，用于解析相对路径
                renderer.setDocumentFromString(htmlContent);
                renderer.layout();
                
                // 7. 输出PDF到字节数组
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                renderer.createPDF(outputStream);
                outputStream.close();
                
                return outputStream.toByteArray();
            });
        } catch (Exception e) {
            log.error("导出PDF文档失败，简历ID: {}, 模板ID: {}", finalResumeId, finalTemplateId, e);
            throw new RuntimeException("导出PDF文档失败", e);
        }
    }
    

    
    // 添加缺失的batchOptimizeResume方法
    @Override
    public List<Resume> batchOptimizeResume(Long userId, List<Long> resumeIds, String jobType) {
        // 临时返回空列表
        return new ArrayList<>();
    }
    
    // 添加缺失的batchUploadResume方法
    @Override
    public List<Resume> batchUploadResume(Long userId, List<MultipartFile> files) {
        // 临时返回空列表
        return new ArrayList<>();
    }
    


    @Override
    public Integer getResumeAiScore(Long resumeId) {
        // 临时返回默认值，避免类型不匹配问题
        return 0;
    }

    @Override
    public String getResumeAiSuggestions(Long resumeId) {
        // 临时返回空字符串，避免类型不匹配问题
        return "";
    }

    @Override
    public boolean checkResumePermission(Long userId, Long resumeId) {
        // 临时返回false，避免Resume对象方法调用
        return false;
    }

    @Override
    public Map<String, Object> getLatestResumeData(Long userId) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 获取用户最新的简历（按创建时间降序排序，取第一个）
            List<Resume> resumeList = resumeRepository.findByUserIdOrderByCreateTimeDesc(userId);
            if (resumeList != null && !resumeList.isEmpty()) {
                Resume latestResume = resumeList.get(0);
                // 调用现有的getResumeFullData方法获取完整数据
                return getResumeFullData(latestResume.getId());
            }
            // 用户没有简历，返回null
            return null;
        });
    }
    
    @Override
    public Resume createResume(Long userId, Map<String, Object> resumeData) {
        // 检查用户是否存在
        Optional<User> userOpt = userService.getUserById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("用户不存在");
        }
        
        // 创建新简历对象
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setStatus(0); // 设置默认状态
        resume.setCreateTime(new Date());
        resume.setUpdateTime(new Date());
        
        // 设置基本字段
        // 1. 模板相关字段
        if (resumeData.containsKey("templateId")) {
            String templateId = (String) resumeData.get("templateId");
            // 确保templateId为正确格式（template-one/template-two）
            if (templateId.equals("template-one") || templateId.equals("template-two")) {
                resume.setTemplateId(templateId);
            }
        }
        
        // 2. 职位相关字段
        if (resumeData.containsKey("jobTitle")) {
            resume.setJobTitle((String) resumeData.get("jobTitle"));
        }
        
        // 3. 期望薪资和到岗时间
        if (resumeData.containsKey("expectedSalary")) {
            resume.setExpectedSalary((String) resumeData.get("expectedSalary"));
        }
        if (resumeData.containsKey("startTime")) {
            resume.setStartTime((String) resumeData.get("startTime"));
        }
        
        // 4. 职位类型ID关联
        if (resumeData.containsKey("jobTypeId")) {
            Object jobTypeIdObj = resumeData.get("jobTypeId");
            if (jobTypeIdObj instanceof Long) {
                resume.setJobTypeId((Long) jobTypeIdObj);
            } else if (jobTypeIdObj instanceof String) {
                try {
                    resume.setJobTypeId(Long.parseLong((String) jobTypeIdObj));
                } catch (NumberFormatException e) {
                    // 忽略无效的jobTypeId
                }
            }
        }
        
        // 处理嵌套对象格式（兼容前端传递的对象格式）
        if (resumeData.containsKey("personalInfo")) {
            Map<String, Object> personalInfoMap = (Map<String, Object>) resumeData.get("personalInfo");
            if (personalInfoMap != null) {
                if (personalInfoMap.containsKey("jobTitle")) {
                    resume.setJobTitle((String) personalInfoMap.get("jobTitle"));
                }
                if (personalInfoMap.containsKey("selfEvaluation")) {
                    resume.setSelfEvaluation((String) personalInfoMap.get("selfEvaluation"));
                }
                if (personalInfoMap.containsKey("interests")) {
                    resume.setInterests((String) personalInfoMap.get("interests"));
                }
            }
        }
        
        // 保存简历以获取ID
        resume = resumeRepository.save(resume);
        
        // 更新关联数据
        updateEducationList(resume.getId(), resumeData);
        updateWorkExperienceList(resume.getId(), resumeData);
        updateProjectList(resume.getId(), resumeData);
        updateSkillList(resume.getId(), resumeData);
        
        return resume;
    }
    
    @Override
    public Map<String, Object> getResumeFullData(Long resumeId) {
        // 创建返回结果Map
        Map<String, Object> result = new HashMap<>();
        
        // 1. 获取简历基本信息
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new EntityNotFoundException("简历不存在: " + resumeId));
        
        // 2. 从User表获取个人基本信息
        User user = userService.getUserById(resume.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + resume.getUserId()));
        
        // 3. 获取教育经历列表
        List<ResumeEducation> educationList = resumeEducationRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
        
        // 4. 获取工作经历列表
        List<ResumeWorkExperience> workExperienceList = resumeWorkExperienceRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
        
        // 5. 获取项目经历列表
        List<ResumeProject> projectList = resumeProjectRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
        
        // 6. 获取技能列表
        List<ResumeSkill> skillList = resumeSkillRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
        
        // 7. 将所有数据添加到返回Map中
        // 添加简历基本信息
        result.put("id", resume.getId());
        result.put("userId", resume.getUserId());
        result.put("originalFilename", resume.getOriginalFilename());
        result.put("expectedSalary", resume.getExpectedSalary());
        result.put("startTime", resume.getStartTime());
        result.put("jobTitle", resume.getJobTitle());
        result.put("jobTypeId", resume.getJobTypeId());
        result.put("templateId", resume.getTemplateId());
        result.put("status", resume.getStatus());
        result.put("createTime", resume.getCreateTime());
        result.put("updateTime", resume.getUpdateTime());
        
        // 添加个人基本信息（从User表）
        result.put("userInfo", new HashMap<String, Object>() {
            {
                put("name", user.getName());
                put("email", user.getEmail());
                put("phone", user.getPhone());
                put("address", user.getAddress());
                put("birthDate", user.getBirthDate());
                put("nickname", user.getNickname());
                put("avatarUrl", user.getAvatarUrl());
                put("gender", user.getGender());
                put("country", user.getCountry());
                put("province", user.getProvince());
                put("city", user.getCity());
            }
        });
        
        // 添加关联数据
        result.put("educationList", educationList);
        result.put("workExperienceList", workExperienceList);
        result.put("projectList", projectList);
        result.put("skillList", skillList);
        
        return result;
    }
    
    @Override
    public Resume createResumeWithFullData(Long userId, Map<String, Object> resumeData) {
        // 创建新简历对象
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setCreateTime(new Date());
        resume.setUpdateTime(new Date());
        
        // 设置模板ID（如果有）
        if (resumeData.containsKey("templateId")) {
            String templateId = String.valueOf(resumeData.get("templateId"));
            // 只允许设置已存在的模板ID
            if ("1".equals(templateId) || "2".equals(templateId) || "3".equals(templateId)) {
                resume.setTemplateId(templateId);
            }
        }
        
        // 设置职位名称
        if (resumeData.containsKey("jobTitle")) {
            resume.setJobTitle((String) resumeData.get("jobTitle"));
        }
        
        // 设置期望薪资
        if (resumeData.containsKey("expectedSalary")) {
            resume.setExpectedSalary((String) resumeData.get("expectedSalary"));
        }
        
        // 设置到岗时间
        if (resumeData.containsKey("startTime")) {
            resume.setStartTime((String) resumeData.get("startTime"));
        }
        
        // 设置职位类型ID
        if (resumeData.containsKey("jobTypeId")) {
            Object jobTypeIdObj = resumeData.get("jobTypeId");
            if (jobTypeIdObj != null) {
                try {
                    Long jobTypeId = Long.valueOf(jobTypeIdObj.toString());
                    resume.setJobTypeId(jobTypeId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid jobTypeId format: {}", jobTypeIdObj);
                }
            }
        }
        
        // 保存简历到数据库
        resume = resumeRepository.save(resume);
        
        // 保存简历相关的结构化数据
        // 处理个人信息（但注意：个人信息和联系方式现在从User表中获取）
        if (resumeData.containsKey("personalInfo")) {
            Map<String, Object> personalInfoMap = (Map<String, Object>) resumeData.get("personalInfo");
            
            // 处理个人信息中的其他字段
            if (personalInfoMap.containsKey("jobTitle")) {
                resume.setJobTitle((String) personalInfoMap.get("jobTitle"));
            }
            
            if (personalInfoMap.containsKey("selfEvaluation")) {
                resume.setSelfEvaluation((String) personalInfoMap.get("selfEvaluation"));
            }
            
            if (personalInfoMap.containsKey("interests")) {
                resume.setInterests((String) personalInfoMap.get("interests"));
            }
        }
        
        // 更新教育经历列表
        updateEducationList(resume.getId(), resumeData);
        
        // 更新工作经历列表
        updateWorkExperienceList(resume.getId(), resumeData);
        
        // 更新项目经历列表
        updateProjectList(resume.getId(), resumeData);
        
        // 更新技能列表
        updateSkillList(resume.getId(), resumeData);
        
        // 再次保存简历，确保所有字段都被更新
        resume.setUpdateTime(new Date());
        return resumeRepository.save(resume);
    }
}