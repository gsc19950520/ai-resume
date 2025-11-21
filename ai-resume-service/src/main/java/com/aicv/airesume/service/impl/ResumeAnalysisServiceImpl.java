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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
                log.info("从缓存获取简历分析结果: {}", cacheKey);
                return cachedAnalysis.getAnalysis();
            }
            
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
            ResumeAnalysisDTO analysisDTO = analyzeResumeContent(resume, educationList, projectList, skillList, workExperienceList, jobType, analysisDepth);
            
            // 存入缓存
            analysisCache.put(cacheKey, new CachedAnalysis(analysisDTO, CACHE_EXPIRATION));
            log.info("简历分析完成并缓存: resumeId={}, 生成了{}个面试问题", resumeId, 
                    analysisDTO.getInterviewQuestions().getQuestions().size());
            
            return analysisDTO;
        } catch (Exception e) {
            log.error("分析简历失败: {}", e.getMessage());
            return createDefaultAnalysisDTO();
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
        
        // 9. 计算综合评分并进行类型转换（Double转Integer）
        Double score = calculateOverallScore(resume, educationList, projectList, skillList, workExperienceList, jobType);
        analysisDTO.setOverallScore(score != null ? score.intValue() : null);
        
        // 10. 设置分析时间
        analysisDTO.setAnalysisTime(new Date());
        
        return analysisDTO;
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
            
            if (workExperienceList != null && !workExperienceList.isEmpty()) {
                for (ResumeWorkExperience workExp : workExperienceList) {
                    // 提取业务领域知识
                    // 2. 从公司名称中提取业务领域
                    if (workExp.getCompanyName() != null && !workExp.getCompanyName().isEmpty()) {
                        String domainFromCompany = extractBusinessDomainFromText(workExp.getCompanyName());
                        if (domainFromCompany != null && !domainKnowledge.contains(domainFromCompany)) {
                            domainKnowledge.add(domainFromCompany);
                        }
                    }
                    // 3. 从职位名称中提取业务领域
                    if (workExp.getPositionName() != null && !workExp.getPositionName().isEmpty()) {
                        String domainFromPosition = extractBusinessDomainFromText(workExp.getPositionName());
                        if (domainFromPosition != null && !domainKnowledge.contains(domainFromPosition)) {
                            domainKnowledge.add(domainFromPosition);
                        }
                    }
                }
                
                // 定义软技能关键词，用于更好地识别软技能
                Set<String> softSkillKeywords = new HashSet<>(Arrays.asList(
                    "沟通", "communication", "团队合作", "teamwork", "协作", "collaboration",
                    "领导", "leadership", "管理", "management", "项目管理", "project management",
                    "分析", "analysis", "解决问题", "problem solving", "创新", "innovation",
                    "学习", "learning", "适应", "adaptation", "组织", "organization",
                    "时间管理", "time management", "决策", "decision making", "压力", "pressure",
                    "协调", "coordination", "谈判", "negotiation", "演讲", "presentation"
                ));
                
                // 提取软技能和职责相关信息
                for (ResumeWorkExperience workExp : workExperienceList) {
                    if (workExp.getDescription() != null && !workExp.getDescription().isEmpty()) {
                        // 完善split方法，包含更多可能的句子分隔符
                        String[] duties = workExp.getDescription().split("；|。|！|？|\n|\r\n|\t|", 0);
                        
                        for (String duty : duties) {
                            String trimmedDuty = duty.trim();
                            
                            // 过滤太短或无效的条目
                            if (trimmedDuty.length() < 4 || !trimmedDuty.matches(".*[a-zA-Z0-9\u4e00-\u9fa5].*")) {
                                continue;
                            }
                            
                            // 检查是否包含软技能关键词或其他重要内容
                            boolean isImportant = false;
                            String lowerDuty = trimmedDuty.toLowerCase();
                            
                            for (String keyword : softSkillKeywords) {
                                if (lowerDuty.contains(keyword.toLowerCase())) {
                                    isImportant = true;
                                    break;
                                }
                            }
                            
                            // 如果是重要内容或者长度适中，添加到软技能列表
                            if (isImportant || trimmedDuty.length() > 10) {
                                if (!softSkills.contains(trimmedDuty)) {
                                    softSkills.add(trimmedDuty);
                                }
                            }
                        }
                    }
                }
            }
            
            // 使用正确的setter方法
            businessAnalysis.setDomainKnowledge(domainKnowledge);
            businessAnalysis.setSoftSkills(softSkills);
            
            return businessAnalysis;
        } catch (Exception e) {
            log.error("解析业务分析失败: {}", e.getMessage());
            return new ResumeAnalysisDTO.BusinessAnalysis();
        }
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
            log.info("评估经验级别");
            
            // 计算技能成熟度总分
            int skillMaturityScore = 0;
            if (skills != null) {
                for (Integer level : skills.values()) {
                    if (level != null) {
                        skillMaturityScore += level;
                    }
                }
                // 计算平均技能水平
                if (!skills.isEmpty()) {
                    skillMaturityScore = skillMaturityScore / skills.size();
                }
            }
            
            // 根据工作年限、项目数量和技能成熟度综合评估
            int totalScore = 0;
            
            // 工作年限评分
            if (workYears != null) {
                if (workYears >= 7) totalScore += 30;
                else if (workYears >= 5) totalScore += 25;
                else if (workYears >= 3) totalScore += 20;
                else if (workYears >= 1) totalScore += 10;
                else totalScore += 5;
            }
            
            // 项目数量评分
            if (projectCount != null) {
                if (projectCount >= 10) totalScore += 20;
                else if (projectCount >= 7) totalScore += 15;
                else if (projectCount >= 4) totalScore += 10;
                else if (projectCount >= 1) totalScore += 5;
                else totalScore += 0;
            }
            
            // 技能成熟度评分
            totalScore += skillMaturityScore;
            
            // 确定经验级别
            if (totalScore >= 70) return "高级";
            else if (totalScore >= 50) return "中级";
            else return "初级";
        } catch (Exception e) {
            log.error("评估经验级别失败: {}", e.getMessage());
            return "未知";
        }
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
            return 0;
        }
        
        int totalMonths = 0;
        for (ResumeWorkExperience exp : workExperienceList) {
            try {
                if (exp.getStartDate() != null) {
                    // 处理字符串类型的日期
                    if (exp.getStartDate() instanceof String) {
                        String startDateStr = (String) exp.getStartDate();
                        String endDateStr = exp.getEndDate() != null && exp.getEndDate() instanceof String ? 
                            (String) exp.getEndDate() : null;
                        
                        // 简单处理年月格式，如"2020-01"
                        if (startDateStr.contains("-")) {
                            String[] startParts = startDateStr.split("-");
                            if (startParts.length >= 2) {
                                try {
                                    int startYear = Integer.parseInt(startParts[0]);
                                    int startMonth = Integer.parseInt(startParts[1]);
                                    
                                    int endYear, endMonth;
                                    if (endDateStr != null && endDateStr.contains("-")) {
                                        String[] endParts = endDateStr.split("-");
                                        if (endParts.length >= 2) {
                                            endYear = Integer.parseInt(endParts[0]);
                                            endMonth = Integer.parseInt(endParts[1]);
                                        } else {
                                            // 使用当前年月
                                            Calendar cal = Calendar.getInstance();
                                            endYear = cal.get(Calendar.YEAR);
                                            endMonth = cal.get(Calendar.MONTH) + 1;
                                        }
                                    } else {
                                        // 使用当前年月
                                        Calendar cal = Calendar.getInstance();
                                        endYear = cal.get(Calendar.YEAR);
                                        endMonth = cal.get(Calendar.MONTH) + 1;
                                    }
                                    
                                    // 计算月数差
                                    totalMonths += (endYear - startYear) * 12 + (endMonth - startMonth);
                                } catch (NumberFormatException e) {
                                    // 日期格式解析失败，跳过
                                    log.warn("日期格式解析失败: {}", startDateStr);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略单个经验项的计算错误，继续处理下一个
                log.warn("计算工作经验时长失败: {}", e.getMessage());
            }
        }
        
        // 转换为年，向下取整
        return Math.max(0, totalMonths / 12);
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
