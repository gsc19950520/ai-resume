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
import com.aicv.airesume.repository.JobTypeRepository;
import com.aicv.airesume.repository.InterviewLogRepository;
import com.aicv.airesume.repository.ResumeRepository;
import com.aicv.airesume.repository.InterviewReportRepository;
import com.aicv.airesume.service.InterviewService;

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
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;

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
    private AiTraceLogRepository aiTraceLogRepository;
    
    @Autowired
    private JobTypeRepository jobTypeRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private DynamicConfigService dynamicConfigService;

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
                
                // 获取完整简历内容
                String resumeContent = getResumeContent(session);
                
                // 创建第一个问题的日志记录
                InterviewLog firstQuestionLog = new InterviewLog();
                firstQuestionLog.setQuestionId(UUID.randomUUID().toString());
                firstQuestionLog.setSessionId(sessionId);
                firstQuestionLog.setRoundNumber(1);
                
                // 保存初始日志记录
                logRepository.save(firstQuestionLog);
                
                // 调用统一的流式生成问题方法，并传递回调函数
                generateQuestionStream(session, "", "", emitter); // 首次问题根据情况选择项目或技术问题
                
                // 获取生成的问题（需要从AI响应中解析，这里简化处理）
                String firstQuestion = "";
                
                // 保存AI生成问题的跟踪日志
                saveAiTraceLog(session.getSessionId(), "generate_question", 
                        "生成第一个问题的prompt内容", firstQuestion);
                
                // 更新会话问题计数
                session.setQuestionCount(1);
    
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
                logRepository.save(currentLog);

                // 3. 计算剩余时间
                session.setSessionTimeRemaining(session.getSessionTimeRemaining() - answerDuration);
                
                // 获取简历内容
                String resumeContent = getResumeContent(session);
            
                
                // 创建下一个问题的日志记录
                InterviewLog nextQuestionLog = new InterviewLog();
                nextQuestionLog.setQuestionId(UUID.randomUUID().toString());
                nextQuestionLog.setSessionId(sessionId);
                nextQuestionLog.setRoundNumber(session.getQuestionCount() + 1);
                
                // 保存新的日志记录
                logRepository.save(nextQuestionLog);
                
                // 生成下一个问题（流式）
                generateQuestionStream(session, currentLog.getQuestionText(), userAnswerText, emitter);
                
                // 7. 更新会话状态（需要从AI响应中获取nextQuestion和stopReason，这里简化处理）
                session.setQuestionCount(session.getQuestionCount() + 1);

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
            if (!ongoingSessions.isEmpty() && !Boolean.TRUE.equals(forceNew)) {
                // 有未完成的面试会话且不强制创建新会话，直接返回最近的一个
                InterviewSession existingSession = ongoingSessions.get(0);
                log.info("用户 {} 有未完成的面试会话，直接返回: {}", userId, existingSession.getSessionId());
                
                return buildInterviewResponse(existingSession, "continue_question", existingSession.getJobName());
            }
            // 2. 获取行业职位标签
            String industryJobTag = getIndustryJobTag(resumeId);
            
            // 3. 创建面试会话 - 优先完成，快速返回给前端
            InterviewSession session = createInterviewSession(userId, resumeId, persona, sessionSeconds, industryJobTag);
            
            // 4. 保存初始会话 - 这是快速返回的关键
            sessionRepository.save(session);

            // 5. 构建并返回响应对象
            return buildInterviewResponse(session, "first_question", industryJobTag);
        } catch (Exception e) {
            // 捕获异常后直接抛出
            log.error("开始面试失败: {}, 详细错误: {}", 
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("面试初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取简历内容的辅助方法
     */
    private String getResumeContent(InterviewSession session) {
        if (StringUtils.hasText(session.getResumeContent())) {
            return session.getResumeContent();
        } else {
            // 如果简历内容不存在，从数据库获取
            Map<String, Object> fullResumeData = resumeService.getResumeFullData(session.getResumeId());
            String resumeContent = convertFullDataToText(fullResumeData);
            // 保存简历内容到会话中
            session.setResumeContent(resumeContent);
            sessionRepository.save(session);
            return resumeContent;
        }
    }
    
    /**
     * 构建面试响应对象
     */
    private InterviewResponseVO buildInterviewResponse(InterviewSession session, String questionType, String industryJobTag) {
        InterviewResponseVO response = new InterviewResponseVO();
        response.setSessionId(session.getSessionId());
        response.setQuestion(null);
        response.setQuestionType(questionType);
        response.setFeedback(null);
        response.setNextQuestion(null);
        response.setIsCompleted(false);
        response.setIndustryJobTag(industryJobTag);
        response.setSessionTimeRemaining(session.getSessionTimeRemaining());
        return response;
    }

    /**
     * 从简历获取行业职位标签
     */
    private String getIndustryJobTag(Long resumeId) {
        try {
            Resume resume = resumeRepository.findById(resumeId).orElse(null);
            if (resume != null && resume.getJobTypeId() != null) {
                Integer jobTypeId = resume.getJobTypeId().intValue();
                log.info("从简历中获取到jobTypeId: {}", jobTypeId);
                
                JobType jobType = jobTypeRepository.findById(jobTypeId).orElse(null);
                if (jobType != null && jobType.getJobName() != null) {
                    return jobType.getJobName();
                }
            } else {
                log.warn("简历中未找到jobTypeId，使用默认值");
            }
        } catch (Exception e) {
            log.error("查询职位信息失败: {}", e.getMessage());
            // 查询失败不影响面试流程，使用默认值
        }
        return "";
    }
    
    /**
     * 创建面试会话对象
     */
    private InterviewSession createInterviewSession(Long userId, Long resumeId, String persona, Integer sessionSeconds, String industryJobTag) {
        InterviewSession session = new InterviewSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setResumeId(resumeId);
        session.setStatus("in_progress");
        session.setJobName(StringUtils.hasText(industryJobTag) ? industryJobTag : "未知职位");
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
        String personaPrompt = String.format("你是%s风格的面试官。%s\n", session.getPersona(), personaStyle);
        session.setPersonaPrompt(personaPrompt);
        
        // 获取完整简历内容并提取结构化信息
        Map<String, Object> fullResumeData = resumeService.getResumeFullData(resumeId);
        // 保存简历内容到会话中
        session.setResumeContent(convertFullDataToText(fullResumeData));

        return session;
    }

    @Override
    public String startReportGeneration(String sessionId, String lastAnswer) {
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
                
                // 2. 处理最后一题：更新或删除
                if (!logs.isEmpty()) {
                    InterviewLog lastLog = logs.get(logs.size() - 1);
                    
                    // 如果有传递最后一题的回答内容
                    if (lastAnswer != null && !lastAnswer.trim().isEmpty()) {
                        log.info("面试时间归零，接收到最后一题回答内容，更新到日志中，sessionId: {}", sessionId);
                        lastLog.setUserAnswerText(lastAnswer.trim());
                        logRepository.save(lastLog);
                    } else {
                        // 如果最后一个问题没有回答内容
                        if (lastLog.getUserAnswerText() == null || lastLog.getUserAnswerText().trim().isEmpty()) {
                            log.info("面试时间归零，最后一题无回答内容，删除最后一个问题，sessionId: {}", sessionId);
                            logRepository.delete(lastLog);
                            logs.remove(logs.size() - 1);
                            // 更新会话的问题计数
                            session.setQuestionCount(session.getQuestionCount() - 1);
                        } else {
                            log.info("面试时间归零，最后一题已有回答内容，保留最后一个问题，sessionId: {}", sessionId);
                        }
                    }
                }

                // 3. 更新会话状态
                session.setStatus("completed");
                session.setEndTime(LocalDateTime.now());
                
                // 4. 保存会话更新
                sessionRepository.save(session);

                // 5. 构建完整的面试会话记录
                StringBuilder sessionContent = new StringBuilder();
                sessionContent.append("面试职位: " + session.getJobName() + "\n");
                sessionContent.append("面试会话记录:\n");
                
                for (InterviewLog log : logs) {
                    sessionContent.append("问题 " + log.getRoundNumber() + ": " + log.getQuestionText() + "\n");
                    sessionContent.append("回答: " + log.getUserAnswerText() + "\n");
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
                        "6. 内容结构：每部分内容保持简洁，重点突出，避免冗长。\n"
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
        
        if (startSection == null || startSection.isEmpty()) {
            return "";
        }
        
        int startIndex = report.indexOf(startSection);
        if (startIndex == -1) {
            return "";
        }
        
        startIndex += startSection.length();
        
        // 如果endSection为null或空，则直接返回从startIndex到报告结尾的内容
        if (endSection == null || endSection.isEmpty()) {
            return report.substring(startIndex).trim();
        }
        
        int endIndex = report.indexOf(endSection, startIndex); // 从startIndex之后开始查找endSection
        if (endIndex == -1 || endIndex < startIndex) {
            return report.substring(startIndex).trim();
        }
        
        return report.substring(startIndex, endIndex).trim();
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

    
    private void generateQuestionStream(InterviewSession session, String previousQuestion, String previousAnswer, SseEmitter emitter) {
        // 构建系统提示词（包含固定的指令和要求）
        StringBuilder systemPromptBuilder = new StringBuilder();
        
        // 从session中获取面试官风格提示词，如果存在则使用，否则重新生成
        try {
            if (StringUtils.hasText(session.getPersonaPrompt())) {
                // 使用session中保存的风格提示词
                systemPromptBuilder.append("\n");
                systemPromptBuilder.append(session.getPersonaPrompt());
            }
        } catch (Exception e) {
            log.error("从session获取或生成personaPrompt失败: {}", e.getMessage());
        }
        
        // 构建用户提示词（包含动态内容和具体问题要求）
        StringBuilder userPromptBuilder = new StringBuilder();
        
        // 根据不同情况构建prompt，完整对话历史由AiServiceUtils的getConversationHistory处理
        if (StringUtils.hasText(previousQuestion) && StringUtils.hasText(previousAnswer)) {
            // 根据用户回答生成有深度的追问或新的相关问题
            userPromptBuilder.append("请基于候选人的回答生成下一个面试题。\n");
            userPromptBuilder.append("如果候选人这个问题回答有效，请务必继续在同一话题上进行深入追问，建立从浅到深的递进关系。\n");
            userPromptBuilder.append("追问应该更加深入、更加专业，挖掘候选人在该技术点上的真实水平。\n");
            userPromptBuilder.append("只有当候选人明显无法回答或者答案质量极差时，才可以转向一个新的话题或知识点，禁止重复之前整个上下文中问过的问题或相似的问题。\n");
        } else {
            userPromptBuilder.append("请直接基于候选人的简历内容生成针对性的面试问题。\n");
            userPromptBuilder.append("问题应该有针对性，考察候选人的实际技术能力。\n");
        }
        userPromptBuilder.append("整句话只能有一个问号。\n");
        String systemPrompt = systemPromptBuilder.toString();
        String userPrompt = userPromptBuilder.toString();
        
        try {
            // 调用AI服务（流式）
            log.info("调用Deepseek API生成问题，systemPrompt: {}, userPrompt: {}", systemPrompt, userPrompt);
            aiServiceUtils.callDeepSeekApiStream(systemPrompt, userPrompt, emitter, () -> {
                                         try {
                                             emitter.send(SseEmitter.event().data("end").name("end").id("2"));
                                             emitter.complete();
                                         } catch (IOException e) {
                                             log.error("发送结束信号失败：{}", e.getMessage(), e);
                                             emitter.completeWithError(e);
                                         }
                                     }, session.getSessionId());
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
            InterviewHistoryVO vo = new InterviewHistoryVO();
            vo.setSessionId(session.getId());
            vo.setUniqueSessionId(session.getSessionId()); // 添加唯一会话ID
            vo.setTitle(session.getJobName());
            
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
            // 从数据库获取所有相关面试日志
            return null;
        } catch (Exception e) {
            log.error("计算薪资范围失败", e);
            throw new RuntimeException("薪资范围计算失败: " + e.getMessage());
        }
    }
    
    @Override
    public InterviewSessionVO getInterviewDetail(String sessionId) {
        InterviewSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(session.getId());
        vo.setSessionId(session.getSessionId());
        vo.setTitle(session.getJobName() + " 面试");
        vo.setDescription("会话ID: " + session.getSessionId());
        vo.setStatus(session.getStatus());
        vo.setPersona(session.getPersona());
        vo.setSessionSeconds(session.getSessionSeconds());
        vo.setSessionTimeRemaining(session.getSessionTimeRemaining());
        vo.setInterviewDuration(session.getSessionSeconds() - session.getSessionTimeRemaining());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setFinishedAt(session.getEndTime());
        
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
        // 获取报告信息
        Optional<InterviewReport> reportOpt = interviewReportRepository.findBySessionId(sessionId);
        
        InterviewReportVO vo = new InterviewReportVO();
        vo.setSessionId(sessionId);
        
        // 如果报告存在，设置报告内容和评分
        if (reportOpt.isPresent()) {
            InterviewReport report = reportOpt.get();
            vo.setTotalScore(report.getTotalScore());
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
            // 1. 提取总分和总体评价
            String overallSection = extractSectionFromReport(reportContent, "## 总体评价和总分", "## 优势分析");
            if (StringUtils.hasText(overallSection)) {
                // 提取总分，查找类似 "总分：85" 或 "总体评分：85" 的模式
                Pattern scorePattern = Pattern.compile("(总分|总体评分)：?(\\d+\\.?\\d*)");
                Matcher scoreMatcher = scorePattern.matcher(overallSection);
                if (scoreMatcher.find()) {
                    double totalScore = Double.parseDouble(scoreMatcher.group(2));
                    reportData.put("totalScore", totalScore);
                }
                
                // 提取总体评价
                reportData.put("overallFeedback", overallSection);
            } else {
                // 设置默认值
                reportData.put("overallFeedback", "**总分：50/100**。候选人表现一般，需要进一步评估。");
                reportData.put("totalScore", 50.0);
            }
            
            // 2. 提取优势列表
            String strengthsSection = extractSectionFromReport(reportContent, "## 优势分析", "## 改进点");
            List<String> strengths = new ArrayList<>();
            if (StringUtils.hasText(strengthsSection)) {
                // 匹配以"- "开头的优势项
                Pattern strengthPattern = Pattern.compile("-\\s+([^\\n]+)", Pattern.MULTILINE);
                Matcher strengthMatcher = strengthPattern.matcher(strengthsSection);
                while (strengthMatcher.find()) {
                    strengths.add(strengthMatcher.group(1).trim());
                }
                // 如果没有找到列表项，直接使用整个章节内容
                if (strengths.isEmpty()) {
                    strengths.add(strengthsSection.trim());
                }
            }
            
            // 确保优势列表不为空
            if (strengths.isEmpty()) {
                strengths.add("有一定的学习意愿和态度。");
            }
            reportData.put("strengths", strengths);
            
            // 3. 提取改进点列表
            String improvementsSection = extractSectionFromReport(reportContent, "## 改进点", "## 技术深度评价");
            List<String> improvements = new ArrayList<>();
            if (StringUtils.hasText(improvementsSection)) {
                // 匹配以"- "开头的改进项
                Pattern improvementPattern = Pattern.compile("-\\s+([^\\n]+)", Pattern.MULTILINE);
                Matcher improvementMatcher = improvementPattern.matcher(improvementsSection);
                while (improvementMatcher.find()) {
                    improvements.add(improvementMatcher.group(1).trim());
                }
                // 如果没有找到列表项，直接使用整个章节内容
                if (improvements.isEmpty()) {
                    improvements.add(improvementsSection.trim());
                }
            }
            
            // 确保改进点列表不为空
            if (improvements.isEmpty()) {
                improvements.add("技术知识需要进一步系统化学习。");
            }
            reportData.put("improvements", improvements);
            
            // 4. 提取技术深度评价并设置默认值
            String techDepthEvaluationSection = extractSectionFromReport(reportContent, "## 技术深度评价", "## 逻辑表达评价");
            if (StringUtils.hasText(techDepthEvaluationSection)) {
                reportData.put("techDepthEvaluation", techDepthEvaluationSection);
            } else {
                reportData.put("techDepthEvaluation", "**技术深度一般**。候选人对部分技术有基本了解，但缺乏深入的原理理解和实践经验。");
            }
            
            // 5. 提取逻辑表达评价并设置默认值
            String logicExpressionEvaluationSection = extractSectionFromReport(reportContent, "## 逻辑表达评价", "## 沟通表达评价");
            if (StringUtils.hasText(logicExpressionEvaluationSection)) {
                reportData.put("logicExpressionEvaluation", logicExpressionEvaluationSection);
            } else {
                reportData.put("logicExpressionEvaluation", "**逻辑表达基本清晰**。候选人能够组织基本的技术阐述，但在复杂问题上逻辑性有待加强。");
            }
            
            // 6. 提取沟通表达评价并设置默认值
            String communicationEvaluationSection = extractSectionFromReport(reportContent, "## 沟通表达评价", "## 回答深度评价");
            if (StringUtils.hasText(communicationEvaluationSection)) {
                reportData.put("communicationEvaluation", communicationEvaluationSection);
            } else {
                reportData.put("communicationEvaluation", "**沟通表达能力一般**。候选人能够基本表达自己的观点，但在主动沟通和澄清问题方面有待提高。");
            }
            
            // 7. 提取回答深度评价并设置默认值
            String answerDepthEvaluationSection = extractSectionFromReport(reportContent, "## 回答深度评价", "## 针对候选人的详细改进建议");
            if (StringUtils.hasText(answerDepthEvaluationSection)) {
                reportData.put("answerDepthEvaluation", answerDepthEvaluationSection);
            } else {
                reportData.put("answerDepthEvaluation", "**回答深度中等**。候选人的回答能够覆盖基本知识点，但缺乏深入的分析和拓展。");
            }
            
            // 8. 提取针对候选人的详细改进建议并设置默认值
            String detailedImprovementSuggestionsSection = extractSectionFromReport(reportContent, "## 针对候选人的详细改进建议", null);
            if (StringUtils.hasText(detailedImprovementSuggestionsSection)) {
                reportData.put("detailedImprovementSuggestions", detailedImprovementSuggestionsSection);
            } else {
                reportData.put("detailedImprovementSuggestions", "1. **系统学习**：建议候选人系统学习核心技术栈的基础理论和实践。\n2. **项目实践**：通过实际项目积累经验，加深对技术的理解。\n3. **深入思考**：面对技术问题时，不仅要知道怎么做，还要理解为什么这么做。");
            }
            
            log.info("解析报告成功，提取的数据: {}", reportData);
        } catch (Exception e) {
            log.error("解析生成的报告失败: {}", e.getMessage(), e);
            
            // 异常情况下设置所有字段的默认值
            reportData.put("overallFeedback", "**总分：50/100**。由于报告解析失败，提供默认评价。");
            reportData.put("totalScore", 50.0);
            
            List<String> defaultStrengths = new ArrayList<>();
            defaultStrengths.add("有一定的学习意愿和态度。");
            reportData.put("strengths", defaultStrengths);
            
            List<String> defaultImprovements = new ArrayList<>();
            defaultImprovements.add("技术知识需要进一步系统化学习。");
            reportData.put("improvements", defaultImprovements);
            
            reportData.put("techDepthEvaluation", "**技术深度一般**。候选人对部分技术有基本了解，但缺乏深入的原理理解和实践经验。");
            reportData.put("logicExpressionEvaluation", "**逻辑表达基本清晰**。候选人能够组织基本的技术阐述，但在复杂问题上逻辑性有待加强。");
            reportData.put("communicationEvaluation", "**沟通表达能力一般**。候选人能够基本表达自己的观点，但在主动沟通和澄清问题方面有待提高。");
            reportData.put("answerDepthEvaluation", "**回答深度中等**。候选人的回答能够覆盖基本知识点，但缺乏深入的分析和拓展。");
            reportData.put("detailedImprovementSuggestions", "1. **系统学习**：建议候选人系统学习核心技术栈的基础理论和实践。\n2. **项目实践**：通过实际项目积累经验，加深对技术的理解。\n3. **深入思考**：面对技术问题时，不仅要知道怎么做，还要理解为什么这么做。");
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