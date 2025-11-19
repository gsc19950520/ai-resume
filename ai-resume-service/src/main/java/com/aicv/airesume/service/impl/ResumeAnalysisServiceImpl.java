package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.model.dto.ResumeAnalysisDTO;
import com.aicv.airesume.model.dto.QuestionAnalysisDTO;
import com.aicv.airesume.service.ResumeAnalysisService;
import com.aicv.airesume.service.ResumeService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    @Autowired
    private ResumeService resumeService;

    @Override
    public ResumeAnalysisDTO analyzeResume(Long resumeId, String jobType, String analysisDepth) {
        try {
            // 获取简历数据
            Resume resume = resumeService.getResumeById(resumeId);
            if (resume == null) {
                throw new RuntimeException("简历不存在");
            }

            // 解析简历内容
            String resumeContent = resume.getOptimizedContent() != null ? 
                resume.getOptimizedContent() : resume.getOriginalContent();
            
            return analyzeResumeContent(resumeContent, jobType, analysisDepth);
        } catch (Exception e) {
            log.error("分析简历失败: {}", e.getMessage(), e);
            throw new RuntimeException("简历分析失败: " + e.getMessage());
        }
    }

    @Override
    public ResumeAnalysisDTO analyzeResumeContent(String resumeContent, String jobType, String analysisDepth) {
        try {
            ResumeAnalysisDTO analysisDTO = new ResumeAnalysisDTO();
            
            // 基础信息设置
            analysisDTO.setAnalysisId(UUID.randomUUID().toString());
            analysisDTO.setAnalysisTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 解析简历JSON数据
            JSONObject resumeJson = JSON.parseObject(resumeContent);
            
            // 1. 分析候选人基本信息
            analysisDTO.setCandidateInfo(parseCandidateInfo(resumeJson));
            
            // 2. 分析技术栈
            analysisDTO.setTechnicalAnalysis(parseTechnicalAnalysis(resumeJson, jobType));
            
            // 3. 分析项目经验
            analysisDTO.setProjectAnalysis(parseProjectAnalysis(resumeJson));
            
            // 4. 分析业务能力
            analysisDTO.setBusinessAnalysis(parseBusinessAnalysis(resumeJson));
            
            // 5. 评估经验级别
            String experienceLevel = assessExperienceLevel(
                analysisDTO.getCandidateInfo().getWorkYears(),
                analysisDTO.getProjectAnalysis().getTotalProjects(),
                analysisDTO.getTechnicalAnalysis().getSkillProficiency()
            );
            analysisDTO.setExperienceLevel(experienceLevel);
            
            // 6. 生成优势分析
            analysisDTO.setStrengths(generateStrengths(analysisDTO));
            
            // 7. 生成待提升项
            analysisDTO.setImprovements(generateImprovements(analysisDTO));
            
            // 8. 生成面试问题清单
            analysisDTO.setInterviewQuestions(generateInterviewQuestions(analysisDTO, jobType, analysisDepth));
            
            // 9. 计算综合评分
            analysisDTO.setOverallScore(calculateOverallScore(analysisDTO));
            
            return analysisDTO;
            
        } catch (Exception e) {
            log.error("分析简历内容失败: {}", e.getMessage(), e);
            throw new RuntimeException("简历内容分析失败: " + e.getMessage());
        }
    }

    private ResumeAnalysisDTO.CandidateInfo parseCandidateInfo(JSONObject resumeJson) {
        ResumeAnalysisDTO.CandidateInfo info = new ResumeAnalysisDTO.CandidateInfo();
        
        try {
            // 解析个人信息
            JSONObject personalInfo = resumeJson.getJSONObject("personalInfo");
            if (personalInfo != null) {
                info.setName(personalInfo.getString("name"));
                info.setJobTitle(personalInfo.getString("desiredPosition"));
                info.setEducationLevel(personalInfo.getString("education"));
                info.setExpectedSalary(personalInfo.getString("expectedSalary"));
            }
            
            // 解析自我评价
            info.setSelfEvaluation(resumeJson.getString("selfEvaluation"));
            
            // 计算工作年限
            Integer workYears = calculateWorkYears(resumeJson);
            info.setWorkYears(workYears);
            
        } catch (Exception e) {
            log.error("解析候选人信息失败: {}", e.getMessage());
        }
        
        return info;
    }

    private ResumeAnalysisDTO.TechnicalAnalysis parseTechnicalAnalysis(JSONObject resumeJson, String jobType) {
        ResumeAnalysisDTO.TechnicalAnalysis analysis = new ResumeAnalysisDTO.TechnicalAnalysis();
        
        try {
            List<ResumeAnalysisDTO.TechSkill> allSkills = new ArrayList<>();
            Map<String, Integer> skillProficiency = new HashMap<>();
            
            // 解析技能列表
            JSONArray skillsArray = resumeJson.getJSONArray("skills");
            if (skillsArray != null) {
                for (int i = 0; i < skillsArray.size(); i++) {
                    JSONObject skillObj = skillsArray.getJSONObject(i);
                    ResumeAnalysisDTO.TechSkill skill = new ResumeAnalysisDTO.TechSkill();
                    skill.setName(skillObj.getString("name"));
                    skill.setProficiency(skillObj.getString("proficiency"));
                    skill.setCategory(determineSkillCategory(skill.getName()));
                    skill.setIsCoreSkill(isCoreSkill(skill.getName(), jobType));
                    
                    allSkills.add(skill);
                    skillProficiency.put(skill.getName(), parseProficiencyLevel(skill.getProficiency()));
                }
            }
            
            // 从技术描述中提取技能
            extractSkillsFromDescription(resumeJson, allSkills, skillProficiency);
            
            // 分类技能
            List<ResumeAnalysisDTO.TechSkill> primarySkills = allSkills.stream()
                .filter(skill -> skill.getIsCoreSkill())
                .collect(Collectors.toList());
            
            List<ResumeAnalysisDTO.TechSkill> secondarySkills = allSkills.stream()
                .filter(skill -> !skill.getIsCoreSkill())
                .collect(Collectors.toList());
            
            analysis.setPrimarySkills(primarySkills);
            analysis.setSecondarySkills(secondarySkills);
            analysis.setSkillProficiency(skillProficiency);
            
            // 识别技能缺口
            analysis.setSkillGaps(identifySkillGaps(primarySkills, jobType));
            
            // 评估技术水平
            analysis.setTechnicalLevel(assessTechnicalLevel(primarySkills, skillProficiency));
            
        } catch (Exception e) {
            log.error("解析技术分析失败: {}", e.getMessage());
        }
        
        return analysis;
    }

    private ResumeAnalysisDTO.ProjectAnalysis parseProjectAnalysis(JSONObject resumeJson) {
        ResumeAnalysisDTO.ProjectAnalysis analysis = new ResumeAnalysisDTO.ProjectAnalysis();
        
        try {
            List<ResumeAnalysisDTO.ProjectInfo> projects = new ArrayList<>();
            Map<String, Integer> techUsageFrequency = new HashMap<>();
            
            // 解析项目经验
            JSONArray projectsArray = resumeJson.getJSONArray("projects");
            if (projectsArray == null) {
                projectsArray = resumeJson.getJSONArray("projectExperienceList");
            }
            
            if (projectsArray != null) {
                for (int i = 0; i < projectsArray.size(); i++) {
                    JSONObject projectObj = projectsArray.getJSONObject(i);
                    ResumeAnalysisDTO.ProjectInfo project = new ResumeAnalysisDTO.ProjectInfo();
                    
                    project.setName(projectObj.getString("name"));
                    project.setDescription(projectObj.getString("description"));
                    project.setDuration(projectObj.getString("duration"));
                    project.setRole(projectObj.getString("role"));
                    
                    // 提取项目中使用的技术
                    List<String> technologies = extractTechnologiesFromProject(project.getDescription());
                    project.setTechnologies(technologies);
                    
                    // 评估项目复杂度
                    project.setComplexity(assessProjectComplexity(project));
                    
                    // 分析业务影响
                    project.setBusinessImpact(analyzeBusinessImpact(project.getDescription()));
                    
                    projects.add(project);
                    
                    // 统计技术使用频率
                    for (String tech : technologies) {
                        techUsageFrequency.put(tech, techUsageFrequency.getOrDefault(tech, 0) + 1);
                    }
                }
            }
            
            analysis.setTotalProjects(projects.size());
            analysis.setKeyProjects(projects.stream()
                .limit(5)
                .collect(Collectors.toList()));
            analysis.setProjectTypes(categorizeProjectTypes(projects));
            analysis.setTechUsageFrequency(techUsageFrequency);
            
            // 评估整体项目复杂度
            String overallComplexity = assessOverallProjectComplexity(projects);
            analysis.setProjectComplexity(overallComplexity);
            
        } catch (Exception e) {
            log.error("解析项目分析失败: {}", e.getMessage());
        }
        
        return analysis;
    }

    private ResumeAnalysisDTO.BusinessAnalysis parseBusinessAnalysis(JSONObject resumeJson) {
        ResumeAnalysisDTO.BusinessAnalysis analysis = new ResumeAnalysisDTO.BusinessAnalysis();
        
        try {
            // 领域知识分析
            List<String> domainKnowledge = extractDomainKnowledge(resumeJson);
            analysis.setDomainKnowledge(domainKnowledge);
            
            // 软技能分析
            List<String> softSkills = analyzeSoftSkills(resumeJson);
            analysis.setSoftSkills(softSkills);
            
            // 领导经验分析
            String leadershipExperience = analyzeLeadershipExperience(resumeJson);
            analysis.setLeadershipExperience(leadershipExperience);
            
            // 沟通能力分析
            String communicationSkills = analyzeCommunicationSkills(resumeJson);
            analysis.setCommunicationSkills(communicationSkills);
            
            // 问题解决能力分析
            String problemSolvingAbility = analyzeProblemSolvingAbility(resumeJson);
            analysis.setProblemSolvingAbility(problemSolvingAbility);
            
        } catch (Exception e) {
            log.error("解析业务分析失败: {}", e.getMessage());
        }
        
        return analysis;
    }

    @Override
    public Map<String, Object> getProfessionalQuestions(String jobType, String experienceLevel) {
        Map<String, Object> questions = new HashMap<>();
        
        // 根据职位类型和经验级别返回专业问题模板
        List<String> technicalQuestions = generateTechnicalQuestionsByJobType(jobType, experienceLevel);
        List<String> businessQuestions = generateBusinessQuestionsByJobType(jobType, experienceLevel);
        
        questions.put("technicalQuestions", technicalQuestions);
        questions.put("businessQuestions", businessQuestions);
        questions.put("jobType", jobType);
        questions.put("experienceLevel", experienceLevel);
        
        return questions;
    }

    @Override
    public Map<String, Object> analyzeTechnicalDepth(Map<String, Integer> skills, String projects) {
        Map<String, Object> analysis = new HashMap<>();
        
        // 分析技术深度
        String depthLevel = assessTechnicalDepth(skills, projects);
        List<String> deepDiveTopics = identifyDeepDiveTopics(skills, projects);
        List<String> advancedQuestions = generateAdvancedTechnicalQuestions(skills);
        
        analysis.put("depthLevel", depthLevel);
        analysis.put("deepDiveTopics", deepDiveTopics);
        analysis.put("advancedQuestions", advancedQuestions);
        
        return analysis;
    }

    @Override
    public QuestionAnalysisDTO generateBehavioralQuestions(String workExperience, String projects) {
        QuestionAnalysisDTO dto = new QuestionAnalysisDTO();
        dto.setCategory("行为面试");
        
        List<QuestionAnalysisDTO.QuestionItem> questions = new ArrayList<>();
        
        // 基于STAR法则的行为问题
        String[] behavioralTemplates = {
            "请描述一次你在项目中遇到困难的经历，你是如何解决的？",
            "讲述一个你需要与困难同事合作的例子，你是如何处理的？",
            "描述一次你需要在短时间内学习新技术并完成项目的经历。",
            "请分享一个你主动承担额外责任的例子，结果如何？",
            "讲述一次你在团队中发挥领导作用的经历。"
        };
        
        for (String template : behavioralTemplates) {
            QuestionAnalysisDTO.QuestionItem item = new QuestionAnalysisDTO.QuestionItem();
            item.setQuestion(template);
            item.setType("behavioral");
            item.setDifficulty("medium");
            item.setPurpose("评估候选人的行为模式和软技能");
            
            questions.add(item);
        }
        
        dto.setQuestions(questions);
        dto.setAnalysisSummary("基于STAR法则的行为面试问题，用于评估候选人的软技能和团队协作能力");
        dto.setKeyFocusPoints(Arrays.asList("情境理解", "任务分析", "行动执行", "结果评估"));
        
        return dto;
    }

    @Override
    public Map<String, Object> analyzeProjectComplexity(String[] projectDescriptions) {
        Map<String, Object> analysis = new HashMap<>();
        
        int totalComplexityScore = 0;
        List<String> complexityFactors = new ArrayList<>();
        
        for (String description : projectDescriptions) {
            int complexityScore = calculateProjectComplexityScore(description);
            totalComplexityScore += complexityScore;
            
            // 识别复杂度因素
            if (description.contains("高并发") || description.contains("分布式")) {
                complexityFactors.add("高并发/分布式系统");
            }
            if (description.contains("微服务") || description.contains("架构设计")) {
                complexityFactors.add("微服务架构");
            }
            if (description.contains("大数据") || description.contains("机器学习")) {
                complexityFactors.add("大数据/AI");
            }
        }
        
        String overallComplexity = assessOverallComplexity(totalComplexityScore, projectDescriptions.length);
        
        analysis.put("averageComplexityScore", totalComplexityScore / projectDescriptions.length);
        analysis.put("overallComplexity", overallComplexity);
        analysis.put("complexityFactors", complexityFactors.stream().distinct().collect(Collectors.toList()));
        
        return analysis;
    }

    @Override
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

    // 辅助方法实现...
    
    private Integer calculateWorkYears(JSONObject resumeJson) {
        try {
            // 从工作经历中计算工作年限
            JSONArray workExperience = resumeJson.getJSONArray("workExperience");
            if (workExperience != null && !workExperience.isEmpty()) {
                // 简化的年限计算逻辑
                return workExperience.size() * 2; // 假设每份工作平均2年
            }
        } catch (Exception e) {
            log.error("计算工作年限失败: {}", e.getMessage());
        }
        return 0;
    }

    private String determineSkillCategory(String skillName) {
        String lowerSkill = skillName.toLowerCase();
        
        if (lowerSkill.contains("java") || lowerSkill.contains("python") || lowerSkill.contains("javascript")) {
            return "编程语言";
        }
        if (lowerSkill.contains("spring") || lowerSkill.contains("django") || lowerSkill.contains("react")) {
            return "框架";
        }
        if (lowerSkill.contains("mysql") || lowerSkill.contains("redis") || lowerSkill.contains("mongodb")) {
            return "数据库";
        }
        if (lowerSkill.contains("linux") || lowerSkill.contains("docker") || lowerSkill.contains("kubernetes")) {
            return "运维工具";
        }
        
        return "其他";
    }

    private Boolean isCoreSkill(String skillName, String jobType) {
        if (jobType == null) return false;
        
        String lowerJobType = jobType.toLowerCase();
        String lowerSkill = skillName.toLowerCase();
        
        if (lowerJobType.contains("后端") || lowerJobType.contains("java")) {
            return lowerSkill.contains("java") || lowerSkill.contains("spring") || 
                   lowerSkill.contains("mysql") || lowerSkill.contains("redis");
        }
        
        if (lowerJobType.contains("前端")) {
            return lowerSkill.contains("javascript") || lowerSkill.contains("react") || 
                   lowerSkill.contains("vue") || lowerSkill.contains("css");
        }
        
        return false;
    }

    private Integer parseProficiencyLevel(String proficiency) {
        if (proficiency == null) return 1;
        
        String lower = proficiency.toLowerCase();
        if (lower.contains("精通")) return 5;
        if (lower.contains("熟练")) return 4;
        if (lower.contains("熟悉")) return 3;
        if (lower.contains("了解")) return 2;
        return 1;
    }

    private List<String> extractTechnologiesFromProject(String description) {
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
        
        if (project.getDescription() != null) {
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

    private List<String> generateStrengths(ResumeAnalysisDTO analysisDTO) {
        List<String> strengths = new ArrayList<>();
        
        // 基于分析结果生成优势
        if (analysisDTO.getTechnicalAnalysis() != null) {
            if (analysisDTO.getTechnicalAnalysis().getPrimarySkills().size() >= 5) {
                strengths.add("技术栈全面，核心技能扎实");
            }
        }
        
        if (analysisDTO.getProjectAnalysis() != null) {
            if (analysisDTO.getProjectAnalysis().getTotalProjects() >= 3) {
                strengths.add("项目经验丰富，实战能力强");
            }
        }
        
        if (analysisDTO.getCandidateInfo() != null) {
            if (analysisDTO.getCandidateInfo().getWorkYears() >= 5) {
                strengths.add("工作经验丰富，行业理解深入");
            }
        }
        
        return strengths;
    }

    private List<String> generateImprovements(ResumeAnalysisDTO analysisDTO) {
        List<String> improvements = new ArrayList<>();
        
        // 基于分析结果生成待提升项
        if (analysisDTO.getTechnicalAnalysis() != null) {
            if (!analysisDTO.getTechnicalAnalysis().getSkillGaps().isEmpty()) {
                improvements.add("建议补充技术栈短板");
            }
        }
        
        if (analysisDTO.getBusinessAnalysis() != null) {
            if (analysisDTO.getBusinessAnalysis().getDomainKnowledge().isEmpty()) {
                improvements.add("建议加强业务领域知识积累");
            }
        }
        
        return improvements;
    }

    private ResumeAnalysisDTO.InterviewQuestions generateInterviewQuestions(
            ResumeAnalysisDTO analysisDTO, String jobType, String analysisDepth) {
        
        ResumeAnalysisDTO.InterviewQuestions questions = new ResumeAnalysisDTO.InterviewQuestions();
        
        // 生成各类面试问题
        questions.setTechnicalQuestions(generateTechnicalQuestions(analysisDTO, jobType, analysisDepth));
        questions.setBehavioralQuestions(generateBehavioralQuestions(analysisDTO));
        questions.setSituationalQuestions(generateSituationalQuestions(analysisDTO));
        questions.setProjectQuestions(generateProjectQuestions(analysisDTO));
        questions.setCultureFitQuestions(generateCultureFitQuestions(analysisDTO));
        
        return questions;
    }

    private List<ResumeAnalysisDTO.QuestionDetail> generateTechnicalQuestions(
            ResumeAnalysisDTO analysisDTO, String jobType, String analysisDepth) {
        
        List<ResumeAnalysisDTO.QuestionDetail> questions = new ArrayList<>();
        
        // 基于技术栈生成问题
        if (analysisDTO.getTechnicalAnalysis() != null) {
            for (ResumeAnalysisDTO.TechSkill skill : analysisDTO.getTechnicalAnalysis().getPrimarySkills()) {
                ResumeAnalysisDTO.QuestionDetail question = new ResumeAnalysisDTO.QuestionDetail();
                question.setQuestion("请详细介绍你在" + skill.getName() + "方面的技术深度和项目应用经验。");
                question.setCategory("技术深度");
                question.setDifficulty("high");
                question.setPurpose("评估候选人对核心技术的掌握程度");
                question.setExpectedPoints(Arrays.asList("技术原理理解", "实际应用经验", "问题解决能力"));
                
                questions.add(question);
            }
        }
        
        // 根据分析深度添加不同级别的问题
        if ("advanced".equals(analysisDepth)) {
            addAdvancedTechnicalQuestions(questions, jobType);
        }
        
        return questions;
    }

    private void addAdvancedTechnicalQuestions(List<ResumeAnalysisDTO.QuestionDetail> questions, String jobType) {
        // 添加高级技术问题
        String[] advancedQuestions = {
            "请描述一次你解决的技术难题，包括问题分析、解决方案设计和最终效果。",
            "在高并发场景下，你会如何设计系统架构来保证性能和稳定性？",
            "请谈谈你对微服务架构的理解，以及在实际项目中的应用经验。"
        };
        
        for (String q : advancedQuestions) {
            ResumeAnalysisDTO.QuestionDetail question = new ResumeAnalysisDTO.QuestionDetail();
            question.setQuestion(q);
            question.setCategory("高级技术");
            question.setDifficulty("high");
            question.setPurpose("评估候选人的高级技术能力");
            questions.add(question);
        }
    }

    private List<ResumeAnalysisDTO.QuestionDetail> generateBehavioralQuestions(ResumeAnalysisDTO analysisDTO) {
        List<ResumeAnalysisDTO.QuestionDetail> questions = new ArrayList<>();
        
        String[] behavioralQuestions = {
            "请描述一次你在项目中遇到重大挑战的经历，你是如何应对的？",
            "讲述一个你需要与团队成员产生分歧的情况，你是如何解决的？",
            "请分享一个你主动学习新技术并应用到项目中的例子。"
        };
        
        for (String q : behavioralQuestions) {
            ResumeAnalysisDTO.QuestionDetail question = new ResumeAnalysisDTO.QuestionDetail();
            question.setQuestion(q);
            question.setCategory("行为面试");
            question.setDifficulty("medium");
            question.setPurpose("评估候选人的行为模式和软技能");
            questions.add(question);
        }
        
        return questions;
    }

    private List<ResumeAnalysisDTO.QuestionDetail> generateSituationalQuestions(ResumeAnalysisDTO analysisDTO) {
        List<ResumeAnalysisDTO.QuestionDetail> questions = new ArrayList<>();
        
        String[] situationalQuestions = {
            "如果你发现项目进度严重滞后，你会如何处理？",
            "当产品经理提出一个技术上不可行的需求时，你会如何沟通？",
            "如果你接手一个代码质量很差的遗留项目，你会如何改进？"
        };
        
        for (String q : situationalQuestions) {
            ResumeAnalysisDTO.QuestionDetail question = new ResumeAnalysisDTO.QuestionDetail();
            question.setQuestion(q);
            question.setCategory("情境问题");
            question.setDifficulty("medium");
            question.setPurpose("评估候选人的问题解决和决策能力");
            questions.add(question);
        }
        
        return questions;
    }

    private List<ResumeAnalysisDTO.QuestionDetail> generateProjectQuestions(ResumeAnalysisDTO analysisDTO) {
        List<ResumeAnalysisDTO.QuestionDetail> questions = new ArrayList<>();
        
        if (analysisDTO.getProjectAnalysis() != null) {
            for (ResumeAnalysisDTO.ProjectInfo project : analysisDTO.getProjectAnalysis().getKeyProjects()) {
                ResumeAnalysisDTO.QuestionDetail question = new ResumeAnalysisDTO.QuestionDetail();
                question.setQuestion("请详细介绍你在" + project.getName() + "项目中的具体职责和技术贡献。");
                question.setCategory("项目经验");
                question.setDifficulty("medium");
                question.setPurpose("深入了解候选人的项目经验和技术应用");
                questions.add(question);
            }
        }
        
        return questions;
    }

    private List<ResumeAnalysisDTO.QuestionDetail> generateCultureFitQuestions(ResumeAnalysisDTO analysisDTO) {
        List<ResumeAnalysisDTO.QuestionDetail> questions = new ArrayList<>();
        
        String[] cultureQuestions = {
            "你理想的工作环境是什么样的？",
            "你更喜欢独立工作还是团队协作？请举例说明。",
            "你对加班和快速迭代的看法是什么？"
        };
        
        for (String q : cultureQuestions) {
            ResumeAnalysisDTO.QuestionDetail question = new ResumeAnalysisDTO.QuestionDetail();
            question.setQuestion(q);
            question.setCategory("文化匹配");
            question.setDifficulty("low");
            question.setPurpose("评估候选人与公司文化的匹配度");
            questions.add(question);
        }
        
        return questions;
    }

    private Integer calculateOverallScore(ResumeAnalysisDTO analysisDTO) {
        int score = 0;
        
        // 技术能力评分 (40%)
        if (analysisDTO.getTechnicalAnalysis() != null) {
            int techScore = Math.min(analysisDTO.getTechnicalAnalysis().getPrimarySkills().size() * 5, 40);
            score += techScore;
        }
        
        // 项目经验评分 (30%)
        if (analysisDTO.getProjectAnalysis() != null) {
            int projectScore = Math.min(analysisDTO.getProjectAnalysis().getTotalProjects() * 6, 30);
            score += projectScore;
        }
        
        // 经验年限评分 (20%)
        if (analysisDTO.getCandidateInfo() != null && analysisDTO.getCandidateInfo().getWorkYears() != null) {
            int expScore = Math.min(analysisDTO.getCandidateInfo().getWorkYears() * 2, 20);
            score += expScore;
        }
        
        // 综合能力评分 (10%)
        score += 10;
        
        return Math.min(score, 100);
    }

    // 其他辅助方法...
    private void extractSkillsFromDescription(JSONObject resumeJson, List<ResumeAnalysisDTO.TechSkill> allSkills, Map<String, Integer> skillProficiency) {
        // 从项目描述和工作描述中提取技能
        // 实现逻辑...
    }

    private List<String> identifySkillGaps(List<ResumeAnalysisDTO.TechSkill> primarySkills, String jobType) {
        List<String> gaps = new ArrayList<>();
        
        // 根据职位类型识别技能缺口
        if (jobType != null && jobType.toLowerCase().contains("后端")) {
            boolean hasRedis = primarySkills.stream().anyMatch(skill -> skill.getName().toLowerCase().contains("redis"));
            boolean hasDocker = primarySkills.stream().anyMatch(skill -> skill.getName().toLowerCase().contains("docker"));
            
            if (!hasRedis) gaps.add("Redis缓存技术");
            if (!hasDocker) gaps.add("容器化技术");
        }
        
        return gaps;
    }

    private String assessTechnicalLevel(List<ResumeAnalysisDTO.TechSkill> primarySkills, Map<String, Integer> skillProficiency) {
        if (primarySkills.isEmpty()) return "初级";
        
        double avgProficiency = skillProficiency.values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(1.0);
        
        if (avgProficiency >= 4.5) return "专家级";
        if (avgProficiency >= 3.5) return "高级";
        if (avgProficiency >= 2.5) return "中级";
        return "初级";
    }

    private List<String> categorizeProjectTypes(List<ResumeAnalysisDTO.ProjectInfo> projects) {
        return projects.stream()
            .map(project -> {
                if (project.getDescription() != null) {
                    String desc = project.getDescription().toLowerCase();
                    if (desc.contains("电商") || desc.contains("购物")) return "电商平台";
                    if (desc.contains("金融") || desc.contains("银行")) return "金融系统";
                    if (desc.contains("教育") || desc.contains("学习")) return "教育平台";
                }
                return "其他系统";
            })
            .distinct()
            .collect(Collectors.toList());
    }

    private String assessOverallProjectComplexity(List<ResumeAnalysisDTO.ProjectInfo> projects) {
        int totalComplexity = projects.stream()
            .mapToInt(project -> {
                String complexity = project.getComplexity();
                if ("高复杂度".equals(complexity)) return 3;
                if ("中等复杂度".equals(complexity)) return 2;
                return 1;
            })
            .sum();
        
        double avgComplexity = projects.isEmpty() ? 0 : (double) totalComplexity / projects.size();
        
        if (avgComplexity >= 2.5) return "高复杂度";
        if (avgComplexity >= 1.5) return "中等复杂度";
        return "基础复杂度";
    }

    private List<String> extractDomainKnowledge(JSONObject resumeJson) {
        List<String> domains = new ArrayList<>();
        
        // 从项目描述和工作描述中提取领域知识
        // 实现逻辑...
        
        return domains;
    }

    private List<String> analyzeSoftSkills(JSONObject resumeJson) {
        return Arrays.asList("团队协作", "沟通能力", "学习能力");
    }

    private String analyzeLeadershipExperience(JSONObject resumeJson) {
        // 分析领导经验
        return "具备一定团队管理经验";
    }

    private String analyzeCommunicationSkills(JSONObject resumeJson) {
        // 分析沟通能力
        return "沟通表达良好";
    }

    private String analyzeProblemSolvingAbility(JSONObject resumeJson) {
        // 分析问题解决能力
        return "具备较强的问题分析和解决能力";
    }

    private List<String> generateTechnicalQuestionsByJobType(String jobType, String experienceLevel) {
        return Arrays.asList(
            "请解释Java中的多线程机制",
            "描述一下Spring框架的核心原理",
            "如何处理数据库性能优化问题？"
        );
    }

    private List<String> generateBusinessQuestionsByJobType(String jobType, String experienceLevel) {
        return Arrays.asList(
            "如何理解业务需求并转化为技术方案？",
            "描述一次你参与的业务流程优化经历",
            "如何平衡技术实现和业务需求？"
        );
    }

    private String assessTechnicalDepth(Map<String, Integer> skills, String projects) {
        return "中等技术深度";
    }

    private List<String> identifyDeepDiveTopics(Map<String, Integer> skills, String projects) {
        return Arrays.asList("Java并发编程", "数据库优化", "系统架构设计");
    }

    private List<String> generateAdvancedTechnicalQuestions(Map<String, Integer> skills) {
        return Arrays.asList(
            "请详细解释JVM内存模型和垃圾回收机制",
            "如何设计一个高并发、高可用的分布式系统？",
            "描述一下你对微服务架构的理解和实践经验"
        );
    }

    private int calculateProjectComplexityScore(String description) {
        int score = 0;
        if (description.contains("高并发")) score += 3;
        if (description.contains("分布式")) score += 2;
        if (description.contains("微服务")) score += 2;
        return score;
    }

    private String assessOverallComplexity(int totalScore, int projectCount) {
        double avgScore = projectCount > 0 ? (double) totalScore / projectCount : 0;
        if (avgScore >= 5) return "高复杂度";
        if (avgScore >= 3) return "中等复杂度";
        return "基础复杂度";
    }
}