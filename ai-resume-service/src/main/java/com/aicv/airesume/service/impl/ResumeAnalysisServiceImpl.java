package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.entity.ResumeEducation;
import com.aicv.airesume.entity.ResumeProject;
import com.aicv.airesume.entity.ResumeSkill;
import com.aicv.airesume.entity.ResumeWorkExperience;
import com.aicv.airesume.entity.User;
import com.aicv.airesume.model.dto.QuestionAnalysisDTO;
import com.aicv.airesume.model.dto.ResumeAnalysisDTO;
import com.aicv.airesume.service.ResumeAnalysisService;
import com.aicv.airesume.service.ResumeService;
import com.aicv.airesume.service.UserService;
import com.aicv.airesume.repository.ResumeEducationRepository;
import com.aicv.airesume.repository.ResumeProjectRepository;
import com.aicv.airesume.repository.ResumeSkillRepository;
import com.aicv.airesume.repository.ResumeWorkExperienceRepository;
import com.aicv.airesume.utils.AiServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisServiceImpl.class);
    
    // 缓存有效期：30分钟
    private static final Duration CACHE_EXPIRATION = Duration.ofMinutes(30);
    
    // 缓存分析结果，使用ConcurrentHashMap保证线程安全
    private final Map<String, CachedAnalysis> analysisCache = new ConcurrentHashMap<>();

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
    
    @Autowired
    private UserService userService;
    
    /**
     * 缓存分析结果的内部类，包含分析结果和过期时间
     */
    private static class CachedAnalysis {
        private final ResumeAnalysisDTO analysis;  // 分析结果
        private final LocalDateTime expirationTime;  // 过期时间
        
        public CachedAnalysis(ResumeAnalysisDTO analysis, Duration expiration) {
            this.analysis = analysis;
            this.expirationTime = LocalDateTime.now().plus(expiration);
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expirationTime);
        }
        
        public ResumeAnalysisDTO getAnalysis() {
            return analysis;
        }
    }

    @Override
    public ResumeAnalysisDTO analyzeResume(Long resumeId, String jobType, String analysisDepth) {
        try {
            log.info("开始分析简历，resumeId: {}, jobType: {}, analysisDepth: {}", resumeId, jobType, analysisDepth);
            
            // 构建缓存键
            String cacheKey = String.format("%d:%s:%s", resumeId, jobType, analysisDepth);
            
            // 检查缓存
            CachedAnalysis cachedAnalysis = analysisCache.get(cacheKey);
            if (cachedAnalysis != null && !cachedAnalysis.isExpired()) {
                ResumeAnalysisDTO cachedResult = cachedAnalysis.getAnalysis();
                // 验证缓存结果的关键字段完整性
                if (validateAnalysisDTO(cachedResult)) {
                    log.info("从缓存获取简历分析结果: {}", cacheKey);
                    return cachedResult;
                } else {
                    log.warn("缓存结果不完整，将重新分析: {}", cacheKey);
                }
            }
            
            // 获取简历基础数据
            Resume resume = resumeService.getResumeById(resumeId);
            if (resume == null) {
                log.error("未找到简历: {}", resumeId);
                throw new RuntimeException("未找到简历");
            }
            
            // 查询关联数据 - 确保集合不为null
            List<ResumeEducation> educationList = resumeEducationRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
            List<ResumeProject> projectList = resumeProjectRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
            List<ResumeSkill> skillList = resumeSkillRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
            List<ResumeWorkExperience> workExperienceList = resumeWorkExperienceRepository.findByResumeIdOrderByOrderIndexAsc(resumeId);
            
            // 确保关联数据集合不为null
            if (educationList == null) educationList = new ArrayList<>();
            if (projectList == null) projectList = new ArrayList<>();
            if (skillList == null) skillList = new ArrayList<>();
            if (workExperienceList == null) workExperienceList = new ArrayList<>();
            
            // 分析简历内容
            ResumeAnalysisDTO analysisDTO = analyzeResumeContent(resume, educationList, projectList, skillList, workExperienceList, jobType, analysisDepth);
            
            // 验证并确保关键字段完整性
            if (!validateAnalysisDTO(analysisDTO)) {
                log.warn("分析结果关键字段不完整，正在补充必要信息: resumeId={}", resumeId);
                ensureKeyFieldsComplete(analysisDTO, workExperienceList, projectList, skillList);
            }
            
            // 存入缓存
            analysisCache.put(cacheKey, new CachedAnalysis(analysisDTO, CACHE_EXPIRATION));
            
            // 记录分析结果的关键信息
            log.info("简历分析完成并缓存: resumeId={}, 工作年限={}, 经验级别={}, 生成了{}个面试问题", 
                    resumeId, 
                    analysisDTO.getCandidateInfo() != null ? analysisDTO.getCandidateInfo().getWorkYears() : null,
                    analysisDTO.getExperienceLevel(),
                    analysisDTO.getInterviewQuestions() != null && analysisDTO.getInterviewQuestions().getQuestions() != null ? 
                            analysisDTO.getInterviewQuestions().getQuestions().size() : 0);
            
            return analysisDTO;
        } catch (Exception e) {
            log.error("分析简历失败: {}", e.getMessage(), e);
            // 返回增强的默认分析结果，确保包含必要的关键字段
            throw new RuntimeException("分析简历失败", e);
        }
    }
    
    /**
     * 提取领导经验信息
     * @param workExperienceList 工作经历列表
     * @param projectList 项目经历列表
     * @return 领导经验描述
     */
    private String extractLeadershipExperience(List<ResumeWorkExperience> workExperienceList, List<ResumeProject> projectList) {
        StringBuilder leadershipInfo = new StringBuilder();
        
        // 从工作经历中提取领导相关信息
        if (workExperienceList != null) {
            for (ResumeWorkExperience exp : workExperienceList) {
                if (exp.getPositionName() != null && (exp.getPositionName().contains("经理") || 
                    exp.getPositionName().contains("主管") || exp.getPositionName().contains("负责人") ||
                    exp.getPositionName().contains("Leader") || exp.getPositionName().contains("Manager"))) {
                    leadershipInfo.append("担任").append(exp.getPositionName()).append("，");
                }
            }
        }
        
        // 从项目经历中提取领导相关信息
        if (projectList != null) {
            int leadProjectCount = 0;
            for (ResumeProject project : projectList) {
                if (project.getRole() != null && (project.getRole().contains("负责") || 
                    project.getRole().contains("主导") || project.getRole().contains("lead") ||
                    project.getRole().contains("负责") || project.getRole().contains("主管"))) {
                    leadProjectCount++;
                }
            }
            if (leadProjectCount > 0) {
                leadershipInfo.append("主导过").append(leadProjectCount).append("个项目");
            }
        }
        
        // 如果没有提取到领导经验，返回默认值
        if (leadershipInfo.length() == 0) {
            return "无明确领导经验描述";
        }
        
        // 移除末尾的逗号并返回
        String result = leadershipInfo.toString();
        if (result.endsWith("，")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
    
    /**
     * 验证分析结果DTO的关键字段完整性
     */
    private boolean validateAnalysisDTO(ResumeAnalysisDTO analysisDTO) {
        if (analysisDTO == null) {
            return false;
        }
        
        // 验证候选人基本信息和工作年限
        if (analysisDTO.getCandidateInfo() == null || analysisDTO.getCandidateInfo().getWorkYears() == null) {
            return false;
        }
        
        // 验证经验级别
        if (analysisDTO.getExperienceLevel() == null || analysisDTO.getExperienceLevel().isEmpty()) {
            return false;
        }
        
        // 验证业务分析能力
        if (analysisDTO.getBusinessAnalysis() == null || 
            analysisDTO.getBusinessAnalysis().getDomainKnowledge() == null || 
            analysisDTO.getBusinessAnalysis().getDomainKnowledge().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 确保关键分析字段完整
     */
    private void ensureKeyFieldsComplete(ResumeAnalysisDTO analysisDTO, 
                                        List<ResumeWorkExperience> workExperienceList,
                                        List<ResumeProject> projectList,
                                        List<ResumeSkill> skillList) {
        // 确保候选人信息存在
        if (analysisDTO.getCandidateInfo() == null) {
            analysisDTO.setCandidateInfo(new ResumeAnalysisDTO.CandidateInfo());
        }
        
        // 确保工作年限存在
        if (analysisDTO.getCandidateInfo().getWorkYears() == null) {
            Integer workYears = calculateWorkYears(workExperienceList);
            analysisDTO.getCandidateInfo().setWorkYears(workYears);
            log.info("补充工作年限: {}", workYears);
        }
        
        // 确保经验级别存在
        if (analysisDTO.getExperienceLevel() == null || analysisDTO.getExperienceLevel().isEmpty()) {
            // 准备评估经验级别所需的数据
            Integer workYears = analysisDTO.getCandidateInfo().getWorkYears();
            int projectCount = projectList != null ? projectList.size() : 0;
            
            // 构建技能等级映射
            Map<String, Integer> skillLevelMap = new HashMap<>();
            if (skillList != null) {
                for (ResumeSkill skill : skillList) {
                    if (skill != null && skill.getName() != null && skill.getLevel() != null) {
                        try {
                            int level = skill.getLevel();
                            skillLevelMap.put(skill.getName(), Math.min(10, Math.max(1, level)));
                        } catch (NumberFormatException e) {
                            // 使用默认级别
                            skillLevelMap.put(skill.getName(), 5);
                        }
                    }
                }
            }
            
            String experienceLevel = assessExperienceLevel(workYears, projectCount, skillLevelMap);
            analysisDTO.setExperienceLevel(experienceLevel);
            log.info("补充经验级别: {}", experienceLevel);
        }
        
        // 确保业务分析能力存在
        if (analysisDTO.getBusinessAnalysis() == null) {
            analysisDTO.setBusinessAnalysis(new ResumeAnalysisDTO.BusinessAnalysis());
        }
        
        // 确保业务领域知识存在
        if (analysisDTO.getBusinessAnalysis().getDomainKnowledge() == null || 
            analysisDTO.getBusinessAnalysis().getDomainKnowledge().isEmpty()) {
            analysisDTO.getBusinessAnalysis().setDomainKnowledge(Collections.singletonList("业务领域分析中"));
        }
        
        // 确保软技能存在
        if (analysisDTO.getBusinessAnalysis().getSoftSkills() == null || 
            analysisDTO.getBusinessAnalysis().getSoftSkills().isEmpty()) {
            analysisDTO.getBusinessAnalysis().setSoftSkills(Arrays.asList("团队协作", "沟通表达", "问题解决"));
        }
    }
    
    /**
     * 刷新简历分析缓存
     * @param resumeId 简历ID，如果为null则刷新所有缓存
     */
    public void refreshCache(Long resumeId) {
        if (resumeId == null) {
            // 刷新所有缓存
            analysisCache.clear();
            log.info("所有简历分析缓存已清空");
        } else {
            // 只刷新指定简历的缓存
            String prefix = resumeId + ":";
            analysisCache.keySet().removeIf(key -> key.startsWith(prefix));
            log.info("简历ID={}的分析缓存已清空", resumeId);
        }
    }

    private ResumeAnalysisDTO analyzeResumeContent(Resume resume, List<ResumeEducation> educationList, 
                                                 List<ResumeProject> projectList, List<ResumeSkill> skillList,
                                                 List<ResumeWorkExperience> workExperienceList, String jobType, 
                                                 String analysisDepth) {
        try {
            log.info("开始分析简历内容: resumeId={}, userId={}, jobType={}", resume.getId(), resume.getUserId(), jobType);
            
            ResumeAnalysisDTO analysisDTO = new ResumeAnalysisDTO();
            
            // 1. 分析候选人基本信息 - 确保返回有效的CandidateInfo对象
            ResumeAnalysisDTO.CandidateInfo candidateInfo = parseCandidateInfo(resume, educationList, workExperienceList);
            if (candidateInfo == null) {
                candidateInfo = new ResumeAnalysisDTO.CandidateInfo();
                log.warn("解析候选人基本信息失败，创建默认CandidateInfo对象");
            }
            analysisDTO.setCandidateInfo(candidateInfo);
            
            // 2. 分析技术栈 - 确保返回有效的TechnicalAnalysis对象
            ResumeAnalysisDTO.TechnicalAnalysis technicalAnalysis = parseTechnicalAnalysis(skillList, jobType);
            if (technicalAnalysis == null) {
                technicalAnalysis = new ResumeAnalysisDTO.TechnicalAnalysis();
                technicalAnalysis.setSkillProficiency(new HashMap<>());
                log.warn("解析技术栈失败，创建默认TechnicalAnalysis对象");
            }
            analysisDTO.setTechnicalAnalysis(technicalAnalysis);
            
            // 3. 分析项目经验 - 确保返回有效的ProjectAnalysis对象
            ResumeAnalysisDTO.ProjectAnalysis projectAnalysis = parseProjectAnalysis(projectList);
            if (projectAnalysis == null) {
                projectAnalysis = new ResumeAnalysisDTO.ProjectAnalysis();
                projectAnalysis.setTotalProjects(0);
                log.warn("解析项目经验失败，创建默认ProjectAnalysis对象");
            }
            analysisDTO.setProjectAnalysis(projectAnalysis);
            
            // 4. 分析业务能力 - 确保返回有效的BusinessAnalysis对象
            ResumeAnalysisDTO.BusinessAnalysis businessAnalysis = parseBusinessAnalysis(workExperienceList);
            if (businessAnalysis == null) {
                // 创建默认业务分析对象并填充基础数据
                businessAnalysis = new ResumeAnalysisDTO.BusinessAnalysis();
                businessAnalysis.setDomainKnowledge(Collections.singletonList("业务领域分析中"));
                businessAnalysis.setSoftSkills(Arrays.asList("团队协作", "沟通表达", "问题解决"));
                log.warn("解析业务能力失败，创建默认BusinessAnalysis对象并设置基础数据");
            } else {
                // 确保业务分析对象的关键字段不为空
                if (businessAnalysis.getDomainKnowledge() == null || businessAnalysis.getDomainKnowledge().isEmpty()) {
                    businessAnalysis.setDomainKnowledge(Collections.singletonList("业务领域知识提取中"));
                }
                if (businessAnalysis.getSoftSkills() == null || businessAnalysis.getSoftSkills().isEmpty()) {
                    businessAnalysis.setSoftSkills(Arrays.asList("基础软技能具备"));
                }
            }
            analysisDTO.setBusinessAnalysis(businessAnalysis);
            
            // 5. 评估经验级别 - 确保生成有效的经验级别
            // 获取或计算工作年限
            Integer workYears = candidateInfo.getWorkYears();
            if (workYears == null) {
                // 重新计算工作年限
                workYears = calculateWorkYears(workExperienceList);
                candidateInfo.setWorkYears(workYears);
                log.info("重新计算工作年限: {}", workYears);
            }
            
            // 获取项目数量
            Integer totalProjects = projectAnalysis.getTotalProjects();
            if (totalProjects == null) {
                totalProjects = projectList != null ? projectList.size() : 0;
                projectAnalysis.setTotalProjects(totalProjects);
                log.info("重置项目总数: {}", totalProjects);
            }
            
            // 获取技能熟练度映射
            Map<String, Integer> skillProficiency = technicalAnalysis.getSkillProficiency();
            if (skillProficiency == null) {
                skillProficiency = new HashMap<>();
                technicalAnalysis.setSkillProficiency(skillProficiency);
            }
            
            // 调用优化后的经验级别评估方法
            String experienceLevel = assessExperienceLevel(workYears, totalProjects, skillProficiency);
            log.info("候选人经验级别评估结果: {}", experienceLevel);
            
            // 设置经验级别
            analysisDTO.setExperienceLevel(experienceLevel);
            
            // 6. 生成优势分析
            List<String> strengths = generateStrengthsWithAI(resume, educationList, projectList, skillList, workExperienceList);
            if (strengths == null || strengths.isEmpty()) {
                strengths = Arrays.asList("具备相关工作经验", "拥有一定的专业技能");
                log.warn("生成优势分析失败，使用默认优势描述");
            }
            analysisDTO.setStrengths(strengths);
            
            // 7. 生成待提升项
            List<String> improvements = generateImprovementsWithAI(resume, educationList, projectList, skillList, workExperienceList);
            if (improvements == null) {
                improvements = new ArrayList<>();
                log.warn("生成待提升项失败，创建空列表");
            }
            analysisDTO.setImprovements(improvements);
            
            // 8. 生成面试问题清单
            ResumeAnalysisDTO.InterviewQuestions interviewQuestions = generateInterviewQuestionsWithAI(
                resume, educationList, projectList, skillList, workExperienceList, jobType, analysisDepth);
            
            // 确保interviewQuestions不为空
            if (interviewQuestions == null) {
                interviewQuestions = new ResumeAnalysisDTO.InterviewQuestions();
                interviewQuestions.setQuestions(new ArrayList<>());
                log.warn("生成面试问题清单失败，创建空的InterviewQuestions对象");
            }
            
            // 确保analysisType已设置
            if (interviewQuestions.getAnalysisType() == null) {
                interviewQuestions.setAnalysisType(analysisDepth);
            }
            
            analysisDTO.setInterviewQuestions(interviewQuestions);
            
            // 9. 计算综合评分
            Double score = calculateOverallScore(resume, educationList, projectList, skillList, workExperienceList, jobType);
            analysisDTO.setOverallScore(score != null ? score.intValue() : null);
            
            // 10. 设置分析时间
            analysisDTO.setAnalysisTime(new Date());
            
            log.info("简历内容分析完成: 工作年限={}, 经验级别={}", workYears, experienceLevel);
            return analysisDTO;
        } catch (Exception e) {
            log.error("分析简历内容失败: {}", e.getMessage(), e);
            throw new RuntimeException("分析简历内容失败");
        }
    }

    private ResumeAnalysisDTO.CandidateInfo parseCandidateInfo(Resume resume, List<ResumeEducation> educationList, List<ResumeWorkExperience> workExperienceList) {
        try {
            ResumeAnalysisDTO.CandidateInfo info = new ResumeAnalysisDTO.CandidateInfo();
            
            // 从User表获取个人基本信息
            if (resume.getUserId() != null) {
                User user = userService.getUserById(resume.getUserId()).orElse(null);
                if (user != null) {
                    info.setName(user.getName());
                    info.setGender(user.getGender());
                    info.setPhone(user.getPhone());
                    info.setEmail(user.getEmail());
                }
            }
            
            // 职位意向仍然从Resume获取
            info.setJobTitle(resume.getJobTitle());
            
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
            List<ResumeAnalysisDTO.TechSkill> primarySkills = new ArrayList<>();
            List<ResumeAnalysisDTO.TechSkill> secondarySkills = new ArrayList<>();
            Map<String, Integer> skillProficiency = new HashMap<>();
            
            if (skillList != null && !skillList.isEmpty()) {
                for (ResumeSkill skill : skillList) {
                    String skillName = skill.getName();
                    if (skillName != null && !skillName.isEmpty()) {
                        // 创建TechSkill对象
                        ResumeAnalysisDTO.TechSkill techSkill = new ResumeAnalysisDTO.TechSkill();
                        techSkill.setName(skillName);
                        // 设置技能熟练度描述
                        Integer level = skill.getLevel() != null ? skill.getLevel() : 5;
                        techSkill.setProficiency(convertLevelToProficiency(level));
                        // 设置是否为核心技能
                        boolean isCore = isPrimarySkill(skillName, jobType);
                        techSkill.setIsCoreSkill(isCore);
                        
                        // 根据jobType判断是否是主要技能
                        if (isCore) {
                            primarySkills.add(techSkill);
                        } else {
                            secondarySkills.add(techSkill);
                        }
                        // 设置技能熟练度
                        skillProficiency.put(skillName, level);
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
    
    /**
     * 将技能等级转换为熟练度描述
     * @param level 技能等级（1-10）
     * @return 熟练度描述
     */
    private String convertLevelToProficiency(Integer level) {
        if (level >= 8) {
            return "精通";
        } else if (level >= 6) {
            return "熟练";
        } else if (level >= 4) {
            return "掌握";
        } else {
            return "了解";
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
                    projectInfo.setDuration(period.toString());
                    
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

    /**
     * 从文本中提取业务领域
     * @param text 要分析的文本内容（公司名称或职位名称）
     * @return 提取的业务领域，如果没有找到则返回null
     */
    private String extractBusinessDomainFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        String lowerText = text.toLowerCase();
        
        // 定义业务领域关键词映射
        Map<String, List<String>> domainKeywords = new HashMap<>();
        
        // 添加各业务领域的关键词
        domainKeywords.put("电商/零售", Arrays.asList("电商", "e-commerce", "retail", "零售", "shop", "商城", "淘宝", "天猫", 
                                                      "京东", "amazon", "购物", "订单", "支付", "payment", "交易", "transaction"));
        
        domainKeywords.put("金融/银行", Arrays.asList("金融", "finance", "banking", "银行", "保险", "insurance", "证券", "stock", 
                                                      "投资", "investment", "风控", "risk", "信用", "credit", "理财", "wealth"));
        
        domainKeywords.put("医疗健康", Arrays.asList("医疗", "healthcare", "hospital", "医院", "健康", "health", "药品", "pharmacy", 
                                                     "诊所", "clinic", "电子病历", "emr", "患者", "patient", "医生", "doctor"));
        
        domainKeywords.put("教育/培训", Arrays.asList("教育", "education", "培训", "training", "学习", "learning", "课程", "course", 
                                                     "学校", "school", "在线教育", "online education", "学习平台", "learning platform"));
        
        domainKeywords.put("社交媒体", Arrays.asList("社交", "social", "media", "媒体", "聊天", "chat", "通讯", "communication", 
                                                     "微博", "weibo", "微信", "wechat", "社区", "community", "论坛", "forum"));
        
        domainKeywords.put("旅游/出行", Arrays.asList("旅游", "travel", "traveling", "出行", "交通", "transport", "航班", "flight", 
                                                     "酒店", "hotel", "机票", "ticket", "预订", "booking", "旅行", "journey"));
        
        domainKeywords.put("房地产", Arrays.asList("房地产", "real estate", "property", "房产", "建筑", "construction", "开发", 
                                                   "物业", "property management", "销售", "sales", "经纪", "broker"));
        
        domainKeywords.put("互联网/科技", Arrays.asList("互联网", "internet", "科技", "technology", "it", "软件", "software", 
                                                        "硬件", "hardware", "通信", "telecommunication", "网络", "network", 
                                                        "人工智能", "ai", "artificial intelligence", "大数据", "big data"));
        
        // 检查文本是否匹配各个业务领域的关键词
        for (Map.Entry<String, List<String>> entry : domainKeywords.entrySet()) {
            String domain = entry.getKey();
            List<String> keywords = entry.getValue();
            
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    return domain;
                }
            }
        }
        
        return null; // 没有匹配的领域
    }
    
    private ResumeAnalysisDTO.BusinessAnalysis parseBusinessAnalysis(List<ResumeWorkExperience> workExperienceList) {
        try {
            ResumeAnalysisDTO.BusinessAnalysis businessAnalysis = new ResumeAnalysisDTO.BusinessAnalysis();
            List<String> domainKnowledge = new ArrayList<>();
            List<String> softSkills = new ArrayList<>();
            List<String> businessAchievements = new ArrayList<>();
            Map<String, Integer> industryExperienceCount = new HashMap<>();
            
            log.info("开始解析业务分析数据，工作经历数量: {}", 
                    workExperienceList != null ? workExperienceList.size() : 0);
            
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                // 1. 提取业务领域知识和行业经验统计
                for (ResumeWorkExperience workExp : workExperienceList) {
                    // 从公司名称中提取业务领域
                    if (workExp.getCompanyName() != null && !workExp.getCompanyName().isEmpty()) {
                        String domainFromCompany = extractBusinessDomainFromText(workExp.getCompanyName());
                        if (domainFromCompany != null) {
                            // 添加到领域知识列表
                            if (!domainKnowledge.contains(domainFromCompany)) {
                                domainKnowledge.add(domainFromCompany);
                                log.debug("从公司名称提取业务领域: {}", domainFromCompany);
                            }
                            // 统计行业经验次数
                            industryExperienceCount.put(domainFromCompany, 
                                    industryExperienceCount.getOrDefault(domainFromCompany, 0) + 1);
                        }
                    }
                    
                    // 从职位名称中提取业务领域
                    if (workExp.getPositionName() != null && !workExp.getPositionName().isEmpty()) {
                        String domainFromPosition = extractBusinessDomainFromText(workExp.getPositionName());
                        if (domainFromPosition != null && !domainKnowledge.contains(domainFromPosition)) {
                            domainKnowledge.add(domainFromPosition);
                            log.debug("从职位名称提取业务领域: {}", domainFromPosition);
                        }
                    }
                    
                    // 从工作描述中提取业务领域
                    if (workExp.getDescription() != null && !workExp.getDescription().isEmpty()) {
                        String domainFromDesc = extractBusinessDomainFromText(workExp.getDescription());
                        if (domainFromDesc != null && !domainKnowledge.contains(domainFromDesc)) {
                            domainKnowledge.add(domainFromDesc);
                            log.debug("从工作描述提取业务领域: {}", domainFromDesc);
                        }
                    }
                }
                
                // 2. 增强软技能识别
                // 定义更全面的软技能关键词，按类别分组
                Map<String, List<String>> softSkillCategories = new HashMap<>();
                softSkillCategories.put("沟通协作", Arrays.asList("沟通", "communication", "团队合作", "teamwork", 
                    "协作", "collaboration", "协调", "coordination", "演讲", "presentation"));
                softSkillCategories.put("领导力", Arrays.asList("领导", "leadership", "管理", "management", 
                    "项目管理", "project management", "团队管理", "team management"));
                softSkillCategories.put("问题解决", Arrays.asList("分析", "analysis", "解决问题", "problem solving", 
                    "故障排除", "troubleshooting", "优化", "optimization"));
                softSkillCategories.put("创新学习", Arrays.asList("创新", "innovation", "学习", "learning", 
                    "适应", "adaptation", "研究", "research"));
                softSkillCategories.put("自我管理", Arrays.asList("组织", "organization", "时间管理", "time management", 
                    "决策", "decision making", "压力", "pressure", "优先级", "priority"));
                softSkillCategories.put("业务能力", Arrays.asList("业务理解", "business understanding", "产品思维", 
                    "product thinking", "用户体验", "ux", "需求分析", "requirements analysis"));
                
                // 提取软技能、业务成就和职责信息
                for (ResumeWorkExperience workExp : workExperienceList) {
                    if (workExp.getDescription() != null && !workExp.getDescription().isEmpty()) {
                        // 分割描述为句子
                        String[] sentences = workExp.getDescription().split("[；。！？\n\r\n\t]");
                        
                        for (String sentence : sentences) {
                            String trimmedSentence = sentence.trim();
                            
                            // 过滤太短或无效的句子
                            if (trimmedSentence.length() < 4 || !trimmedSentence.matches(".*[a-zA-Z0-9\u4e00-\u9fa5].*")) {
                                continue;
                            }
                            
                            // 检查是否为业务成就（包含数字、成果等关键词）
                            if (isAchievementSentence(trimmedSentence)) {
                                businessAchievements.add(trimmedSentence);
                                log.debug("提取业务成就: {}", trimmedSentence);
                                continue;
                            }
                            
                            // 检查是否包含软技能关键词
                            boolean skillFound = false;
                            String lowerSentence = trimmedSentence.toLowerCase();
                            
                            for (Map.Entry<String, List<String>> category : softSkillCategories.entrySet()) {
                                for (String keyword : category.getValue()) {
                                    if (lowerSentence.contains(keyword.toLowerCase())) {
                                        // 提取更精确的软技能描述
                                        String skillDescription = extractSkillFromSentence(trimmedSentence, keyword);
                                        if (!softSkills.contains(skillDescription)) {
                                            softSkills.add(skillDescription);
                                            log.debug("从句子提取软技能[{}]: {}", category.getKey(), skillDescription);
                                        }
                                        skillFound = true;
                                        break;
                                    }
                                }
                                if (skillFound) break;
                            }
                            
                            // 如果没有识别到具体软技能，但句子包含职责描述，也作为重要信息保存
                            if (!skillFound && isResponsibilitySentence(trimmedSentence)) {
                                if (!softSkills.contains(trimmedSentence) && softSkills.size() < 15) { // 限制数量避免过多
                                    softSkills.add(trimmedSentence);
                                    log.debug("添加职责描述: {}", trimmedSentence);
                                }
                            }
                        }
                    }
                }
                
                // 3. 确保业务领域知识不为空
                if (domainKnowledge.isEmpty()) {
                    log.warn("未能从工作经历中提取业务领域知识，使用通用描述");
                    domainKnowledge.add("通用业务领域");
                }
                
                // 4. 确保软技能不为空
                if (softSkills.isEmpty()) {
                    log.warn("未能从工作经历中提取软技能，使用通用软技能描述");
                    softSkills.add("具备团队协作能力");
                    softSkills.add("良好的沟通表达能力");
                    softSkills.add("具备问题分析与解决能力");
                }
            } else {
                // 当没有工作经历时，设置默认值确保返回数据不为空
                log.warn("工作经历列表为空，设置默认业务分析数据");
                domainKnowledge.add("待补充业务领域经验");
                softSkills.add("具备基本的团队协作能力");
                softSkills.add("良好的学习能力");
            }
            
            // 5. 找出主要行业经验
            String primaryIndustry = null;
            int maxCount = 0;
            for (Map.Entry<String, Integer> entry : industryExperienceCount.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    primaryIndustry = entry.getKey();
                }
            }
            
            // 6. 设置业务分析数据
            businessAnalysis.setDomainKnowledge(domainKnowledge);
            businessAnalysis.setSoftSkills(softSkills);
            businessAnalysis.setLeadershipExperience(extractLeadershipExperience(workExperienceList, null));
            businessAnalysis.setCommunicationSkills("良好的团队协作与沟通能力");
            businessAnalysis.setProblemSolvingAbility("具备问题分析与解决能力");
            
            log.info("业务分析解析完成，提取领域知识{}个，软技能{}个", 
                    domainKnowledge.size(), softSkills.size());
            
            return businessAnalysis;
        } catch (Exception e) {
            log.error("解析业务分析失败: {}", e.getMessage(), e);
            // 发生异常时返回包含默认值的对象，确保不会返回空数据
            ResumeAnalysisDTO.BusinessAnalysis fallbackAnalysis = new ResumeAnalysisDTO.BusinessAnalysis();
            fallbackAnalysis.setDomainKnowledge(Collections.singletonList("业务领域分析中"));
            fallbackAnalysis.setSoftSkills(Arrays.asList("团队协作", "沟通表达", "问题解决"));
            fallbackAnalysis.setLeadershipExperience("待评估");
            fallbackAnalysis.setCommunicationSkills("待评估");
            fallbackAnalysis.setProblemSolvingAbility("待评估");
            return fallbackAnalysis;
        }
    }
    
    /**
     * 判断句子是否为成就描述（通常包含数字、成果等关键词）
     */
    private boolean isAchievementSentence(String sentence) {
        // 成就关键词
        Set<String> achievementKeywords = new HashSet<>(Arrays.asList(
            "提升", "优化", "节省", "增加", "减少", "提高", "改进", "成功", "完成",
            "实现", "突破", "首创", "主导", "负责", "带领", "创建", "设计", "开发",
            "deliver", "improve", "optimize", "reduce", "increase", "save", "achieve", "complete",
            "implement", "design", "develop", "lead", "create"
        ));
        
        // 检查是否包含数字
        boolean containsNumber = sentence.matches(".*\\d+.*");
        
        // 检查是否包含百分比符号
        boolean containsPercent = sentence.contains("%") || sentence.contains("percent");
        
        // 检查是否包含成就关键词
        boolean containsKeyword = false;
        String lowerSentence = sentence.toLowerCase();
        for (String keyword : achievementKeywords) {
            if (lowerSentence.contains(keyword.toLowerCase())) {
                containsKeyword = true;
                break;
            }
        }
        
        return containsNumber || containsPercent || containsKeyword;
    }
    
    /**
     * 判断句子是否为职责描述
     */
    private boolean isResponsibilitySentence(String sentence) {
        // 职责关键词
        Set<String> responsibilityKeywords = new HashSet<>(Arrays.asList(
            "负责", "参与", "协助", "跟进", "处理", "管理", "维护", "支持", "协调",
            "主导", "组织", "规划", "执行", "监督", "评估", "report to", "responsible for",
            "in charge of", "participate in", "assist with", "manage", "coordinate", "lead"
        ));
        
        String lowerSentence = sentence.toLowerCase();
        for (String keyword : responsibilityKeywords) {
            if (lowerSentence.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 从句子中提取更精确的技能描述
     */
    private String extractSkillFromSentence(String sentence, String keyword) {
        // 简单实现：返回包含关键词的短句或原文
        // 可以根据需要优化为更复杂的提取逻辑
        int keywordIndex = sentence.toLowerCase().indexOf(keyword.toLowerCase());
        if (keywordIndex > -1) {
            // 尝试提取关键词前后的相关内容
            int start = Math.max(0, keywordIndex - 20);
            int end = Math.min(sentence.length(), keywordIndex + keyword.length() + 30);
            
            // 尝试找到句子的自然边界
            if (start > 0 && Character.isLetterOrDigit(sentence.charAt(start))) {
                for (int i = start; i > 0; i--) {
                    if (!Character.isLetterOrDigit(sentence.charAt(i)) && sentence.charAt(i) != ' ') {
                        start = i + 1;
                        break;
                    }
                }
            }
            
            if (end < sentence.length() && Character.isLetterOrDigit(sentence.charAt(end))) {
                for (int i = end; i < sentence.length(); i++) {
                    if (!Character.isLetterOrDigit(sentence.charAt(i)) && sentence.charAt(i) != ' ') {
                        end = i;
                        break;
                    }
                }
            }
            
            String extracted = sentence.substring(start, end).trim();
            return extracted.length() > 0 ? extracted : sentence;
        }
        
        return sentence;
    }

    public Map<String, Object> getProfessionalQuestions(String jobType, String experienceLevel) {
        try {
            log.info("获取专业问题模板，jobType: {}, experienceLevel: {}", jobType, experienceLevel);
            
            Map<String, Object> result = new HashMap<>();
            List<String> questions = new ArrayList<>();
            
            // 根据职位类型和经验级别生成不同的专业问题
            if (jobType == null || jobType.isEmpty()) {
                jobType = "通用"; // 默认通用问题
            }
            
            // 添加基础问题
            questions.add("请介绍您在这个领域的核心技能和经验");
            questions.add("您如何解决工作中遇到的技术难题？");
            
            // 根据不同职位类型添加专业问题
            if (jobType.contains("Java") || jobType.contains("后端")) {
                questions.add("请解释Spring Boot的核心特性及其工作原理");
                questions.add("您如何处理高并发场景下的性能优化？");
                questions.add("请描述您对微服务架构的理解和实践经验");
            } else if (jobType.contains("前端") || jobType.contains("React") || jobType.contains("Vue")) {
                questions.add("请解释前端框架中的虚拟DOM工作原理");
                questions.add("您如何优化前端性能？");
                questions.add("请描述您对前端工程化的理解");
            } else if (jobType.contains("测试")) {
                questions.add("请描述您的测试方法论和常用测试工具");
                questions.add("您如何设计测试用例以确保代码质量？");
                questions.add("请解释自动化测试的优势和实施策略");
            }
            
            // 根据经验级别添加进阶问题
            if ("高级".equals(experienceLevel) || "专家".equals(experienceLevel)) {
                questions.add("请分享您的技术团队管理经验");
                questions.add("您如何制定技术路线图和架构决策？");
                questions.add("请描述您如何推动创新和技术团队成长");
            }
            
            result.put("questions", questions);
            result.put("jobType", jobType);
            result.put("experienceLevel", experienceLevel);
            
            return result;
        } catch (Exception e) {
            log.error("获取专业问题模板失败: {}", e.getMessage());
            Map<String, Object> defaultResult = new HashMap<>();
            defaultResult.put("questions", Arrays.asList("请介绍您的专业背景和技能"));
            return defaultResult;
        }
    }
    
    public Map<String, Object> analyzeTechnicalDepth(Map<String, Integer> skills, String projects) {
        try {
            log.info("分析技术栈深度");
            
            Map<String, Object> result = new HashMap<>();
            Map<String, String> skillDepth = new HashMap<>();
            List<String> technicalGaps = new ArrayList<>();
            
            // 分析技能深度
            if (skills != null) {
                for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                    String skillName = entry.getKey();
                    Integer proficiency = entry.getValue();
                    
                    if (proficiency >= 8) {
                        skillDepth.put(skillName, "精通");
                    } else if (proficiency >= 6) {
                        skillDepth.put(skillName, "熟练");
                    } else if (proficiency >= 4) {
                        skillDepth.put(skillName, "熟悉");
                    } else {
                        skillDepth.put(skillName, "了解");
                        technicalGaps.add(skillName);
                    }
                }
            }
            
            // 分析项目复杂度（简单实现）
            String complexityLevel = "中等";
            if (projects != null && projects.length() > 500) {
                complexityLevel = "较高";
            }
            
            result.put("skillDepth", skillDepth);
            result.put("technicalGaps", technicalGaps);
            result.put("overallDepth", calculateOverallDepth(skillDepth));
            result.put("projectComplexityLevel", complexityLevel);
            
            return result;
        } catch (Exception e) {
            log.error("分析技术栈深度失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    public Map<String, Object> analyzeProjectComplexity(String[] projectDescriptions) {
        try {
            log.info("分析项目复杂度");
            
            Map<String, Object> result = new HashMap<>();
            List<String> highComplexityProjects = new ArrayList<>();
            List<String> mediumComplexityProjects = new ArrayList<>();
            List<String> lowComplexityProjects = new ArrayList<>();
            
            if (projectDescriptions != null) {
                for (String description : projectDescriptions) {
                    if (description == null) continue;
                    
                    // 简单的复杂度评估：基于描述长度和关键词
                    int complexityScore = 0;
                    
                    // 基于描述长度
                    if (description.length() > 500) complexityScore += 3;
                    else if (description.length() > 200) complexityScore += 2;
                    else complexityScore += 1;
                    
                    // 基于关键词
                    String[] highComplexityKeywords = {"架构", "系统设计", "分布式", "高并发", "微服务", "性能优化"};
                    String[] mediumComplexityKeywords = {"模块", "功能开发", "API设计", "数据库设计"};
                    
                    for (String keyword : highComplexityKeywords) {
                        if (description.contains(keyword)) {
                            complexityScore += 2;
                            break;
                        }
                    }
                    
                    for (String keyword : mediumComplexityKeywords) {
                        if (description.contains(keyword)) {
                            complexityScore += 1;
                            break;
                        }
                    }
                    
                    // 分类项目复杂度
                    if (complexityScore >= 5) {
                        highComplexityProjects.add(description.substring(0, Math.min(50, description.length())) + "...");
                    } else if (complexityScore >= 3) {
                        mediumComplexityProjects.add(description.substring(0, Math.min(50, description.length())) + "...");
                    } else {
                        lowComplexityProjects.add(description.substring(0, Math.min(50, description.length())) + "...");
                    }
                }
            }
            
            result.put("highComplexityProjects", highComplexityProjects);
            result.put("mediumComplexityProjects", mediumComplexityProjects);
            result.put("lowComplexityProjects", lowComplexityProjects);
            result.put("totalProjectsAnalyzed", projectDescriptions != null ? projectDescriptions.length : 0);
            
            return result;
        } catch (Exception e) {
            log.error("分析项目复杂度失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    public QuestionAnalysisDTO generateBehavioralQuestions(String workExperience, String projects) {
        QuestionAnalysisDTO dto = new QuestionAnalysisDTO();
        dto.setCategory("behavioral");
        
        List<QuestionAnalysisDTO.QuestionItem> questionItems = new ArrayList<>();
        
        // 生成基于工作经历的问题
        if (workExperience != null && !workExperience.isEmpty()) {
            QuestionAnalysisDTO.QuestionItem item1 = new QuestionAnalysisDTO.QuestionItem();
            item1.setQuestion("请描述您在工作中遇到的最大挑战，以及您是如何克服的？");
            item1.setType("challenges");
            item1.setDifficulty("medium");
            item1.setPurpose("评估候选人的问题解决能力和应对挑战的态度");
            item1.setEvaluationCriteria(Arrays.asList("问题描述清晰度", "解决方案的合理性", "结果的有效性", "学习能力"));
            item1.setExpectedAnswers(Collections.singletonMap("structure", "STAR法则（情境、任务、行动、结果）"));
            item1.setFollowUpStrategy("深入询问具体行动步骤和决策依据");
            questionItems.add(item1);
            
            QuestionAnalysisDTO.QuestionItem item2 = new QuestionAnalysisDTO.QuestionItem();
            item2.setQuestion("请分享一次您与团队成员意见分歧的经历，您是如何处理的？");
            item2.setType("collaboration");
            item2.setDifficulty("medium");
            item2.setPurpose("评估候选人的团队协作能力和沟通技巧");
            item2.setEvaluationCriteria(Arrays.asList("沟通能力", "冲突解决能力", "团队合作精神", "情绪管理"));
            item2.setExpectedAnswers(Collections.singletonMap("structure", "强调倾听、理解和寻求共识的过程"));
            item2.setFollowUpStrategy("询问最终结果和从中学到的经验");
            questionItems.add(item2);
        }
        
        // 生成基于项目经验的问题
        if (projects != null && !projects.isEmpty()) {
            QuestionAnalysisDTO.QuestionItem item3 = new QuestionAnalysisDTO.QuestionItem();
            item3.setQuestion("请详细描述一个您负责的成功项目，您在其中扮演了什么角色？");
            item3.setType("project_management");
            item3.setDifficulty("medium");
            item3.setPurpose("评估候选人的项目管理能力和责任意识");
            item3.setEvaluationCriteria(Arrays.asList("项目规划能力", "执行能力", "团队领导能力", "结果导向"));
            item3.setExpectedAnswers(Collections.singletonMap("structure", "清晰描述项目背景、目标、个人贡献和最终成果"));
            item3.setFollowUpStrategy("询问项目中的具体挑战和解决方案");
            questionItems.add(item3);
        }
        
        // 如果没有提供足够信息，生成通用问题
        if (questionItems.isEmpty()) {
            QuestionAnalysisDTO.QuestionItem item4 = new QuestionAnalysisDTO.QuestionItem();
            item4.setQuestion("请描述您的职业发展目标，以及您计划如何实现这些目标？");
            item4.setType("career_planning");
            item4.setDifficulty("easy");
            item4.setPurpose("了解候选人的职业规划和自我认知");
            item4.setEvaluationCriteria(Arrays.asList("目标清晰度", "计划合理性", "与职位匹配度", "长期规划能力"));
            item4.setExpectedAnswers(Collections.singletonMap("structure", "短期和长期目标相结合，具体可行"));
            item4.setFollowUpStrategy("询问如何应对可能的职业发展障碍");
            questionItems.add(item4);
        }
        
        dto.setQuestions(questionItems);
        dto.setAnalysisSummary("基于提供的工作经历和项目经验，生成了针对性的行为面试问题，旨在评估候选人的核心能力和素质");
        dto.setKeyFocusPoints(Arrays.asList(
            "候选人的问题解决和决策能力",
            "团队协作和沟通技巧",
            "压力管理和适应性",
            "自我认知和职业规划"
        ));
        
        return dto;
    }
    
    public String assessExperienceLevel(Integer workYears, Integer projectCount, Map<String, Integer> skills) {
        try {
            // 确保参数不为空
            int years = workYears != null ? workYears : 0;
            int projects = projectCount != null ? projectCount : 0;
            
            log.info("评估经验级别: 工作年限={}, 项目数量={}, 技能数量={}", 
                    years, projects, skills != null ? skills.size() : 0);
            
            // 1. 基于工作年限的评分（更精细化）
            int yearsScore = calculateYearsScore(years);
            
            // 2. 基于项目数量的评分（更合理的权重）
            int projectScore = calculateProjectScore(projects);
            
            // 3. 基于技能成熟度的评分（更综合的评估）
            int skillScore = calculateSkillScore(skills);
            
            // 计算总分
            int totalScore = yearsScore + projectScore + skillScore;
            log.info("经验级别评估得分: 年限分={}, 项目分={}, 技能分={}, 总分={}", 
                    yearsScore, projectScore, skillScore, totalScore);
            
            // 根据总分确定经验级别（更精细的分级）
            String experienceLevel = determineExperienceLevel(totalScore, years);
            
            log.info("最终评估经验级别: {}", experienceLevel);
            return experienceLevel;
        } catch (Exception e) {
            log.error("评估经验级别失败: {}", e.getMessage(), e);
            return "初级"; // 发生异常时返回保守的默认值
        }
    }
    
    /**
     * 计算工作年限得分
     */
    private int calculateYearsScore(int workYears) {
        // 非线性评分，随着工作年限增长，分数增速逐渐放缓
        if (workYears <= 0) return 0;
        else if (workYears <= 1) return 10;
        else if (workYears <= 3) return 20 + (workYears - 1) * 10; // 20-40分
        else if (workYears <= 5) return 40 + (workYears - 3) * 8;  // 48-56分
        else if (workYears <= 8) return 56 + (workYears - 5) * 5;  // 61-71分
        else if (workYears <= 10) return 71 + (workYears - 8) * 4; // 75-79分
        else return 80; // 最高80分
    }
    
    /**
     * 计算项目数量得分
     */
    private int calculateProjectScore(int projectCount) {
        // 基于项目数量的非线性评分
        if (projectCount <= 0) return 0;
        else if (projectCount <= 3) return projectCount * 5;      // 5-15分
        else if (projectCount <= 5) return 15 + (projectCount - 3) * 4; // 19-23分
        else if (projectCount <= 10) return 23 + (projectCount - 5) * 3; // 26-41分
        else return 45; // 最高45分
    }
    
    /**
     * 计算技能成熟度得分
     */
    private int calculateSkillScore(Map<String, Integer> skills) {
        if (skills == null || skills.isEmpty()) {
            log.warn("技能列表为空，技能成熟度得分记为0");
            return 0;
        }
        
        int totalProficiency = 0;
        int count = 0;
        int highLevelSkills = 0;
        
        for (Map.Entry<String, Integer> entry : skills.entrySet()) {
            Integer level = entry.getValue();
            if (level != null) {
                totalProficiency += level;
                count++;
                
                // 统计高级技能数量（等级>=7）
                if (level >= 7) {
                    highLevelSkills++;
                }
            }
        }
        
        if (count == 0) {
            log.warn("没有有效的技能等级数据，技能成熟度得分记为0");
            return 0;
        }
        
        // 基础技能分（基于平均熟练度）
        double avgProficiency = (double) totalProficiency / count;
        int baseSkillScore = (int) (avgProficiency * 5); // 平均熟练度 * 5
        
        // 高级技能加分
        int highLevelBonus = Math.min(highLevelSkills * 3, 15);
        
        // 技能数量适当加分
        int quantityBonus = Math.min((count - 5) * 2, 20);
        
        int totalSkillScore = baseSkillScore + highLevelBonus + 
                (quantityBonus > 0 ? quantityBonus : 0);
        
        log.debug("技能评分明细: 基础分={}, 高级技能加={}, 数量加={}", 
                baseSkillScore, highLevelBonus, quantityBonus);
        
        return Math.min(totalSkillScore, 60); // 最高60分
    }
    
    /**
     * 根据总分确定经验级别
     */
    private String determineExperienceLevel(int totalScore, int workYears) {
        // 基础级别判定
        String level;
        if (totalScore >= 120) {
            level = "资深";
        } else if (totalScore >= 90) {
            level = "高级";
        } else if (totalScore >= 60) {
            level = "中级";
        } else {
            level = "初级";
        }
        
        // 结合工作年限进行微调
        // 确保工作年限与级别匹配
        if (workYears >= 8 && !level.equals("资深")) {
            level = "高级";
        } else if (workYears >= 4 && level.equals("初级")) {
            level = "中级";
        } else if (workYears <= 1 && level.equals("高级")) {
            level = "中级";
        } else if (workYears <= 0.5 && !level.equals("初级")) {
            level = "初级";
        }
        
        return level;
    }
    
    // 辅助方法：计算整体技术深度
    private String calculateOverallDepth(Map<String, String> skillDepth) {
        if (skillDepth == null || skillDepth.isEmpty()) return "未知";
        
        int expertCount = 0;
        int proficientCount = 0;
        int familiarCount = 0;
        
        for (String depth : skillDepth.values()) {
            if (depth != null) {
                switch (depth) {
                    case "精通":
                        expertCount++;
                        break;
                    case "熟练":
                        proficientCount++;
                        break;
                    case "熟悉":
                        familiarCount++;
                        break;
                }
            }
        }
        
        double totalSkills = skillDepth.size();
        double expertRatio = expertCount / totalSkills;
        double proficientRatio = proficientCount / totalSkills;
        
        if (expertRatio >= 0.5) {
            return "很深";
        } else if (proficientRatio >= 0.5) {
            return "较深";
        } else {
            return "一般";
        }
    }
    
    /**
     * 使用AI生成简历的优势分析
     * @param resume 简历对象
     * @param educationList 教育经历列表
     * @param projectList 项目经历列表
     * @param skillList 技能列表
     * @param workExperienceList 工作经历列表
     * @return 优势分析列表
     */
    private List<String> generateStrengthsWithAI(Resume resume, List<ResumeEducation> educationList, List<ResumeProject> projectList, List<ResumeSkill> skillList, List<ResumeWorkExperience> workExperienceList) {
        try {
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下简历信息，分析候选人的主要优势和亮点：\n\n");
            
            // 添加个人信息
            prompt.append("【基本信息】\n");
            // 从User表获取姓名
            String userName = "未知";
            if (resume.getUserId() != null) {
                User user = userService.getUserById(resume.getUserId()).orElse(null);
                if (user != null && user.getName() != null) {
                    userName = user.getName();
                }
            }
            prompt.append("姓名：").append(userName).append("\n");
            prompt.append("求职意向：").append(resume.getJobTitle() != null ? resume.getJobTitle() : "未知").append("\n\n");
            
            // 添加教育经历
            if (educationList != null && !educationList.isEmpty()) {
                prompt.append("【教育经历】\n");
                for (ResumeEducation education : educationList) {
                    prompt.append(education.getSchool()).append(" ")
                          .append(education.getDegree()).append(" ")
                          .append(education.getMajor()).append("\n");
                }
                prompt.append("\n");
            }
            
            // 添加工作经历
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                prompt.append("【工作经历】\n");
                for (ResumeWorkExperience exp : workExperienceList) {
                    prompt.append(exp.getCompanyName()).append(" ")
                          .append(exp.getPositionName()).append("\n");
                }
                prompt.append("\n");
            }
            
            // 添加项目经历
            if (projectList != null && !projectList.isEmpty()) {
                prompt.append("【项目经历】\n");
                for (ResumeProject project : projectList) {
                    prompt.append(project.getProjectName()).append("\n");
                }
                prompt.append("\n");
            }
            
            // 添加技能
            if (skillList != null && !skillList.isEmpty()) {
                prompt.append("【技能】\n");
                for (ResumeSkill skill : skillList) {
                    prompt.append(skill.getName()).append(" (").append(skill.getLevel() != null ? skill.getLevel() : 5).append("/10)").append("\n");
                }
                prompt.append("\n");
            }
            
            prompt.append("请列出候选人的5-8个主要优势，每个优势用简洁的句子描述，不要使用编号，直接返回要点列表。");
            
            // 调用AI服务
            String response = aiServiceUtils.callAiService(prompt.toString());
            
            // 解析响应
            List<String> strengths = new ArrayList<>();
            if (response != null && !response.isEmpty()) {
                // 按行分割并过滤空行
                String[] lines = response.split("\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        // 移除可能的编号前缀
                        line = line.replaceFirst("^\\s*[0-9]+[.、)]\\s*", "");
                        strengths.add(line);
                    }
                }
            }
            
            return strengths;
        } catch (Exception e) {
            log.error("生成优势分析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 使用AI生成简历的待提升项
     * @param resume 简历对象
     * @param educationList 教育经历列表
     * @param projectList 项目经历列表
     * @param skillList 技能列表
     * @param workExperienceList 工作经历列表
     * @return 待提升项列表
     */
    private List<String> generateImprovementsWithAI(Resume resume, List<ResumeEducation> educationList, List<ResumeProject> projectList, List<ResumeSkill> skillList, List<ResumeWorkExperience> workExperienceList) {
        try {
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下简历信息，分析候选人可以提升的地方：\n\n");
            
            // 添加个人信息
            prompt.append("【基本信息】\n");
            // 从User表获取姓名
            String userName = "未知";
            if (resume.getUserId() != null) {
                User user = userService.getUserById(resume.getUserId()).orElse(null);
                if (user != null && user.getName() != null) {
                    userName = user.getName();
                }
            }
            prompt.append("姓名：").append(userName).append("\n");
            prompt.append("求职意向：").append(resume.getJobTitle() != null ? resume.getJobTitle() : "未知").append("\n\n");
            
            // 添加教育经历
            if (educationList != null && !educationList.isEmpty()) {
                prompt.append("【教育经历】\n");
                for (ResumeEducation education : educationList) {
                    prompt.append(education.getSchool()).append(" ")
                          .append(education.getDegree()).append(" ")
                          .append(education.getMajor()).append("\n");
                }
                prompt.append("\n");
            }
            
            // 添加工作经历
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                prompt.append("【工作经历】\n");
                for (ResumeWorkExperience exp : workExperienceList) {
                    prompt.append(exp.getCompanyName()).append(" ")
                          .append(exp.getPositionName()).append("\n");
                }
                prompt.append("\n");
            }
            
            // 添加项目经历
            if (projectList != null && !projectList.isEmpty()) {
                prompt.append("【项目经历】\n");
                for (ResumeProject project : projectList) {
                    prompt.append(project.getProjectName()).append("\n");
                }
                prompt.append("\n");
            }
            
            // 添加技能
            if (skillList != null && !skillList.isEmpty()) {
                prompt.append("【技能】\n");
                for (ResumeSkill skill : skillList) {
                    prompt.append(skill.getName()).append(" (").append(skill.getLevel() != null ? skill.getLevel() : 5).append("/10)").append("\n");
                }
                prompt.append("\n");
            }
            
            prompt.append("请列出候选人的3-5个主要提升点，每个提升点用简洁的句子描述，不要使用编号，直接返回要点列表。");
            
            // 调用AI服务
            String response = aiServiceUtils.callAiService(prompt.toString());
            
            // 解析响应
            List<String> improvements = new ArrayList<>();
            if (response != null && !response.isEmpty()) {
                // 按行分割并过滤空行
                String[] lines = response.split("\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        // 移除可能的编号前缀
                        line = line.replaceFirst("^\\s*[0-9]+[.、)]\\s*", "");
                        improvements.add(line);
                    }
                }
            }
            
            return improvements;
        } catch (Exception e) {
            log.error("生成待提升项失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private ResumeAnalysisDTO.InterviewQuestions generateInterviewQuestionsWithAI(Resume resume, List<ResumeEducation> educationList, List<ResumeProject> projectList, List<ResumeSkill> skillList, List<ResumeWorkExperience> workExperienceList, String jobType, String analysisDepth) {
        try {
            // 创建结果对象
            ResumeAnalysisDTO.InterviewQuestions result = new ResumeAnalysisDTO.InterviewQuestions();
            List<String> questions = new ArrayList<>();
            
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
            // 从User表获取姓名
            String userName = "未知";
            if (resume.getUserId() != null) {
                User user = userService.getUserById(resume.getUserId()).orElse(null);
                if (user != null && user.getName() != null) {
                    userName = user.getName();
                }
            }
            prompt.append("姓名: " + userName + "\n");
            prompt.append("求职意向: " + (resume.getJobTitle() != null ? resume.getJobTitle() : "未知") + "\n");
            prompt.append("主要技术栈: " + mainTechnicalStack + "\n\n");
            
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
                    prompt.append("项目名称: " + (project.getProjectName() != null ? project.getProjectName() : "未知") + "\n");
                    prompt.append("角色: " + (project.getRole() != null ? project.getRole() : "未知") + "\n");
                    prompt.append("技术栈: " + (project.getTechStack() != null ? project.getTechStack() : "未知") + "\n");
                    prompt.append("描述: " + (project.getDescription() != null ? project.getDescription() : "无") + "\n\n");
                }
            }
            
            // 添加最近工作经验
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                prompt.append("【最近工作经验】\n");
                // 假设列表是按时间倒序排列的，取第一个
                ResumeWorkExperience latestWork = workExperienceList.get(0);
                prompt.append("公司: " + (latestWork.getCompanyName() != null ? latestWork.getCompanyName() : "未知") + "\n");
                prompt.append("职位: " + (latestWork.getPositionName() != null ? latestWork.getPositionName() : (latestWork.getPositionName() != null ? latestWork.getPositionName() : "未知")) + "\n");
                prompt.append("职责: " + (latestWork.getDescription() != null ? latestWork.getDescription() : "无") + "\n\n");
            }
            
            // 添加教育背景（如果有较高学历）
            if (educationList != null && !educationList.isEmpty()) {
                ResumeEducation highestEducation = educationList.stream()
                    .max(Comparator.comparing(ResumeEducation::getOrderIndex))
                    .orElse(null);
                
                if (highestEducation != null && highestEducation.getDegree() != null && 
                    (highestEducation.getDegree().contains("硕士") || highestEducation.getDegree().contains("博士"))) {
                    prompt.append("【教育背景】\n");
                    prompt.append("学历: " + highestEducation.getDegree() + "\n");
                    prompt.append("学校: " + (highestEducation.getSchool() != null ? highestEducation.getSchool() : "未知") + "\n");
                    prompt.append("专业: " + (highestEducation.getMajor() != null ? highestEducation.getMajor() : "未知") + "\n\n");
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
                  .append("\n1. 技术深度问题: 针对候选人技能的深入技术问题，适合" + experienceLevel + "工程师水平")
                  .append("\n2. 项目挑战问题: 基于项目经验，提问候选人如何解决技术难题和做出技术决策")
                  .append("\n3. 职位相关问题: 与" + (jobType != null ? jobType : "目标职位") + "直接相关的专业知识和能力")
                  .append("\n4. 团队协作问题: 考察候选人在团队中的沟通能力、协作方式和冲突处理");
                  
            // 高级工程师额外要求
            if ("高级".equals(experienceLevel)) {
                prompt.append("\n5. 架构设计问题: 考察候选人的系统设计能力和架构思维")
                      .append("\n6. 技术领导力问题: 如何带领团队、指导新人、制定技术路线");
            }
            
            prompt.append("\n\n问题要求:")
                  .append("\n- 问题应该具体、有针对性，避免过于宽泛或基础的问题")
                  .append("\n- 问题难度要符合" + experienceLevel + "工程师的职位要求")
                  .append("\n- 问题应该能够有效评估候选人的实际能力和经验")
                  .append("\n- 部分问题可以设计成追问的形式，帮助面试官深入了解候选人");
            
            prompt.append("\n\n请以问题列表形式返回，每个问题单独一行，不要添加序号和额外说明。");
            
            // 调用DeepSeek API
            String apiResult = aiServiceUtils.callDeepSeekApi(prompt.toString());
            
            // 解析结果
            if (apiResult != null && !apiResult.isEmpty()) {
                String[] lines = apiResult.split("\n");
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        // 移除可能的序号和前缀
                        String question = trimmedLine.replaceAll("^\\d+\\.?\\s*", "")
                                                    .replaceAll("^[\\u4e00-\\u9fa5]+：", "")
                                                    .replaceAll("^问题[0-9]?:", "")
                                                    .trim();
                        
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
            if (finalQuestions.size() > maxQuestions) {
                finalQuestions = finalQuestions.subList(0, maxQuestions);
            }
            
            result.setQuestions(finalQuestions);
            result.setAnalysisType("interview");
            
            return result;
            
        } catch (Exception e) {
            log.error("使用AI生成面试问题失败: {}", e.getMessage());
            
            // 返回默认问题，根据经验级别调整
            ResumeAnalysisDTO.InterviewQuestions defaultResult = new ResumeAnalysisDTO.InterviewQuestions();
            int workYears = calculateWorkYears(workExperienceList);
            List<String> defaultQuestions = new ArrayList<>();
            
            if (workYears >= 5) {
                defaultQuestions.addAll(Arrays.asList(
                    "请详细介绍您负责过的一个项目，您在其中的角色和贡献。",
                    "您如何设计和实现一个高性能、可扩展的系统？",
                    "在技术选型上，您会考虑哪些因素？请举例说明。",
                    "您如何处理团队中的技术分歧和冲突？",
                    "您如何评估和提升团队成员的技术能力？"
                ));
            } else {
                defaultQuestions.addAll(Arrays.asList(
                    "请介绍一下您的技术背景和工作经验。",
                    "您最擅长的编程语言或技术栈是什么？",
                    "您在项目中遇到过哪些技术难题，如何解决的？",
                    "您如何理解软件工程的最佳实践？",
                    "您如何与团队成员进行技术沟通和协作？"
                ));
            }
            
            defaultResult.setQuestions(defaultQuestions);
            defaultResult.setAnalysisType("interview");
            
            return defaultResult;
        }
    }
    
    /**
     * 计算工作年限
     */
    /**
     * 创建默认的简历分析DTO对象
     * @return 默认的ResumeAnalysisDTO对象
     */
    /**
     * 计算简历综合评分
     * @param technicalScore 技术评分 (0-100)
     * @param projectScore 项目评分 (0-100)
     * @param businessScore 业务能力评分 (0-100)
     * @param educationScore 教育背景评分 (0-100)
     * @param experienceScore 工作经验评分 (0-100)
     * @return 综合评分 (0-100)
     */
    /**
     * 计算简历综合评分
     * @param resume 简历实体
     * @param educationList 教育经历列表
     * @param projectList 项目经历列表
     * @param skillList 技能列表
     * @param workExperienceList 工作经历列表
     * @param jobType 职位类型
     * @return 综合评分（Double类型，便于后续类型转换）
     */
    private Double calculateOverallScore(Resume resume, List<ResumeEducation> educationList, List<ResumeProject> projectList, List<ResumeSkill> skillList, List<ResumeWorkExperience> workExperienceList, String jobType) {
        try {
            // 计算各项评分
            int technicalScore = calculateTechnicalScore(skillList, jobType);
            int projectScore = calculateProjectScore(projectList);
            int businessScore = calculateBusinessScore(workExperienceList);
            int educationScore = calculateEducationScore(educationList);
            int experienceScore = calculateExperienceScore(workExperienceList);
            
            // 调用现有的评分计算方法
            return (double) calculateOverallScore(technicalScore, projectScore, businessScore, educationScore, experienceScore);
        } catch (Exception e) {
            log.error("计算综合评分失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 计算技术能力评分
     */
    private int calculateTechnicalScore(List<ResumeSkill> skillList, String jobType) {
        // 简单实现，实际项目中可能需要更复杂的计算逻辑
        if (skillList == null || skillList.isEmpty()) {
            return 0;
        }
        
        // 基础分数
        int baseScore = 60;
        // 根据技能数量加分
        int scorePerSkill = 2;
        int skillBonus = Math.min(skillList.size() * scorePerSkill, 40);
        
        return baseScore + skillBonus;
    }
    
    /**
     * 计算项目经验评分
     */
    private int calculateProjectScore(List<ResumeProject> projectList) {
        // 简单实现
        if (projectList == null || projectList.isEmpty()) {
            return 0;
        }
        
        int baseScore = 60;
        int scorePerProject = 8;
        int projectBonus = Math.min(projectList.size() * scorePerProject, 40);
        
        return baseScore + projectBonus;
    }
    
    /**
     * 计算业务理解能力评分
     */
    private int calculateBusinessScore(List<ResumeWorkExperience> workExperienceList) {
        // 简单实现
        if (workExperienceList == null || workExperienceList.isEmpty()) {
            return 0;
        }
        
        int baseScore = 60;
        int experienceYears = calculateWorkYears(workExperienceList);
        int experienceBonus = Math.min(experienceYears * 8, 40);
        
        return baseScore + experienceBonus;
    }
    
    /**
     * 计算教育背景评分
     */
    private int calculateEducationScore(List<ResumeEducation> educationList) {
        // 简单实现
        if (educationList == null || educationList.isEmpty()) {
            return 0;
        }
        
        // 默认本科60分，硕士+20分，博士+30分
        int baseScore = 60;
        
        for (ResumeEducation education : educationList) {
            if (education.getDegree() != null) {
                String degree = education.getDegree().toLowerCase();
                if (degree.contains("硕士") || degree.contains("master")) {
                    baseScore = 80;
                } else if (degree.contains("博士") || degree.contains("phd")) {
                    baseScore = 90;
                }
            }
        }
        
        return Math.min(baseScore, 100);
    }
    
    /**
     * 计算工作经验评分
     */
    private int calculateExperienceScore(List<ResumeWorkExperience> workExperienceList) {
        // 简单实现
        if (workExperienceList == null || workExperienceList.isEmpty()) {
            return 0;
        }
        
        int baseScore = 60;
        int experienceYears = calculateWorkYears(workExperienceList);
        int experienceBonus = Math.min(experienceYears * 5, 40);
        
        return baseScore + experienceBonus;
    }
    
    private int calculateOverallScore(int technicalScore, int projectScore, int businessScore, int educationScore, int experienceScore) {
        try {
            // 定义各部分权重
            double technicalWeight = 0.35;  // 技术能力权重最高
            double projectWeight = 0.25;    // 项目经验权重次之
            double businessWeight = 0.15;   // 业务理解能力
            double educationWeight = 0.10;  // 教育背景
            double experienceWeight = 0.15; // 工作经验
            
            // 计算加权平均分
            double weightedScore = (
                technicalScore * technicalWeight +
                projectScore * projectWeight +
                businessScore * businessWeight +
                educationScore * educationWeight +
                experienceScore * experienceWeight
            );
            
            // 四舍五入到整数
            int finalScore = (int) Math.round(weightedScore);
            
            // 确保分数在0-100范围内
            return Math.max(0, Math.min(100, finalScore));
        } catch (Exception e) {
            log.error("计算综合评分失败: {}", e.getMessage());
            return 50; // 默认中等分数
        }
    }
    
    /**
     * 评估项目复杂度（重载版本，接受ProjectInfo类型）
     * @param projectInfo 项目信息DTO
     * @return 复杂度级别 (低、中、高、很高)
     */
    private String assessProjectComplexity(ResumeAnalysisDTO.ProjectInfo projectInfo) {
        // 创建临时ResumeProject对象来调用现有方法
        ResumeProject project = new ResumeProject();
        project.setProjectName(projectInfo.getName());
        project.setDescription(projectInfo.getDescription());
        project.setRole(projectInfo.getRole());
        // 将技术列表转换为逗号分隔的字符串
        if (projectInfo.getTechnologies() != null && !projectInfo.getTechnologies().isEmpty()) {
            project.setTechStack(String.join(",", projectInfo.getTechnologies()));
        }
        return assessProjectComplexity(project);
    }
    
    /**
     * 评估项目复杂度
     * @param project 项目对象
     * @return 复杂度级别 (低、中、高、很高)
     */
    private String assessProjectComplexity(ResumeProject project) {
        try {
            // 初始化复杂度分数
            int complexityScore = 0;
            
            // 根据项目名称中的关键词评估复杂度
            String projectName = project.getProjectName();
            if (projectName != null) {
                projectName = projectName.toLowerCase();
                // 大型项目关键词
                if (projectName.contains("大型") || projectName.contains("平台") || 
                    projectName.contains("系统") || projectName.contains("enterprise") || 
                    projectName.contains("platform") || projectName.contains("system")) {
                    complexityScore += 20;
                }
                // 复杂度相关关键词
                if (projectName.contains("分布式") || projectName.contains("微服务") || 
                    projectName.contains("高并发") || projectName.contains("大数据") ||
                    projectName.contains("distributed") || projectName.contains("microservice") ||
                    projectName.contains("high concurrency")) {
                    complexityScore += 15;
                }
            }
            
            // 根据技术栈数量和复杂度评估
            String techStack = project.getTechStack();
            if (techStack != null && !techStack.isEmpty()) {
                // 简单计算技术栈数量（以逗号分隔）
                String[] technologies = techStack.split(",");
                if (technologies.length >= 8) {
                    complexityScore += 30;
                } else if (technologies.length >= 5) {
                    complexityScore += 20;
                } else if (technologies.length >= 3) {
                    complexityScore += 10;
                }
                
                // 检查复杂技术关键词
                techStack = techStack.toLowerCase();
                String[] complexTechKeywords = {
                    "分布式", "微服务", "高并发", "大数据", "机器学习", "ai", 
                    "distributed", "microservice", "high concurrency", "big data", "machine learning"
                };
                for (String keyword : complexTechKeywords) {
                    if (techStack.contains(keyword)) {
                        complexityScore += 5; // 每个复杂技术关键词加5分
                        break; // 避免重复加分
                    }
                }
            }
            
            // 根据项目描述评估
            String description = project.getDescription();
            if (description != null && !description.isEmpty()) {
                // 描述长度评估
                if (description.length() > 500) {
                    complexityScore += 20;
                } else if (description.length() > 200) {
                    complexityScore += 15;
                } else if (description.length() > 100) {
                    complexityScore += 10;
                }
                
                // 描述中关键词评估
                description = description.toLowerCase();
                String[] complexityKeywords = {
                    "重构", "优化", "性能", "扩展", "架构", "设计模式",
                    "refactor", "optimize", "performance", "scale", "architecture", "design pattern"
                };
                for (String keyword : complexityKeywords) {
                    if (description.contains(keyword)) {
                        complexityScore += 5;
                        break;
                    }
                }
                
                // 检查团队相关描述，间接推断团队规模
                String[] teamSizeKeywords = {"团队", "协作", "跨部门", "多人", "team", "collaborate", "cross-department"};
                for (String keyword : teamSizeKeywords) {
                    if (description.contains(keyword)) {
                        complexityScore += 5;
                        break;
                    }
                }
            }
            
            // 根据复杂度分数返回相应级别
            if (complexityScore >= 70) {
                return "很高";
            } else if (complexityScore >= 50) {
                return "高";
            } else if (complexityScore >= 30) {
                return "中";
            } else {
                return "低";
            }
        } catch (Exception e) {
            log.error("评估项目复杂度失败: {}", e.getMessage());
            return "中"; // 默认中等复杂度
        }
    }
    
    /**
     * 分析项目的业务影响（重载版本，接受String类型）
     * @param projectDescription 项目描述字符串
     * @return 业务影响程度描述
     */
    private String analyzeBusinessImpact(String projectDescription) {
        // 创建临时ResumeProject对象来调用现有方法
        ResumeProject project = new ResumeProject();
        project.setDescription(projectDescription);
        return analyzeBusinessImpact(project);
    }
    
    /**
     * 分析项目的业务影响
     * @param project 项目对象
     * @return 业务影响程度描述
     */
    private String analyzeBusinessImpact(ResumeProject project) {
        try {
            // 初始化业务影响分数
            int impactScore = 0;
            
            // 检查是否有明确的业务成果或指标
            // String achievements = project.getAchievements();
            // if (achievements != null && !achievements.isEmpty()) {
            //     achievements = achievements.toLowerCase();
            //     // 检查关键词和数字指标
            //     if (achievements.matches(".*\\d+[%].*")) { // 包含百分比
            //         impactScore += 30;
            //     }
            //     if (achievements.matches(".*\\d+[万千百万亿].*")) { // 包含大数字单位
            //         impactScore += 25;
            //     }
            //     if (achievements.contains("提升") || achievements.contains("增长") || 
            //         achievements.contains("increase") || achievements.contains("improve")) {
            //         impactScore += 20;
            //     }
            //     if (achievements.contains("优化") || achievements.contains("optimize") || 
            //         achievements.contains("reduce") || achievements.contains("降低")) {
            //         impactScore += 15;
            //     }
            //     if (achievements.contains("创新") || achievements.contains("创新") || 
            //         achievements.contains("new") || achievements.contains("创新")) {
            //         impactScore += 10;
            //     }
            // }
            
            // 分析项目描述中的业务价值
            String description = project.getDescription();
            if (description != null && !description.isEmpty()) {
                description = description.toLowerCase();
                // 检查业务价值相关关键词
                if (description.contains("用户") || description.contains("customer")) {
                    impactScore += 15;
                }
                if (description.contains("收入") || description.contains("revenue") || 
                    description.contains("profit") || description.contains("利润")) {
                    impactScore += 25;
                }
                if (description.contains("效率") || description.contains("efficiency") || 
                    description.contains("performance") || description.contains("性能")) {
                    impactScore += 15;
                }
                if (description.contains("市场") || description.contains("market") || 
                    description.contains("competitive") || description.contains("竞争")) {
                    impactScore += 10;
                }
            }
            
            // 分析项目类型
            String projectName = project.getProjectName();
            if (projectName != null && !projectName.isEmpty()) {
                projectName = projectName.toLowerCase();
                // 检查关键业务系统关键词
                if (projectName.contains("核心") || projectName.contains("core") || 
                    projectName.contains("平台") || projectName.contains("platform")) {
                    impactScore += 20;
                }
                if (projectName.contains("电商") || projectName.contains("e-commerce") || 
                    projectName.contains("交易") || projectName.contains("transaction")) {
                    impactScore += 15;
                }
                if (projectName.contains("数据") || projectName.contains("data") || 
                    projectName.contains("analytics") || projectName.contains("分析")) {
                    impactScore += 10;
                }
            }
            
            // 根据影响分数返回相应的业务影响描述
            if (impactScore >= 80) {
                return "重大业务影响 - 直接推动业务增长或创造显著收入"; 
            } else if (impactScore >= 60) {
                return "高业务影响 - 显著优化业务流程或提升用户体验"; 
            } else if (impactScore >= 40) {
                return "中等业务影响 - 改善现有系统或功能，有可衡量的业务价值"; 
            } else if (impactScore >= 20) {
                return "一般业务影响 - 支持日常业务运营，有一定价值"; 
            } else {
                return "有限业务影响 - 主要是技术改进或小规模优化"; 
            }
        } catch (Exception e) {
            log.error("分析业务影响失败: {}", e.getMessage());
            return "一般业务影响 - 支持日常业务运营，有一定价值"; // 默认中等影响
        }
    }
    
    private ResumeAnalysisDTO createDefaultAnalysisDTO() {
        ResumeAnalysisDTO dto = new ResumeAnalysisDTO();
        dto.setCandidateInfo(new ResumeAnalysisDTO.CandidateInfo());
        dto.setTechnicalAnalysis(new ResumeAnalysisDTO.TechnicalAnalysis());
        dto.setProjectAnalysis(new ResumeAnalysisDTO.ProjectAnalysis());
        dto.setBusinessAnalysis(new ResumeAnalysisDTO.BusinessAnalysis());
        dto.setExperienceLevel("未知");
        dto.setStrengths(Collections.emptyList());
        dto.setImprovements(Collections.emptyList());
        dto.setInterviewQuestions(new ResumeAnalysisDTO.InterviewQuestions());
        dto.setOverallScore(0);
        dto.setAnalysisTime(new Date());
        return dto;
    }
    
    private int calculateWorkYears(List<ResumeWorkExperience> workExperienceList) {
        if (workExperienceList == null || workExperienceList.isEmpty()) {
            log.info("工作经历列表为空，返回工作年限0");
            return 0;
        }
        
        int totalMonths = 0;
        Calendar currentCal = Calendar.getInstance();
        int currentYear = currentCal.get(Calendar.YEAR);
        int currentMonth = currentCal.get(Calendar.MONTH) + 1;
        
        for (ResumeWorkExperience exp : workExperienceList) {
            try {
                // 假设日期字段是字符串类型，这是简历系统中常见的存储方式
                String startDateStr = exp.getStartDate();
                String endDateStr = exp.getEndDate();
                
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    // 处理多种可能的日期格式：YYYY-MM、YYYY年MM月、YYYY/MM等
                    String normalizedStart = startDateStr.replaceAll("[年月/]", "-");
                    String normalizedEnd = endDateStr != null ? endDateStr.replaceAll("[年月/]", "-") : null;
                    
                    // 简单处理年月格式
                    if (normalizedStart.contains("-")) {
                        String[] startParts = normalizedStart.split("-");
                        if (startParts.length >= 2) {
                            try {
                                int startYear = Integer.parseInt(startParts[0].trim());
                                int startMonth = Integer.parseInt(startParts[1].trim());
                                
                                int endYear = currentYear;
                                int endMonth = currentMonth;
                                
                                // 如果有结束日期，则使用结束日期
                                if (normalizedEnd != null && normalizedEnd.contains("-")) {
                                    String[] endParts = normalizedEnd.split("-");
                                    if (endParts.length >= 2) {
                                        endYear = Integer.parseInt(endParts[0].trim());
                                        endMonth = Integer.parseInt(endParts[1].trim());
                                    }
                                }
                                
                                // 检查日期有效性
                                if (startYear > 1900 && startYear <= currentYear && 
                                    startMonth >= 1 && startMonth <= 12 &&
                                    endYear >= startYear && endMonth >= 1 && endMonth <= 12) {
                                    
                                    // 计算月数差
                                    int monthsDiff = (endYear - startYear) * 12 + (endMonth - startMonth);
                                    if (monthsDiff > 0) {
                                        totalMonths += monthsDiff;
                                        log.debug("计算工作经验: {}-{} 到 {}-{}，月数差: {}", 
                                                startYear, startMonth, endYear, endMonth, monthsDiff);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                // 日期格式解析失败，尝试其他方法
                                log.warn("日期格式解析失败: {}, 尝试基于文本描述估算", startDateStr);
                                
                                // 备用方案：基于文本描述估算
                                int estimatedMonths = estimateWorkDurationFromText(exp.getDescription(), exp.getStartDate(), exp.getEndDate());
                                if (estimatedMonths > 0) {
                                    totalMonths += estimatedMonths;
                                    log.debug("基于文本估算工作经验月数: {}", estimatedMonths);
                                }
                            }
                        }
                    } else {
                        // 没有连字符的日期格式，尝试其他方法
                        int estimatedMonths = estimateWorkDurationFromText(exp.getDescription(), exp.getStartDate(), exp.getEndDate());
                        if (estimatedMonths > 0) {
                            totalMonths += estimatedMonths;
                            log.debug("无连字符日期，基于文本估算工作经验月数: {}", estimatedMonths);
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略单个经验项的计算错误，继续处理下一个
                log.warn("计算工作经验时长失败: {}", e.getMessage());
            }
        }
        
        // 转换为年，向下取整
        int years = Math.max(0, totalMonths / 12);
        log.info("计算总工作年限: {}年 ({}个月)", years, totalMonths);
        return years;
    }
    
    /**
     * 基于文本描述估算工作时长
     */
    private int estimateWorkDurationFromText(String description, String startDate, String endDate) {
        // 检查是否包含"至今"、"现在"等表示仍在工作的关键词
        if (endDate != null && (endDate.contains("至今") || endDate.contains("现在") || endDate.contains("present"))) {
            // 如果只有开始年份，估算为当前年减去开始年
            if (startDate != null) {
                try {
                    // 提取年份
                    Pattern yearPattern = Pattern.compile("(\\d{4})");
                    Matcher matcher = yearPattern.matcher(startDate);
                    if (matcher.find()) {
                        int startYear = Integer.parseInt(matcher.group(1));
                        Calendar cal = Calendar.getInstance();
                        int currentYear = cal.get(Calendar.YEAR);
                        return (currentYear - startYear) * 12;
                    }
                } catch (Exception e) {
                    log.debug("从文本提取年份失败", e);
                }
            }
        }
        
        // 检查描述中是否包含工作时长信息（如"3年"、"18个月"等）
        if (description != null) {
            Pattern durationPattern = Pattern.compile("(\\d+)[年]\\s*(\\d+)?[个月]?|(\\d+)[个月]");
            Matcher matcher = durationPattern.matcher(description);
            if (matcher.find()) {
                try {
                    int years = 0;
                    int months = 0;
                    
                    if (matcher.group(1) != null) {
                        years = Integer.parseInt(matcher.group(1));
                    }
                    if (matcher.group(2) != null) {
                        months = Integer.parseInt(matcher.group(2));
                    }
                    if (matcher.group(3) != null) {
                        months = Integer.parseInt(matcher.group(3));
                    }
                    
                    return years * 12 + months;
                } catch (Exception e) {
                    log.debug("从描述提取工作时长失败", e);
                }
            }
        }
        
        // 默认返回0，表示无法估算
        return 0;
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
            .filter(skill -> skill.getLevel() != null)
            .sorted(Comparator.comparing(ResumeSkill::getLevel).reversed())
            .limit(5)
            .map(ResumeSkill::getName)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(", "));
    }
    
    /**
     * 判断技能是否为与职位相关的主要技能
     */
    private boolean isPrimarySkill(String skillName, String jobType) {
        if (skillName == null || jobType == null || skillName.isEmpty() || jobType.isEmpty()) {
            return false;
        }
        
        // 创建新的final变量存储小写后的字符串，避免修改原始变量
        final String lowerSkillName = skillName.toLowerCase();
        final String lowerJobType = jobType.toLowerCase();
        
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
            if (lowerJobType.contains(jobKey) && keySkills.stream().anyMatch(keySkill -> lowerSkillName.contains(keySkill))) {
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
        return commonTechnicalSkills.stream().anyMatch(keyword -> lowerSkillName.contains(keyword));
    }
}
