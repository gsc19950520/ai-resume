package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.entity.ResumeEducation;
import com.aicv.airesume.entity.ResumeProject;
import com.aicv.airesume.entity.ResumeSkill;
import com.aicv.airesume.entity.ResumeWorkExperience;
import com.aicv.airesume.entity.User;
import com.aicv.airesume.model.dto.ResumeDataDTO;
import com.aicv.airesume.model.dto.PersonalInfoDTO;
import com.aicv.airesume.model.dto.EducationDTO;
import com.aicv.airesume.model.dto.WorkExperienceDTO;
import com.aicv.airesume.model.dto.ProjectDTO;
import com.aicv.airesume.model.dto.SkillWithLevelDTO;
import com.aicv.airesume.repository.*;
import com.aicv.airesume.service.ResumeService;


import com.aicv.airesume.service.UserService;
import com.aicv.airesume.utils.AiServiceUtils;
import com.aicv.airesume.utils.FileUtils;
import com.aicv.airesume.utils.FreeMarkerUtil;
import com.aicv.airesume.utils.OssUtils;
import com.aicv.airesume.utils.RetryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import javax.persistence.EntityNotFoundException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import java.io.StringReader;

import lombok.extern.slf4j.Slf4j;

/**
 * 简历服务实现类
 */
@Slf4j
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
    public List<Resume> getResumeListByUserId(Long userId) {
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
    // 添加模板文件基础路径配置
    @Value("${resume.template.base-path:d:/owner_project/ai-resume/ai-resume-miniprogram/pages/template/preview}")
    private String templateBasePath;
    
    /**
     * 读取模板文件内容
     * @param templateId 模板ID
     * @param fileType 文件类型（wxml或wxss）
     * @return 文件内容
     */
    private String readTemplateFile(String templateId, String fileType) throws IOException {
        // 根据模板ID构建文件路径
        String templateDir = templateId.replace("-", "/");
        String filePath = templateBasePath + "/" + templateDir + "/" + templateId + "." + fileType;
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("模板文件不存在: " + filePath);
        }
        
        // 读取文件内容
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
    
    
    @Override
    public byte[] exportResumeToPdf(Long resumeId, String templateId) {
        log.info("开始导出简历PDF，resumeId: {}, templateId: {}", resumeId, templateId);

        final Long finalResumeId = resumeId;
        final String finalTemplateId =
                (templateId == null || templateId.isEmpty()) ? "template-one" : templateId;

        try {
            return retryUtils.executeWithDefaultRetry(() -> {
                try {
                    if (finalResumeId == null) {
                        throw new IllegalArgumentException("resumeId cannot be null");
                    }

                    log.info("使用模板: {}", finalTemplateId);

                    // 1. 获取简历数据
                    Resume resume = resumeRepository.findById(finalResumeId)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Resume not found with id: " + finalResumeId));

                    // 2. 获取关联数据（你原有的）
                    Map<String, Object> resumeData = getResumeFullData(finalResumeId);
                    log.info("获取到简历完整数据，用户ID: {}", resume.getUserId());

                    // 3. 读取模板文件 WXML + WXSS
                    String wxmlContent = readTemplateFile(finalTemplateId, "wxml");
                    String wxssContent = readTemplateFile(finalTemplateId, "wxss");

                    log.info("模板文件读取成功, wxml字节: {}, wxss字节: {}",
                            wxmlContent.length(), wxssContent.length());

                    // --------------------------------------------------------
                    // 4. WXML 渲染动态数据 -> 转换成 HTML BODY
                    // --------------------------------------------------------
                    String htmlBody = FreeMarkerUtil.parse(wxmlContent, resumeData);

                    // --------------------------------------------------------
                    // 5. WXSS 直接作为 CSS 使用
                    // --------------------------------------------------------
                    String cssStyle = wxssContent;

                    // --------------------------------------------------------
                    // 6. 拼装完整 HTML 内容
                    // --------------------------------------------------------
                    String htmlContent =
                            "<!DOCTYPE html>\n" +
                            "<html lang=\"zh-CN\">\n" +
                            "<head>\n" +
                            "   <meta charset=\"UTF-8\">\n" +
                            "   <title>简历PDF</title>\n" +
                            "   <style>\n" +
                            "       @page { size: A4; margin: 20px; }\n" +
                            "       body { font-family: \"Microsoft YaHei\"; font-size: 14px; }\n" +
                            "       * { box-sizing: border-box; }\n" +
                            cssStyle +
                            "   </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            htmlBody +
                            "\n</body>\n</html>";

                    // --------------------------------------------------------
                    // 7. 将 HTML 渲染为 PDF
                    // --------------------------------------------------------
                    byte[] pdfBytes = convertHtmlToPdf(htmlContent);

                    log.info("PDF 导出成功, 大小: {} KB", pdfBytes.length / 1024.0);

                    return pdfBytes;

                } catch (Exception e) {
                    log.error("导出简历PDF失败, resumeId: {}, templateId: {}",
                            finalResumeId, finalTemplateId, e);
                    throw new RuntimeException("Failed to export resume PDF", e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("导出PDF文档失败", e);
        }
    }

    
    public byte[] convertHtmlToPdf(String htmlContent) { 
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) { 
            // 创建PDF文档
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, os);
            document.open();
            
            // 使用HTMLWorker解析HTML内容
            HTMLWorker htmlWorker = new HTMLWorker(document);
            htmlWorker.parse(new StringReader(htmlContent));
            
            document.close();
            return os.toByteArray(); 
        } catch (DocumentException | IOException e) { 
            throw new RuntimeException("HTML 转 PDF 失败", e); 
        } 
    }

    

    
    // 添加缺失的batchOptimizeResume方法
    // batchOptimizeResume方法已移除
    
    // 添加缺失的batchUploadResume方法
    @Override
    public List<Resume> batchUploadResume(Long userId, List<MultipartFile> files) {
        // 临时返回空列表
        return new ArrayList<>();
    }
    


    @Override
    public Map<String, Object> getResumeAiScore(Long resumeId) {
        // 临时返回默认值，避免类型不匹配问题
        Map<String, Object> result = new HashMap<>();
        result.put("score", 0);
        result.put("maxScore", 100);
        result.put("description", "暂无评分");
        return result;
    }

    @Override
    public Map<String, Object> getResumeAiSuggestions(Long resumeId) {
        // 临时返回默认值，避免类型不匹配问题
        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", new ArrayList<>());
        result.put("improvements", new ArrayList<>());
        return result;
    }

    // checkResumePermission方法已移除

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
    
    // createResume方法已移除，使用createResumeWithFullData方法代替
    
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
        result.put("interests", resume.getInterests());
        result.put("selfEvaluation", resume.getSelfEvaluation());
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
    @Transactional
    public Resume createResumeWithFullData(Long userId, ResumeDataDTO resumeDataDTO) {
        try {
            // 首先检查用户是否已存在简历
            List<Resume> existingResumes = resumeRepository.findByUserIdOrderByCreateTimeDesc(userId);
            
            if (existingResumes != null && !existingResumes.isEmpty()) {
                // 用户已存在简历，更新现有简历
                Resume existingResume = existingResumes.get(0);
                log.info("用户已存在简历，将更新现有简历。用户ID: {}, 简历ID: {}", userId, existingResume.getId());
                return updateResumeWithFullData(existingResume.getId(), resumeDataDTO);
            }
            
            // 用户不存在简历，创建新简历
            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.setCreateTime(new Date());
            resume.setUpdateTime(new Date());
            
            // 处理个人信息
            if (resumeDataDTO.getPersonalInfo() != null) {
                // 设置职位名称
                if (resumeDataDTO.getPersonalInfo().getJobTitle() != null) {
                    resume.setJobTitle(resumeDataDTO.getPersonalInfo().getJobTitle());
                }
                
                // 设置期望薪资
                if (resumeDataDTO.getPersonalInfo().getExpectedSalary() != null) {
                    resume.setExpectedSalary(resumeDataDTO.getPersonalInfo().getExpectedSalary());
                }
                
                // 设置到岗时间
                if (resumeDataDTO.getPersonalInfo().getStartTime() != null) {
                    resume.setStartTime(resumeDataDTO.getPersonalInfo().getStartTime());
                }
                
                // 设置自我评价
                if (resumeDataDTO.getPersonalInfo().getSelfEvaluation() != null) {
                    resume.setSelfEvaluation(resumeDataDTO.getPersonalInfo().getSelfEvaluation());
                }
                
                // 设置兴趣爱好
                if (resumeDataDTO.getPersonalInfo().getInterests() != null) {
                    resume.setInterests(String.join(",", resumeDataDTO.getPersonalInfo().getInterests()));
                }
            }
            
            // 保存简历到数据库
            resume = resumeRepository.save(resume);
            
            // 更新教育经历列表
            updateEducationListWithDTO(resume.getId(), resumeDataDTO.getEducation());
            
            // 更新工作经历列表
            updateWorkExperienceListWithDTO(resume.getId(), resumeDataDTO.getWorkExperience());
            
            // 更新项目经历列表
            updateProjectListWithDTO(resume.getId(), resumeDataDTO.getProjects());
            
            // 更新技能列表
            updateSkillListWithDTO(resume.getId(), resumeDataDTO.getSkillsWithLevel());
            
            // 再次保存简历，确保所有字段都被更新
            resume.setUpdateTime(new Date());
            return resumeRepository.save(resume);
        } catch (Exception e) {
            log.error("创建简历失败，用户ID：{}", userId, e);
        }
        return null;
    }
    
    @Override
    public boolean checkResumePermission(Long userId, Long resumeId) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
            if (resumeOpt.isPresent()) {
                Resume resume = resumeOpt.get();
                // 检查用户ID是否匹配
                return resume.getUserId().equals(userId);
            }
            // 简历不存在，返回false
            return false;
        });
    }
    
    @Override
    @Transactional
    public Resume updateResumeWithFullData(Long resumeId, ResumeDataDTO resumeDataDTO) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 获取简历
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new RuntimeException("简历不存在"));
            
            // 处理个人信息中的字段
            if (resumeDataDTO.getPersonalInfo() != null) {
                PersonalInfoDTO personalInfo = resumeDataDTO.getPersonalInfo();
                
                // 设置期望薪资
                if (personalInfo.getExpectedSalary() != null) {
                    resume.setExpectedSalary(personalInfo.getExpectedSalary());
                }
                
                // 设置到岗时间
                if (personalInfo.getStartTime() != null) {
                    resume.setStartTime(personalInfo.getStartTime());
                }

                if (personalInfo.getJobTitle() != null) {
                    resume.setJobTitle(personalInfo.getJobTitle());
                }
                
                if (personalInfo.getSelfEvaluation() != null) {
                    resume.setSelfEvaluation(personalInfo.getSelfEvaluation());
                }
                
                if (personalInfo.getInterests() != null) {
                    resume.setInterests(String.join(",", personalInfo.getInterests()));
                }
            }
            
            // 更新教育经历列表
            updateEducationListWithDTO(resumeId, resumeDataDTO.getEducation());
            
            // 更新工作经历列表
            updateWorkExperienceListWithDTO(resumeId, resumeDataDTO.getWorkExperience());
            
            // 更新项目经历列表
            updateProjectListWithDTO(resumeId, resumeDataDTO.getProjects());
            
            // 更新技能列表
            updateSkillListWithDTO(resumeId, resumeDataDTO.getSkillsWithLevel());
            
            // 更新时间戳
            resume.setUpdateTime(new Date());
            
            // 保存更新后的简历
            return resumeRepository.save(resume);
        });
    }
    
    /**
     * 使用DTO更新教育经历列表
     * @param resumeId 简历ID
     * @param educationDTOs 教育经历DTO列表
     */
    private void updateEducationListWithDTO(Long resumeId, List<EducationDTO> educationDTOs) {
        if (educationDTOs == null) {
            return;
        }
        
        // 删除现有教育经历
        resumeEducationRepository.deleteByResumeId(resumeId);
        
        // 添加新的教育经历
        for (int i = 0; i < educationDTOs.size(); i++) {
            EducationDTO dto = educationDTOs.get(i);
            ResumeEducation education = new ResumeEducation();
            education.setResumeId(resumeId);
            education.setSchool(dto.getSchool());
            education.setMajor(dto.getMajor());
            education.setDegree(dto.getDegree());
            education.setStartDate(dto.getStartDate());
            education.setEndDate(dto.getEndDate());
            education.setDescription(dto.getDescription());
            education.setOrderIndex(i);
            
            resumeEducationRepository.save(education);
        }
    }
    
    /**
     * 使用DTO更新工作经历列表
     * @param resumeId 简历ID
     * @param workExperienceDTOs 工作经历DTO列表
     */
    private void updateWorkExperienceListWithDTO(Long resumeId, List<WorkExperienceDTO> workExperienceDTOs) {
        if (workExperienceDTOs == null) {
            return;
        }
        
        // 删除现有工作经历
        resumeWorkExperienceRepository.deleteByResumeId(resumeId);
        
        // 添加新的工作经历
        for (int i = 0; i < workExperienceDTOs.size(); i++) {
            WorkExperienceDTO dto = workExperienceDTOs.get(i);
            ResumeWorkExperience workExperience = new ResumeWorkExperience();
            workExperience.setResumeId(resumeId);
            workExperience.setCompanyName(dto.getCompanyName());
            workExperience.setPositionName(dto.getPositionName());
            workExperience.setStartDate(dto.getStartDate());
            workExperience.setEndDate(dto.getEndDate());
            workExperience.setDescription(dto.getDescription());
            workExperience.setOrderIndex(i);
            
            resumeWorkExperienceRepository.save(workExperience);
        }
    }
    
    /**
     * 使用DTO更新项目经历列表
     * @param resumeId 简历ID
     * @param projectDTOs 项目经历DTO列表
     */
    private void updateProjectListWithDTO(Long resumeId, List<ProjectDTO> projectDTOs) {
        if (projectDTOs == null) {
            return;
        }
        
        // 删除现有项目经历
        resumeProjectRepository.deleteByResumeId(resumeId);
        
        // 添加新的项目经历
        for (int i = 0; i < projectDTOs.size(); i++) {
            ProjectDTO dto = projectDTOs.get(i);
            ResumeProject project = new ResumeProject();
            project.setResumeId(resumeId);
            project.setProjectName(dto.getProjectName());
            project.setDescription(dto.getDescription());
            project.setStartDate(dto.getStartDate());
            project.setEndDate(dto.getEndDate());
            project.setOrderIndex(i);
            
            resumeProjectRepository.save(project);
        }
    }
    
    /**
     * 使用DTO更新技能列表
     * @param resumeId 简历ID
     * @param skillDTOs 技能DTO列表
     */
    private void updateSkillListWithDTO(Long resumeId, List<SkillWithLevelDTO> skillDTOs) {
        if (skillDTOs == null) {
            return;
        }
        
        // 删除现有技能
        resumeSkillRepository.deleteByResumeId(resumeId);
        
        // 添加新的技能
        for (int i = 0; i < skillDTOs.size(); i++) {
            SkillWithLevelDTO dto = skillDTOs.get(i);
            ResumeSkill skill = new ResumeSkill();
            skill.setResumeId(resumeId);
            skill.setName(dto.getName());
            skill.setLevel(dto.getLevel());
            skill.setOrderIndex(i);
            
            resumeSkillRepository.save(skill);
        }
    }
}