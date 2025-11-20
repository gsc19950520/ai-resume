package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.entity.ResumeEducation;
import com.aicv.airesume.entity.ResumeProject;
import com.aicv.airesume.entity.ResumeSkill;
import com.aicv.airesume.entity.ResumeWorkExperience;
import com.aicv.airesume.model.dto.ResumeAnalysisDTO;
import com.aicv.airesume.service.ResumeAnalysisService;
import com.aicv.airesume.service.ResumeService;
import com.aicv.airesume.repository.ResumeEducationRepository;
import com.aicv.airesume.repository.ResumeProjectRepository;
import com.aicv.airesume.repository.ResumeSkillRepository;
import com.aicv.airesume.repository.ResumeWorkExperienceRepository;
import com.aicv.airesume.utils.AiServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.validation.constraints.NotBlank;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisServiceImpl.class);

    @Autowired
    private ResumeService resumeService;
    
    @Autowired
    private AiServiceUtils aiServiceUtils;
    
    @Autowired
    private ResumeEducationRepository resumeEducationRepository;
    
    @Autowired
    private ResumeProjectRepository resumeProjectRepository;
    
    @Autowired
    private ResumeSkillRepository resumeSkillRepository;
    
    @Autowired
    private ResumeWorkExperienceRepository resumeWorkExperienceRepository;

    @Override
    public ResumeAnalysisDTO analyzeResume(Long resumeId, String jobType, String analysisDepth) {
        try {
            log.info("开始分析简历，resumeId: {}, jobType: {}, analysisDepth: {}", resumeId, jobType, analysisDepth);
            
            // 获取简历基础数据
            Resume resume = resumeService.getResumeById(resumeId);
            if (resume == null) {
                log.error("未找到简历: {}", resumeId);
                return createDefaultAnalysisDTO();
            }
            
            // 查询关联数据
            List<ResumeEducation> educationList = resumeEducationRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
            List<ResumeProject> projectList = resumeProjectRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
            List<ResumeSkill> skillList = resumeSkillRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
            List<ResumeWorkExperience> workExperienceList = resumeWorkExperienceRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
            
            // 分析简历内容
            return analyzeResumeContent(resume, educationList, projectList, skillList, workExperienceList, jobType, analysisDepth);
        } catch (Exception e) {
            log.error("分析简历失败: {}", e.getMessage());
            return createDefaultAnalysisDTO();
        }
    }

    private ResumeAnalysisDTO analyzeResumeContent(Resume resume, List<ResumeEducation> educationList, 
                                                 List<ResumeProject> projectList, List<ResumeSkill> skillList,
                                                 List<ResumeWorkExperience> workExperienceList, String jobType, 
                                                 String analysisDepth) {
        ResumeAnalysisDTO analysisDTO = new ResumeAnalysisDTO();
        
        // 1. 分析候选人基本信息
        analysisDTO.setCandidateInfo(parseCandidateInfo(resume, educationList, workExperienceList));
        
        // 2. 分析技术栈
        analysisDTO.setTechnicalAnalysis(parseTechnicalAnalysis(skillList, jobType));
        
        // 3. 分析项目经验
        analysisDTO.setProjectAnalysis(parseProjectAnalysis(projectList));
        
        // 4. 分析业务能力
        analysisDTO.setBusinessAnalysis(parseBusinessAnalysis(workExperienceList));
        
        // 5. 评估经验级别
        Integer workYears = analysisDTO.getCandidateInfo().getWorkYears();
        Integer totalProjects = analysisDTO.getProjectAnalysis().getTotalProjects();
        Map<String, Integer> skillProficiency = analysisDTO.getTechnicalAnalysis().getSkillProficiency();
        
        String experienceLevel = assessExperienceLevel(
            workYears != null ? workYears : 0,
            totalProjects != null ? totalProjects : 0,
            skillProficiency
        );
        analysisDTO.setExperienceLevel(experienceLevel);
        
        // 6. 生成优势分析（使用DeepSeek AI）
        analysisDTO.setStrengths(generateStrengthsWithAI(resume, educationList, projectList, skillList, workExperienceList));
        
        // 7. 生成待提升项（使用DeepSeek AI）
        analysisDTO.setImprovements(generateImprovementsWithAI(resume, educationList, projectList, skillList, workExperienceList));
        
        // 8. 生成面试问题清单（通过DeepSeek AI）
        analysisDTO.setInterviewQuestions(generateInterviewQuestionsWithAI(
            resume, educationList, projectList, skillList, workExperienceList, jobType, analysisDepth));
        
        // 9. 计算综合评分
        analysisDTO.setOverallScore(calculateOverallScore(resume, educationList, projectList, skillList, workExperienceList, jobType));
        
        // 10. 设置分析时间
        analysisDTO.setAnalysisTime(new Date());
        
        return analysisDTO;
    }

    private ResumeAnalysisDTO.CandidateInfo parseCandidateInfo(Resume resume, List<ResumeEducation> educationList, List<ResumeWorkExperience> workExperienceList) {
        try {
            ResumeAnalysisDTO.CandidateInfo info = new ResumeAnalysisDTO.CandidateInfo();
            
            // 设置个人基本信息
            info.setName(resume.getName());
            info.setGender(resume.getGender());
            info.setPhone(resume.getPhone());
            info.setEmail(resume.getEmail());
            info.setJobIntention(resume.getJobTitle());
            
            // 从教育经历中获取最高学历
            if (educationList != null && !educationList.isEmpty()) {
                ResumeEducation highestEducation = educationList.stream()
                    .max(Comparator.comparing(ResumeEducation::getOrderIndex))
                    .orElse(null);
                if (highestEducation != null) {
                    info.setEducation(highestEducation.getDegree());
                    info.setMajor(highestEducation.getMajor());
                    info.setSchool(highestEducation.getSchool());
                }
            }
            
            // 计算工作年限
            Integer workYears = calculateWorkYears(workExperienceList);
            info.setWorkYears(workYears);
            
            return info;
        } catch (Exception e) {
            log.error("解析候选人信息失败: {}", e.getMessage());
            return new ResumeAnalysisDTO.CandidateInfo();
        }
    }

    private ResumeAnalysisDTO.TechnicalAnalysis parseTechnicalAnalysis(List<ResumeSkill> skillList, String jobType) {
        try {
            ResumeAnalysisDTO.TechnicalAnalysis techAnalysis = new ResumeAnalysisDTO.TechnicalAnalysis();
            List<String> primarySkills = new ArrayList<>();
            List<String> secondarySkills = new ArrayList<>();
            Map<String, Integer> skillProficiency = new HashMap<>();
            
            if (skillList != null && !skillList.isEmpty()) {
                for (ResumeSkill skill : skillList) {
                    String skillName = skill.getName();
                    if (skillName != null && !skillName.isEmpty()) {
                        // 根据jobType判断是否是主要技能
                        if (isPrimarySkill(skillName, jobType)) {
                            primarySkills.add(skillName);
                        } else {
                            secondarySkills.add(skillName);
                        }
                        // 设置技能熟练度
                        skillProficiency.put(skillName, skill.getLevel() != null ? skill.getLevel() : 5);
                    }
                }
            }
            
            techAnalysis.setPrimarySkills(primarySkills);
            techAnalysis.setSecondarySkills(secondarySkills);
            techAnalysis.setSkillProficiency(skillProficiency);
            
            return techAnalysis;
        } catch (Exception e) {
            log.error("解析技术分析失败: {}", e.getMessage());
            return new ResumeAnalysisDTO.TechnicalAnalysis();
        }
    }

    private ResumeAnalysisDTO.ProjectAnalysis parseProjectAnalysis(List<ResumeProject> projectList) {
        try {
            ResumeAnalysisDTO.ProjectAnalysis projectAnalysis = new ResumeAnalysisDTO.ProjectAnalysis();
            List<ResumeAnalysisDTO.ProjectInfo> keyProjects = new ArrayList<>();
            
            if (projectList != null && !projectList.isEmpty()) {
                // 最多取3个主要项目
                List<ResumeProject> mainProjects = projectList.stream()
                    .limit(3)
                    .collect(Collectors.toList());
                
                for (ResumeProject project : mainProjects) {
                    ResumeAnalysisDTO.ProjectInfo projectInfo = new ResumeAnalysisDTO.ProjectInfo();
                    
                    projectInfo.setName(project.getProjectName());
                    projectInfo.setDescription(project.getDescription());
                    projectInfo.setRole(project.getRole());
                    
                    // 设置项目时间段
                    StringBuilder period = new StringBuilder();
                    if (project.getStartDate() != null) {
                        period.append(project.getStartDate());
                    }
                    if (project.getEndDate() != null) {
                        period.append(" - ").append(project.getEndDate());
                    }
                    projectInfo.setPeriod(period.toString());
                    
                    // 提取技术栈
                    if (project.getTechStack() != null) {
                        List<String> technologies = Arrays.stream(project.getTechStack().split(","))
                            .map(String::trim)
                            .filter(tech -> !tech.isEmpty())
                            .collect(Collectors.toList());
                        projectInfo.setTechnologies(technologies);
                    }
                    
                    // 评估项目复杂度
                    projectInfo.setComplexity(assessProjectComplexity(projectInfo));
                    
                    // 分析业务影响
                    projectInfo.setBusinessImpact(analyzeBusinessImpact(project.getDescription()));
                    
                    keyProjects.add(projectInfo);
                }
            }
            
            projectAnalysis.setTotalProjects(projectList != null ? projectList.size() : 0);
            projectAnalysis.setKeyProjects(keyProjects);
            
            return projectAnalysis;
        } catch (Exception e) {
            log.error("解析项目分析失败: {}", e.getMessage());
            return new ResumeAnalysisDTO.ProjectAnalysis();
        }
    }

    private ResumeAnalysisDTO.BusinessAnalysis parseBusinessAnalysis(List<ResumeWorkExperience> workExperienceList) {
        try {
            ResumeAnalysisDTO.BusinessAnalysis businessAnalysis = new ResumeAnalysisDTO.BusinessAnalysis();
            List<String> businessDomains = new ArrayList<>();
            List<String> responsibilities = new ArrayList<>();
            
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                for (ResumeWorkExperience workExp : workExperienceList) {
                    // 提取业务领域
                    if (workExp.getIndustry() != null && !workExp.getIndustry().isEmpty() && 
                        !businessDomains.contains(workExp.getIndustry())) {
                        businessDomains.add(workExp.getIndustry());
                    } else if (workExp.getCompanyName() != null && !workExp.getCompanyName().isEmpty()) {
                        String domain = extractBusinessDomain(workExp.getCompanyName());
                        if (domain != null && !businessDomains.contains(domain)) {
                            businessDomains.add(domain);
                        }
                    }
                    
                    // 提取职责
                    if (workExp.getDescription() != null && !workExp.getDescription().isEmpty()) {
                        String[] duties = workExp.getDescription().split("；|");
                        for (String duty : duties) {
                            if (!duty.trim().isEmpty() && !responsibilities.contains(duty.trim())) {
                                responsibilities.add(duty.trim());
                            }
                        }
                    }
                }
            }
            
            businessAnalysis.setBusinessDomains(businessDomains);
            businessAnalysis.setResponsibilities(responsibilities);
            
            return businessAnalysis;
        } catch (Exception e) {
            log.error("解析业务分析失败: {}", e.getMessage());
            return new ResumeAnalysisDTO.BusinessAnalysis();
        }
    }

    public String assessExperienceLevel(Integer workYears, Integer projectCount, Map<String, Integer> skills) {
        if (workYears == null) workYears = 0;
        if (projectCount == null) projectCount = 0;
        
        int skillScore = skills != null ? skills.size() * 2 : 0;
        int projectScore = projectCount * 5;
        int experienceScore = workYears * 10;
        
        int totalScore = skillScore + projectScore + experienceScore;
        
        if (totalScore >= 100) return "专家级";
        if (totalScore >= 70) return "高级";
        if (totalScore >= 40) return "中级";
        if (totalScore >= 20) return "初级";
        return "入门级";
    }

    private Integer calculateWorkYears(List<ResumeWorkExperience> workExperienceList) {
        int totalMonths = 0;
        
        try {
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                for (ResumeWorkExperience workExp : workExperienceList) {
                    String startDate = workExp.getStartDate();
                    String endDate = workExp.getEndDate();
                    
                    if (startDate != null && endDate != null) {
                        // 简单计算工作月份，假设格式为YYYY-MM
                        try {
                            String[] startParts = startDate.split("-");
                            String[] endParts = endDate.split("-");
                            
                            if (startParts.length >= 2 && endParts.length >= 2) {
                                int startYear = Integer.parseInt(startParts[0]);
                                int startMonth = Integer.parseInt(startParts[1]);
                                int endYear = Integer.parseInt(endParts[0]);
                                int endMonth = Integer.parseInt(endParts[1]);
                                
                                totalMonths += (endYear - startYear) * 12 + (endMonth - startMonth);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("日期格式解析失败: {}-{}", startDate, endDate);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("计算工作年限失败: {}", e.getMessage());
        }
        
        // 返回年数，向上取整
        return Math.max(0, (totalMonths + 11) / 12);
    }

    private boolean isPrimarySkill(String skillName, String jobType) {
        // 根据职位类型判断是否是主要技能
        Map<String, List<String>> jobSkillsMap = new HashMap<>();
        jobSkillsMap.put("前端开发", Arrays.asList("HTML", "CSS", "JavaScript", "React", "Vue", "Angular", "TypeScript"));
        jobSkillsMap.put("后端开发", Arrays.asList("Java", "Python", "C++", "Spring", "Django", "Flask", "Node.js"));
        jobSkillsMap.put("数据开发", Arrays.asList("Hadoop", "Spark", "Hive", "Kafka", "ETL", "SQL"));
        jobSkillsMap.put("测试开发", Arrays.asList("Java", "Python", "Junit", "TestNG", "Selenium", "JMeter"));
        
        if (jobType != null && jobSkillsMap.containsKey(jobType)) {
            List<String> primarySkills = jobSkillsMap.get(jobType);
            return primarySkills.stream().anyMatch(skill -> skillName.contains(skill));
        }
        return false;
    }
    
    private String extractBusinessDomain(String companyName) {
        // 简单的业务领域提取逻辑
        if (companyName.contains("科技") || companyName.contains("技术")) {
            return "互联网科技";
        } else if (companyName.contains("金融") || companyName.contains("银行")) {
            return "金融服务";
        } else if (companyName.contains("教育")) {
            return "教育培训";
        } else if (companyName.contains("医疗")) {
            return "医疗健康";
        } else if (companyName.contains("电商") || companyName.contains("购物")) {
            return "电子商务";
        } else {
            return "其他行业";
        }
    }
    
    private List<String> generateInterviewQuestionsWithAI(Resume resume, List<ResumeEducation> educationList,
                                                       List<ResumeProject> projectList, List<ResumeSkill> skillList,
                                                       List<ResumeWorkExperience> workExperienceList,
                                                       String jobType, String analysisDepth) {
        try {
            // 计算工作年限，用于调整问题难度
            int workYears = calculateWorkYears(workExperienceList);
            String experienceLevel = workYears >= 7 ? "高级" : (workYears >= 3 ? "中级" : "初级");
            
            // 获取主要技术栈，用于生成针对性问题
            String mainTechnicalStack = extractMainTechnicalStack(skillList);
            
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下简历信息，为面试")
                  .append(jobType != null ? jobType : "技术岗位")
                  .append("的候选人生成针对性的面试问题。候选人经验级别为:")
                  .append(experienceLevel).append("（工作经验约").append(workYears).append("年）。\n\n");
            
            // 添加个人信息
            prompt.append("【基本信息】\n");
            prompt.append("姓名: ").append(resume.getName() != null ? resume.getName() : "未知").append("\n");
            prompt.append("求职意向: ").append(resume.getJobTitle() != null ? resume.getJobTitle() : "未知").append("\n");
            prompt.append("主要技术栈: ").append(mainTechnicalStack).append("\n\n");
            
            // 添加技能信息，重点突出高熟练度技能和与职位相关的技能
            if (skillList != null && !skillList.isEmpty()) {
                prompt.append("【核心技能】\n");
                // 优先展示高熟练度技能
                List<ResumeSkill> advancedSkills = skillList.stream()
                    .filter(s -> s.getLevel() != null && s.getLevel() >= 8)
                    .collect(Collectors.toList());
                
                // 补充与职位相关的技能
                List<ResumeSkill> relevantSkills = skillList.stream()
                    .filter(s -> s.getLevel() != null && s.getLevel() >= 6 && 
                               isPrimarySkill(s.getName() != null ? s.getName() : "", jobType))
                    .filter(s -> !advancedSkills.contains(s))
                    .limit(3)
                    .collect(Collectors.toList());
                
                // 合并并去重
                Set<ResumeSkill> skillsToShow = new HashSet<>();
                skillsToShow.addAll(advancedSkills);
                skillsToShow.addAll(relevantSkills);
                
                for (ResumeSkill skill : skillsToShow) {
                    prompt.append(skill.getName()).append(" (熟练度: ")
                          .append(skill.getLevel() != null ? skill.getLevel() : "未知").append(")\n");
                }
                
                if (skillsToShow.isEmpty()) {
                    // 如果没有高熟练度或相关技能，展示所有技能
                    for (ResumeSkill skill : skillList) {
                        prompt.append(skill.getName()).append("\n");
                    }
                }
                prompt.append("\n");
            }
            
            // 添加关键项目经验，重点关注描述详细的项目
            if (projectList != null && !projectList.isEmpty()) {
                prompt.append("【重点项目经验】\n");
                // 筛选有详细描述的项目，按描述长度排序
                List<ResumeProject> sortedProjects = projectList.stream()
                    .filter(p -> p.getDescription() != null && p.getDescription().length() > 30)
                    .sorted(Comparator.comparingInt(p -> -p.getDescription().length()))
                    .limit(2)
                    .collect(Collectors.toList());
                
                if (sortedProjects.isEmpty()) {
                    // 如果没有详细描述的项目，使用前两个项目
                    sortedProjects = projectList.subList(0, Math.min(2, projectList.size()));
                }
                
                for (ResumeProject project : sortedProjects) {
                    prompt.append("项目名称: ").append(project.getProjectName()).append("\n");
                    prompt.append("角色: ").append(project.getRole() != null ? project.getRole() : "未知").append("\n");
                    prompt.append("技术栈: ").append(project.getTechStack() != null ? project.getTechStack() : "未知").append("\n");
                    prompt.append("描述: ").append(project.getDescription() != null ? project.getDescription() : "无").append("\n\n");
                }
            }
            
            // 添加最近工作经验
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                prompt.append("【最近工作经验】\n");
                // 假设列表是按时间倒序排列的，取第一个
                ResumeWorkExperience latestWork = workExperienceList.get(0);
                prompt.append("公司: ").append(latestWork.getCompanyName()).append("\n");
                prompt.append("职位: ").append(latestWork.getPositionName()).append("\n");
                prompt.append("职责: ").append(latestWork.getDescription() != null ? latestWork.getDescription() : "无").append("\n\n");
            }
            
            // 添加教育背景（如果有较高学历）
            if (educationList != null && !educationList.isEmpty()) {
                ResumeEducation highestEducation = educationList.stream()
                    .max(Comparator.comparing(ResumeEducation::getOrderIndex))
                    .orElse(null);
                
                if (highestEducation != null && highestEducation.getDegree() != null && 
                    (highestEducation.getDegree().contains("硕士") || highestEducation.getDegree().contains("博士"))) {
                    prompt.append("【教育背景】\n");
                    prompt.append("学历: ").append(highestEducation.getDegree()).append("\n");
                    prompt.append("学校: ").append(highestEducation.getSchool()).append("\n");
                    prompt.append("专业: ").append(highestEducation.getMajor()).append("\n\n");
                }
            }
            
            // 添加问题生成要求，根据经验级别调整难度
            prompt.append("请根据候选人的经验级别生成");
            if ("comprehensive".equals(analysisDepth)) {
                prompt.append("10-12个");
            } else if ("高级".equals(experienceLevel)) {
                prompt.append("8-10个");
            } else {
                prompt.append("6-8个");
            }
            
            prompt.append("高质量的面试问题，问题应该涵盖以下几个维度:")
                  .append("\n1. 技术深度问题: 针对候选人技能的深入技术问题，适合").append(experienceLevel).append("工程师水平")
                  .append("\n2. 项目挑战问题: 基于项目经验，提问候选人如何解决技术难题和做出技术决策")
                  .append("\n3. 职位相关问题: 与").append(jobType != null ? jobType : "目标职位").append("直接相关的专业知识和能力")
                  .append("\n4. 团队协作问题: 考察候选人在团队中的沟通能力、协作方式和冲突处理");
                  
            // 高级工程师额外要求
            if ("高级".equals(experienceLevel)) {
                prompt.append("\n5. 架构设计问题: 考察候选人的系统设计能力和架构思维")
                      .append("\n6. 技术领导力问题: 如何带领团队、指导新人、制定技术路线");
            }
            
            prompt.append("\n\n问题要求:")
                  .append("\n- 问题应该具体、有针对性，避免过于宽泛或基础的问题")
                  .append("\n- 问题难度要符合").append(experienceLevel).append("工程师的职位要求")
                  .append("\n- 问题应该能够有效评估候选人的实际能力和经验")
                  .append("\n- 部分问题可以设计成追问的形式，帮助面试官深入了解候选人");
            
            prompt.append("\n\n请以问题列表形式返回，每个问题单独一行，不要添加序号和额外说明。");
            
            // 调用DeepSeek API
            String result = aiServiceUtils.callDeepSeekApi(prompt.toString());
            
            // 解析结果
            List<String> questions = new ArrayList<>();
            if (result != null && !result.isEmpty()) {
                String[] lines = result.split("\n");
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        // 移除可能的序号和前缀
                        String question = trimmedLine.replaceAll("^\\d+\\.?\\s*", "")
                                                    .replaceAll("^[\\u4e00-\\u9fa5]+：", "")
                                                    .replaceAll("^问题[0-9]?:", "");
                        
                        // 确保是问题格式（以问号结尾或包含疑问词）
                        if (question.endsWith("?") || 
                            question.contains("吗") || 
                            question.contains("如何") || 
                            question.contains("什么") || 
                            question.contains("为什么") || 
                            question.contains("请描述") || 
                            question.contains("请解释")) {
                            questions.add(question);
                        }
                    }
                }
            }
            
            // 确保问题质量和多样性
            if (questions.size() < 5) {
                // 根据经验级别生成相应的默认问题
                if ("高级".equals(experienceLevel)) {
                    questions.addAll(Arrays.asList(
                        "您在架构设计时如何考虑系统的可扩展性和可维护性？",
                        "请详细描述您主导过的一个复杂项目的技术架构和决策过程。",
                        "在您的工作经历中，如何解决团队中的技术分歧和冲突？",
                        "您如何评估和选择技术栈，有哪些考虑因素？",
                        "作为技术负责人，您如何制定团队的技术路线和发展规划？"
                    ));
                } else if ("中级".equals(experienceLevel)) {
                    questions.addAll(Arrays.asList(
                        "请详细说明您在项目中解决的一个复杂技术难题。",
                        "您如何优化系统性能，能否举例说明？",
                        "在团队协作中，您如何确保代码质量和开发效率？",
                        "您如何学习新技术并将其应用到实际项目中？",
                        "在项目开发过程中，您如何处理需求变更？"
                    ));
                } else {
                    questions.addAll(Arrays.asList(
                        "请介绍一下您的技术背景和主要项目经验。",
                        "您最熟悉的编程语言是什么，能详细说明您的实际应用经验吗？",
                        "您如何理解面向对象编程的基本原则？",
                        "在团队开发中，您如何进行代码协作和版本管理？",
                        "您如何解决在开发过程中遇到的技术难题？"
                    ));
                }
            }
            
            // 去重并限制数量
            Set<String> uniqueQuestions = new LinkedHashSet<>(questions);
            List<String> finalQuestions = new ArrayList<>(uniqueQuestions);
            int maxQuestions = "comprehensive".equals(analysisDepth) ? 12 : 8;
            return finalQuestions.subList(0, Math.min(finalQuestions.size(), maxQuestions));
            
        } catch (Exception e) {
            log.error("使用AI生成面试问题失败: {}", e.getMessage());
            // 返回默认问题，根据经验级别调整
            int workYears = calculateWorkYears(workExperienceList);
            if (workYears >= 5) {
                return Arrays.asList(
                    "请详细介绍您负责过的一个重要项目，您在其中的角色和贡献。",
                    "您如何设计和实现一个高性能、可扩展的系统？",
                    "在技术选型上，您会考虑哪些因素？请举例说明。",
                    "您如何处理团队中的技术分歧和冲突？",
                    "您如何评估和提升团队成员的技术能力？"
                );
            } else {
                return Arrays.asList(
                    "请介绍一下您的技术背景和工作经验。",
                    "您最擅长的编程语言或技术栈是什么？",
                    "您在项目中遇到过哪些技术难题，如何解决的？",
                    "您如何理解软件工程的最佳实践？",
                    "您如何与团队成员进行技术沟通和协作？"
                );
            }
        }
    }
    
    /**
     * 计算工作年限
     */
    private int calculateWorkYears(List<ResumeWorkExperience> workExperienceList) {
        if (workExperienceList == null || workExperienceList.isEmpty()) {
            return 0;
        }
        
        int totalMonths = 0;
        for (ResumeWorkExperience exp : workExperienceList) {
            if (exp.getStartDate() != null) {
                Date startDate = exp.getStartDate();
                Date endDate = exp.getEndDate() != null ? exp.getEndDate() : new Date();
                
                long diffInMillies = endDate.getTime() - startDate.getTime();
                long diffInMonths = diffInMillies / (1000 * 60 * 60 * 24 * 30);
                totalMonths += diffInMonths;
            }
        }
        
        // 转换为年，向下取整
        return totalMonths / 12;
    }
    
    /**
     * 提取主要技术栈
     */
    private String extractMainTechnicalStack(List<ResumeSkill> skillList) {
        if (skillList == null || skillList.isEmpty()) {
            return "未知";
        }
        
        // 按熟练度排序，取前5个技能
        return skillList.stream()
            .sorted(Comparator.comparing(ResumeSkill::getLevel, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .map(ResumeSkill::getName)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
    }
    
    /**
     * 判断技能是否为与职位相关的主要技能
     */
    private boolean isPrimarySkill(String skillName, String jobType) {
        if (skillName == null || jobType == null) {
            return false;
        }
        
        skillName = skillName.toLowerCase();
        jobType = jobType.toLowerCase();
        
        // 根据职位类型定义主要技能关键词
        Map<String, List<String>> jobKeySkills = new HashMap<>();
        jobKeySkills.put("java", Arrays.asList("java", "spring", "mybatis", "maven", "jvm"));
        jobKeySkills.put(".net", Arrays.asList(".net", "c#", "asp.net", "entity framework", "microsoft sql server"));
        jobKeySkills.put("前端", Arrays.asList("javascript", "react", "vue", "angular", "typescript", "css", "html"));
        jobKeySkills.put("后端", Arrays.asList("java", "python", "go", "node.js", "spring", "django", "flask"));
        jobKeySkills.put("python", Arrays.asList("python", "django", "flask", "pandas", "numpy", "tensorflow"));
        jobKeySkills.put("算法", Arrays.asList("python", "c++", "机器学习", "深度学习", "数据结构", "算法"));
        jobKeySkills.put("数据分析", Arrays.asList("python", "sql", "pandas", "numpy", "数据挖掘", "bi"));
        jobKeySkills.put("测试", Arrays.asList("java", "python", "selenium", "jmeter", "appium", "测试自动化"));
        jobKeySkills.put("运维", Arrays.asList("linux", "docker", "kubernetes", "ci/cd", "shell", "脚本"));
        jobKeySkills.put("全栈", Arrays.asList("javascript", "python", "java", "react", "vue", "node.js", "spring"));
        jobKeySkills.put("ios", Arrays.asList("swift", "objective-c", "ios", "xcode"));
        jobKeySkills.put("android", Arrays.asList("kotlin", "java", "android", "gradle"));
        
        // 检查是否包含在任何主要技能列表中
        for (Map.Entry<String, List<String>> entry : jobKeySkills.entrySet()) {
            String jobKey = entry.getKey();
            List<String> keySkills = entry.getValue();
            
            // 如果职位类型包含某个关键词，且技能在对应列表中
            if (jobType.contains(jobKey) && keySkills.stream().anyMatch(keySkill -> skillName.contains(keySkill))) {
                return true;
            }
        }
        
        // 通用技能检查
        List<String> commonTechnicalSkills = Arrays.asList(
            "java", "python", "c++", "c#", "javascript", "typescript", "go", "rust", "php", "ruby",
            "sql", "nosql", "mysql", "oracle", "postgresql", "mongodb", "redis", "elasticsearch",
            "spring", "hibernate", "mybatis", "django", "flask", "express", "vue", "react", "angular",
            "docker", "kubernetes", "ci/cd", "jenkins", "git", "linux", "aws", "azure", "gcp",
            "微服务", "分布式", "架构", "高并发", "性能优化", "安全", "加密", "算法", "数据结构"
        );
        
        // 检查是否为通用技术技能
        return commonTechnicalSkills.stream().anyMatch(keyword -> skillName.contains(keyword));
        }
    }

    private List<String> extractTechnologies(String description) {
        List<String> technologies = new ArrayList<>();
        
        // 常见的技术关键词
        String[] techKeywords = {
            "Java", "Spring", "MySQL", "Redis", "Docker", "Kubernetes",
            "React", "Vue", "JavaScript", "Python", "MongoDB", "Elasticsearch",
            "Kafka", "RabbitMQ", "Nginx", "Linux", "Git", "Jenkins"
        };
        
        if (description != null) {
            for (String keyword : techKeywords) {
                if (description.toLowerCase().contains(keyword.toLowerCase())) {
                    technologies.add(keyword);
                }
            }
        }
        
        return technologies;
    }

    private String assessProjectComplexity(ResumeAnalysisDTO.ProjectInfo project) {
        int complexityScore = 0;
        
        if (project != null && project.getDescription() != null) {
            String desc = project.getDescription().toLowerCase();
            
            if (desc.contains("高并发") || desc.contains("分布式")) complexityScore += 3;
            if (desc.contains("微服务") || desc.contains("架构")) complexityScore += 2;
            if (desc.contains("大数据") || desc.contains("ai") || desc.contains("机器学习")) complexityScore += 2;
            if (desc.contains("性能优化") || desc.contains("调优")) complexityScore += 1;
        }
        
        if (complexityScore >= 5) return "高复杂度";
        if (complexityScore >= 3) return "中等复杂度";
        return "基础复杂度";
    }

    private String analyzeBusinessImpact(String description) {
        if (description == null) return "一般业务价值";
        
        String desc = description.toLowerCase();
        
        if (desc.contains("营收") || desc.contains("利润") || desc.contains("成本降低")) {
            return "高业务价值";
        }
        if (desc.contains("效率提升") || desc.contains("用户体验")) {
            return "中等业务价值";
        }
        
        return "基础业务价值";
    }

    private List<String> generateStrengthsWithAI(Resume resume, List<ResumeEducation> educationList, 
                                             List<ResumeProject> projectList, List<ResumeSkill> skillList, 
                                             List<ResumeWorkExperience> workExperienceList) {
        try {
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请基于以下简历信息，分析候选人的优势，列出3-5点主要优势。\n\n");
            
            // 添加求职意向
            if (resume.getJobTitle() != null && !resume.getJobTitle().isEmpty()) {
                prompt.append("【求职意向】\n").append(resume.getJobTitle()).append("\n\n");
            }
            
            // 添加技能信息
            if (skillList != null && !skillList.isEmpty()) {
                prompt.append("【技能】\n");
                for (ResumeSkill skill : skillList) {
                    if (skill.getName() != null && !skill.getName().isEmpty()) {
                        prompt.append(skill.getName()).append(" (熟练度: ")
                              .append(skill.getLevel() != null ? skill.getLevel() : "未知").append(")\n");
                    }
                }
                prompt.append("\n");
            }
            
            // 添加项目经验
            if (projectList != null && !projectList.isEmpty()) {
                prompt.append("【项目经验】\n");
                for (ResumeProject project : projectList.subList(0, Math.min(2, projectList.size()))) {
                    if (project.getProjectName() != null) {
                        prompt.append("项目名称: ").append(project.getProjectName()).append("\n");
                        prompt.append("角色: ").append(project.getRole() != null ? project.getRole() : "未知").append("\n");
                        prompt.append("技术栈: ").append(project.getTechStack() != null ? project.getTechStack() : "未知").append("\n\n");
                    }
                }
            }
            
            // 添加工作经验
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                prompt.append("【工作经验】\n");
                for (ResumeWorkExperience workExp : workExperienceList.subList(0, Math.min(2, workExperienceList.size()))) {
                    if (workExp.getCompanyName() != null && workExp.getPositionName() != null) {
                        prompt.append(workExp.getCompanyName()).append(" - ")
                              .append(workExp.getPositionName()).append("\n");
                    }
                }
                prompt.append("\n");
            }
            
            // 添加教育背景
            if (educationList != null && !educationList.isEmpty()) {
                ResumeEducation highestEducation = educationList.stream()
                    .max(Comparator.comparing(ResumeEducation::getOrderIndex))
                    .orElse(null);
                if (highestEducation != null && highestEducation.getSchool() != null) {
                    prompt.append("【教育背景】\n");
                    prompt.append(highestEducation.getSchool()).append(" - ")
                          .append(highestEducation.getDegree()).append(" - ")
                          .append(highestEducation.getMajor()).append("\n\n");
                }
            }
            
            prompt.append("请以简洁的方式列出候选人的主要优势，每点优势单独一行，不要添加序号或前缀。优势应该具体、有针对性，能够突出候选人的竞争优势。");
            
            // 调用DeepSeek API
            String result = aiServiceUtils.callDeepSeekApi(prompt.toString());
            
            // 解析结果
            List<String> strengths = new ArrayList<>();
            if (result != null && !result.isEmpty()) {
                String[] lines = result.split("\n");
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        // 移除可能的序号
                        String strength = trimmedLine.replaceAll("^\\d+\\.?\\s*", "");
                        // 移除可能的前缀
                        strength = strength.replaceAll("^[\u4e00-\u9fa5]+：", "");
                        strength = strength.replaceAll("^优势[0-9]?:", "");
                        if (!strength.isEmpty()) {
                            strengths.add(strength);
                        }
                    }
                }
            }
            
            // 如果AI返回为空，使用默认优势
            if (strengths.isEmpty()) {
                strengths.add("具备扎实的技术基础");
                strengths.add("有相关项目开发经验");
                strengths.add("具备学习新技术的能力");
            }
            
            return strengths;
        } catch (Exception e) {
            log.error("使用AI生成优势分析失败: {}", e.getMessage());
            // 返回默认优势
            return Arrays.asList("具备基本的开发能力", 
                                "有一定的学习能力");
        }
    }
    
    // 保留原有方法但标记为过时，确保兼容性
    @Deprecated
    private List<String> generateStrengths(ResumeAnalysisDTO analysisDTO) {
        List<String> strengths = new ArrayList<>();
        strengths.add("具备基本的技术能力");
        return strengths;
    }

    private List<String> generateImprovementsWithAI(Resume resume, List<ResumeEducation> educationList, 
                                                List<ResumeProject> projectList, List<ResumeSkill> skillList, 
                                                List<ResumeWorkExperience> workExperienceList) {
        try {
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请基于以下简历信息，分析候选人可以改进的方面，提出3-5点具体的改进建议。\n\n");
            
            // 添加求职意向
            if (resume.getJobTitle() != null && !resume.getJobTitle().isEmpty()) {
                prompt.append("【求职意向】\n").append(resume.getJobTitle()).append("\n\n");
            }
            
            // 添加简历中可能存在的不足信息
            if (skillList == null || skillList.isEmpty()) {
                prompt.append("【技能信息较少】\n技能列表为空或不完整\n\n");
            }
            
            if (projectList == null || projectList.isEmpty()) {
                prompt.append("【项目经验较少】\n项目经验为空或不完整\n\n");
            } else {
                // 检查项目描述是否详细
                boolean hasDetailedProjects = false;
                for (ResumeProject project : projectList) {
                    if (project.getDescription() != null && project.getDescription().length() > 50) {
                        hasDetailedProjects = true;
                        break;
                    }
                }
                if (!hasDetailedProjects) {
                    prompt.append("【项目描述较为简略】\n项目描述缺乏技术细节和业务价值说明\n\n");
                }
            }
            
            // 添加现有的技能信息
            if (skillList != null && !skillList.isEmpty()) {
                prompt.append("【当前技能】\n");
                for (ResumeSkill skill : skillList) {
                    if (skill.getName() != null) {
                        prompt.append(skill.getName()).append("\n");
                    }
                }
                prompt.append("\n");
            }
            
            // 添加项目信息
            if (projectList != null && !projectList.isEmpty() && projectList.size() > 0) {
                ResumeProject mainProject = projectList.get(0);
                if (mainProject != null && mainProject.getProjectName() != null) {
                    prompt.append("【主要项目】\n项目名称: ")
                          .append(mainProject.getProjectName()).append("\n描述: ")
                          .append(mainProject.getDescription() != null ? mainProject.getDescription() : "无").append("\n\n");
                }
            }
            
            prompt.append("请提供具体、实用的改进建议，每点建议单独一行，不要添加序号或前缀。建议应该具体可行，能够帮助候选人提升简历质量和竞争力。");
            
            // 调用DeepSeek API
            String result = aiServiceUtils.callDeepSeekApi(prompt.toString());
            
            // 解析结果
            List<String> improvements = new ArrayList<>();
            if (result != null && !result.isEmpty()) {
                String[] lines = result.split("\n");
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        // 移除可能的序号
                        String improvement = trimmedLine.replaceAll("^\\d+\\.?\\s*", "");
                        // 移除可能的前缀
                        improvement = improvement.replaceAll("^[\u4e00-\u9fa5]+：", "");
                        improvement = improvement.replaceAll("^建议[0-9]?:", "");
                        if (!improvement.isEmpty()) {
                            improvements.add(improvement);
                        }
                    }
                }
            }
            
            // 如果AI返回为空，使用默认改进建议
            if (improvements.isEmpty()) {
                improvements.add("建议详细列出具体的项目经验和技术实现细节");
                improvements.add("可以增加技能熟练度的具体说明");
                improvements.add("建议突出在项目中解决的技术难题和取得的成果");
            }
            
            return improvements;
        } catch (Exception e) {
            log.error("使用AI生成改进建议失败: {}", e.getMessage());
            // 返回默认改进建议
            return Arrays.asList("可以进一步丰富项目经验描述", 
                                "建议详细列出具体的技术技能和熟练度");
        }
    }
    
    // 保留原有方法但标记为过时，确保兼容性
    @Deprecated
    private List<String> generateImprovements(ResumeAnalysisDTO analysisDTO) {
        List<String> improvements = new ArrayList<>();
        improvements.add("建议提升技术深度和广度");
        return improvements;
    }

    // 用于测试generateInterviewQuestionsWithAI方法的功能验证方法
    public void validateInterviewQuestionsGeneration() {
        try {
            log.info("开始验证面试问题生成功能...");
            
            // 创建测试数据
            Resume testResume = new Resume();
            testResume.setName("测试候选人");
            testResume.setJobTitle("Java后端工程师");
            
            // 创建测试技能列表
            List<ResumeSkill> testSkills = new ArrayList<>();
            ResumeSkill skill1 = new ResumeSkill();
            skill1.setName("Java");
            skill1.setLevel(9);
            testSkills.add(skill1);
            
            ResumeSkill skill2 = new ResumeSkill();
            skill2.setName("Spring Boot");
            skill2.setLevel(8);
            testSkills.add(skill2);
            
            // 创建测试工作经验
            List<ResumeWorkExperience> testWorkExp = new ArrayList<>();
            ResumeWorkExperience exp1 = new ResumeWorkExperience();
            exp1.setCompanyName("测试公司");
            exp1.setPositionName("后端开发工程师");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -3);
            exp1.setStartDate(cal.getTime());
            testWorkExp.add(exp1);
            
            // 调用改进后的方法生成面试问题
            List<String> questions = generateInterviewQuestionsWithAI(
                testResume, new ArrayList<>(), new ArrayList<>(), testSkills, testWorkExp, "Java", "comprehensive");
            
            log.info("生成的面试问题数量: {}", questions.size());
            for (int i = 0; i < questions.size(); i++) {
                log.info("问题{}: {}", i + 1, questions.get(i));
            }
            
            // 验证结果
            if (questions.size() >= 8) {
                log.info("面试问题生成功能验证通过：成功生成了足够数量的问题");
            } else {
                log.warn("面试问题生成功能验证警告：生成的问题数量少于预期");
            }
            
        } catch (Exception e) {
            log.error("验证面试问题生成功能失败: {}", e.getMessage(), e);
        }
    }
    
    private List<String> generateInterviewQuestions(ResumeAnalysisDTO analysisDTO, String jobType, String analysisDepth) {
        List<String> questions = new ArrayList<>();
        
        if (analysisDTO == null) {
            questions.add("请介绍一下您的技术背景和工作经验");
            return questions;
        }
        
        try {
            // 基础问题
            questions.add("请介绍一下您的技术背景和工作经验");
            
            // 基于技能的问题
            if (analysisDTO.getTechnicalAnalysis() != null && analysisDTO.getTechnicalAnalysis().getPrimarySkills() != null) {
                List<String> primarySkills = analysisDTO.getTechnicalAnalysis().getPrimarySkills();
                if (!primarySkills.isEmpty()) {
                    String mainSkill = primarySkills.get(0);
                    questions.add("请详细说明您在" + mainSkill + "方面的具体实践经验");
                }
            }
            
            // 基于项目的问题
            if (analysisDTO.getProjectAnalysis() != null && analysisDTO.getProjectAnalysis().getKeyProjects() != null) {
                List<ResumeAnalysisDTO.ProjectInfo> keyProjects = analysisDTO.getProjectAnalysis().getKeyProjects();
                if (!keyProjects.isEmpty()) {
                    ResumeAnalysisDTO.ProjectInfo mainProject = keyProjects.get(0);
                    questions.add("请详细介绍" + (mainProject.getName() != null ? mainProject.getName() : "您的主要项目") + "，您在其中承担的角色和职责是什么？");
                    questions.add("在项目中遇到过哪些技术难题，您是如何解决的？");
                }
            }
            
            // 基于经验级别的问题
            String experienceLevel = analysisDTO.getExperienceLevel();
            if (experienceLevel != null && ("高级".equals(experienceLevel) || "专家级".equals(experienceLevel))) {
                questions.add("请分享一个您主导或架构设计的项目案例");
                questions.add("您如何评估和优化系统性能？");
            }
            
            // 如果是全面分析，添加更多问题
            if ("comprehensive".equals(analysisDepth)) {
                questions.add("您如何看待技术栈的选择和演进？");
                questions.add("请分享您在团队协作中的经验和体会");
                questions.add("您对未来的技术发展有什么规划？");
            }
        } catch (Exception e) {
            log.error("生成面试问题时发生异常: {}", e.getMessage());
            questions.clear();
            questions.add("请介绍一下您的技术背景和工作经验");
        }
        
        return questions;
    }

    private Double calculateOverallScore(Resume resume, List<ResumeEducation> educationList, 
                                      List<ResumeProject> projectList, List<ResumeSkill> skillList, 
                                      List<ResumeWorkExperience> workExperienceList, String jobType) {
        double score = 0.0;
        
        try {
            // 基于工作年限的评分（最高40分）
            Integer workYears = calculateWorkYears(workExperienceList);
            if (workYears >= 7) {
                score += 40;
            } else if (workYears >= 5) {
                score += 30;
            } else if (workYears >= 3) {
                score += 20;
            } else if (workYears >= 1) {
                score += 10;
            }
            
            // 基于项目经验的评分（最高30分）
            if (projectList != null && !projectList.isEmpty()) {
                // 项目数量评分
                int projectCount = Math.min(projectList.size(), 5);
                score += projectCount * 5;
                
                // 项目质量评分（有详细描述的项目加分）
                long detailedProjects = projectList.stream()
                    .filter(p -> p.getDescription() != null && p.getDescription().length() > 100)
                    .count();
                score += detailedProjects * 2;
            }
            
            // 基于技能的评分（最高20分）
            if (skillList != null && !skillList.isEmpty()) {
                // 技能数量评分
                int skillCount = Math.min(skillList.size(), 8);
                score += skillCount * 2;
                
                // 高级技能评分
                long advancedSkills = skillList.stream()
                    .filter(s -> s.getLevel() != null && s.getLevel() >= 8)
                    .count();
                score += advancedSkills * 2;
                
                // 与职位相关的技能评分
                long relevantSkills = skillList.stream()
                    .filter(s -> s.getName() != null && isPrimarySkill(s.getName(), jobType))
                    .count();
                score += relevantSkills * 1;
            }
            
            // 基于教育背景的评分（最高10分）
            if (educationList != null && !educationList.isEmpty()) {
                ResumeEducation highestEducation = educationList.stream()
                    .max(Comparator.comparing(ResumeEducation::getOrderIndex))
                    .orElse(null);
                
                if (highestEducation != null && highestEducation.getDegree() != null) {
                    String degree = highestEducation.getDegree();
                    if (degree.contains("博士")) {
                        score += 10;
                    } else if (degree.contains("硕士")) {
                        score += 8;
                    } else if (degree.contains("本科")) {
                        score += 5;
                    }
                }
            }
        } catch (Exception e) {
            log.error("计算综合评分失败: {}", e.getMessage());
        }
        
        // 确保分数在0-100之间
        return Math.min(100.0, Math.max(0.0, score));
    }
    
    // 保留原有方法但标记为过时，确保兼容性
    @Deprecated
    private Integer calculateOverallScore(ResumeAnalysisDTO analysisDTO) {
        return 60; // 返回默认分数
    }

    private ResumeAnalysisDTO createDefaultAnalysisDTO() {
        ResumeAnalysisDTO defaultDTO = new ResumeAnalysisDTO();
        
        // 设置默认候选人信息
        defaultDTO.setCandidateInfo(new ResumeAnalysisDTO.CandidateInfo());
        
        // 设置默认技术分析
        ResumeAnalysisDTO.TechnicalAnalysis techAnalysis = new ResumeAnalysisDTO.TechnicalAnalysis();
        techAnalysis.setPrimarySkills(new ArrayList<>());
        techAnalysis.setSecondarySkills(new ArrayList<>());
        techAnalysis.setSkillProficiency(new HashMap<>());
        defaultDTO.setTechnicalAnalysis(techAnalysis);
        
        // 设置默认项目分析
        ResumeAnalysisDTO.ProjectAnalysis projectAnalysis = new ResumeAnalysisDTO.ProjectAnalysis();
        projectAnalysis.setTotalProjects(0);
        projectAnalysis.setKeyProjects(new ArrayList<>());
        defaultDTO.setProjectAnalysis(projectAnalysis);
        
        // 设置默认业务分析
        defaultDTO.setBusinessAnalysis(new ResumeAnalysisDTO.BusinessAnalysis());
        
        // 设置默认经验级别
        defaultDTO.setExperienceLevel("入门级");
        
        // 设置默认优势
        defaultDTO.setStrengths(Collections.singletonList("基础能力具备"));
        
        // 设置默认待提升项
        defaultDTO.setImprovements(Collections.singletonList("建议提升技术深度和广度"));
        
        // 设置默认面试问题
        defaultDTO.setInterviewQuestions(Collections.singletonList("请介绍一下您的技术背景和工作经验"));
        
        // 设置默认评分
        defaultDTO.setOverallScore(60);
        
        return defaultDTO;
    }
}