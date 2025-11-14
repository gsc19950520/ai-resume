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

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import javax.persistence.EntityNotFoundException;

/**
 * 简历服务实现类
 */
@Service
public class ResumeServiceImpl implements ResumeService {

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
            resume.setTemplateId((String) resumeData.get("templateId"));
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
    
    // 添加缺失的exportResumeToPdf方法（单参数版本）
    @Override
    public byte[] exportResumeToPdf(Long resumeId) {
        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                Resume resume = resumeRepository.findById(resumeId)
                        .orElseThrow(() -> new RuntimeException("简历不存在"));
                
                // PDF和Word导出功能已移除，因为不再需要后端渲染
                // 返回空的byte数组作为默认值
                return new byte[0];
            });
        } catch (Exception e) {
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
}