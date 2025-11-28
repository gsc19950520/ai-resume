package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.InterviewSession;
import com.aicv.airesume.entity.JobType;
import com.aicv.airesume.entity.InterviewLog;
import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.model.dto.InterviewQuestionDTO;
import com.aicv.airesume.model.dto.InterviewReportDTO;
import com.aicv.airesume.model.dto.InterviewResponseDTO;
import com.aicv.airesume.model.vo.InterviewResponseVO;
import com.aicv.airesume.model.vo.InterviewHistoryVO;
import com.aicv.airesume.model.vo.InterviewSessionVO;
import com.aicv.airesume.model.vo.SalaryRangeVO;
import com.aicv.airesume.repository.InterviewSessionRepository;
import com.aicv.airesume.repository.InterviewLogRepository;
import com.aicv.airesume.repository.ResumeRepository;
import com.aicv.airesume.repository.InterviewQuestionRepository;
import com.aicv.airesume.repository.JobTypeRepository;
import com.aicv.airesume.entity.InterviewQuestion;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.service.AIGenerateService;
import com.aicv.airesume.service.ResumeAnalysisService;
import com.aicv.airesume.service.ResumeService;
import com.aicv.airesume.model.dto.ResumeAnalysisDTO;
import com.aicv.airesume.utils.AiServiceUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import com.aicv.airesume.entity.AiTraceLog;
import com.aicv.airesume.repository.AiTraceLogRepository;
import com.aicv.airesume.service.config.DynamicConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 面试服务实现类
 */
@Service
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    @Autowired
    private InterviewSessionRepository sessionRepository;

    @Autowired
    private InterviewLogRepository logRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private AiServiceUtils aiServiceUtils;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AiTraceLogRepository aiTraceLogRepository;
    
    @Autowired
    private InterviewQuestionRepository questionRepository;
    
    @Autowired
    private AIGenerateService aiGenerateService;
    
    @Autowired
    private DynamicConfigService dynamicConfigService;

    @Autowired
    private JobTypeRepository jobTypeRepository;
    
    @Autowired
    private ResumeService resumeService;
    
    // 保存AI调用的跟踪日志
     private void saveAiTraceLog(String sessionId, String actionType, String promptInput, String aiResponse) {
         try {
             AiTraceLog traceLog = new AiTraceLog();
             traceLog.setSessionId(sessionId);
             traceLog.setActionType(actionType);
             traceLog.setPromptInput(promptInput);
             traceLog.setAiResponse(aiResponse);
             traceLog.setCreatedAt(LocalDateTime.now());
             aiTraceLogRepository.save(traceLog);
         } catch (Exception e) {
             log.error("保存AI跟踪日志失败", e);
         }
     }

    @Override
    public InterviewResponseVO startInterview(Long userId, Long resumeId, String persona, Integer sessionSeconds, Integer jobTypeId) {
        try {
            // 1. 初始化变量
            List<String> techItems = new ArrayList<>();
            List<Map<String, Object>> projectPoints = new ArrayList<>();
            String initialDepthLevel = "usage"; // 默认深度级别
            Map<String, Object> interviewState = new HashMap<>();
            
            // 2. 从简历表中获取jobTypeId
            Integer actualJobTypeIdTemp = 1; // 默认职位类型为general
            try {
                Resume resume = resumeRepository.findById(resumeId).orElse(null);
                if (resume != null && resume.getJobTypeId() != null) {
                    actualJobTypeIdTemp = resume.getJobTypeId().intValue(); // 将Long转换为Integer
                    log.info("从简历中获取到jobTypeId: {}", actualJobTypeIdTemp);
                } else {
                    log.warn("简历中未找到jobTypeId，使用默认值1");
                }
            } catch (Exception e) {
                log.error("查询简历失败: {}", e.getMessage());
                // 查询失败不影响面试流程，使用默认值
            }
            final Integer actualJobTypeId = actualJobTypeIdTemp;
            
            // 3. 创建面试会话 - 优先完成，快速返回给前端
            InterviewSession session = new InterviewSession();
            session.setSessionId(UUID.randomUUID().toString());
            session.setUserId(userId);
            session.setResumeId(resumeId);
            session.setStatus("IN_PROGRESS");
            session.setJobTypeId(actualJobTypeId); // 使用从简历中获取的jobTypeId
            session.setCity("未知城市");
            session.setQuestionCount(0);
            session.setAdaptiveLevel("auto");
            session.setAiQuestionSeed(new Random().nextInt(1000));
            
            // 设置动态面试参数
            session.setPersona(StringUtils.hasText(persona) ? persona : dynamicConfigService.getDefaultPersona());
            session.setSessionSeconds(sessionSeconds != null ? sessionSeconds : dynamicConfigService.getDefaultSessionSeconds());
            session.setSessionTimeRemaining(session.getSessionSeconds());
            
            // 初始化面试状态，先不进行耗时的简历分析
            interviewState.put("usedTechItems", new ArrayList<>());
            interviewState.put("usedProjectPoints", new ArrayList<>());
            interviewState.put("currentDepthLevel", initialDepthLevel);
            session.setInterviewState(objectMapper.writeValueAsString(interviewState));
            
            // 保存初始会话 - 这是快速返回的关键
            sessionRepository.save(session);
            
            // 4. 异步处理简历分析和第一个问题生成
            CompletableFuture.runAsync(() -> {
                processFirstQuestionAsync(session.getSessionId(), resumeId, actualJobTypeId);
            });

            // 5. 通过jobTypeId查询job_type表数据获取jobName
            String industryJobTag = "";
            try {
                JobType jobType = jobTypeRepository.findById(actualJobTypeId).orElse(null);
                if (jobType != null && jobType.getJobName() != null) {
                    industryJobTag = jobType.getJobName();
                }
            } catch (Exception e) {
                log.error("查询职位类型失败: {}", e.getMessage());
                // 查询失败不影响面试流程，使用空字符串作为默认值
            }
            
            // 7. 构建返回对象 - 快速返回，不等待异步处理完成
            InterviewResponseVO response = new InterviewResponseVO();
            response.setSessionId(session.getSessionId());
            // 这里暂时不返回具体问题，由前端异步获取
            response.setQuestion(null);
            response.setQuestionType("first_question"); // 标记为第一个问题
            response.setFeedback(null); // 首次没有反馈
            response.setNextQuestion(null); // 第一个问题没有下一个问题
            response.setIsCompleted(false); // 面试未完成
            response.setIndustryJobTag(industryJobTag); // 设置行业职位标签

            return response;
        } catch (Exception e) {
            // 捕获异常后直接抛出
            log.error("开始面试失败: {}, 详细错误: {}", 
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("面试初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从简历分析结果中提取技术项列表
     * @param analysisDTO 简历分析结果
     * @return 技术项列表
     */
    private List<String> extractTechItemsFromAnalysis(ResumeAnalysisDTO analysisDTO) {
        // 添加空值检查，避免空指针异常
        if (analysisDTO == null) {
            log.warn("提取技术项：分析结果为空");
            return new ArrayList<>();
        }
        List<String> techItems = new ArrayList<>();
        
        // 从主要技能中提取
        if (analysisDTO.getTechnicalAnalysis() != null && 
            analysisDTO.getTechnicalAnalysis().getPrimarySkills() != null) {
            analysisDTO.getTechnicalAnalysis().getPrimarySkills().stream()
                .filter(Objects::nonNull)
                .map(ResumeAnalysisDTO.TechSkill::getName)
                .forEach(techItems::add);
        }
        
        // 从次要技能中提取
        if (analysisDTO.getTechnicalAnalysis() != null && 
            analysisDTO.getTechnicalAnalysis().getSecondarySkills() != null) {
            analysisDTO.getTechnicalAnalysis().getSecondarySkills().stream()
                .filter(Objects::nonNull)
                .map(ResumeAnalysisDTO.TechSkill::getName)
                .forEach(techItems::add);
        }
        
        // 从技能熟练度映射中提取
        if (analysisDTO.getTechnicalAnalysis() != null && 
            analysisDTO.getTechnicalAnalysis().getSkillProficiency() != null) {
            techItems.addAll(analysisDTO.getTechnicalAnalysis().getSkillProficiency().keySet());
        }
        
        return techItems;
    }
    
    /**
     * 从简历分析结果中提取项目点列表
     * @param analysisDTO 简历分析结果
     * @return 项目点列表
     */
    private List<Map<String, Object>> extractProjectPointsFromAnalysis(ResumeAnalysisDTO analysisDTO) {
        // 添加空值检查，避免空指针异常
        if (analysisDTO == null) {
            log.warn("提取项目点：分析结果为空");
            return new ArrayList<>();
        }
        List<Map<String, Object>> projectPoints = new ArrayList<>();
        
        if (analysisDTO.getProjectAnalysis() != null && 
            analysisDTO.getProjectAnalysis().getKeyProjects() != null) {
            for (ResumeAnalysisDTO.ProjectInfo project : analysisDTO.getProjectAnalysis().getKeyProjects()) {
                if (project != null) {
                    Map<String, Object> projectPoint = new HashMap<>();
                    projectPoint.put("name", project.getName());
                    projectPoint.put("description", project.getDescription());
                    projectPoint.put("role", project.getRole());
                    projectPoint.put("technologies", project.getTechnologies());
                    projectPoint.put("complexity", project.getComplexity());
                    projectPoint.put("businessImpact", project.getBusinessImpact());
                    projectPoints.add(projectPoint);
                }
            }
        }
        
        return projectPoints;
    }
    
    /**
     * 根据经验级别确定初始深度
     * @param experienceLevel 经验级别
     * @return 初始深度级别
     */
    private String getInitialDepthLevelByExperience(String experienceLevel) {
        if (experienceLevel == null) {
            return "usage";
        }
        
        switch (experienceLevel.toLowerCase()) {
            case "初级":
            case "助理":
                return "usage";
            case "中级":
                return "application";
            case "高级":
            case "资深":
                return "principle";
            case "专家":
            case "架构师":
                return "design";
            default:
                return "usage";
        }
    }

    /**
     * 异步处理第一个面试问题的生成
     * 
     * @param sessionId   会话ID
     * @param resumeId    简历ID
     * @param jobTypeId   职位类型ID
     */
    private void processFirstQuestionAsync(String sessionId, Long resumeId, Integer jobTypeId) {
        try {
            // 从数据库获取会话信息
            InterviewSession session = sessionRepository.findBySessionId(sessionId).orElse(null);
            if (session == null) {
                log.error("异步处理: 会话不存在，sessionId: {}", sessionId);
                return;
            }
            
            // 获取完整简历内容并提取结构化信息
            Map<String, Object> fullResumeData = resumeService.getResumeFullData(resumeId);
            String resumeContent = convertFullDataToText(fullResumeData);
            log.info("异步处理: 成功获取完整简历内容，将用于生成面试问题");
            
            // 从简历内容中提取技术项和项目点
            Map<String, Object> extractedData = extractTechItemsAndProjectPoints(resumeContent);
            List<String> techItems = new ArrayList<>();
            List<Map<String, Object>> projectPoints = new ArrayList<>();
            
            if (extractedData != null) {
                if (extractedData.containsKey("techItems")) {
                    techItems = (List<String>) extractedData.get("techItems");
                }
                if (extractedData.containsKey("projectPoints")) {
                    projectPoints = (List<Map<String, Object>>) extractedData.get("projectPoints");
                }
                log.info("异步处理: 从简历内容中提取数据: 技术项{}个，项目点{}个", techItems.size(), projectPoints.size());
            }
            
            // 解析面试状态
            Map<String, Object> interviewState = new HashMap<>();
            if (StringUtils.hasText(session.getInterviewState())) {
                try {
                    interviewState = objectMapper.readValue(session.getInterviewState(), new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.error("解析面试状态失败: {}", e.getMessage());
                    interviewState.put("usedTechItems", new ArrayList<>());
                    interviewState.put("usedProjectPoints", new ArrayList<>());
                    interviewState.put("currentDepthLevel", "usage");
                }
            } else {
                interviewState.put("usedTechItems", new ArrayList<>());
                interviewState.put("usedProjectPoints", new ArrayList<>());
                interviewState.put("currentDepthLevel", "usage");
            }
            
            // 更新面试状态，包含完整简历内容
            interviewState.put("fullResumeContent", resumeContent);
            
            // 存储提取的数据到会话
            session.setTechItems(objectMapper.writeValueAsString(techItems));
            session.setProjectPoints(objectMapper.writeValueAsString(projectPoints));
            
            // 获取职位类型名称，用于生成更精准的问题
            String jobType = "";
            if (jobTypeId != null) {
                Optional<JobType> jobTypeOptional = jobTypeRepository.findById(jobTypeId);
                if (jobTypeOptional.isPresent()) {
                    jobType = jobTypeOptional.get().getJobName();
                }
            }
            
            // 检查问题库中是否有足够的针对当前职位的第一道题（至少20个）
            String firstSkillTag = techItems != null && !techItems.isEmpty() ? techItems.get(0) : "general";
            List<InterviewQuestion> existingQuestions = questionRepository.findBySkillTagAndDepthLevel(firstSkillTag, "usage");
            int requiredCount = 20;
            int existingCount = existingQuestions.size();
            
            // 扩展：同时检查职位类型相关的问题
            List<InterviewQuestion> jobTypeQuestions = new ArrayList<>();
            if (jobTypeId != null) {
                // 使用已有的方法查询职位类型相关的问题
                jobTypeQuestions = questionRepository.findByJobTypeIdAndSkillTagOrderByUsageCountDesc(jobTypeId.longValue(), firstSkillTag);
                existingCount += jobTypeQuestions.size();
            }
            
            if (existingCount < requiredCount) {
                // 批量生成缺少的问题
                int questionsToGenerate = requiredCount - existingCount;
                log.info("批量生成针对当前职位的第一道题，需要生成{}个", questionsToGenerate);
                
                // 使用完整的简历内容和技术项生成多个问题
                for (int i = 0; i < questionsToGenerate; i++) {
                    try {
                        // 调用AI生成新问题，传入职位类型以生成更精准的问题
                        Map<String, Object> questionData = generateNewQuestionWithAI(
                                techItems, projectPoints, new ArrayList<>(), new ArrayList<>(),
                                "usage", session.getSessionTimeRemaining(), session.getPersona(), jobType, resumeContent, "", "");
                        
                        if (questionData.containsKey("nextQuestion")) {
                            String questionText = (String) questionData.get("nextQuestion");
                            boolean isSimilar = false;
                            
                            // 1. 先检查完整问题库中是否有高度相似的问题
                            // 获取所有相关问题
                            List<InterviewQuestion> allQuestions = questionRepository.findBySkillTagAndDepthLevel(firstSkillTag, "usage");
                            if (jobTypeId != null && jobTypeQuestions != null && !jobTypeQuestions.isEmpty()) {
                                allQuestions.addAll(jobTypeQuestions);
                            }
                            
                            // 2. 计算新生成问题与现有问题的相似度
                            for (InterviewQuestion existingQuestion : allQuestions) {
                                double similarity = calculateQuestionSimilarity(questionText, existingQuestion.getQuestionText());
                                if (similarity > 0.75) { // 相似度阈值设为75%
                                    log.info("生成的问题与现有问题相似度较高({:.2f}%)，跳过保存", similarity * 100);
                                    isSimilar = true;
                                    break;
                                }
                            }
                            
                            // 3. 如果不相似，再检查哈希值是否完全相同（防止完全重复）
                            if (!isSimilar) {
                                String similarityHash = aiServiceUtils.getSemanticHash(questionText);
                                if (questionRepository.findAllBySimilarityHash(similarityHash).isEmpty()) {
                                    // 保存新问题到数据库
                                    InterviewQuestion newQuestion = new InterviewQuestion();
                                    newQuestion.setQuestionText(questionText);
                                    newQuestion.setExpectedKeyPoints(String.join(",", (List<String>) questionData.getOrDefault("expectedKeyPoints", Collections.emptyList())));
                                    newQuestion.setSkillTag(firstSkillTag);
                                    newQuestion.setDepthLevel((String) questionData.getOrDefault("depthLevel", "usage"));
                                    newQuestion.setPersona(session.getPersona());
                                    newQuestion.setAiGenerated(true);
                                    newQuestion.setUsageCount(0); // 初始使用次数为0
                                    newQuestion.setSimilarityHash(similarityHash);
                                    newQuestion.setCreatedAt(LocalDateTime.now());
                                    newQuestion.setUpdatedAt(LocalDateTime.now());
                                    
                                    // 将jobTypeId从Integer转换为Long类型
                                    if (jobTypeId != null) {
                                        try {
                                            newQuestion.setJobTypeId(jobTypeId.longValue());
                                        } catch (NumberFormatException e) {
                                            log.warn("无效的jobTypeId格式: {}", jobTypeId);
                                            newQuestion.setJobTypeId(1L);
                                        }
                                    }
                                    
                                    questionRepository.save(newQuestion);
                                    log.info("成功生成并保存第{}个面试问题", i+1);
                                } else {
                                    log.info("生成的问题已存在，跳过保存");
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("批量生成问题失败: {}", e.getMessage());
                    }
                }
            }
            
            // 生成第一个问题
            Map<String, Object> firstQuestionData = generateNextQuestion(
                    techItems,
                    projectPoints,
                    interviewState,
                    session.getSessionTimeRemaining(),
                    session.getPersona(),
                    jobTypeId
            );
            
            if (firstQuestionData != null && firstQuestionData.get("nextQuestion") != null) {
                String firstQuestion = (String) firstQuestionData.get("nextQuestion");
                
                // 保存AI生成问题的跟踪日志
                saveAiTraceLog(session.getSessionId(), "generate_question", 
                        "生成第一个问题的prompt内容", firstQuestion);
                
                // 更新会话问题计数
                session.setQuestionCount(1);
                session.setInterviewState(objectMapper.writeValueAsString(interviewState));
                sessionRepository.save(session);
                
                // 创建第一个问题日志
                InterviewLog firstLog = new InterviewLog();
                firstLog.setQuestionId(UUID.randomUUID().toString());
                firstLog.setSessionId(session.getSessionId());
                firstLog.setQuestionText(firstQuestion);
                firstLog.setDepthLevel((String) firstQuestionData.get("depthLevel"));
                firstLog.setRoundNumber(1);
                // 保存期望的关键点
                if (firstQuestionData.containsKey("expectedKeyPoints")) {
                    try {
                        List<String> expectedKeyPoints = (List<String>) firstQuestionData.get("expectedKeyPoints");
                        firstLog.setExpectedKeyPoints(objectMapper.writeValueAsString(expectedKeyPoints));
                    } catch (Exception e) {
                        log.error("保存期望关键点失败: {}", e.getMessage());
                    }
                }
                logRepository.save(firstLog);
                
                log.info("异步处理: 成功生成第一个面试问题并保存，会话ID: {}", session.getSessionId());
            } else {
                log.error("异步处理: 生成的问题数据为空，会话ID: {}", session.getSessionId());
                // 可以设置默认问题作为备选
                InterviewLog defaultLog = new InterviewLog();
                defaultLog.setQuestionId(UUID.randomUUID().toString());
                defaultLog.setSessionId(session.getSessionId());
                defaultLog.setQuestionText("请简单介绍一下你自己，以及你为什么适合这个职位？");
                defaultLog.setDepthLevel("usage");
                defaultLog.setRoundNumber(1);
                logRepository.save(defaultLog);
                
                session.setQuestionCount(1);
                sessionRepository.save(session);
            }
        } catch (Exception e) {
            log.error("异步处理简历分析和问题生成失败: {}, 会话ID: {}", 
                    e.getMessage(), sessionId, e);
            
            // 异步处理失败时，确保有一个默认问题
            try {
                // 从数据库获取会话信息
                InterviewSession session = sessionRepository.findBySessionId(sessionId).orElse(null);
                if (session == null) {
                    log.error("异步处理异常: 会话不存在，sessionId: {}", sessionId);
                    return;
                }
                
                InterviewLog defaultLog = new InterviewLog();
                defaultLog.setQuestionId(UUID.randomUUID().toString());
                defaultLog.setSessionId(session.getSessionId());
                defaultLog.setQuestionText("请简单介绍一下你自己，以及你为什么适合这个职位？");
                defaultLog.setDepthLevel("usage");
                defaultLog.setRoundNumber(1);
                logRepository.save(defaultLog);
                
                session.setQuestionCount(1);
                sessionRepository.save(session);
            } catch (Exception ex) {
                log.error("设置默认问题失败: {}, 会话ID: {}", ex.getMessage(), sessionId, ex);
            }
        }
    }

    @Override
    public String getFirstQuestion(String sessionId) {
        try {
            // 查询会话是否存在
            InterviewSession session = sessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
            
            // 查询该会话的第一个面试日志
            List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);
            
            // 如果日志不为空，直接返回第一个问题
            if (!logs.isEmpty()) {
                InterviewLog firstLog = logs.get(0);
                return firstLog.getQuestionText(); // 返回第一个问题
            }
            
            // 如果日志为空，说明异步处理可能失败，直接生成第一个问题
            log.info("第一个问题尚未生成，直接生成");
            
            // 从数据库获取会话信息
            String resumeContent = "";
            List<String> techItems = new ArrayList<>();
            List<Map<String, Object>> projectPoints = new ArrayList<>();
            Map<String, Object> interviewState = new HashMap<>();
            
            // 解析面试状态
            if (StringUtils.hasText(session.getInterviewState())) {
                try {
                    interviewState = objectMapper.readValue(session.getInterviewState(), new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.error("解析面试状态失败: {}", e.getMessage());
                    interviewState.put("usedTechItems", new ArrayList<>());
                    interviewState.put("usedProjectPoints", new ArrayList<>());
                    interviewState.put("currentDepthLevel", "usage");
                }
            } else {
                interviewState.put("usedTechItems", new ArrayList<>());
                interviewState.put("usedProjectPoints", new ArrayList<>());
                interviewState.put("currentDepthLevel", "usage");
            }
            
            // 获取完整简历内容
            if (interviewState.containsKey("fullResumeContent")) {
                resumeContent = (String) interviewState.get("fullResumeContent");
            } else {
                // 如果简历内容不存在，从数据库获取
                Map<String, Object> fullResumeData = resumeService.getResumeFullData(session.getResumeId());
                resumeContent = convertFullDataToText(fullResumeData);
                interviewState.put("fullResumeContent", resumeContent);
            }
            
            // 获取技术项和项目点
            if (StringUtils.hasText(session.getTechItems())) {
                techItems = objectMapper.readValue(session.getTechItems(), new TypeReference<List<String>>() {});
            } else {
                // 如果技术项不存在，从简历内容中提取
                Map<String, Object> extractedData = extractTechItemsAndProjectPoints(resumeContent);
                if (extractedData != null) {
                    if (extractedData.containsKey("techItems")) {
                        techItems = (List<String>) extractedData.get("techItems");
                    }
                    if (extractedData.containsKey("projectPoints")) {
                        projectPoints = (List<Map<String, Object>>) extractedData.get("projectPoints");
                    }
                    // 保存技术项和项目点到会话
                    session.setTechItems(objectMapper.writeValueAsString(techItems));
                    session.setProjectPoints(objectMapper.writeValueAsString(projectPoints));
                    sessionRepository.save(session);
                }
            }
            
            // 生成第一个问题
            Map<String, Object> firstQuestionData = generateNextQuestion(
                    techItems,
                    projectPoints,
                    interviewState,
                    session.getSessionTimeRemaining(),
                    session.getPersona(),
                    session.getJobTypeId()
            );
            
            String firstQuestion = null;
            if (firstQuestionData != null && firstQuestionData.get("nextQuestion") != null) {
                firstQuestion = (String) firstQuestionData.get("nextQuestion");
            } else {
                // 如果生成问题失败，从问题库中随机选择一个
                List<InterviewQuestion> questions = questionRepository.findAllByDepthLevel("usage");
                if (!questions.isEmpty()) {
                    // 随机选择一个问题
                    Random random = new Random();
                    InterviewQuestion selectedQuestion = questions.get(random.nextInt(questions.size()));
                    firstQuestion = selectedQuestion.getQuestionText();
                } else {
                    // 如果问题库中也没有，使用默认问题
                    firstQuestion = "请简单介绍一下你自己，以及你为什么适合这个职位？";
                }
            }
            
            // 保存AI生成问题的跟踪日志
            saveAiTraceLog(session.getSessionId(), "generate_question", 
                    "生成第一个问题的prompt内容", firstQuestion);
            
            // 更新会话问题计数
            session.setQuestionCount(1);
            session.setInterviewState(objectMapper.writeValueAsString(interviewState));
            sessionRepository.save(session);
            
            // 创建第一个问题日志
            InterviewLog firstLog = new InterviewLog();
            firstLog.setQuestionId(UUID.randomUUID().toString());
            firstLog.setSessionId(session.getSessionId());
            firstLog.setQuestionText(firstQuestion);
            firstLog.setDepthLevel((String) firstQuestionData.getOrDefault("depthLevel", "usage"));
            firstLog.setRoundNumber(1);
            // 保存期望的关键点
            if (firstQuestionData != null && firstQuestionData.containsKey("expectedKeyPoints")) {
                try {
                    List<String> expectedKeyPoints = (List<String>) firstQuestionData.get("expectedKeyPoints");
                    firstLog.setExpectedKeyPoints(objectMapper.writeValueAsString(expectedKeyPoints));
                } catch (Exception e) {
                    log.error("保存期望关键点失败: {}", e.getMessage());
                }
            }
            logRepository.save(firstLog);
            
            log.info("直接生成第一个面试问题并保存，会话ID: {}", session.getSessionId());
            return firstQuestion;
        } catch (Exception e) {
            log.error("获取第一个问题失败: {}", e.getMessage(), e);
            // 即使出现异常，也从问题库中随机返回一个问题
            try {
                List<InterviewQuestion> questions = questionRepository.findAllByDepthLevel("usage");
                if (!questions.isEmpty()) {
                    // 随机选择一个问题
                    Random random = new Random();
                    InterviewQuestion selectedQuestion = questions.get(random.nextInt(questions.size()));
                    return selectedQuestion.getQuestionText();
                }
            } catch (Exception ex) {
                log.error("从问题库获取默认问题失败: {}", ex.getMessage());
            }
            // 如果问题库也不可用，返回默认问题
            return "请简单介绍一下你自己，以及你为什么适合这个职位？";
        }
    }

    @Override
    public InterviewResponseDTO submitAnswer(String sessionId, String userAnswerText, Integer answerDuration, String toneStyle) {
        try {
            // 1. 获取会话信息
            InterviewSession session = sessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
            
            // 获取最新的问题日志
            List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);
            if (logs.isEmpty()) {
                throw new RuntimeException("问题不存在");
            }
            InterviewLog currentLog = logs.get(logs.size() - 1);

            // 2. 更新当前问题日志
            currentLog.setUserAnswerText(userAnswerText);
            currentLog.setAnswerDuration(answerDuration);
            
            // 3. 计算剩余时间
            session.setSessionTimeRemaining(session.getSessionTimeRemaining() - answerDuration);
            
            // 4. 调用aiAssessmentPerQuestion模块评分
            List<String> expectedKeyPoints = new ArrayList<>(); 
            // 从当前日志中获取期望的关键点
            if (StringUtils.hasText(currentLog.getExpectedKeyPoints())) {
                try {
                    expectedKeyPoints = objectMapper.readValue(currentLog.getExpectedKeyPoints(), List.class);
                } catch (Exception e) {
                    log.error("解析期望关键点失败: {}", e.getMessage());
                }
            }
            Map<String, Object> assessment = assessAnswer(currentLog.getQuestionText(), userAnswerText, expectedKeyPoints, session.getPersona());
            
            // 5. 更新评分和反馈
            Map<String, Double> scoreDetail = (Map<String, Double>) assessment.get("score_detail");
            // 确保scoreDetail不为null
            if (scoreDetail == null) {
                scoreDetail = new HashMap<>();
            }
            currentLog.setTechScore(scoreDetail.getOrDefault("tech", 0.0));
            currentLog.setLogicScore(scoreDetail.getOrDefault("logic", 0.0));
            currentLog.setClarityScore(scoreDetail.getOrDefault("clarity", 0.0));
            currentLog.setDepthScore(scoreDetail.getOrDefault("depth", 0.0));
            currentLog.setFeedback((String) assessment.get("feedback"));
            currentLog.setMatchedPoints(objectMapper.valueToTree(assessment.getOrDefault("matchedPoints", new ArrayList<>())).toString());
            
            // 保存AI原始评分和分析结果
            if (assessment.containsKey("aiFeedbackJson")) {
                currentLog.setAiFeedbackJson((String) assessment.get("aiFeedbackJson"));
            }
            
            logRepository.save(currentLog);
            
            // 更新问题库中的使用次数和平均得分
            updateQuestionUsageStatistics(currentLog.getQuestionText(), assessment);
            
            // 保存AI评分的跟踪日志
              saveAiTraceLog(sessionId, "score_answer", 
                      "回答评分的prompt内容：问题=" + currentLog.getQuestionText() + ",回答=" + userAnswerText, 
                      (String) assessment.getOrDefault("aiFeedbackJson", ""));

            // 6. 解析技术项和项目点
            List<String> techItems = objectMapper.readValue(session.getTechItems(), List.class);
            List<Map<String, Object>> projectPoints = objectMapper.readValue(session.getProjectPoints(), List.class);
            Map<String, Object> interviewState = objectMapper.readValue(session.getInterviewState(), Map.class);

            // 7. 检查停止条件
            boolean shouldStop = false;
            String stopReason = "";
            
            // 检查时间是否用完
            if (session.getSessionTimeRemaining() <= 60) {
                shouldStop = true;
                stopReason = "time_up";
                session.setStopReason(stopReason);
            }
            
            // 检查连续不匹配次数
            Double techScore = scoreDetail.getOrDefault("tech", 0.0);
            if (techScore < 3.0) {
                session.setConsecutiveNoMatchCount(session.getConsecutiveNoMatchCount() + 1);
                if (session.getConsecutiveNoMatchCount() >= 2) {
                    shouldStop = true;
                    stopReason = "no_more_followups";
                    session.setStopReason(stopReason);
                }
            } else {
                session.setConsecutiveNoMatchCount(0);
            }

            // 8. 构建响应对象
            InterviewResponseDTO response = new InterviewResponseDTO();
            response.setSessionId(sessionId);
            
            // 设置当前问题和类型（使用当前日志中的问题）
            response.setQuestion(currentLog.getQuestionText());
            response.setQuestionType("技术问题"); // 可以根据实际情况从currentLog获取
            
            // 设置评分（计算总分）
            double totalScore = scoreDetail.getOrDefault("tech", 0.0) + 
                               scoreDetail.getOrDefault("logic", 0.0) + 
                               scoreDetail.getOrDefault("clarity", 0.0) + 
                               scoreDetail.getOrDefault("depth", 0.0);
            response.setScore(totalScore);
            
            // 设置反馈
            response.setFeedback((String) assessment.get("feedback"));
            
            // 设置评分和反馈信息到additionalInfo
            Map<String, Object> additionalInfo = response.getAdditionalInfo();
            if (additionalInfo == null) {
                additionalInfo = new HashMap<>();
                response.setAdditionalInfo(additionalInfo);
            }
            additionalInfo.put("scoreDetail", scoreDetail);
            additionalInfo.put("feedback", assessment.get("feedback"));
            
            if (!shouldStop) {
                // 9. 动态生成下一个问题
                Map<String, Object> nextQuestionData = generateNextQuestion(
                        techItems,
                        projectPoints,
                        interviewState,
                        session.getSessionTimeRemaining(),
                        session.getPersona(),
                        session.getJobTypeId()
                );
                
                // 检查AI是否要求停止
                if (StringUtils.hasText((String) nextQuestionData.get("stopReason"))) {
                    shouldStop = true;
                    stopReason = (String) nextQuestionData.get("stopReason");
                    session.setStopReason(stopReason);
                }
                
                if (!shouldStop) {
                    // 10. 创建下一个问题日志
                    InterviewLog nextLog = new InterviewLog();
                    nextLog.setQuestionId(UUID.randomUUID().toString());
                    nextLog.setSessionId(session.getSessionId()); // 使用sessionId而不是被注释掉的session关联
                    nextLog.setQuestionText((String) nextQuestionData.get("nextQuestion"));
                    nextLog.setDepthLevel((String) nextQuestionData.get("depthLevel"));
                    nextLog.setRoundNumber(currentLog.getRoundNumber() + 1);
                    
                    // 存储关联的技术项和项目点
                    if (nextQuestionData.containsKey("relatedTechItems")) {
                        nextLog.setRelatedTechItems(objectMapper.writeValueAsString(nextQuestionData.get("relatedTechItems")));
                    }
                    if (nextQuestionData.containsKey("relatedProjectPoints")) {
                        nextLog.setRelatedProjectPoints(objectMapper.writeValueAsString(nextQuestionData.get("relatedProjectPoints")));
                    }
                    
                    // 设置当前问题的面试官风格
                    nextLog.setPersona(session.getPersona());
                    
                    // 保存期望的关键点
                    if (nextQuestionData.containsKey("expectedKeyPoints")) {
                        try {
                            List<String> nextExpectedKeyPoints = (List<String>) nextQuestionData.get("expectedKeyPoints");
                            nextLog.setExpectedKeyPoints(objectMapper.writeValueAsString(nextExpectedKeyPoints));
                        } catch (Exception e) {
                            log.error("保存期望关键点失败: {}", e.getMessage());
                        }
                    }
                    
                    logRepository.save(nextLog);
                    
                    // 增加问题计数
                    session.setQuestionCount(session.getQuestionCount() + 1);
                    
                    // 保存AI生成问题的跟踪日志
                     saveAiTraceLog(session.getSessionId(), "generate_question", 
                             "生成后续问题的prompt内容", 
                             (String) nextQuestionData.get("nextQuestion"));

                    // 更新面试状态
                    interviewState.put("usedTechItems", nextQuestionData.getOrDefault("usedTechItems", interviewState.get("usedTechItems")));
                    interviewState.put("usedProjectPoints", nextQuestionData.getOrDefault("usedProjectPoints", interviewState.get("usedProjectPoints")));
                    interviewState.put("currentDepthLevel", nextQuestionData.getOrDefault("depthLevel", "usage"));
                    session.setInterviewState(objectMapper.writeValueAsString(interviewState));

                    // 设置下一个问题和类型
                    response.setNextQuestion((String) nextQuestionData.get("nextQuestion"));
                    response.setNextQuestionType("技术问题"); // 可以根据实际情况获取
                    
                    // 使用已有的additionalInfo变量，避免重复定义
                    additionalInfo.put("nextQuestion", nextQuestionData.get("nextQuestion"));
                    additionalInfo.put("depthLevel", nextQuestionData.get("depthLevel"));
                    additionalInfo.put("sessionTimeRemaining", session.getSessionTimeRemaining());
                    additionalInfo.put("nextQuestionId", nextLog.getQuestionId());
                }
            }
            
            // 设置面试是否完成
            response.setIsCompleted(shouldStop);
            
            // 保存会话更新
            sessionRepository.save(session);
            
            // 获取已存在的additionalInfo
            Map<String, Object> responseAdditionalInfo = response.getAdditionalInfo();
            if (responseAdditionalInfo == null) {
                responseAdditionalInfo = new HashMap<>();
                response.setAdditionalInfo(responseAdditionalInfo);
            }
            responseAdditionalInfo.put("shouldStop", shouldStop);
            responseAdditionalInfo.put("stopReason", stopReason);
            
            return response;
        } catch (Exception e) {
            log.error("提交答案失败", e);
            throw new RuntimeException("处理答案失败: " + e.getMessage());
        }
    }

    @Override
    public InterviewReportDTO finishInterview(String sessionId) {
        try {
            // 1. 获取会话信息和所有日志
            InterviewSession session = sessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("会话不存在"));
            List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);

            // 2. 计算聚合评分
            Map<String, Double> aggregatedScores = calculateAggregatedScores(logs);
            double totalScore = aggregatedScores.get("total");

            // 3. 更新会话评分和状态
            session.setTotalScore(totalScore);
            session.setStatus("FINISHED");
            session.setEndTime(LocalDateTime.now());
            
            // 4. 保存会话更新
            sessionRepository.save(session);

            // 5. 生成面试报告
            InterviewReportDTO reportDTO = new InterviewReportDTO();
            reportDTO.setSessionId(sessionId);
            reportDTO.setFinalScore(totalScore);
            reportDTO.setScores(aggregatedScores);
            // 设置整体反馈（简化处理）
            reportDTO.setOverallFeedback("面试完成，整体表现良好。");
            reportDTO.setStrengths("技术能力扎实，表达清晰。");
            reportDTO.setAreasForImprovement("可以在项目细节和深度方面进一步提升。");
            
            return reportDTO;
        } catch (Exception e) {
            log.error("结束面试失败", e);
            throw new RuntimeException("生成面试报告失败: " + e.getMessage());
        }
    }

    public Map<String, Object> extractTechItemsAndProjectPoints(String resumeText) {
        try {
            // 记录简历文本的长度和部分内容，确保不为空
            log.info("extractTechItemsAndProjectPoints - 简历文本长度: {}, 开始部分: {}", 
                     resumeText.length(), 
                     resumeText.length() > 50 ? resumeText.substring(0, 50) + "..." : resumeText);
                      
            // 构建prompt调用projectAnalyzer，优化prompt以更好地提取技术项和项目点
            String prompt = String.format("请仔细分析以下简历内容，提取候选人掌握的核心技术项（编程语言、框架、工具等）以及可追问的项目点。\n请确保即使简历格式不规范也尝试提取信息。\n简历内容：%s\n请严格按照以下JSON格式输出，不要包含任何其他文字：\n{\"techItems\":[\"技术1\",\"技术2\",\"技术3\"],\"projectPoints\":[{\"title\":\"项目点描述\",\"difficulty\":\"easy|intermediate|advanced\"}]}", resumeText);
            
            // 调用AI服务
            String aiResponse = aiServiceUtils.callDeepSeekApi(prompt);
            if(aiResponse == null || aiResponse.isEmpty()){
                log.error("will TODO");
                aiResponse = "{\r\n" + //
                                        "\"techItems\": [\"java\", \"node\", \"pathon\"],\r\n" + //
                                        "\"projectPoints\": [\r\n" + //
                                        "{\r\n" + //
                                        "\"title\": \"后端项目的具体技术实现和架构设计\",\r\n" + //
                                        "\"difficulty\": \"intermediate\"\r\n" + //
                                        "}\r\n" + //
                                        "]\r\n" + //
                                        "}";
            }
            log.info("extractTechItemsAndProjectPoints - AI服务响应: {}", aiResponse);
            
            // 解析结果
            JsonNode jsonNode = objectMapper.readTree(aiResponse);
            List<String> techItems = new ArrayList<>();
            List<Map<String, Object>> projectPoints = new ArrayList<>();
            
            if (jsonNode.has("techItems")) {
                for (JsonNode node : jsonNode.get("techItems")) {
                    techItems.add(node.asText());
                }
            }
            
            if (jsonNode.has("projectPoints")) {
                for (JsonNode node : jsonNode.get("projectPoints")) {
                    Map<String, Object> point = new HashMap<>();
                    if (node.has("title")) point.put("title", node.get("title").asText());
                    if (node.has("difficulty")) point.put("difficulty", node.get("difficulty").asText());
                    projectPoints.add(point);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            
            // 如果AI没有提取到技术项，尝试从简历文本中简单提取
            if (techItems.isEmpty()) {
                log.warn("AI未提取到技术项，尝试备用提取方法");
                // 简单的技术词模式匹配
                List<String> commonTechnologies = Arrays.asList(
                    "Java", "Python", "JavaScript", "Spring", "MySQL", "Redis", "React", 
                    "Vue", "Angular", "Node.js", "Docker", "Kubernetes", "Git", "MongoDB",
                    "PostgreSQL", "HTML", "CSS", "SpringBoot", "MyBatis", "Redis"
                );
                
                for (String tech : commonTechnologies) {
                    if (resumeText.contains(tech) && !techItems.contains(tech)) {
                        techItems.add(tech);
                    }
                }
            }
            
            // 如果AI没有提取到项目点，尝试从简历文本中简单提取
            if (projectPoints.isEmpty()) {
                log.warn("AI未提取到项目点，尝试备用提取方法");
                // 检查是否包含项目经历部分
                if (resumeText.contains("项目经历") || resumeText.contains("项目名称")) {
                    Map<String, Object> defaultProject = new HashMap<>();
                    defaultProject.put("title", "简历中提到的项目经验");
                    defaultProject.put("difficulty", "intermediate");
                    projectPoints.add(defaultProject);
                }
            }
            
            result.put("techItems", techItems);
            result.put("projectPoints", projectPoints);
            log.info("最终提取结果 - 技术项数量: {}, 项目点数量: {}", techItems.size(), projectPoints.size());
            return result;
        } catch (Exception e) {
            log.error("提取技术项和项目点失败", e);
            // 返回默认值以确保系统继续运行
            Map<String, Object> result = new HashMap<>();
            result.put("techItems", Arrays.asList("Java", "Spring", "MySQL"));
            Map<String, String> projectPointMap = new HashMap<>();
            projectPointMap.put("title", "项目经验");
            projectPointMap.put("difficulty", "intermediate");
            result.put("projectPoints", Arrays.asList(projectPointMap));
            return result;
        }
    }
    
    /**
     * 根据面试官风格ID增强风格描述
     */
    private String enhancePersonaWithStyle(String personaId) {
        // 优先从动态配置获取面试官风格描述
        try {
            Optional<List<Map<String, Object>>> personasOpt = dynamicConfigService.getInterviewPersonas();
            if (personasOpt.isPresent()) {
                for (Map<String, Object> persona : personasOpt.get()) {
                    if (personaId.equals(persona.get("id"))) {
                        // 只返回基本描述，保持原有风格
                        return (String) persona.get("description");
                    }
                }
            }
        } catch (Exception e) {
            log.error("从动态配置获取面试官风格失败", e);
        }
        
        // 回退到硬编码的风格描述，保持与原有项目一致
        switch (personaId) {
            case "friendly":
                return "语气友好、平易近人，创造轻松的面试氛围。";
            case "colloquial":
                return "轻松自然，像朋友聊天一样。适合练习表达与思维。";
            case "formal":
                return "逻辑清晰、专业正式，模拟真实企业面试场景。";
            case "manager":
                return "偏重项目成果与业务价值，关注你的思考与协作方式。";
            case "analytical":
                return "冷静分析型面试官，逻辑严谨、问题拆解式提问，适合技术深度练习。";
            case "encouraging":
                return "鼓励型面试官，语气温和积极，注重引导思考与成长体验。";
            case "pressure":
                return "压力面风格，高强度提问，快速节奏模拟顶级面试场景。";
            case "mentor":
                return "友善风格面试官，以友好、鼓励的方式进行面试。";
            case "neutral":
                return "中性面试官，保持客观、专业的面试风格。";
            case "challenging":
                return "挑战性面试官，提出深入的技术问题，挑战候选人的极限。";
            default:
                return "专业面试官，语气客观中立，关注事实和技术能力。";
        }
    }

    public Map<String, Object> generateNextQuestion(List<String> techItems, List<Map<String, Object>> projectPoints,
                                                    Map<String, Object> interviewState, Integer sessionTimeRemaining,
                                                    String persona, Integer jobTypeId) {
        try {
            // 获取已使用的技术项和项目点
            List<String> usedTechItems = (List<String>) interviewState.getOrDefault("usedTechItems", new ArrayList<>());
            List<String> usedProjectPoints = (List<String>) interviewState.getOrDefault("usedProjectPoints", new ArrayList<>());
            String currentDepthLevel = (String) interviewState.getOrDefault("currentDepthLevel", "usage");
            String jobType = (String) interviewState.getOrDefault("jobType", "");
            
            // 获取完整简历内容
            String fullResumeContent = (String) interviewState.getOrDefault("fullResumeContent", "");
            log.info("将使用完整简历内容生成问题，长度: {}字符", fullResumeContent != null ? fullResumeContent.length() : 0);
            
            // 获取最新的问题和回答日志，用于上下文生成
            List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(interviewState.get("sessionId").toString());
            String previousQuestion = "";
            String previousAnswer = "";
            if (!logs.isEmpty()) {
                InterviewLog lastLog = logs.get(logs.size() - 1);
                previousQuestion = lastLog.getQuestionText();
                previousAnswer = lastLog.getUserAnswerText();
            }
            
            // 直接调用AI生成下一个问题，基于上一题的题目和回答内容
            Map<String, Object> result = generateNewQuestionWithAI(techItems, projectPoints, usedTechItems, usedProjectPoints,
                                             currentDepthLevel, sessionTimeRemaining, persona, jobType, fullResumeContent, 
                                             previousQuestion, previousAnswer);
            
            // 更新已使用的技术项和项目点
            updateUsedItems(result, techItems, projectPoints, usedTechItems, usedProjectPoints);
            
            return result;
        } catch (Exception e) {
            log.error("生成下一个问题失败", e);
            // 返回默认值
            Map<String, Object> result = new HashMap<>();
            result.put("nextQuestion", "请简单总结一下你在项目中的主要职责和贡献。");
            result.put("expectedKeyPoints", Arrays.asList("主要职责", "技术贡献"));
            result.put("depthLevel", "summary");
            result.put("stopReason", "");
            return result;
        }
    }
    

    
    /**
     * 调用AI生成新问题
     */
    private Map<String, Object> generateNewQuestionWithAI(List<String> techItems, List<Map<String, Object>> projectPoints,
                                                        List<String> usedTechItems, List<String> usedProjectPoints,
                                                        String currentDepthLevel, Integer sessionTimeRemaining,
                                                        String persona, String jobType, String fullResumeContent,
                                                        String previousQuestion, String previousAnswer) throws Exception {
        // 构建prompt调用dynamicInterviewer
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format("你是%s风格的面试官。问题需自然、亲和、不死板。\n", persona));
        
        // 优化：直接使用完整简历内容作为生成问题的基础，这样对用户更方便
        if (StringUtils.hasText(fullResumeContent)) {
            promptBuilder.append("以下是候选人的完整简历内容：\n");
            promptBuilder.append(fullResumeContent).append("\n\n");
        }
        
        // 添加上一题的上下文信息
        if (StringUtils.hasText(previousQuestion) && StringUtils.hasText(previousAnswer)) {
            promptBuilder.append("上一轮面试问答：\n");
            promptBuilder.append(String.format("问题：%s\n", previousQuestion));
            promptBuilder.append(String.format("回答：%s\n\n", previousAnswer));
            promptBuilder.append("请根据上一题的问答内容，结合候选人的简历，生成下一个相关的面试问题。\n");
        } else {
            promptBuilder.append("请直接基于候选人的简历内容生成针对性的面试问题。\n");
        }
        
        promptBuilder.append("规则：\n");
        promptBuilder.append("- 从用法或项目实践切入，逐步深入原理/优化。\n");
        promptBuilder.append(String.format("- 已使用技术项：%s\n", usedTechItems));
        promptBuilder.append(String.format("- 已使用项目点：%s\n", usedProjectPoints));
        promptBuilder.append(String.format("- 当前深度级别：%s\n", currentDepthLevel));
        promptBuilder.append(String.format("- 剩余时间：%d秒\n", sessionTimeRemaining));
        promptBuilder.append("- 若时间<60s或连续两次回答偏离主题，则进入总结问题。\n");
        promptBuilder.append("输出：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("\"nextQuestion\": \"你的项目中Redis主要解决了什么问题？\",\n");
        promptBuilder.append("\"expectedKeyPoints\":[\"缓存策略\",\"并发控制\"],\n");
        promptBuilder.append("\"depthLevel\":\"实现\",\n");
        promptBuilder.append("\"stopReason\":\"\"\n");
        promptBuilder.append("}\n");
        
        String prompt = promptBuilder.toString();
        
        // 调用AI服务
        // 优化：传入完整简历内容后，Deepseek模型可以生成更准确、更符合候选人背景的问题
        log.info("使用完整简历内容调用Deepseek API生成问题prompt: {}", prompt);
        String aiResponse = aiServiceUtils.callDeepSeekApi(prompt);
        if(aiResponse.isEmpty()){
            log.error("will TODO");
            aiResponse = "{\n" +
                                "\"nextQuestion\": \"看到你的技能里有Java，能聊聊你在项目中通常用Java来处理哪些具体的任务吗？比如在Web开发或者数据处理方面？\",\n" +
                                "\"expectedKeyPoints\": [\"Web后端开发\", \"数据处理逻辑\", \"API设计与实现\"],\n" +
                                "\"depthLevel\": \"usage\",\n" +
                                "\"stopReason\": \"\"\n" +
                                "}";
        }
        log.info("使用完整简历内容调用Deepseek API生成问题完成");
        
        // 解析结果
        JsonNode jsonNode = objectMapper.readTree(aiResponse);
        Map<String, Object> result = new HashMap<>();
        
        if (jsonNode.has("nextQuestion")) {
            result.put("nextQuestion", jsonNode.get("nextQuestion").asText());
        }
        if (jsonNode.has("expectedKeyPoints")) {
            List<String> keyPoints = new ArrayList<>();
            for (JsonNode node : jsonNode.get("expectedKeyPoints")) {
                keyPoints.add(node.asText());
            }
            result.put("expectedKeyPoints", keyPoints);
        }
        if (jsonNode.has("depthLevel")) {
            result.put("depthLevel", jsonNode.get("depthLevel").asText());
        }
        if (jsonNode.has("stopReason")) {
            result.put("stopReason", jsonNode.get("stopReason").asText());
        }
        
        // 更新已使用的技术项和项目点
        updateUsedItems(result, techItems, projectPoints, usedTechItems, usedProjectPoints);
        
        return result;
    }
    
    /**
     * 计算两个问题文本的相似度
     * 使用基于关键词匹配的相似度算法
     * 
     * @param question1 第一个问题文本
     * @param question2 第二个问题文本
     * @return 相似度得分，范围0-1，值越大越相似
     */
    private double calculateQuestionSimilarity(String question1, String question2) {
        // 预处理文本：去除标点符号，转换为小写
        String processed1 = question1.toLowerCase()
                .replaceAll("[\\p{Punct}]", "")
                .trim();
        String processed2 = question2.toLowerCase()
                .replaceAll("[\\p{Punct}]", "")
                .trim();
        
        // 分割为单词
        Set<String> words1 = new HashSet<>(Arrays.asList(processed1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(processed2.split("\\s+")));
        
        // 移除停用词
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "的", "了", "和", "是", "在", "有", "你", "我", "他", "她", "它",
                "能", "能能", "可以", "会", "吗", "呢", "啊", "哦", "吧", "啦",
                "请", "具体", "聊聊", "一下", "如何", "怎样", "什么", "哪些", "哪",
                "个", "些", "这", "那", "这样", "那样", "为什么", "怎么", "多少",
                "是否", "有没有", "是不是", "或者", "然后", "所以", "因为", "但是",
                "如果", "比如", "例如", "比如", "等等", "诸如", "包括", "还有"
        ));
        
        words1.removeAll(stopWords);
        words2.removeAll(stopWords);
        
        // 如果两个问题都没有有效词汇，返回低相似度
        if (words1.isEmpty() && words2.isEmpty()) {
            return 0.0;
        }
        
        // 计算交集大小
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        // 计算并集大小
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        // 计算Jaccard相似度
        return (double) intersection.size() / union.size();
    }
    
    /**
     * 更新已使用的技术项和项目点
     */
    private void updateUsedItems(Map<String, Object> result, List<String> techItems, 
                               List<Map<String, Object>> projectPoints, 
                               List<String> usedTechItems, List<String> usedProjectPoints) {
        if (result.containsKey("nextQuestion")) {
            // 简单逻辑：将下一个问题中提到的技术项标记为已使用
            String nextQuestion = (String) result.get("nextQuestion");
            List<String> newlyUsedTechItems = new ArrayList<>(usedTechItems);
            for (String tech : techItems) {
                if (nextQuestion.contains(tech) && !newlyUsedTechItems.contains(tech)) {
                    newlyUsedTechItems.add(tech);
                    break; // 每次只标记一个新的技术项
                }
            }
            result.put("usedTechItems", newlyUsedTechItems);
            
            // 类似处理项目点
            List<String> newlyUsedProjectPoints = new ArrayList<>(usedProjectPoints);
            for (Map<String, Object> point : projectPoints) {
                String projectTitle = (String) point.get("title");
                if (nextQuestion.contains(projectTitle) && !newlyUsedProjectPoints.contains(projectTitle)) {
                    newlyUsedProjectPoints.add(projectTitle);
                    break;
                }
            }
            result.put("usedProjectPoints", newlyUsedProjectPoints);
        }
    }

    /**
     * 更新问题库中的使用次数和平均得分
     * @param questionText 问题文本
     * @param assessment 评估结果
     */
    private void updateQuestionUsageStatistics(String questionText, Map<String, Object> assessment) {
        try {
            // 计算问题的语义哈希
            String similarityHash = aiServiceUtils.getSemanticHash(questionText);
            
            // 查找对应的问题
            List<InterviewQuestion> questions = questionRepository.findAllBySimilarityHash(similarityHash);
            if (!questions.isEmpty()) {
                InterviewQuestion question = questions.get(0);
                
                // 从评估结果中获取得分
                double score = 0.0;
                if (assessment.containsKey("score_detail")) {
                    Map<String, Double> scoreDetail = (Map<String, Double>) assessment.get("score_detail");
                    // 计算平均分
                    score = (scoreDetail.getOrDefault("tech", 0.0) + 
                            scoreDetail.getOrDefault("logic", 0.0) + 
                            scoreDetail.getOrDefault("clarity", 0.0) + 
                            scoreDetail.getOrDefault("depth", 0.0)) / 4.0;
                }
                
                // 更新使用次数和平均得分
                int newUsageCount = question.getUsageCount() + 1;
                double currentAvgScore = question.getAvgScore();
                double newAvgScore = ((currentAvgScore * question.getUsageCount()) + score) / newUsageCount;
                
                question.setUsageCount(newUsageCount);
                question.setAvgScore(newAvgScore);
                question.setUpdatedAt(LocalDateTime.now());
                
                questionRepository.save(question);
                
                log.info("更新问题使用统计: 问题ID={}, 新使用次数={}, 新平均得分={}", 
                        question.getId(), newUsageCount, newAvgScore);
            }
        } catch (Exception e) {
            log.error("更新问题使用统计失败", e);
        }
    }
    
    private Map<String, Object> assessAnswer(String question, String answer, List<String> expectedKeyPoints, String persona) {
        try {
            // 调用AI评分服务
            String prompt = String.format("评估回答的质量：\n问题：%s\n回答：%s\n期望要点：%s\n面试官风格：%s\n请返回包含score_detail（tech, logic, clarity, depth四项评分，每项1-5分）、feedback和matchedPoints的JSON。", 
                    question, answer, expectedKeyPoints, persona);
            String aiResponse = aiServiceUtils.callDeepSeekApi(prompt);
            
            // 解析结果
            JsonNode jsonNode = objectMapper.readTree(aiResponse);
            Map<String, Object> result = new HashMap<>();
            
            // 提取评分详情
            Map<String, Double> scoreDetail = new HashMap<>();
            if (jsonNode.has("score_detail")) {
                JsonNode scoreNode = jsonNode.get("score_detail");
                if (scoreNode.has("tech")) scoreDetail.put("tech", scoreNode.get("tech").asDouble());
                if (scoreNode.has("logic")) scoreDetail.put("logic", scoreNode.get("logic").asDouble());
                if (scoreNode.has("clarity")) scoreDetail.put("clarity", scoreNode.get("clarity").asDouble());
                if (scoreNode.has("depth")) scoreDetail.put("depth", scoreNode.get("depth").asDouble());
            }
            
            // 计算总分
            double total = scoreDetail.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            scoreDetail.put("total", total);
            
            result.put("score_detail", scoreDetail);
            result.put("feedback", jsonNode.has("feedback") ? jsonNode.get("feedback").asText() : "");
            
            // 提取匹配的要点
            List<String> matchedPoints = new ArrayList<>();
            if (jsonNode.has("matchedPoints")) {
                for (JsonNode node : jsonNode.get("matchedPoints")) {
                    matchedPoints.add(node.asText());
                }
            }
            result.put("matchedPoints", matchedPoints);
            
            // 生成AI原始反馈JSON
            Map<String, Object> aiFeedback = new HashMap<>();
            aiFeedback.put("question", question);
            aiFeedback.put("answer", answer);
            aiFeedback.put("score_detail", scoreDetail);
            aiFeedback.put("feedback", result.get("feedback"));
            aiFeedback.put("matchedPoints", matchedPoints);
            aiFeedback.put("personaUsed", persona);
            
            try {
                result.put("aiFeedbackJson", objectMapper.writeValueAsString(aiFeedback));
            } catch (Exception e) {
                log.error("JSON序列化失败", e);
                result.put("aiFeedbackJson", "{}");
            }
            
            return result;
        } catch (Exception e) {
            log.error("评估答案失败", e);
            // 返回默认评分
            Map<String, Double> scoreDetail = new HashMap<>();
            scoreDetail.put("tech", 3.0);
            scoreDetail.put("logic", 3.0);
            scoreDetail.put("clarity", 3.0);
            scoreDetail.put("depth", 3.0);
            scoreDetail.put("total", 3.0);
            
            Map<String, Object> result = new HashMap<>();
            result.put("score_detail", scoreDetail);
            result.put("feedback", "无法评估，请重新提交。");
            result.put("matchedPoints", new ArrayList<>());
            
            // 生成默认AI原始反馈JSON
            Map<String, Object> aiFeedback = new HashMap<>();
            aiFeedback.put("question", question);
            aiFeedback.put("answer", answer);
            aiFeedback.put("score_detail", scoreDetail);
            aiFeedback.put("feedback", result.get("feedback"));
            aiFeedback.put("matchedPoints", new ArrayList<>());
            aiFeedback.put("personaUsed", persona);
            
            try {
                result.put("aiFeedbackJson", objectMapper.writeValueAsString(aiFeedback));
            } catch (Exception ex) {
                log.error("JSON序列化失败", ex);
                result.put("aiFeedbackJson", "{}");
            }
            
            return result;
        }
    }

    private Map<String, Double> calculateAggregatedScoresForReport(List<InterviewLog> logs) {
        Map<String, Double> aggregated = new HashMap<>();
        
        if (logs == null || logs.isEmpty()) {
            aggregated.put("tech", 0.0);
            aggregated.put("logic", 0.0);
            aggregated.put("clarity", 0.0);
            aggregated.put("depth", 0.0);
            aggregated.put("total", 0.0);
            return aggregated;
        }
        
        double techSum = 0;
        double logicSum = 0;
        double claritySum = 0;
        double depthSum = 0;

        for (InterviewLog log : logs) {
            techSum += log.getTechScore() != null ? log.getTechScore() : 0;
            logicSum += log.getLogicScore() != null ? log.getLogicScore() : 0;
            claritySum += log.getClarityScore() != null ? log.getClarityScore() : 0;
            depthSum += log.getDepthScore() != null ? log.getDepthScore() : 0;
        }
        
        int count = logs.size();
        aggregated.put("tech", techSum / count);
        aggregated.put("logic", logicSum / count);
        aggregated.put("clarity", claritySum / count);
        aggregated.put("depth", depthSum / count);
        aggregated.put("total", (techSum + logicSum + claritySum + depthSum) / (count * 4));

        return aggregated;
    }

    @Override
    public List<InterviewHistoryVO> getInterviewHistory(Long userId) {
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream().map(session -> {
            String jobType = jobTypeRepository.findById(session.getJobTypeId()).map(JobType::getJobName).orElse("面试");
            InterviewHistoryVO vo = new InterviewHistoryVO();
            vo.setSessionId(session.getId());
            vo.setUniqueSessionId(session.getSessionId()); // 添加唯一会话ID
            vo.setTitle(jobType);
            vo.setFinalScore(session.getTotalScore());
            vo.setStatus(session.getStatus());
            
            // 转换时间格式
            if (session.getStartTime() != null) {
                vo.setStartTime(session.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            if (session.getEndTime() != null) {
                vo.setEndTime(session.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            
            // 获取问题数量
            List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(session.getSessionId());
            vo.setQuestionCount(logs.size());
            
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SalaryRangeVO calculateSalary(String sessionId) {
        try {
            log.info("AI动态生成薪资范围，会话ID: {}", sessionId);
            // 根据sessionId获取面试会话
            InterviewSession session = sessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("Interview session not found"));
            String jobType = jobTypeRepository.findById(session.getJobTypeId()).map(JobType::getJobName).orElse("面试");

            // 获取评分信息
            Map<String, Double> aggregatedScores = new HashMap<>();
            aggregatedScores.put("tech", session.getTechScore());
            aggregatedScores.put("logic", session.getLogicScore());
            aggregatedScores.put("clarity", session.getClarityScore());
            aggregatedScores.put("depth", session.getDepthScore());
            aggregatedScores.put("total", session.getTotalScore());
            
            // 准备用户性能数据
            Map<String, Object> userPerformanceData = new HashMap<>();
            userPerformanceData.put("experienceYears", estimateExperienceYears(aggregatedScores));
            userPerformanceData.put("industryTrend", getIndustryTrend(jobType));
            
            // 使用AI动态生成薪资评估
            SalaryRangeVO vo = aiGenerateService.generateSalaryRange(
                sessionId, session.getCity(), jobType, aggregatedScores, userPerformanceData);
            
            // 保存AI薪资建议的跟踪日志
            saveAiTraceLog(sessionId, "give_advice", 
                    "AI薪资范围生成：城市=" + session.getCity() + ",职位=" + jobType + ",评分=" + aggregatedScores,
                    "薪资范围=" + vo.getMinSalary() + "-" + vo.getMaxSalary() + "K/月，建议薪资=" + vo.getSuggestedSalary() + "K/月");
            
            return vo;
        } catch (Exception e) {
            log.error("计算薪资范围失败", e);
            throw new RuntimeException("薪资范围计算失败: " + e.getMessage());
        }
    }
    
    /**
     * 估算工作经验年限
     */
    private String estimateExperienceYears(Map<String, Double> aggregatedScores) {
        double techScore = aggregatedScores.getOrDefault("tech", 5.0);
        double depthScore = aggregatedScores.getOrDefault("depth", 5.0);
        
        if (techScore >= 8.5 && depthScore >= 8.0) {
            return "3-5年";
        } else if (techScore >= 7.0) {
            return "1-3年";
        } else {
            return "应届或1年以下";
        }
    }
    
    /**
     * 获取行业趋势
     */
    private String getIndustryTrend(String jobType) {
        Map<String, String> trendMap = new HashMap<>();
        trendMap.put("Java开发", "需求稳定，分布式架构方向薪资较高");
        trendMap.put("前端开发", "稳定上升，AI相关技术需求增加");
        trendMap.put("前端工程师", "稳定上升，AI相关技术需求增加");
        trendMap.put("大数据工程师", "快速增长，数据治理方向需求旺盛");
        trendMap.put("产品经理", "竞争激烈，AI产品能力成为新增长点");
        trendMap.put("算法工程师", "爆发式增长，大模型相关岗位稀缺");
        trendMap.put("UI设计师", "平稳发展，交互体验设计更受重视");
        trendMap.put("后端开发", "需求稳定，全栈能力更受青睐");
        trendMap.put("数据分析师", "持续增长，业务理解能力重要性提升");
        
        return trendMap.getOrDefault(jobType, "行业趋势良好，持续发展中");
    }
    
    @Override
    public InterviewSessionVO getInterviewDetail(String sessionId) {
        InterviewSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));
        String jobType = jobTypeRepository.findById(session.getJobTypeId()).map(JobType::getJobName).orElse("面试");
        
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(session.getId());
        vo.setTitle(jobType + " 面试");
        vo.setDescription("会话ID: " + session.getSessionId());
        vo.setStatus(session.getStatus());
        vo.setPersona(session.getPersona());
        vo.setSessionSeconds(session.getSessionSeconds());
        vo.setTotalScore(session.getTotalScore());
        vo.setInterviewDuration(session.getSessionSeconds() - session.getSessionTimeRemaining());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setFinishedAt(session.getEndTime());
        vo.setStopReason(session.getStopReason());
        
        // 获取面试日志
        List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);
        vo.setLogs(logs);
        vo.setTotalQuestions(logs.size());
        vo.setAnsweredQuestions(logs.size()); // 假设所有问题都已回答
        
        if (session.getStartTime() != null) {
            vo.setStartTime(session.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (session.getEndTime() != null) {
            vo.setEndTime(session.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        
        return vo;
    }

    // ----------- 功能模块实现 -----------

    /**
     * projectAnalyzer模块：提取项目点
     */
    private List<Map<String, Object>> extractProjectPoints(String resumeText) {
        try {
            // 调用DeepSeek API提取项目点
            String prompt = "从以下简历文本中提取项目职责、技术栈、关键难点和成果量化信息，并为每个项目生成1-3个候选追问点，标注难度（basic/intermediate/advanced）。\n\n" + resumeText;
            
            String response = aiServiceUtils.callDeepSeekApi(prompt);
            JsonNode rootNode = objectMapper.readTree(response);
            
            List<Map<String, Object>> projectPoints = new ArrayList<>();
            // 简化实现，实际应根据API返回格式解析
            return projectPoints;
        } catch (Exception e) {
            log.error("Extract project points failed:", e);
            // 返回mock数据
            return Collections.emptyList();
        }
    }

    /**
     * dynamicInterviewer模块：生成下一个问题
     */
    /**
     * 将完整的简历数据转换为文本格式，用于提取技术项和项目点
     * @param fullResumeData 完整的简历数据
     * @return 转换后的文本内容
     */
    private String convertFullDataToText(Map<String, Object> fullResumeData) {
        StringBuilder content = new StringBuilder();
        // 添加日志记录，检查传入的数据结构
        log.info("convertFullDataToText - fullResumeData包含的键: {}", fullResumeData.keySet());
        if (fullResumeData.containsKey("projectList")) {
            Object projectList = fullResumeData.get("projectList");
            if (projectList instanceof List) {
                log.info("convertFullDataToText - 项目数量: {}", ((List<?>) projectList).size());
            }
        }
        if (fullResumeData.containsKey("skillList")) {
            Object skillList = fullResumeData.get("skillList");
            if (skillList instanceof List) {
                log.info("convertFullDataToText - 技能数量: {}", ((List<?>) skillList).size());
            }
        }
        
        // 添加个人基本信息
        Map<String, Object> userInfo = (Map<String, Object>) fullResumeData.get("userInfo");
        if (userInfo != null) {
            content.append("个人信息：\n");
            if (userInfo.containsKey("name")) content.append("姓名：").append(userInfo.get("name")).append("\n");
            if (userInfo.containsKey("phone")) content.append("电话：").append(userInfo.get("phone")).append("\n");
            if (userInfo.containsKey("email")) content.append("邮箱：").append(userInfo.get("email")).append("\n");
            content.append("\n");
        }
        
        // 添加简历基本信息
        if (fullResumeData.containsKey("jobTitle")) content.append("求职意向：").append(fullResumeData.get("jobTitle")).append("\n\n");
        if (fullResumeData.containsKey("selfEvaluation")) {
            content.append("自我评价：\n")
                  .append(fullResumeData.get("selfEvaluation"))
                  .append("\n\n");
        }
        
        // 添加教育经历
        if (fullResumeData.containsKey("educationList")) {
            Object educationObj = fullResumeData.get("educationList");
            if (educationObj instanceof List) {
                List<?> educationList = (List<?>) educationObj;
                if (!educationList.isEmpty()) {
                    content.append("教育经历：\n");
                    for (Object eduObj : educationList) {
                        if (eduObj instanceof com.aicv.airesume.entity.ResumeEducation) {
                            com.aicv.airesume.entity.ResumeEducation education = (com.aicv.airesume.entity.ResumeEducation) eduObj;
                            content.append("- 学校：").append(education.getSchool() != null ? education.getSchool() : "");
                            content.append(", 专业：").append(education.getMajor() != null ? education.getMajor() : "");
                            content.append(", 学历：").append(education.getDegree() != null ? education.getDegree() : "");
                            content.append(", 时间段：")
                                  .append(education.getStartDate() != null ? education.getStartDate() : "")
                                  .append(" - ")
                                  .append(education.getEndDate() != null ? education.getEndDate() : "")
                                  .append("\n");
                            if (education.getDescription() != null) {
                                content.append("  描述：").append(education.getDescription()).append("\n");
                            }
                        }
                    }
                    content.append("\n");
                }
            }
        }
        
        // 添加工作经历
        if (fullResumeData.containsKey("workExperienceList")) {
            Object workExperienceObj = fullResumeData.get("workExperienceList");
            if (workExperienceObj instanceof List) {
                List<?> workExperienceList = (List<?>) workExperienceObj;
                if (!workExperienceList.isEmpty()) {
                    content.append("工作经历：\n");
                    for (Object workObj : workExperienceList) {
                        if (workObj instanceof com.aicv.airesume.entity.ResumeWorkExperience) {
                            com.aicv.airesume.entity.ResumeWorkExperience work = (com.aicv.airesume.entity.ResumeWorkExperience) workObj;
                            content.append("- 公司：").append(work.getCompanyName() != null ? work.getCompanyName() : "");
                            content.append(", 职位：").append(work.getPositionName() != null ? work.getPositionName() : "");
                            content.append(", 时间段：")
                                  .append(work.getStartDate() != null ? work.getStartDate() : "")
                                  .append(" - ")
                                  .append(work.getEndDate() != null ? work.getEndDate() : "")
                                  .append("\n");
                            if (work.getDescription() != null) {
                                content.append("  工作描述：").append(work.getDescription()).append("\n");
                            }
                        }
                    }
                    content.append("\n");
                }
            }
        }
        
        // 添加项目经历
        if (fullResumeData.containsKey("projectList")) {
            Object projectObj = fullResumeData.get("projectList");
            if (projectObj instanceof List) {
                List<?> projectList = (List<?>) projectObj;
                if (!projectList.isEmpty()) {
                    content.append("项目经历：\n");
                    for (Object projObj : projectList) {
                        if (projObj instanceof com.aicv.airesume.entity.ResumeProject) {
                            com.aicv.airesume.entity.ResumeProject project = (com.aicv.airesume.entity.ResumeProject) projObj;
                            content.append("- 项目名称：").append(project.getProjectName() != null ? project.getProjectName() : "");
                            content.append(", 角色：").append(project.getRole() != null ? project.getRole() : "");
                            content.append(", 时间段：")
                                  .append(project.getStartDate() != null ? project.getStartDate() : "")
                                  .append(" - ")
                                  .append(project.getEndDate() != null ? project.getEndDate() : "")
                                  .append("\n");
                            if (project.getTechStack() != null) {
                                content.append("  技术栈：").append(project.getTechStack()).append("\n");
                            }
                            if (project.getDescription() != null) {
                                content.append("  项目描述：").append(project.getDescription()).append("\n");
                            }
                        }
                    }
                    content.append("\n");
                }
            }
        }
        
        // 添加技能
        if (fullResumeData.containsKey("skillList")) {
            Object skillObj = fullResumeData.get("skillList");
            if (skillObj instanceof List) {
                List<?> skillList = (List<?>) skillObj;
                if (!skillList.isEmpty()) {
                    content.append("技能：\n");
                    for (Object skillObjItem : skillList) {
                        if (skillObjItem instanceof com.aicv.airesume.entity.ResumeSkill) {
                            com.aicv.airesume.entity.ResumeSkill skill = (com.aicv.airesume.entity.ResumeSkill) skillObjItem;
                            content.append("- ").append(skill.getName() != null ? skill.getName() : "");
                            if (skill.getLevel() != null) {
                                content.append(" (熟练度：").append(skill.getLevel()).append(")");
                            }
                            content.append("\n");
                        }
                    }
                }
            }
        }
        
        return content.toString();
    }
    
    private Map<String, Object> generateNextQuestionForDynamicInterviewer(List<Map<String, Object>> projectPoints, String jobType, Map<String, Object> interviewState) {
        try {
            String lastAnswer = (String) interviewState.get("lastAnswer");
            String prompt = "You are a technical interviewer. Based on user's previous answer: " + lastAnswer + ", generate the next round of questions. Follow depth sequence: Usage -> Implementation -> Principle -> Optimization.";
            String response = aiServiceUtils.callDeepSeekApi(prompt);
            Map<String, Object> result = new HashMap<>();
            result.put("nextQuestion", response);
            result.put("expectedKeyPoints", Collections.emptyList());
            result.put("depthLevel", "intermediate");
            result.put("stopReason", "");
            return result;
        } catch (Exception e) {
            log.error("Generate next question failed:", e);
            // Return mock data
            Map<String, Object> result = new HashMap<>();
            result.put("nextQuestion", "Could you elaborate on the technical implementation details you just mentioned?");
            result.put("expectedKeyPoints", Collections.emptyList());
            result.put("depthLevel", "intermediate");
            result.put("stopReason", "");
            return result;
        }
    }

    /**
     * aiAssessmentPerQuestion模块：评分
     */
    private Map<String, Object> assessAnswerForDynamicInterviewer(String question, String userAnswer, List<String> expectedKeyPoints, String persona) {
        Map<String, Object> assessment = new HashMap<>();
        Map<String, Double> scoreDetail = new HashMap<>();
        scoreDetail.put("tech", 8.5);
        scoreDetail.put("logic", 7.8);
        scoreDetail.put("clarity", 8.0);
        scoreDetail.put("depth", 7.5);
        assessment.put("score_detail", scoreDetail);
        assessment.put("feedback", "The answer is complete with good technical understanding. Suggestion: more details could be added.");
        assessment.put("matchedPoints", Collections.emptyList());
        
        // 生成AI原始反馈JSON
        Map<String, Object> aiFeedback = new HashMap<>();
        aiFeedback.put("question", question);
        aiFeedback.put("answer", userAnswer);
        aiFeedback.put("score_detail", scoreDetail);
        aiFeedback.put("feedback", assessment.get("feedback"));
        aiFeedback.put("matchedPoints", assessment.get("matchedPoints"));
        aiFeedback.put("personaUsed", persona);
        
        try {
            assessment.put("aiFeedbackJson", objectMapper.writeValueAsString(aiFeedback));
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            assessment.put("aiFeedbackJson", "{}");
        }
        
        return assessment;
    }

    /**
     * sessionScorer模块：计算聚合评分
     */
    private Map<String, Double> calculateAggregatedScores(List<InterviewLog> logs) {
        Map<String, Double> scores = new HashMap<>();
        
        // 如果没有面试日志，返回默认分数
        if (logs == null || logs.isEmpty()) {
            scores.put("tech", 0.0);
            scores.put("logic", 0.0);
            scores.put("clarity", 0.0);
            scores.put("depth", 0.0);
            scores.put("total", 0.0);
            return scores;
        }
        
        // 计算各维度的平均分
        double totalTech = 0.0, totalLogic = 0.0, totalClarity = 0.0, totalDepth = 0.0;
        int scoredCount = 0;
        
        for (InterviewLog log : logs) {
            if (log.getTechScore() != null && log.getLogicScore() != null && 
                log.getClarityScore() != null && log.getDepthScore() != null) {
                
                totalTech += log.getTechScore();
                totalLogic += log.getLogicScore();
                totalClarity += log.getClarityScore();
                totalDepth += log.getDepthScore();
                scoredCount++;
            }
        }
        
        // 计算平均分，保留一位小数
        if (scoredCount > 0) {
            scores.put("tech", Math.round((totalTech / scoredCount) * 10) / 10.0);
            scores.put("logic", Math.round((totalLogic / scoredCount) * 10) / 10.0);
            scores.put("clarity", Math.round((totalClarity / scoredCount) * 10) / 10.0);
            scores.put("depth", Math.round((totalDepth / scoredCount) * 10) / 10.0);
            
            // 计算总分（各维度平均分）
            double totalScore = (scores.get("tech") + scores.get("logic") + 
                               scores.get("clarity") + scores.get("depth")) / 4.0;
            scores.put("total", Math.round(totalScore * 10) / 10.0);
        } else {
            // 如果没有评分记录，返回默认值
            scores.put("tech", 0.0);
            scores.put("logic", 0.0);
            scores.put("clarity", 0.0);
            scores.put("depth", 0.0);
            scores.put("total", 0.0);
        }
        
        return scores;
    }

    /**
     * salaryMatcherAdvanced模块：基于AI面试结果的薪资匹配
     * 更准确地根据面试评分、技术深度等多维度计算薪资范围
     */

    
    /**
     * 提供mock薪资范围数据
     */


    /**
     * reportGeneratorAdvanced模块：生成报告
     */
    private String generateReport(String sessionId, List<InterviewLog> logs, Map<String, Double> aggregatedScores, Map<String, Object> salaryInfo) {
        try {
            // 获取面试会话
            InterviewSession session = sessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("Interview session not found"));
            
            // 分析技能短板
            List<String> weakSkills = aiGenerateService.analyzeWeakSkills(sessionId, 
                logs.stream().map(this::convertToMap).collect(Collectors.toList()));
            
            // 获取职位领域 - 简化处理，使用jobType作为领域
            String domain = jobTypeRepository.findById(session.getJobTypeId()).map(JobType::getJobName).orElse("未知");
            
            // 准备用户性能数据
            Map<String, Object> userPerformanceData = new HashMap<>();
            userPerformanceData.put("industryTrends", getIndustryTrend(domain));
            userPerformanceData.put("performanceSummary", generatePerformanceSummary(aggregatedScores));
            
            // 使用AI动态生成成长建议
            Map<String, Object> growthAdvice = aiGenerateService.generateGrowthAdvice(
                sessionId, domain, domain, aggregatedScores, weakSkills, userPerformanceData);
            
            // 生成完整报告内容
            String reportContent = generateReportFromAdvice(
                session.getJobTypeId(), aggregatedScores, weakSkills, growthAdvice, salaryInfo);
            
            // 报告内容不需要保存到会话实体中
            // 可以考虑后续添加report_content字段来保存
            
            // Generate report URL (simplified implementation)
            String reportUrl = "/reports/" + sessionId + ".pdf";
            
            return reportUrl;
        } catch (Exception e) {
            log.error("Generate report failed:", e);
            return "/reports/placeholder.pdf";
        }
    }
    
    /**
     * 从InterviewLog转换为Map，用于AI分析
     */
    private Map<String, Object> convertToMap(InterviewLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("techScore", log.getTechScore());
        map.put("logicScore", log.getLogicScore());
        map.put("clarityScore", log.getClarityScore());
        map.put("depthScore", log.getDepthScore());
        map.put("feedback", log.getFeedback());
        map.put("question", log.getQuestionText());
        map.put("answer", log.getUserAnswerText());
        return map;
    }
    
    /**
     * 生成性能总结
     */
    private String generatePerformanceSummary(Map<String, Double> aggregatedScores) {
        StringBuilder summary = new StringBuilder();
        summary.append("技术能力评分：").append(aggregatedScores.getOrDefault("tech", 0.0)).append("/10\n");
        summary.append("逻辑思维评分：").append(aggregatedScores.getOrDefault("logic", 0.0)).append("/10\n");
        summary.append("沟通表达评分：").append(aggregatedScores.getOrDefault("clarity", 0.0)).append("/10\n");
        summary.append("创新潜力评分：").append(aggregatedScores.getOrDefault("depth", 0.0)).append("/10\n");
        summary.append("总体评分：").append(aggregatedScores.getOrDefault("total", 0.0)).append("/10");
        return summary.toString();
    }
    
    /**
     * 从成长建议生成报告内容
     */
    private String generateReportFromAdvice(Integer jobTypeId, Map<String, Double> scores, 
                                          List<String> weakSkills, Map<String, Object> growthAdvice, 
                                          Map<String, Object> salaryInfo) {
        StringBuilder report = new StringBuilder();
        
        // 报告标题
        String jobType = jobTypeRepository.findById(jobTypeId).map(JobType::getJobName).orElse("面试");
        report.append("# 职业成长评估报告\n\n");
        
        // 基本信息
        report.append("## 基本信息\n");
        report.append("- **职位类型**：").append(jobType).append("\n");
        report.append("- **评估时间**：").append(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");
        
        // 评分概览
        report.append("## 评分概览\n");
        report.append("- **技术能力**：").append(scores.getOrDefault("tech", 0.0)).append("/10\n");
        report.append("- **逻辑思维**：").append(scores.getOrDefault("logic", 0.0)).append("/10\n");
        report.append("- **沟通表达**：").append(scores.getOrDefault("clarity", 0.0)).append("/10\n");
        report.append("- **创新潜力**：").append(scores.getOrDefault("depth", 0.0)).append("/10\n");
        report.append("- **总体评分**：").append(scores.getOrDefault("total", 0.0)).append("/10\n\n");
        
        // 薪资评估
        report.append("## 薪资评估\n");
        report.append("- **薪资范围**：").append(salaryInfo.getOrDefault("ai_salary_range", "待评估")).append("\n");
        report.append("- **估算经验**：").append(salaryInfo.getOrDefault("ai_estimated_years", "待评估")).append("年\n");
        report.append("- **置信度**：").append(salaryInfo.getOrDefault("confidence", 0.0)).append("\n\n");
        
        // 技能短板
        report.append("## 技能短板\n");
        if (weakSkills.isEmpty()) {
            report.append("没有发现明显的技能短板，继续保持！\n\n");
        } else {
            for (String skill : weakSkills) {
                report.append("- ").append(skill).append("\n");
            }
            report.append("\n");
        }
        
        // 推荐技能
        report.append("## 推荐学习技能\n");
        List<String> recommendedSkills = (List<String>) growthAdvice.getOrDefault("recommended_skills", new ArrayList<>());
        for (String skill : recommendedSkills) {
            report.append("- ").append(skill).append("\n");
        }
        report.append("\n");
        
        // 发展路径
        report.append("## 长期职业发展路径\n");
        List<String> longTermPath = (List<String>) growthAdvice.getOrDefault("long_term_path", new ArrayList<>());
        for (int i = 0; i < longTermPath.size(); i++) {
            report.append("- **阶段").append(i + 1).append("**：").append(longTermPath.get(i)).append("\n");
        }
        report.append("\n");
        
        // 短期建议
        report.append("## 短期建议（1-3个月）\n");
        report.append(growthAdvice.getOrDefault("short_term_advice", "暂无建议")).append("\n\n");
        
        // 中期建议
        report.append("## 中期建议（3-6个月）\n");
        report.append(growthAdvice.getOrDefault("mid_term_advice", "暂无建议")).append("\n\n");
        
        // 长期建议
        report.append("## 长期建议（6-12个月）\n");
        report.append(growthAdvice.getOrDefault("long_term_advice", "暂无建议")).append("\n\n");
        
        return report.toString();
    }

}