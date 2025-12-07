package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.DynamicConfig;
import com.aicv.airesume.entity.InterviewSession;
import com.aicv.airesume.entity.JobType;
import com.aicv.airesume.entity.InterviewLog;
import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.entity.InterviewReport;
import com.aicv.airesume.model.vo.InterviewHistoryVO;
import com.aicv.airesume.model.vo.InterviewReportVO;
import com.aicv.airesume.model.vo.InterviewResponseVO;
import com.aicv.airesume.model.vo.InterviewSessionVO;
import com.aicv.airesume.model.vo.ReportChunksVO;
import com.aicv.airesume.model.vo.SalaryRangeVO;
import com.aicv.airesume.model.vo.InterviewHistoryItemVO;
import com.aicv.airesume.repository.InterviewSessionRepository;
import com.aicv.airesume.repository.InterviewLogRepository;
import com.aicv.airesume.repository.ResumeRepository;
import com.aicv.airesume.repository.InterviewQuestionRepository;
import com.aicv.airesume.repository.JobTypeRepository;
import com.aicv.airesume.repository.InterviewReportRepository;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.service.AIGenerateService;
import com.aicv.airesume.service.ResumeService;
import com.aicv.airesume.utils.AiServiceUtils;
import com.aicv.airesume.service.ReportGenerationService;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    
    @Autowired
    private ReportGenerationService reportGenerationService;
    
    @Autowired
    private InterviewReportRepository interviewReportRepository;
    
    // 线程池，用于处理异步任务
    private final ExecutorService executorService = java.util.concurrent.Executors.newFixedThreadPool(2);
    
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
    public SseEmitter getFirstQuestionStream(String sessionId) {
        SseEmitter emitter = new SseEmitter(60000L); // 设置60秒超时
        
        new Thread(() -> {
            try {
                // 查询会话是否存在
                InterviewSession session = sessionRepository.findBySessionId(sessionId)
                        .orElseThrow(() -> new RuntimeException("会话不存在"));
                
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
                    interviewState.put("exhaustedTechItems", new ArrayList<>());
                    interviewState.put("exhaustedProjectPoints", new ArrayList<>());
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
                // 先检查Resume表中是否有提取结果
                Optional<Resume> resumeOpt = resumeRepository.findById(session.getResumeId());
                if (resumeOpt.isPresent()) {
                    Resume resume = resumeOpt.get();
                    if (StringUtils.hasText(resume.getTechItems()) && StringUtils.hasText(resume.getProjectPoints())) {
                        // 从Resume表中获取提取结果
                        techItems = objectMapper.readValue(resume.getTechItems(), new TypeReference<List<String>>() {});
                        projectPoints = objectMapper.readValue(resume.getProjectPoints(), new TypeReference<List<Map<String, Object>>>() {});
                        log.info("getFirstQuestionStream - 从Resume表获取提取结果，简历ID: {}", session.getResumeId());
                    } else {
                        // 如果Resume表中也没有，再从简历内容中提取
                        Map<String, Object> extractedData = extractTechItemsAndProjectPoints(session.getResumeId(), resumeContent);
                        if (extractedData != null) {
                            if (extractedData.containsKey("techItems")) {
                                techItems = (List<String>) extractedData.get("techItems");
                            }
                            if (extractedData.containsKey("projectPoints")) {
                                projectPoints = (List<Map<String, Object>>) extractedData.get("projectPoints");
                            }
                        }
                    }
                    // 保存技术项和项目点到会话
                    session.setTechItems(objectMapper.writeValueAsString(techItems));
                    session.setProjectPoints(objectMapper.writeValueAsString(projectPoints));
                    sessionRepository.save(session);
                }
            }
                
                // 生成第一个问题（流式）
                List<String> usedTechItems = (List<String>) interviewState.get("usedTechItems");
                List<String> usedProjectPoints = (List<String>) interviewState.get("usedProjectPoints");
                String currentDepthLevel = (String) interviewState.get("currentDepthLevel");
                String jobType = String.valueOf(session.getJobTypeId());
                
                // 创建第一个问题的日志记录
                InterviewLog firstQuestionLog = new InterviewLog();
                firstQuestionLog.setQuestionId(UUID.randomUUID().toString());
                firstQuestionLog.setSessionId(sessionId);
                firstQuestionLog.setRoundNumber(1);
                firstQuestionLog.setPersona(session.getPersona());
                
                // 保存初始日志记录
                logRepository.save(firstQuestionLog);
                
                // 确定初始问题类型：如果有项目点，优先选择项目问题
                String initialQuestionType = "project";
                if (projectPoints == null || projectPoints.isEmpty()) {
                    initialQuestionType = "tech";
                    log.info("没有项目点，初始问题类型设置为tech");
                } else {
                    log.info("有项目点，初始问题类型设置为project");
                }
                
                // 调用统一的流式生成问题方法，并传递回调函数
                generateQuestionStream(techItems, projectPoints, usedTechItems, usedProjectPoints,
                                     currentDepthLevel, session.getSessionTimeRemaining(),
                                     session.getPersona(), jobType, resumeContent, "", "", emitter,
                                     () -> {
                                         // 流结束回调：发送结束信号并完成emitter
                                         try {
                                             emitter.send(SseEmitter.event().data("end").name("end").id("2"));
                                             emitter.complete();
                                         } catch (IOException e) {
                                             log.error("发送结束信号失败：{}", e.getMessage(), e);
                                             emitter.completeWithError(e);
                                         }
                                     }, sessionId, initialQuestionType, session); // 首次问题根据情况选择项目或技术问题
                
                // 获取生成的问题（需要从AI响应中解析，这里简化处理）
                String firstQuestion = "";
                
                // 保存AI生成问题的跟踪日志
                saveAiTraceLog(session.getSessionId(), "generate_question", 
                        "生成第一个问题的prompt内容", firstQuestion);
                
                // 更新会话问题计数
                session.setQuestionCount(1);
                session.setInterviewState(objectMapper.writeValueAsString(interviewState));
                sessionRepository.save(session);
                
                log.info("直接生成第一个面试问题并保存，会话ID: {}", session.getSessionId());
                
                // 注意：问题已经通过generateNewQuestionWithAIStream方法流式发送给客户端了
            } catch (Exception e) {
                log.error("获取第一个问题失败: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }

    @Override
    public SseEmitter submitAnswerStream(String sessionId, String userAnswerText, Integer answerDuration, String toneStyle) {
        SseEmitter emitter = new SseEmitter(60000L); // 设置60秒超时
        
        new Thread(() -> {
            try {
                // 1. 获取会话信息
                InterviewSession session = sessionRepository.findBySessionId(sessionId)
                        .orElseThrow(() -> new RuntimeException("会话不存在"));
                
                // 获取最新的问题日志，需要找到有问题文本的最新记录
                List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);
                if (logs.isEmpty()) {
                    throw new RuntimeException("问题不存在");
                }
                
                // 找到包含问题文本的最新记录
                InterviewLog currentLog = null;
                for (int i = logs.size() - 1; i >= 0; i--) {
                    InterviewLog log = logs.get(i);
                    if (StringUtils.hasText(log.getQuestionText())) {
                        currentLog = log;
                        break;
                    }
                }
                
                if (currentLog == null) {
                    throw new RuntimeException("找不到有问题文本的记录");
                }

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
                        expectedKeyPoints = objectMapper.readValue(currentLog.getExpectedKeyPoints(), new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        log.error("解析期望关键点失败: {}", e.getMessage());
                    }
                }
                
                // 调用AI评分
                Map<String, Double> scores = aiServiceUtils.assessInterviewAnswer(
                        currentLog.getQuestionText(),
                        userAnswerText,
                        expectedKeyPoints,
                        currentLog.getDepthLevel(),
                        currentLog.getRelatedTechItems(),
                        session.getPersona()
                );
                
                // 5. 更新当前日志的评分信息
                currentLog.setTechScore(scores.get("tech"));
                currentLog.setLogicScore(scores.get("logic"));
                currentLog.setClarityScore(scores.get("clarity"));
                currentLog.setDepthScore(scores.get("depth"));
                logRepository.save(currentLog);
                
                // 6. 生成下一个问题
                // 获取技术项和项目点
                List<String> techItems = new ArrayList<>();
                List<Map<String, Object>> projectPoints = new ArrayList<>();
                Map<String, Object> interviewState = new HashMap<>();
                
                // 解析面试状态
                if (StringUtils.hasText(session.getInterviewState())) {
                    interviewState = objectMapper.readValue(session.getInterviewState(), new TypeReference<Map<String, Object>>() {});
                }
                
                // 获取简历内容
                String resumeContent = "";
                if (interviewState.containsKey("fullResumeContent")) {
                    resumeContent = (String) interviewState.get("fullResumeContent");
                } else {
                    // 如果面试状态中没有完整简历内容，尝试从简历表获取
                    Long resumeId = session.getResumeId();
                    Map<String, Object> fullResumeData = resumeService.getResumeFullData(resumeId);
                    resumeContent = convertFullDataToText(fullResumeData);
                    interviewState.put("fullResumeContent", resumeContent);
                }
                
                if (StringUtils.hasText(session.getTechItems())) {
                    techItems = objectMapper.readValue(session.getTechItems(), new TypeReference<List<String>>() {});
                }
                
                if (StringUtils.hasText(session.getProjectPoints())) {
                    projectPoints = objectMapper.readValue(session.getProjectPoints(), new TypeReference<List<Map<String, Object>>>() {});
                }
                
                // 获取当前深度级别和使用记录
                String currentDepthLevel = (String) interviewState.getOrDefault("currentDepthLevel", "usage");
                List<String> usedTechItems = (List<String>) interviewState.getOrDefault("usedTechItems", new ArrayList<>());
                List<String> usedProjectPoints = (List<String>) interviewState.getOrDefault("usedProjectPoints", new ArrayList<>());
                List<String> exhaustedTechItems = (List<String>) interviewState.getOrDefault("exhaustedTechItems", new ArrayList<>());
                List<String> exhaustedProjectPoints = (List<String>) interviewState.getOrDefault("exhaustedProjectPoints", new ArrayList<>());
                Integer consecutiveFailures = (Integer) interviewState.getOrDefault("consecutiveFailures", 0);
                String currentQuestionType = (String) interviewState.getOrDefault("currentQuestionType", "tech");
                
                // 计算四个维度的平均分
                double avgScore = (scores.get("tech") + scores.get("logic") + scores.get("clarity") + scores.get("depth")) / 4;
                
                // 如果平均分低于60分，降低深度级别或切换话题
                if (avgScore < 60) {
                    consecutiveFailures++;
                    log.info("用户回答平均分{:.2f}低于60分，当前级别：{}，连续失败次数：{}", avgScore, currentDepthLevel, consecutiveFailures);
                    
                    // 定义深度级别降级规则：optimization -> principle -> implementation -> usage
                    switch (currentDepthLevel) {
                        case "optimization":
                            currentDepthLevel = "principle";
                            consecutiveFailures = 0; // 降级成功，重置连续失败次数
                            log.info("降级到principle级别");
                            break;
                        case "principle":
                            currentDepthLevel = "implementation";
                            consecutiveFailures = 0; // 降级成功，重置连续失败次数
                            log.info("降级到implementation级别");
                            break;
                        case "implementation":
                            currentDepthLevel = "usage";
                            consecutiveFailures = 0; // 降级成功，重置连续失败次数
                            log.info("降级到usage级别");
                            break;
                        case "usage":
                            // 已经是最低级别，需要切换话题
                            log.info("已经是最低深度级别(usage)，连续失败次数：{}", consecutiveFailures);
                            
                            // 如果连续失败2次，切换话题
                            if (consecutiveFailures >= 2) {
                                log.info("连续失败2次，切换话题");
                                consecutiveFailures = 0; // 重置连续失败次数
                                
                                // 标记当前技术项或项目点为已耗尽
                                if ("tech".equals(currentQuestionType) && !usedTechItems.isEmpty()) {
                                    // 将当前使用的技术项添加到已耗尽列表
                                    String lastTechItem = usedTechItems.get(usedTechItems.size() - 1);
                                    if (!exhaustedTechItems.contains(lastTechItem)) {
                                        exhaustedTechItems.add(lastTechItem);
                                        log.info("标记技术项为已耗尽：{}", lastTechItem);
                                    }
                                } else if ("project".equals(currentQuestionType) && !usedProjectPoints.isEmpty()) {
                                    // 将当前使用的项目点添加到已耗尽列表
                                    String lastProjectPoint = usedProjectPoints.get(usedProjectPoints.size() - 1);
                                    if (!exhaustedProjectPoints.contains(lastProjectPoint)) {
                                        exhaustedProjectPoints.add(lastProjectPoint);
                                        log.info("标记项目点为已耗尽：{}", lastProjectPoint);
                                    }
                                }
                                
                                // 清空当前使用列表，准备新的话题
                                usedTechItems.clear();
                                usedProjectPoints.clear();
                                
                                // 选择新的话题类型
                                // 1. 首先检查是否有未耗尽的技术项
                                List<String> availableTechItems = techItems.stream()
                                    .filter(item -> !exhaustedTechItems.contains(item))
                                    .collect(Collectors.toList());
                                
                                // 2. 检查是否有未耗尽的项目点
                                List<String> availableProjectPoints = new ArrayList<>();
                                if (projectPoints != null) {
                                    availableProjectPoints = projectPoints.stream()
                                        .map(point -> String.valueOf(point.get("title")))
                                        .filter(title -> !exhaustedProjectPoints.contains(title))
                                        .collect(Collectors.toList());
                                }
                                
                                // 3. 选择新话题 - 优先选择项目点，确保先问完项目相关问题
                                if (!availableProjectPoints.isEmpty()) {
                                    currentQuestionType = "project";
                                    log.info("切换到新的项目点，可用项目点数量：{}", availableProjectPoints.size());
                                } else if (!availableTechItems.isEmpty()) {
                                    currentQuestionType = "tech";
                                    log.info("切换到新的技术项，可用技术项数量：{}", availableTechItems.size());
                                } else {
                                    currentQuestionType = "hr";
                                    log.info("所有技术和项目点已耗尽，切换到HR问题");
                                }
                            }
                            break;
                        default:
                            currentDepthLevel = "usage";
                            consecutiveFailures = 0;
                            log.info("重置到usage级别");
                            break;
                    }
                    
                    log.info("调整后：深度级别={}，连续失败次数={}，当前话题类型={}", currentDepthLevel, consecutiveFailures, currentQuestionType);
                    // 更新面试状态
                    interviewState.put("currentDepthLevel", currentDepthLevel);
                    interviewState.put("consecutiveFailures", consecutiveFailures);
                    interviewState.put("currentQuestionType", currentQuestionType);
                    interviewState.put("usedTechItems", usedTechItems);
                    interviewState.put("usedProjectPoints", usedProjectPoints);
                    interviewState.put("exhaustedTechItems", exhaustedTechItems);
                    interviewState.put("exhaustedProjectPoints", exhaustedProjectPoints);
                } else {
                    // 回答良好，重置连续失败次数
                    consecutiveFailures = 0;
                    interviewState.put("consecutiveFailures", consecutiveFailures);
                    // 如果回答良好，将当前技术项或项目点添加到已使用列表
                    if ("tech".equals(currentQuestionType) && !usedTechItems.isEmpty() && techItems.size() > 0) {
                        String lastTechItem = usedTechItems.get(usedTechItems.size() - 1);
                        if (!exhaustedTechItems.contains(lastTechItem)) {
                            exhaustedTechItems.add(lastTechItem);
                            log.info("回答良好，标记技术项为已耗尽：{}", lastTechItem);
                        }
                        usedTechItems.clear();
                    } else if ("project".equals(currentQuestionType) && !usedProjectPoints.isEmpty() && projectPoints != null && projectPoints.size() > 0) {
                        String lastProjectPoint = usedProjectPoints.get(usedProjectPoints.size() - 1);
                        if (!exhaustedProjectPoints.contains(lastProjectPoint)) {
                            exhaustedProjectPoints.add(lastProjectPoint);
                            log.info("回答良好，标记项目点为已耗尽：{}", lastProjectPoint);
                        }
                        usedProjectPoints.clear();
                    }
                }
                
                // 构建响应，包含评分
                Map<String, Object> response = new HashMap<>();
                response.put("techScore", scores.get("tech"));
                response.put("logicScore", scores.get("logic"));
                response.put("clarityScore", scores.get("clarity"));
                response.put("depthScore", scores.get("depth"));
                
                // 流式发送评分
                emitter.send(SseEmitter.event().data(response).name("score").id("1"));
                
                // 创建下一个问题的日志记录
                InterviewLog nextQuestionLog = new InterviewLog();
                nextQuestionLog.setQuestionId(UUID.randomUUID().toString());
                nextQuestionLog.setSessionId(sessionId);
                nextQuestionLog.setRoundNumber(session.getQuestionCount() + 1);
                nextQuestionLog.setPersona(session.getPersona());
                
                // 保存新的日志记录
                logRepository.save(nextQuestionLog);
                
                // 生成下一个问题（流式）
                generateQuestionStream(techItems, projectPoints, usedTechItems, usedProjectPoints,
                                     currentDepthLevel, session.getSessionTimeRemaining(),
                                     session.getPersona(), String.valueOf(session.getJobTypeId()), resumeContent,
                                     currentLog.getQuestionText(), userAnswerText, emitter,
                                     () -> {
                                         // 流结束回调：发送结束信号并完成emitter
                                         try {
                                             emitter.send(SseEmitter.event().data("end").name("end").id("3"));
                                             emitter.complete();
                                         } catch (IOException e) {
                                             log.error("发送结束信号失败：{}", e.getMessage(), e);
                                             emitter.completeWithError(e);
                                         }
                                     }, sessionId, currentQuestionType, session);
                
                // 7. 更新会话状态（需要从AI响应中获取nextQuestion和stopReason，这里简化处理）
                session.setQuestionCount(session.getQuestionCount() + 1);
                session.setInterviewState(objectMapper.writeValueAsString(interviewState));
                // 保存会话，确保剩余时间的更新被持久化
                sessionRepository.save(session);
            } catch (Exception e) {
                log.error("提交回答失败: {}", e.getMessage(), e);
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }

    /**
     * 开始面试（支持强制创建新会话）
     * @param userId 用户ID
     * @param resumeId 简历ID
     * @param persona 面试官风格
     * @param sessionSeconds 会话时长（秒）
     * @param jobTypeId 职位类型ID
     * @param forceNew 是否强制创建新会话
     * @return 面试响应VO
     */
    @Override
    public InterviewResponseVO startInterview(Long userId, Long resumeId, String persona, Integer sessionSeconds, Integer jobTypeId, Boolean forceNew) {
        try {
            // 1. 检查用户是否有未完成的面试会话
            List<InterviewSession> ongoingSessions = sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "in_progress");
            if (!ongoingSessions.isEmpty() && !forceNew) {
                // 有未完成的面试会话且不强制创建新会话，直接返回最近的一个
                InterviewSession existingSession = ongoingSessions.get(0);
                log.info("用户 {} 有未完成的面试会话，直接返回: {}", userId, existingSession.getSessionId());
                
                // 通过jobTypeId查询job_type表数据获取jobName
                String industryJobTag = "";
                try {
                    JobType jobType = jobTypeRepository.findById(existingSession.getJobTypeId()).orElse(null);
                    if (jobType != null && jobType.getJobName() != null) {
                        industryJobTag = jobType.getJobName();
                    }
                } catch (Exception e) {
                    log.error("查询职位类型失败: {}", e.getMessage());
                    // 查询失败不影响面试流程，使用空字符串作为默认值
                }
                
                // 构建返回对象
                InterviewResponseVO response = new InterviewResponseVO();
                response.setSessionId(existingSession.getSessionId());
                response.setQuestion(null);
                response.setQuestionType("continue_question"); // 标记为继续面试
                response.setFeedback(null);
                response.setNextQuestion(null);
                response.setIsCompleted(false); // 面试未完成
                response.setIndustryJobTag(industryJobTag); // 设置行业职位标签
                response.setSessionTimeRemaining(existingSession.getSessionTimeRemaining()); // 设置剩余时间
                
                return response;
            }
            
            // 2. 初始化变量
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
            session.setStatus("in_progress");
            session.setJobTypeId(actualJobTypeId); // 使用从简历中获取的jobTypeId
            session.setCity("未知城市");
            session.setQuestionCount(0);
            session.setAdaptiveLevel("auto");
            session.setAiQuestionSeed(new Random().nextInt(1000));
            
            // 设置动态面试参数
            session.setPersona(StringUtils.hasText(persona) ? persona : "professional");
            session.setSessionSeconds(sessionSeconds != null ? sessionSeconds : dynamicConfigService.getDefaultSessionSeconds());
            session.setSessionTimeRemaining(session.getSessionSeconds());
            
            // 生成并保存面试官风格提示词
            String personaStyle = enhancePersonaWithStyle(session.getPersona());
            String personaPrompt = String.format("你是%s风格的面试官。%s\n请确保你生成的问题是单一的、独立的，只关注一个具体的知识点或技术点。\n", session.getPersona(), personaStyle);
            session.setPersonaPrompt(personaPrompt);
            
            // 初始化面试状态，先不进行耗时的简历分析
            interviewState.put("usedTechItems", new ArrayList<>());
            interviewState.put("usedProjectPoints", new ArrayList<>());
            interviewState.put("exhaustedTechItems", new ArrayList<>());
            interviewState.put("exhaustedProjectPoints", new ArrayList<>());
            interviewState.put("currentDepthLevel", initialDepthLevel);
            session.setInterviewState(objectMapper.writeValueAsString(interviewState));
            
            // 保存初始会话 - 这是快速返回的关键
            sessionRepository.save(session);
            
            // 4. 异步处理简历分析和会话初始化
            CompletableFuture.runAsync(() -> {
                analyzeResumeAndInitSessionAsync(session.getSessionId(), resumeId, actualJobTypeId);
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
            response.setSessionTimeRemaining(session.getSessionTimeRemaining()); // 设置剩余时间

            return response;
        } catch (Exception e) {
            // 捕获异常后直接抛出
            log.error("开始面试失败: {}, 详细错误: {}", 
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("面试初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 异步处理简历分析和会话初始化
     * 
     * @param sessionId   会话ID
     * @param resumeId    简历ID
     * @param jobTypeId   职位类型ID
     */
    private void analyzeResumeAndInitSessionAsync(String sessionId, Long resumeId, Integer jobTypeId) {
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
            log.info("异步处理: 成功获取完整简历内容");
            
            // 从简历内容中提取技术项和项目点（调用deepseek分析简历）
            Map<String, Object> extractedData = extractTechItemsAndProjectPoints(resumeId, resumeContent);
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
                    interviewState.put("exhaustedTechItems", new ArrayList<>());
                    interviewState.put("exhaustedProjectPoints", new ArrayList<>());
                    interviewState.put("currentDepthLevel", "usage");
                }
            } else {
                interviewState.put("usedTechItems", new ArrayList<>());
                interviewState.put("usedProjectPoints", new ArrayList<>());
                interviewState.put("exhaustedTechItems", new ArrayList<>());
                interviewState.put("exhaustedProjectPoints", new ArrayList<>());
                interviewState.put("currentDepthLevel", "usage");
            }
            
            // 更新面试状态，包含完整简历内容和会话ID
            interviewState.put("fullResumeContent", resumeContent);
            interviewState.put("sessionId", session.getSessionId());
            
            // 将提取的技术项和项目点分配到interviewState的相关字段中
            // 初始时，usedTechItems和usedProjectPoints为空
            // exhaustedTechItems和exhaustedProjectPoints包含所有提取的技术项和项目点
            List<String> exhaustedTechItems = new ArrayList<>(techItems);
            List<String> exhaustedProjectPoints = new ArrayList<>();
            for (Map<String, Object> projectPoint : projectPoints) {
                if (projectPoint.containsKey("title")) {
                    exhaustedProjectPoints.add((String) projectPoint.get("title"));
                }
            }
            
            interviewState.put("exhaustedTechItems", exhaustedTechItems);
            interviewState.put("exhaustedProjectPoints", exhaustedProjectPoints);
            
            // 存储提取的数据到会话
            session.setTechItems(objectMapper.writeValueAsString(techItems));
            session.setProjectPoints(objectMapper.writeValueAsString(projectPoints));
            session.setInterviewState(objectMapper.writeValueAsString(interviewState));
            // 保存完整简历内容到session
            session.setResumeContent(resumeContent);
            
            // 保存更新后的会话
            sessionRepository.save(session);
            
            log.info("异步处理: 成功分析简历并更新会话，会话ID: {}", session.getSessionId());
        } catch (Exception e) {
            log.error("异步处理简历分析失败: {}, 会话ID: {}", 
                    e.getMessage(), sessionId, e);
        }
    }

    @Override
    public String startReportGeneration(String sessionId) {
        // 生成唯一的reportId
        String reportId = "report_" + UUID.randomUUID().toString().replace("-", "");
        
        // 创建报告生成记录
        ReportGenerationService.ReportGenerationRecord record = reportGenerationService.createReportRecord(reportId);
        SseEmitter emitter = new SseEmitter(60000L);
        // 启动异步线程生成报告
        executorService.submit(() -> {
            try {
                // 1. 获取会话信息和所有日志
                InterviewSession session = sessionRepository.findBySessionId(sessionId)
                        .orElseThrow(() -> new RuntimeException("会话不存在"));
                List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);

                // 2. 计算聚合评分
                Map<String, Double> aggregatedScores = calculateAggregatedScores(logs);
                double totalScore = aggregatedScores.get("total");

                // 3. 更新会话状态
                session.setStatus("completed");
                session.setEndTime(LocalDateTime.now());
                
                // 4. 保存会话更新
                sessionRepository.save(session);

                // 5. 构建完整的面试会话记录
                StringBuilder sessionContent = new StringBuilder();
                sessionContent.append("面试职位ID: " + session.getJobTypeId() + "\n");
                sessionContent.append("面试城市: " + session.getCity() + "\n\n");
                sessionContent.append("面试会话记录:\n");
                
                for (InterviewLog log : logs) {
                    sessionContent.append("问题 " + log.getRoundNumber() + ": " + log.getQuestionText() + "\n");
                    sessionContent.append("回答: " + log.getUserAnswerText() + "\n");
                    sessionContent.append("AI反馈: " + log.getFeedback() + "\n");
                    sessionContent.append("技术评分: " + log.getTechScore() + "\n");
                    sessionContent.append("逻辑评分: " + log.getLogicScore() + "\n");
                    sessionContent.append("表达清晰度评分: " + log.getClarityScore() + "\n");
                    sessionContent.append("深度评分: " + log.getDepthScore() + "\n\n");
                }
                
                // 6. 构建prompt让DeepSeek全面分析面试情况
                // 从动态配置中获取系统提示词，如果不存在则使用默认值
                String systemPrompt = dynamicConfigService.getReportGenerationSystemPrompt().orElse(
                        "请作为资深技术面试官，全面分析以下面试会话记录，生成一份详细的面试报告。\n" +
                        "报告需要包含以下内容：\n" +
                        "1. 总体评价和总分\n" +
                        "2. 优势分析\n" +
                        "3. 改进点\n" +
                        "4. 技术深度评价\n" +
                        "5. 逻辑表达评价\n" +
                        "6. 沟通表达评价\n" +
                        "7. 回答深度评价\n" +
                        "8. 针对候选人的详细改进建议\n" +
                        "请确保报告内容具体、针对性强，基于面试中的实际表现。\n" +
                        "请将报告总字数控制在500字左右，确保内容精炼且全面。\n" +
                        "重要要求：\n" +
                        "1. 请使用简洁清晰的Markdown格式，避免过度使用格式。\n" +
                        "2. 标题使用：仅使用##（二级标题）作为各部分标题，例如：## 总体评价和总分\n" +
                        "3. 列表使用：使用-（减号）作为无序列表标记，例如：- 优势1\n" +
                        "4. 强调使用：仅对核心关键词使用**（粗体），例如：**核心优势**\n" +
                        "5. 避免使用：代码块、链接、图片、分割线等复杂格式。\n" +
                        "6. 内容结构：每部分内容保持简洁，重点突出，避免冗长。\n" +
                        "7. 格式对应：请确保生成的Markdown格式与前端展示能力匹配。"
                );
                
                String userPrompt = "面试会话记录：\n" + sessionContent.toString();

                // 7. 使用流式方式调用DeepSeek API，将结果按块存储到reportGenerationService
                StringBuilder currentChunk = new StringBuilder();
                aiServiceUtils.callDeepSeekApiStream(systemPrompt, userPrompt, emitter, content -> {
                    if (content != null && !content.isEmpty()) {
                        currentChunk.append(content);
                        // 当当前块超过20字时，存储并清空
                        if (currentChunk.length() >= 20) {
                            record.addChunk(currentChunk.toString());
                            currentChunk.setLength(0);
                        }
                        log.debug("当前内容块为：{}", currentChunk.toString());
                    }
                }, () -> {
                    try {
                        // 保存剩余的内容
                        if (currentChunk.length() > 0) {
                            record.addChunk(currentChunk.toString());
                        }
                        // 标记报告生成完成
                        record.setStatus(ReportGenerationService.ReportStatus.COMPLETED);
                        
                        // 自动保存报告到数据库
                        saveGeneratedReportToDatabase(sessionId, record);
                    } catch (Exception e) {
                        log.error("自动保存报告失败: {}", e.getMessage(), e);
                    }
                }, sessionId, "report");

            } catch (Exception e) {
                log.error("生成报告失败", e);
                record.setStatus(ReportGenerationService.ReportStatus.FAILED);
                record.setErrorMessage("生成面试报告失败: " + e.getMessage());
            }
        });
        
        return reportId;
    }

    @Override
    public ReportChunksVO getReportChunks(String reportId, int lastIndex) {
        ReportGenerationService.ReportGenerationRecord record = reportGenerationService.getReportRecord(reportId);
        ReportChunksVO result = new ReportChunksVO();
        
        if (record == null) {
            result.setStatus("not_found");
            return result;
        }
        
        List<ReportGenerationService.ReportChunk> chunks = reportGenerationService.getReportChunks(reportId, lastIndex);
        
        result.setStatus(record.getStatus().name());
        result.setChunks(chunks);
        result.setLastIndex(record.getChunks().size() - 1);
        result.setCompleted(record.getStatus() == ReportGenerationService.ReportStatus.COMPLETED);
        
        if (record.getStatus() == ReportGenerationService.ReportStatus.FAILED) {
            result.setErrorMessage(record.getErrorMessage());
        }
        
        return result;
    }
    
    /**
     * 从AI生成的报告中提取指定章节的内容
     * @param report 完整的报告内容
     * @param startSection 开始章节标题
     * @param endSection 结束章节标题
     * @return 提取的章节内容
     */
    private String extractSectionFromReport(String report, String startSection, String endSection) {
        if (report == null || report.isEmpty()) {
            return "";
        }
        
        int startIndex = report.indexOf(startSection);
        if (startIndex == -1) {
            return "";
        }
        
        startIndex += startSection.length();
        
        int endIndex = report.indexOf(endSection);
        if (endIndex == -1) {
            return report.substring(startIndex).trim();
        }
        
        return report.substring(startIndex, endIndex).trim();
    }

    public Map<String, Object> extractTechItemsAndProjectPoints(Long resumeId, String resumeText) {
        try {
            // 记录简历文本的长度和部分内容，确保不为空
            log.info("extractTechItemsAndProjectPoints - 简历ID: {}, 简历文本长度: {}, 开始部分: {}", 
                     resumeId, resumeText.length(), 
                     resumeText.length() > 50 ? resumeText.substring(0, 50) + "..." : resumeText);
                       
            // 检查是否已有提取结果且简历未修改
            Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
            if (resumeOpt.isPresent()) {
                Resume resume = resumeOpt.get();
                if (StringUtils.hasText(resume.getTechItems()) && StringUtils.hasText(resume.getProjectPoints()) && resume.getLastExtractedTime() != null) {
                    // 检查简历是否有修改，lastExtractedTime >= resumeUpdateTime表示简历未修改
                    Date resumeUpdateTime = resume.getUpdateTime();
                    Date lastExtractedTime = resume.getLastExtractedTime();
                    if (lastExtractedTime.after(resumeUpdateTime) || lastExtractedTime.equals(resumeUpdateTime)) {
                        // 简历未修改，使用缓存结果
                        log.info("extractTechItemsAndProjectPoints - 使用缓存结果，简历ID: {}", resumeId);
                        Map<String, Object> result = new HashMap<>();
                        result.put("techItems", objectMapper.readValue(resume.getTechItems(), new TypeReference<List<String>>() {}));
                        result.put("projectPoints", objectMapper.readValue(resume.getProjectPoints(), new TypeReference<List<Map<String, Object>>>() {}));
                        return result;
                    }
                }
            }
            
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
            
            // 保存提取结果到简历
            Optional<Resume> resumeOptSave = resumeRepository.findById(resumeId);
            if (resumeOptSave.isPresent()) {
                Resume resume = resumeOptSave.get();
                resume.setTechItems(objectMapper.writeValueAsString(techItems));
                resume.setProjectPoints(objectMapper.writeValueAsString(projectPoints));
                resume.setLastExtractedTime(new Date());
                resumeRepository.save(resume);
                log.info("提取结果已保存到简历，简历ID: {}", resumeId);
            }
            
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
        // 优先从动态配置获取面试官风格描述，直接使用DynamicConfig实体类对象
        try {
            Optional<List<DynamicConfig>> personasOpt = dynamicConfigService.getInterviewPersonasAsDynamicConfigs();
            if (personasOpt.isPresent()) {
                for (DynamicConfig persona : personasOpt.get()) {
                    // 匹配configKey作为personaId
                    if (personaId.equals(persona.getConfigKey())) {
                        // 直接使用configValue作为prompt
                        if (StringUtils.hasText(persona.getConfigValue())) {
                            return persona.getConfigValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("从动态配置获取面试官风格失败", e);
        }
        
        // 回退到硬编码的风格描述，保持与原有项目一致
        switch (personaId) {
            case "professional":
                return "你是一位专业严谨的资深技术面试官。请使用以下风格：1.问题结构清晰，逻辑严密，层层递进；2.聚焦技术深度和实现细节；3.要求候选人提供具体案例和数据支撑；4.严格按照专业标准进行评估；5.语言正式、客观，避免情绪化表达；6.注重考察系统设计能力和问题解决思路。";
            case "funny":
                return "你是一位搞怪幽默的技术面试官。请使用以下风格：1.语言风趣幽默，加入适当的网络流行语和调侃；2.问题设计充满创意，避免枯燥的常规提问；3.用轻松的方式引导候选人，缓解紧张情绪；4.在专业评估的基础上增加趣味性；5.保持微笑和热情的语气；6.可以用比喻、夸张等修辞手法增加表达的趣味性。";
            case "philosophical":
                return "你是一位抽象哲学型技术面试官。请使用以下风格：1.从哲学层面提问，探索技术的本质和意义；2.引导候选人思考技术与人类、社会、伦理的关系；3.问题具有启发性和开放性，不追求标准答案；4.关注候选人的思维深度和洞察力；5.使用富有哲理的语言表达；6.鼓励候选人提出自己的观点和思考过程。";
            case "crazy":
                return "你是一位抽风跳跃型技术面试官。请使用以下风格：1.问题之间没有明显的逻辑关联，随机切换话题；2.从技术细节突然跳到宏观架构，再到个人兴趣；3.考验候选人的快速思维和随机应变能力；4.保持活泼、随意的语气；5.问题形式多样化，包括技术、非技术、突发场景等；6.观察候选人在压力下的表现和适应能力。";
            case "anime":
                return "你是一位中二热血型技术面试官。请使用以下风格：1.语言充满动漫风格，使用夸张的词汇和语气；2.将技术面试比喻为冒险、战斗或拯救世界的任务；3.使用热血、激情的表达，如\"让我看看你的真正实力！\"、\"这就是你的极限吗？\"；4.加入动漫式的感叹词和肢体语言描述；5.激发候选人的斗志和竞争意识；6.保持积极向上的热血氛围。";
            case "healing":
                return "你是一位温柔治愈型技术面试官。请使用以下风格：1.语气温柔亲切，语速适中，充满耐心；2.使用鼓励性的语言，如\"没关系，慢慢想\"、\"这个想法很不错\"；3.像心理咨询师一样倾听和引导，关注候选人的情绪变化；4.提供积极的反馈和肯定；5.营造安全、包容的面试氛围；6.帮助候选人放松心情，减轻压力。";
            case "sharp":
                return "你是一位毒舌犀利型技术面试官。请使用以下风格：1.语言尖锐直接，一针见血地指出问题；2.加入幽默的调侃和讽刺，避免生硬指责；3.提问具有挑战性，考验候选人的抗压能力；4.使用反问和质疑的方式激发候选人思考；5.保持专业但略带调侃的语气；6.点评犀利但有建设性，帮助候选人认识不足。";
            case "retro":
                return "你是一位怀旧复古型技术面试官。请使用以下风格：1.使用传统的面试方式，注重基础知识和经典技术；2.语言正式庄重，避免现代流行语；3.提问聚焦于算法、数据结构、编程语言基础等核心内容；4.重视扎实的基本功和传统价值观；5.保持礼貌和谦逊的态度；6.可以提及过去的技术发展历史，增加怀旧感。";
            default:
                return "专业面试官，语气客观中立，关注事实和技术能力。";
        }
    }

    /**
     * 统一的流式生成问题方法，用于生成第一道题和提交答案生成下一道题
     * @param techItems 技术项列表
     * @param projectPoints 项目点列表
     * @param usedTechItems 已使用的技术项列表
     * @param usedProjectPoints 已使用的项目点列表
     * @param currentDepthLevel 当前深度级别
     * @param sessionTimeRemaining 剩余会话时间（秒）
     * @param persona 面试官风格
     * @param jobType 职位类型
     * @param fullResumeContent 完整简历内容
     * @param previousQuestion 上一个问题
     * @param previousAnswer 上一个回答
     * @param emitter SseEmitter对象，用于流式输出
     * @param onComplete 完成回调函数
     * @param sessionId 会话ID
     */
    private void generateQuestionStream(List<String> techItems, List<Map<String, Object>> projectPoints,
                                      List<String> usedTechItems, List<String> usedProjectPoints,
                                      String currentDepthLevel, Integer sessionTimeRemaining,
                                      String persona, String jobType, String fullResumeContent,
                                      String previousQuestion, String previousAnswer, SseEmitter emitter,
                                      Runnable onComplete, String sessionId, String currentQuestionType, InterviewSession session) {
        // 构建系统提示词（包含固定的指令和要求）
        StringBuilder systemPromptBuilder = new StringBuilder();
        
        // 从session中获取面试官风格提示词，如果存在则使用，否则重新生成
        try {
            if (session != null && StringUtils.hasText(session.getPersonaPrompt())) {
                // 使用session中保存的风格提示词
                systemPromptBuilder.append(session.getPersonaPrompt());
            } else {
                // 否则重新生成并保存
                String personaStyle = enhancePersonaWithStyle(persona);
                String personaPrompt = String.format("你是%s风格的面试官。%s\n请确保你生成的问题是单一的、独立的，只关注一个具体的知识点或技术点。\n", persona, personaStyle);
                systemPromptBuilder.append(personaPrompt);
                
                // 保存到session中（如果session存在）
                if (session != null) {
                    session.setPersonaPrompt(personaPrompt);
                    sessionRepository.save(session);
                }
            }
        } catch (Exception e) {
            log.error("从session获取或生成personaPrompt失败: {}", e.getMessage());
            // 失败时使用默认方式生成
            String personaStyle = enhancePersonaWithStyle(persona);
            systemPromptBuilder.append(String.format("你是%s风格的面试官。%s\n", persona, personaStyle));
            systemPromptBuilder.append("请确保你生成的问题是单一的、独立的，只关注一个具体的知识点或技术点。\n");
        }
        
        // 构建用户提示词（包含动态内容和具体问题要求）
        StringBuilder userPromptBuilder = new StringBuilder();
        
        // 检查是否需要切换话题（已使用列表为空，且有之前的问答）
        boolean needSwitchTopic = (usedTechItems.isEmpty() && usedProjectPoints.isEmpty()) && 
                                StringUtils.hasText(previousQuestion) && StringUtils.hasText(previousAnswer);
        
        // 根据当前问题类型生成不同的问题
        String questionTypePrompt = ""; 
        if ("hr".equals(currentQuestionType)) {
            questionTypePrompt = "请生成一个HR面试问题，关注候选人的职业规划、团队协作能力、沟通能力或个人发展等非技术方面。\n";
        } else if ("project".equals(currentQuestionType)) {
            questionTypePrompt = "请生成一个关于项目经验的问题，关注候选人在项目中的具体贡献、遇到的挑战和解决方法。\n";
        } else {
            // 默认是技术问题
            questionTypePrompt = "请生成一个技术问题，关注具体的技术知识点或技术实践。\n";
        }
        
        // 根据不同情况构建prompt，完整对话历史由AiServiceUtils的getConversationHistory处理
        if (needSwitchTopic) {
            userPromptBuilder.append("请生成下一个面试题。\n");
            // 切换话题：生成全新的问题
            // log.info("切换话题，生成全新问题，类型：{}", currentQuestionType);
            // userPromptBuilder.append("请基于候选人的简历内容和之前的面试上下文，生成一个全新的、与之前完全不同的面试问题。\n");
            // userPromptBuilder.append(questionTypePrompt);
            // userPromptBuilder.append("不要与上一题有任何关联。\n");
            // // 动态信息（每次生成问题时需要更新）
            // userPromptBuilder.append(String.format("- 已使用技术项：%s\n", usedTechItems));
            // userPromptBuilder.append(String.format("- 已使用项目点：%s\n", usedProjectPoints));
            // userPromptBuilder.append(String.format("- 当前深度级别：%s\n", currentDepthLevel));
            // userPromptBuilder.append(String.format("- 剩余时间：%d秒\n", sessionTimeRemaining));
            // userPromptBuilder.append("- 若时间<60s或连续两次回答偏离主题，则进入总结问题。\n");
        } else if (StringUtils.hasText(previousQuestion) && StringUtils.hasText(previousAnswer)) {
            userPromptBuilder.append("请生成下一个面试题。\n");
            // 后续问题：基于完整的对话历史生成下一个相关问题
            // userPromptBuilder.append("请根据之前的完整面试对话历史，生成下一个相关的面试问题。\n");
            // userPromptBuilder.append(questionTypePrompt);
            // // 动态信息（每次生成问题时需要更新）
            // userPromptBuilder.append(String.format("- 已使用技术项：%s\n", usedTechItems));
            // userPromptBuilder.append(String.format("- 已使用项目点：%s\n", usedProjectPoints));
            // userPromptBuilder.append(String.format("- 当前深度级别：%s\n", currentDepthLevel));
            // userPromptBuilder.append(String.format("- 剩余时间：%d秒\n", sessionTimeRemaining));
            // userPromptBuilder.append("- 若时间<60s或连续两次回答偏离主题，则进入总结问题。\n");
        } else {
            userPromptBuilder.append("请直接基于候选人的简历内容生成针对性的面试问题。\n");
            userPromptBuilder.append(questionTypePrompt);
        }
        
        String systemPrompt = systemPromptBuilder.toString();
        String userPrompt = userPromptBuilder.toString();
        
        try {
            // 调用AI服务（流式）
            log.info("调用Deepseek API生成问题，systemPrompt: {}, userPrompt: {}", systemPrompt, userPrompt);
            aiServiceUtils.callDeepSeekApiStream(systemPrompt, userPrompt, emitter, onComplete, sessionId);
            log.info("调用Deepseek API生成问题完成（流式）");
        } catch (Exception e) {
            log.error("生成问题失败：{}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().data("生成问题失败，请稍后重试").name("error").id("error"));
                emitter.complete();
            } catch (IOException ex) {
                log.error("发送错误信息失败：{}", ex.getMessage(), ex);
            }
        }
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
            
            // 从面试报告获取总评分
            Optional<InterviewReport> reportOpt = interviewReportRepository.findBySessionId(session.getSessionId());
            reportOpt.ifPresent(report -> vo.setFinalScore(report.getTotalScore()));
            
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
    public InterviewSessionVO checkOngoingInterview(Long userId) {
        // 查找用户的进行中面试会话
        List<InterviewSession> sessions = sessionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "in_progress");
        
        if (sessions.isEmpty()) {
            return null; // 没有进行中的面试
        }
        
        // 返回最新的进行中面试会话
        InterviewSession session = sessions.get(0);
        return getInterviewDetail(session.getSessionId());
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
            
            // 优先从面试报告获取评分
            Optional<InterviewReport> reportOpt = interviewReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                InterviewReport report = reportOpt.get();
                aggregatedScores.put("tech", report.getTechScore());
                aggregatedScores.put("logic", report.getLogicScore());
                aggregatedScores.put("clarity", report.getClarityScore());
                aggregatedScores.put("depth", report.getDepthScore());
                aggregatedScores.put("total", report.getTotalScore());
            } else {
                // 如果没有报告，则从面试日志计算平均评分
                List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);
                Map<String, Double> avgScores = calculateAggregatedScores(logs);
                aggregatedScores.put("tech", avgScores.get("tech"));
                aggregatedScores.put("logic", avgScores.get("logic"));
                aggregatedScores.put("clarity", avgScores.get("clarity"));
                aggregatedScores.put("depth", avgScores.get("depth"));
                aggregatedScores.put("total", avgScores.get("total"));
            }
            
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
        vo.setSessionId(session.getSessionId());
        vo.setTitle(jobType + " 面试");
        vo.setDescription("会话ID: " + session.getSessionId());
        vo.setStatus(session.getStatus());
        vo.setPersona(session.getPersona());
        vo.setSessionSeconds(session.getSessionSeconds());
        vo.setSessionTimeRemaining(session.getSessionTimeRemaining());
        vo.setInterviewDuration(session.getSessionSeconds() - session.getSessionTimeRemaining());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setFinishedAt(session.getEndTime());
        vo.setStopReason(session.getStopReason());
        
        // 获取面试日志
        List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);
        vo.setLogs(logs);
        vo.setTotalQuestions(logs.size());
        vo.setAnsweredQuestions(logs.size()); // 假设所有问题都已回答
        
        // 检查是否有当前问题
        boolean hasQuestion = false;
        String currentQuestion = "";
        if (!logs.isEmpty()) {
            // 查找最新的问题
            for (int i = logs.size() - 1; i >= 0; i--) {
                InterviewLog log = logs.get(i);
                if (log.getQuestionText() != null && !log.getQuestionText().isEmpty()) {
                    currentQuestion = log.getQuestionText();
                    hasQuestion = true;
                    break;
                }
            }
        }
        
        vo.setHasQuestion(hasQuestion);
        vo.setCurrentQuestion(currentQuestion);
        
        if (session.getStartTime() != null) {
            vo.setStartTime(session.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (session.getEndTime() != null) {
            vo.setEndTime(session.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        
        return vo;
    }
    
    @Override
    public InterviewReportVO getInterviewReport(String sessionId) {
        // 获取会话信息
        InterviewSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));
        
        // 获取报告信息
        Optional<InterviewReport> reportOpt = interviewReportRepository.findBySessionId(sessionId);
        
        InterviewReportVO vo = new InterviewReportVO();
        vo.setSessionId(sessionId);
        
        // 如果报告存在，设置报告内容和评分
        if (reportOpt.isPresent()) {
            InterviewReport report = reportOpt.get();
            vo.setTotalScore(report.getTotalScore());
            vo.setTechScore(report.getTechScore());
            vo.setLogicScore(report.getLogicScore());
            vo.setClarityScore(report.getClarityScore());
            vo.setDepthScore(report.getDepthScore());
            vo.setOverallFeedback(report.getOverallFeedback());
            vo.setStrengths(report.getStrengths());
            vo.setImprovements(report.getImprovements());
            vo.setTechDepthEvaluation(report.getTechDepthEvaluation());
            vo.setLogicExpressionEvaluation(report.getLogicExpressionEvaluation());
            vo.setCommunicationEvaluation(report.getCommunicationEvaluation());
            vo.setAnswerDepthEvaluation(report.getAnswerDepthEvaluation());
            vo.setDetailedImprovementSuggestions(report.getDetailedImprovementSuggestions());
            return vo;
        }
        return null;
    }

    /**
     * 根据会话ID获取面试历史记录
     */
    @Override
    public List<InterviewHistoryItemVO> getInterviewHistory(String sessionId) {
        // 获取指定会话的面试日志，按轮次排序
        List<InterviewLog> logs = logRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);
        List<InterviewHistoryItemVO> result = new ArrayList<>();
        
        for (InterviewLog log : logs) {
            // 创建问题记录
            result.add(InterviewHistoryItemVO.createQuestionItem(log));
            
            // 如果有用户回答，创建回答记录
            if (log.getUserAnswerText() != null && !log.getUserAnswerText().isEmpty()) {
                result.add(InterviewHistoryItemVO.createAnswerItem(log));
            }
        }
        
        return result;
    }
    
    @Override
    public void saveReport(String sessionId, Map<String, Object> reportData) {
        try {
            // 1. 根据sessionId获取面试会话
            InterviewSession session = sessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("面试会话不存在: " + sessionId));
            
            // 2. 解析报告数据
            Double totalScore = reportData.get("totalScore") != null ? Double.parseDouble(reportData.get("totalScore").toString()) : 0.0;
            Double techScore = reportData.get("techScore") != null ? Double.parseDouble(reportData.get("techScore").toString()) : null;
            Double logicScore = reportData.get("logicScore") != null ? Double.parseDouble(reportData.get("logicScore").toString()) : null;
            Double clarityScore = reportData.get("clarityScore") != null ? Double.parseDouble(reportData.get("clarityScore").toString()) : null;
            Double depthScore = reportData.get("depthScore") != null ? Double.parseDouble(reportData.get("depthScore").toString()) : null;
            String overallFeedback = reportData.get("overallFeedback") != null ? reportData.get("overallFeedback").toString() : "";
            
            // 处理优势列表
            List<String> strengths = new ArrayList<>();
            if (reportData.get("strengths") instanceof List) {
                strengths = (List<String>) reportData.get("strengths");
            } else if (reportData.get("strengths") != null) {
                strengths.add(reportData.get("strengths").toString());
            }
            String strengthsJson = objectMapper.writeValueAsString(strengths);
            
            // 处理改进点列表
            List<String> improvements = new ArrayList<>();
            if (reportData.get("improvements") instanceof List) {
                improvements = (List<String>) reportData.get("improvements");
            } else if (reportData.get("improvements") != null) {
                improvements.add(reportData.get("improvements").toString());
            }
            String improvementsJson = objectMapper.writeValueAsString(improvements);
            
            // 解析新的报告字段
            String techDepthEvaluation = reportData.get("techDepthEvaluation") != null ? reportData.get("techDepthEvaluation").toString() : "";
            String logicExpressionEvaluation = reportData.get("logicExpressionEvaluation") != null ? reportData.get("logicExpressionEvaluation").toString() : "";
            String communicationEvaluation = reportData.get("communicationEvaluation") != null ? reportData.get("communicationEvaluation").toString() : "";
            String answerDepthEvaluation = reportData.get("answerDepthEvaluation") != null ? reportData.get("answerDepthEvaluation").toString() : "";
            String detailedImprovementSuggestions = reportData.get("detailedImprovementSuggestions") != null ? reportData.get("detailedImprovementSuggestions").toString() : "";
            
            // 3. 保存报告到interview_report表
            // 检查是否已存在报告
            Optional<InterviewReport> existingReport = interviewReportRepository.findBySessionId(sessionId);
            if (existingReport.isPresent()) {
                // 更新现有报告
                InterviewReport report = existingReport.get();
                report.setTotalScore(totalScore);
                report.setTechScore(techScore);
                report.setLogicScore(logicScore);
                report.setClarityScore(clarityScore);
                report.setDepthScore(depthScore);
                report.setOverallFeedback(overallFeedback);
                report.setStrengths(strengthsJson);
                report.setImprovements(improvementsJson);
                report.setTechDepthEvaluation(techDepthEvaluation);
                report.setLogicExpressionEvaluation(logicExpressionEvaluation);
                report.setCommunicationEvaluation(communicationEvaluation);
                report.setAnswerDepthEvaluation(answerDepthEvaluation);
                report.setDetailedImprovementSuggestions(detailedImprovementSuggestions);
                interviewReportRepository.save(report);
            } else {
                // 创建新报告
                InterviewReport report = new InterviewReport();
                report.setSessionId(sessionId);
                report.setTotalScore(totalScore);
                report.setTechScore(techScore);
                report.setLogicScore(logicScore);
                report.setClarityScore(clarityScore);
                report.setDepthScore(depthScore);
                report.setOverallFeedback(overallFeedback);
                report.setStrengths(strengthsJson);
                report.setImprovements(improvementsJson);
                report.setTechDepthEvaluation(techDepthEvaluation);
                report.setLogicExpressionEvaluation(logicExpressionEvaluation);
                report.setCommunicationEvaluation(communicationEvaluation);
                report.setAnswerDepthEvaluation(answerDepthEvaluation);
                report.setDetailedImprovementSuggestions(detailedImprovementSuggestions);
                interviewReportRepository.save(report);
            }
            
            // 4. 不再需要更新interview_session表中的总分，评分已迁移到interview_report表
            // sessionRepository.save(session);
            
            // 5. 清空报告缓存
            // 如果使用了缓存机制，可以在这里清空缓存
            
            log.info("保存报告成功，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("保存报告失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存报告失败: " + e.getMessage());
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
     * 自动保存生成的报告到数据库
     * @param sessionId 会话ID
     * @param record 报告生成记录
     */
    private void saveGeneratedReportToDatabase(String sessionId, ReportGenerationService.ReportGenerationRecord record) {
        try {
            // 1. 获取完整报告内容
            StringBuilder fullReport = new StringBuilder();
            for (ReportGenerationService.ReportChunk chunk : record.getChunks()) {
                fullReport.append(chunk.getContent());
            }
            String reportContent = fullReport.toString();
            log.info("自动保存报告，sessionId: {}, 报告内容: {}", sessionId, reportContent);
            
            // 2. 解析Markdown报告
            Map<String, Object> reportData = parseGeneratedReport(reportContent);
            
            // 3. 使用现有的saveReport方法保存到数据库
            saveReport(sessionId, reportData);
            
            log.info("自动保存报告成功，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("自动保存报告到数据库失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 解析生成的Markdown报告
     * @param reportContent 报告内容
     * @return 解析后的报告数据
     */
    private Map<String, Object> parseGeneratedReport(String reportContent) {
        Map<String, Object> reportData = new HashMap<>();
        
        try {
            // 提取总分（从"## 总体评价和总分"部分）
            String overallSection = extractSectionFromReport(reportContent, "## 总体评价和总分", "## 优势分析");
            if (StringUtils.hasText(overallSection)) {
                // 提取总分，查找类似 "总分：85" 或 "总体评分：85" 的模式
                Pattern scorePattern = Pattern.compile("(总分|总体评分)：?(\\d+\\.?\\d*)");
                Matcher scoreMatcher = scorePattern.matcher(overallSection);
                if (scoreMatcher.find()) {
                    double totalScore = Double.parseDouble(scoreMatcher.group(2));
                    reportData.put("totalScore", totalScore);
                }
                
                // 提取总体评价（总分之后的内容）
                String overallFeedback = overallSection;
                reportData.put("overallFeedback", overallFeedback);
            }
            
            // 提取优势列表
            String strengthsSection = extractSectionFromReport(reportContent, "## 优势分析", "## 改进点");
            if (StringUtils.hasText(strengthsSection)) {
                List<String> strengths = new ArrayList<>();
                // 匹配以"- "开头的优势项
                Pattern strengthPattern = Pattern.compile("-\\s+([^\\n]+)", Pattern.MULTILINE);
                Matcher strengthMatcher = strengthPattern.matcher(strengthsSection);
                while (strengthMatcher.find()) {
                    strengths.add(strengthMatcher.group(1).trim());
                }
                reportData.put("strengths", strengths);
            }
            
            // 提取改进点列表
            String improvementsSection = extractSectionFromReport(reportContent, "## 改进点", "##");
            if (StringUtils.hasText(improvementsSection)) {
                List<String> improvements = new ArrayList<>();
                // 匹配以"- "开头的改进项
                Pattern improvementPattern = Pattern.compile("-\\s+([^\\n]+)", Pattern.MULTILINE);
                Matcher improvementMatcher = improvementPattern.matcher(improvementsSection);
                while (improvementMatcher.find()) {
                    improvements.add(improvementMatcher.group(1).trim());
                }
                reportData.put("improvements", improvements);
            }
            
            log.info("解析报告成功，提取的数据: {}", reportData);
        } catch (Exception e) {
            log.error("解析生成的报告失败: {}", e.getMessage(), e);
        }
        
        return reportData;
    }
    
    /**
     * 删除面试记录
     * @param sessionId 会话ID
     */
    @Override
    @Transactional
    public void deleteInterview(String sessionId) {
        try {
            // 删除该会话下的所有面试日志
            logRepository.deleteBySessionId(sessionId);
            
            // 删除该会话下的所有AI跟踪日志
            aiTraceLogRepository.deleteBySessionId(sessionId);
            
            // 删除该会话下的面试报告
            interviewReportRepository.deleteBySessionId(sessionId);
            
            // 最后删除面试会话本身
            sessionRepository.deleteBySessionId(sessionId);
            
            log.info("成功删除面试记录，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("删除面试记录失败，sessionId: {}", sessionId, e);
            throw new RuntimeException("删除面试记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新面试剩余时间
     * @param sessionId 会话ID
     * @param remainingTime 剩余时间（秒）
     * @return 是否更新成功
     */
    @Override
    public boolean updateRemainingTime(String sessionId, Integer remainingTime) {
        try {
            Optional<InterviewSession> sessionOpt = sessionRepository.findBySessionId(sessionId);
            if (sessionOpt.isPresent()) {
                InterviewSession session = sessionOpt.get();
                session.setSessionTimeRemaining(remainingTime);
                sessionRepository.save(session);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}