package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.GrowthReport;
import com.aicv.airesume.entity.InterviewReport;
import com.aicv.airesume.entity.InterviewSession;
import com.aicv.airesume.entity.User;
import com.aicv.airesume.model.vo.GrowthReportVO;
import com.aicv.airesume.repository.GrowthReportRepository;
import com.aicv.airesume.repository.InterviewReportRepository;
import com.aicv.airesume.repository.InterviewSessionRepository;
import com.aicv.airesume.repository.UserRepository;
import com.aicv.airesume.service.GrowthReportService;
import com.aicv.airesume.service.ReportGenerationService;
import com.aicv.airesume.utils.AiServiceUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI成长报告服务实现类
 */
@Service
@Slf4j
public class GrowthReportServiceImpl implements GrowthReportService {
    // DeepSeek API 提示词常量 - 优化版：增加个性化信息、上下文和更明确的要求
    private static final String RECOMMEND_JOBS_PROMPT_START = "请基于用户的面试表现、技能优势、改进空间、职业背景和面试得分趋势，推荐3个最适合的职业方向。\n" +
            "请结合用户的实际面试经历、得分变化趋势、最高得分领域和薄弱环节，确保推荐的职业方向与用户的成长轨迹和能力特征高度匹配。\n" +
            "请考虑用户的面试目标岗位、行业偏好和技能发展潜力，提供精准的职业发展建议。\n";
    private static final String RECOMMEND_JOBS_PROMPT_END = "\n请为每个推荐的职业方向提供：\n" +
            "1. 职业名称：具体明确（如'高级Java后端工程师'而非'后端工程师'）\n" +
            "2. 匹配理由：基于用户的面试表现和技能特点，30字以内\n" +
            "3. 匹配分数：0-100分，精确反映匹配度\n" +
            "格式要求：JSON格式，包含recommendations数组，每个元素包含jobName、matchReason、matchScore字段。";
    
    private static final String GROWTH_PLANS_PROMPT_START = "请基于用户的面试表现、当前技能水平、优势领域、改进空间和得分趋势，制定高度个性化的成长规划。\n" +
            "规划应包含短期（1-3个月）、中期（3-6个月）和长期（6-12个月）三个阶段，每个阶段都要有明确的发展目标和行动步骤。\n" +
            "请结合用户的实际面试得分、薄弱环节改进趋势、行业发展需求和职业目标，确保规划具有高度可操作性和针对性。\n" +
            "短期规划应聚焦于快速提升薄弱环节，中期规划应注重技能体系构建，长期规划应关注职业发展和晋升路径。\n";
    private static final String GROWTH_PLANS_PROMPT_END = "\n请为每个时间阶段提供：\n" +
            "1. 目标：3-5条具体可量化的成长目标\n" +
            "2. 行动步骤：3-5条详细、可执行的具体行动\n" +
            "3. 预期成果：清晰描述每个阶段结束时的预期效果\n" +
            "格式要求：JSON格式，包含plans数组，每个元素包含timeFrame（'短期'/'中期'/'长期'）、goals数组、actionSteps数组、expectedResults数组。";
    
    private static final String AI_SUGGESTIONS_PROMPT_START = "请基于用户的面试表现分析、技能得分趋势、优势领域、改进空间和职业目标，生成高度个性化的AI建议。\n" +
            "建议应包含以下部分：\n" +
            "1. 一条简短而精准的推荐职业方向（20-30字），基于用户的最高得分领域和技能优势\n" +
            "2. 近期目标（3-6个月）的3条具体行动建议，聚焦于快速提升薄弱环节和核心技能\n" +
            "3. 中期目标（6-12个月）的3条具体行动建议，聚焦于巩固优势、拓展技能广度和深度\n" +
            "4. 长期目标（1-3年）的3条具体行动建议，聚焦于职业发展、晋升路径和行业影响力\n" +
            "请结合用户的实际面试得分、薄弱环节改进趋势、行业发展需求和职业目标，确保建议具有极强的针对性和可操作性。\n";
    private static final String AI_SUGGESTIONS_PROMPT_END = "\n建议要求：\n" +
            "1. 每条建议30-50字，具体明确，避免模糊表述\n" +
            "2. 结合用户的面试表现数据，提供针对性的指导\n" +
            "3. 重点关注用户的改进空间，帮助其快速提升\n" +
            "格式要求：每行一条，按照推荐方向、近期目标1-3、中期目标1-3、长期目标1-3的顺序排列。";

    @Autowired
    private GrowthReportRepository growthReportRepository;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private InterviewReportRepository interviewReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiServiceUtils aiServiceUtils;
    
    // 报告生成服务，用于流式生成报告内容
    @Autowired
    private ReportGenerationService reportGenerationService;

    /**
     * 获取或生成成长报告的主方法，包含条件判断逻辑
     * 1. 检查用户是否有至少两次面试记录
     * 2. 检查是否有新的面试记录
     * 3. 如果没有新记录且有报告，则直接返回现有报告
     * 4. 如果没有新记录且没有报告，则生成新报告
     * 5. 如果有新记录，则重新生成报告并替换原数据
     */
    public String getOrGenerateGrowthReport(Long userId, SseEmitter emitter) {
        try {
            // 1. 发送初始化进度信息
            emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 0, \"stage\": \"初始化报告生成...\"}"));
            
            // 2. 获取用户信息
            emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 5, \"stage\": \"获取用户信息...\"}"));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
            
            // 3. 获取用户所有面试会话
            emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 10, \"stage\": \"收集面试记录...\"}"));
            List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 12, \"stage\": \"验证面试次数...\"}"));
            if (sessions.size() < 2) {
                // 发送提示：需要至少两次面试
                emitter.send(SseEmitter.event().name("interview-count-error").data("需要至少2次完整的面试记录才能生成成长报告"));
                emitter.complete();
                return null;
            }
            
            // 4. 获取用户最新的面试日期
            emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 13, \"stage\": \"分析面试数据...\"}"));
            LocalDateTime latestInterviewDate = sessions.get(0).getCreatedAt();
            
            // 5. 检查是否有现有报告
            emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 15, \"stage\": \"检查已有报告...\"}"));
            Optional<GrowthReport> existingReportOpt = growthReportRepository.findFirstByUserIdOrderByGeneratedAtDesc(userId);
            
            if (existingReportOpt.isPresent()) {
                GrowthReport existingReport = existingReportOpt.get();
                
                // 6. 检查是否有新的面试记录
                if (latestInterviewDate.isAfter(existingReport.getLastInterviewDate())) {
                    // 有新的面试记录，需要重新生成报告
                    emitter.send(SseEmitter.event().name("new-interview-found").data("发现新的面试记录，正在重新生成成长报告..."));
                    
                    // 删除旧报告
                    growthReportRepository.delete(existingReport);
                    
                    // 生成新报告
                    return generateGrowthReportStream(userId, emitter);
                } else {
                    // 没有新的面试记录，直接返回现有报告
                    emitter.send(SseEmitter.event().name("report-found").data("使用已有的成长报告..."));
                    emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 80, \"stage\": \"加载现有报告...\"}"));
                    
                    // 将现有报告转换为VO并流式返回
                    GrowthReportVO reportVO = regenerateGrowthReport(existingReport);
                    emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 100, \"stage\": \"报告加载完成！\"}"));
                    emitter.send(SseEmitter.event().name("report-content").data(reportVO));
                    emitter.complete();
                    return "existing_" + existingReport.getId();
                }
            } else {
                // 没有现有报告，需要生成新报告
                emitter.send(SseEmitter.event().name("no-report-found").data("未找到成长报告，正在生成..."));
                return generateGrowthReportStream(userId, emitter);
            }
        } catch (Exception e) {
            log.error("获取或生成成长报告失败", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("获取或生成成长报告失败: " + e.getMessage()));
            } catch (IOException ignored) {}
            emitter.completeWithError(e);
            return null;
        }
    }
    
    /**
     * 异步流式生成用户的AI成长报告
     */
    @Override
    public String generateGrowthReportStream(Long userId, SseEmitter emitter) {
        // 生成报告ID
        String reportId = UUID.randomUUID().toString();
        
        // 创建报告生成记录
        ReportGenerationService.ReportGenerationRecord record = reportGenerationService.createReportRecord(reportId);
        
        // 异步处理报告生成
        CompletableFuture.runAsync(() -> {
            try {
                // 发送初始进度
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 20, \"stage\": \"初始化报告生成...\"}"));
            } catch (IOException ignored) {}
            try {
                // 获取用户信息
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 25, \"stage\": \"获取用户信息...\"}"));
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
                
                // 获取用户最近的面试会话和报告
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 30, \"stage\": \"获取面试会话和报告...\"}"));
                List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
                // 反转列表以获得正序
                Collections.reverse(sessions);
                List<Map<String, Object>> sessionReportPairs = new ArrayList<>();
                
                // 构建会话和报告的映射关系
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 35, \"stage\": \"构建会话和报告映射...\"}"));
                for (InterviewSession session : sessions) {
                    Optional<InterviewReport> reportOpt = interviewReportRepository.findBySessionId(session.getSessionId());
                    if (reportOpt.isPresent()) {
                        Map<String, Object> pair = new HashMap<>();
                        pair.put("session", session);
                        pair.put("report", reportOpt.get());
                        sessionReportPairs.add(pair);
                    }
                }
                
                if (sessionReportPairs.size() < 2) {
                    throw new IllegalArgumentException("需要至少2次完整的面试记录才能生成成长报告");
                }
                
                // 生成概览信息并流式输出
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 40, \"stage\": \"提取面试特征...\"}"));
                GrowthReportVO.OverviewVO overview = generateOverview(user, sessionReportPairs, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String overviewJson = objectMapper.writeValueAsString(overview);
                String overviewChunk = "概览信息：" + overviewJson;
                record.addChunk(overviewChunk);
                emitter.send(SseEmitter.event().name("report").data(overviewChunk));
                
                // 生成得分趋势、能力成长、优势和改进点
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 45, \"stage\": \"分析成长数据...\"}"));
                List<GrowthReportVO.ScoreTrendItemVO> scoreTrend = generateScoreTrend(sessionReportPairs, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth = generateAbilityGrowth(scoreTrend);
                List<GrowthReportVO.StrengthVO> strengths = generateStrengths(sessionReportPairs);
                List<GrowthReportVO.ImprovementVO> improvements = generateImprovements(sessionReportPairs);
                
                // 准备AI调用的prompts列表
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 50, \"stage\": \"准备AI分析...\"}"));
                List<String> prompts = new ArrayList<>();
                
                // 生成AI建议的prompt
                StringBuilder suggestionsPrompt = new StringBuilder();
                suggestionsPrompt.append(AI_SUGGESTIONS_PROMPT_START);
                suggestionsPrompt.append("\n用户在以下方面需要改进：\n");
                improvements.forEach(improvement -> {
                    suggestionsPrompt.append("- " + improvement.getArea() + "（改进进度：" + improvement.getProgress() + "%）\n");
                });
                suggestionsPrompt.append(AI_SUGGESTIONS_PROMPT_END);
                prompts.add(suggestionsPrompt.toString());
                
                // 生成职业推荐的prompt
                StringBuilder jobsPrompt = new StringBuilder();
                jobsPrompt.append(RECOMMEND_JOBS_PROMPT_START);
                jobsPrompt.append("用户基本信息：姓名：").append(user.getName() != null ? user.getName() : "未知").append("，");
                jobsPrompt.append("用户优势：");
                strengths.forEach(strength -> {
                    jobsPrompt.append("\"").append(strength.getSkill()).append("\" (").append(strength.getAnalysis()).append(")，");
                });
                jobsPrompt.append("需要改进：");
                improvements.forEach(improvement -> {
                    jobsPrompt.append("\"").append(improvement.getArea()).append("\"，");
                });
                jobsPrompt.append(RECOMMEND_JOBS_PROMPT_END);
                prompts.add(jobsPrompt.toString());
                
                // 生成成长规划的prompt
                StringBuilder plansPrompt = new StringBuilder();
                plansPrompt.append(GROWTH_PLANS_PROMPT_START);
                plansPrompt.append("用户优势：");
                strengths.forEach(strength -> {
                    plansPrompt.append("\"").append(strength.getSkill()).append("\"，");
                });
                plansPrompt.append("需要改进：");
                improvements.forEach(improvement -> {
                    plansPrompt.append("\"").append(improvement.getArea()).append("\"，");
                });
                plansPrompt.append(GROWTH_PLANS_PROMPT_END);
                prompts.add(plansPrompt.toString());
                
                // 顺序调用DeepSeek API - 更细粒度的进度更新
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 55, \"stage\": \"AI分析 - 生成个性化建议...\"}"));
                emitter.send(SseEmitter.event().name("ai-start").data("{\"total\": " + prompts.size() + "}"));
                aiServiceUtils.callDeepSeekApiStreamSequential("", prompts, emitter, null);
                
                // 生成完整报告并保存 - 更细粒度的进度更新
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 70, \"stage\": \"构建报告结构...\"}"));
                GrowthReport report = new GrowthReport();
                report.setUserId(userId);
                report.setUserName(user.getName());
                
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 72, \"stage\": \"填充报告内容...\"}"));
                // 设置概览信息字段
                report.setLatestJobName(overview.getLatestJobName());
                report.setTimeRange(overview.getTimeRange());
                report.setFirstTotalScore(overview.getFirstTotalScore());
                report.setLatestTotalScore(overview.getLatestTotalScore());
                report.setAverageScore(overview.getAverageScore());
                report.setImprovementRate(overview.getImprovementRate());
                
                report.setInterviewCount(sessionReportPairs.size());
                report.setStartDate(sessions.get(0).getCreatedAt());
                report.setEndDate(sessions.get(sessions.size() - 1).getCreatedAt());
                report.setLastInterviewDate(sessions.get(sessions.size() - 1).getCreatedAt());
                
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 75, \"stage\": \"组合报告内容...\"}"));
                // 组合报告内容
                StringBuilder fullReport = new StringBuilder();
                for (ReportGenerationService.ReportChunk chunk : record.getChunks()) {
                    fullReport.append(chunk.getContent());
                }
                
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 78, \"stage\": \"数据序列化...\"}"));
                // 将复杂数据转换为JSON并存储
                try {
                    report.setScoreTrendJson(objectMapper.writeValueAsString(scoreTrend));
                    report.setAbilityGrowthJson(objectMapper.writeValueAsString(abilityGrowth));
                    report.setStrengthsJson(objectMapper.writeValueAsString(strengths));
                    report.setImprovementsJson(objectMapper.writeValueAsString(improvements));
                } catch (Exception e) {
                    log.error("JSON序列化失败: {}", e.getMessage(), e);
                }
                
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 85, \"stage\": \"格式化报告内容...\"}"));
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 90, \"stage\": \"保存报告到数据库...\"}"));
                growthReportRepository.save(report);
                
                // 标记报告生成完成
                record.setStatus(ReportGenerationService.ReportStatus.COMPLETED);
                emitter.send(SseEmitter.event().name("progress").data("{\"percentage\": 100, \"stage\": \"报告生成完成！\"}"));
                
            } catch (Exception e) {
                log.error("生成成长报告失败", e);
                record.setStatus(ReportGenerationService.ReportStatus.FAILED);
                record.setErrorMessage("生成成长报告失败: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });
        
        return reportId;
    }
    
    /**
     * 生成职业推荐方向
     */
    private List<GrowthReportVO.RecommendedJobVO> generateRecommendedJobs(User user, List<Map<String, Object>> sessionReportPairs, 
                                                                         List<GrowthReportVO.StrengthVO> strengths, 
                                                                         List<GrowthReportVO.ImprovementVO> improvements) {
        List<GrowthReportVO.RecommendedJobVO> recommendations = new ArrayList<>();
        
        try {
            // 准备提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append(RECOMMEND_JOBS_PROMPT_START);
            prompt.append("用户基本信息：姓名：").append(user.getName() != null ? user.getName() : "未知").append("，");
            
            // 添加用户的优势
            prompt.append("用户优势：");
            strengths.forEach(strength -> {
                prompt.append("\"").append(strength.getSkill()).append("\" (").append(strength.getAnalysis()).append(")，");
            });
            
            // 添加需要改进的方面
            prompt.append("需要改进：");
            improvements.forEach(improvement -> {
                prompt.append("\"").append(improvement.getArea()).append("\"，");
            });
            
            // 添加面试经历
            prompt.append("最近面试：");
            for (int i = 0; i < Math.min(3, sessionReportPairs.size()); i++) {
                InterviewSession session = (InterviewSession) sessionReportPairs.get(sessionReportPairs.size() - 1 - i).get("session");
                prompt.append(session.getJobName()).append("，");
            }
            
            prompt.append(RECOMMEND_JOBS_PROMPT_END);
            
            // 调用DeepSeek API
            String response = aiServiceUtils.callDeepSeekApi(prompt.toString());
            
            // 清理响应，移除代码块标记
            if (response != null) {
                response = response.trim();
                // 移除 ```json 和 ``` 标记
                if (response.startsWith("```json")) {
                    response = response.substring(7);
                }
                if (response.endsWith("```")) {
                    response = response.substring(0, response.length() - 3);
                }
                response = response.trim();
            }
            
            // 解析响应
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode recommendationsNode = rootNode.path("recommendations");
            
            if (recommendationsNode.isArray()) {
                for (JsonNode node : recommendationsNode) {
                    GrowthReportVO.RecommendedJobVO job = new GrowthReportVO.RecommendedJobVO();
                    job.setJobName(node.path("jobName").asText());
                    job.setMatchReason(node.path("matchReason").asText());
                    job.setMatchScore(node.path("matchScore").asInt());
                    recommendations.add(job);
                }
            }
        } catch (Exception e) {
            log.error("生成职业推荐失败: {}", e.getMessage(), e);
            // 添加默认推荐
            GrowthReportVO.RecommendedJobVO defaultJob1 = new GrowthReportVO.RecommendedJobVO();
            defaultJob1.setJobName("高级软件工程师");
            defaultJob1.setMatchReason("技术能力强，逻辑清晰");
            defaultJob1.setMatchScore(85);
            recommendations.add(defaultJob1);
        }
        
        return recommendations;
    }
    
    /**
     * 生成未来成长规划
     */
    private List<GrowthReportVO.GrowthPlanVO> generateGrowthPlans(User user, List<Map<String, Object>> sessionReportPairs, 
                                                                 List<GrowthReportVO.StrengthVO> strengths, 
                                                                 List<GrowthReportVO.ImprovementVO> improvements) {
        List<GrowthReportVO.GrowthPlanVO> plans = new ArrayList<>();
        
        try {
            // 准备提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append(GROWTH_PLANS_PROMPT_START);
            prompt.append("用户优势：");
            strengths.forEach(strength -> {
                prompt.append("\"").append(strength.getSkill()).append("\"，");
            });
            
            prompt.append("需要改进：");
            improvements.forEach(improvement -> {
                prompt.append("\"").append(improvement.getArea()).append("\"，");
            });
            
            prompt.append(GROWTH_PLANS_PROMPT_END);
            
            // 调用DeepSeek API
            String response = aiServiceUtils.callDeepSeekApi(prompt.toString());
            
            // 解析响应
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode plansNode = rootNode.path("plans");
            
            if (plansNode.isArray()) {
                for (JsonNode node : plansNode) {
                    GrowthReportVO.GrowthPlanVO plan = new GrowthReportVO.GrowthPlanVO();
                    plan.setTimeFrame(node.path("timeFrame").asText());
                    
                    List<String> goals = new ArrayList<>();
                    JsonNode goalsNode = node.path("goals");
                    if (goalsNode.isArray()) {
                        for (JsonNode goalNode : goalsNode) {
                            goals.add(goalNode.asText());
                        }
                    }
                    plan.setGoals(goals);
                    
                    List<String> actionSteps = new ArrayList<>();
                    JsonNode stepsNode = node.path("actionSteps");
                    if (stepsNode.isArray()) {
                        for (JsonNode stepNode : stepsNode) {
                            actionSteps.add(stepNode.asText());
                        }
                    }
                    plan.setActionSteps(actionSteps);
                    plans.add(plan);
                }
            }
        } catch (Exception e) {
            log.error("生成成长规划失败: {}", e.getMessage(), e);
            // 添加默认规划
            GrowthReportVO.GrowthPlanVO defaultPlan = new GrowthReportVO.GrowthPlanVO();
            defaultPlan.setTimeFrame("短期");
            List<String> defaultGoals = Arrays.asList("提升技术深度", "增强沟通能力");
            List<String> defaultSteps = Arrays.asList("每日学习新技术1小时", "练习面试表达");
            defaultPlan.setGoals(defaultGoals);
            defaultPlan.setActionSteps(defaultSteps);
            plans.add(defaultPlan);
        }
        
        return plans;
    }
    
    @Override
    public GrowthReportVO generateGrowthReport(Long userId) {
        try {
            // 获取用户信息
            Optional<User> userOptional = userRepository.findById(userId);
            if (!userOptional.isPresent()) {
                throw new IllegalArgumentException("用户不存在");
            }
            User user = userOptional.get();

            // 获取用户的所有面试会话（按时间顺序）
            List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
            // 反转列表以获得正序
            Collections.reverse(sessions);
            if (sessions.size() < 2) {
                throw new IllegalArgumentException("需要至少2次面试记录才能生成成长报告");
            }

            // 为每个会话获取对应的面试报告
            List<Map<String, Object>> sessionReportPairs = new ArrayList<>();
            for (InterviewSession session : sessions) {
                Optional<InterviewReport> reportOptional = interviewReportRepository.findBySessionId(session.getSessionId());
                if (reportOptional.isPresent()) {
                    Map<String, Object> pair = new HashMap<>();
                    pair.put("session", session);
                    pair.put("report", reportOptional.get());
                    sessionReportPairs.add(pair);
                }
            }

            if (sessionReportPairs.size() < 2) {
                throw new IllegalArgumentException("需要至少2次完整的面试记录才能生成成长报告");
            }

            // 生成报告内容
            GrowthReportVO.ReportContentVO reportContent = generateReportContent(user, sessionReportPairs);

            // 创建并保存成长报告
        GrowthReport growthReport = new GrowthReport();
        growthReport.setUserId(userId);
        
        // 设置概览信息字段
        GrowthReportVO.OverviewVO overview = reportContent.getOverview();
        growthReport.setUserName(overview.getUserName());
        growthReport.setLatestJobName(overview.getLatestJobName());
        growthReport.setTimeRange(overview.getTimeRange());
        growthReport.setFirstTotalScore(overview.getFirstTotalScore());
        growthReport.setLatestTotalScore(overview.getLatestTotalScore());
        growthReport.setAverageScore(overview.getAverageScore());
        growthReport.setImprovementRate(overview.getImprovementRate());
        
        growthReport.setInterviewCount(sessionReportPairs.size());
        growthReport.setStartDate(sessions.get(0).getCreatedAt());
        growthReport.setEndDate(sessions.get(sessions.size() - 1).getCreatedAt());
        growthReport.setLastInterviewDate(sessions.get(sessions.size() - 1).getCreatedAt());
        
        // 将复杂数据转换为JSON并存储
        try {
            growthReport.setScoreTrendJson(objectMapper.writeValueAsString(reportContent.getScoreTrend()));
            growthReport.setAbilityGrowthJson(objectMapper.writeValueAsString(reportContent.getAbilityGrowth()));
            growthReport.setStrengthsJson(objectMapper.writeValueAsString(reportContent.getStrengths()));
            growthReport.setImprovementsJson(objectMapper.writeValueAsString(reportContent.getImprovements()));
            growthReport.setAiSuggestionsJson(objectMapper.writeValueAsString(reportContent.getAiSuggestions()));
            growthReport.setVisualizationDataJson(objectMapper.writeValueAsString(reportContent.getVisualizationData()));
            // 使用现有的aiSuggestionsJson字段存储AI建议
            growthReport.setAiSuggestionsJson(objectMapper.writeValueAsString(reportContent.getAiSuggestions()));
        } catch (Exception e) {
            log.error("JSON序列化失败: {}", e.getMessage(), e);
        }
        
        GrowthReport savedReport = growthReportRepository.save(growthReport);

            // 转换为VO并返回
            return convertToVO(savedReport, reportContent);
        } catch (Exception e) {
            log.error("生成成长报告失败: userId={}", userId, e);
            throw new RuntimeException("生成成长报告失败: " + e.getMessage());
        }
    }

    private GrowthReportVO.ReportContentVO generateReportContent(User user, List<Map<String, Object>> sessionReportPairs) {
        GrowthReportVO.ReportContentVO content = new GrowthReportVO.ReportContentVO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 生成整体概览
        GrowthReportVO.OverviewVO overview = generateOverview(user, sessionReportPairs, formatter);
        content.setOverview(overview);

        // 生成得分趋势
        List<GrowthReportVO.ScoreTrendItemVO> scoreTrend = generateScoreTrend(sessionReportPairs, formatter);
        content.setScoreTrend(scoreTrend);

        // 生成能力成长分析
        Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth = generateAbilityGrowth(scoreTrend);
        content.setAbilityGrowth(abilityGrowth);

        // 生成优势分析
        List<GrowthReportVO.StrengthVO> strengths = generateStrengths(sessionReportPairs);
        content.setStrengths(strengths);

        // 生成改进点分析
        List<GrowthReportVO.ImprovementVO> improvements = generateImprovements(sessionReportPairs);
        content.setImprovements(improvements);

        // 生成AI建议
        List<String> aiSuggestions = generateAISuggestions(abilityGrowth, improvements);
        content.setAiSuggestions(aiSuggestions);

        // 生成可视化数据
        Map<String, Object> visualizationData = generateVisualizationData(scoreTrend, abilityGrowth);
        content.setVisualizationData(visualizationData);
        
        // 生成职业推荐方向
        List<GrowthReportVO.RecommendedJobVO> recommendedJobs = generateRecommendedJobs(user, sessionReportPairs, strengths, improvements);
        content.setRecommendedJobs(recommendedJobs);
        
        // 生成未来成长规划
        List<GrowthReportVO.GrowthPlanVO> growthPlans = generateGrowthPlans(user, sessionReportPairs, strengths, improvements);
        content.setGrowthPlans(growthPlans);

        return content;
    }

    private GrowthReportVO.OverviewVO generateOverview(User user, List<Map<String, Object>> sessionReportPairs, DateTimeFormatter formatter) {
        GrowthReportVO.OverviewVO overview = new GrowthReportVO.OverviewVO();

        // 获取第一个和最后一个会话
        InterviewSession firstSession = (InterviewSession) sessionReportPairs.get(0).get("session");
        InterviewSession lastSession = (InterviewSession) sessionReportPairs.get(sessionReportPairs.size() - 1).get("session");
        InterviewReport firstReport = (InterviewReport) sessionReportPairs.get(0).get("report");
        InterviewReport lastReport = (InterviewReport) sessionReportPairs.get(sessionReportPairs.size() - 1).get("report");

        // 设置基本信息
        overview.setUserName(user.getName() != null ? user.getName() : user.getNickname() != null ? user.getNickname() : "用户");
        overview.setLatestJobName(lastSession.getJobName());
        overview.setInterviewCount(sessionReportPairs.size());
        overview.setTimeRange(firstSession.getCreatedAt().format(formatter) + " 至 " + lastSession.getCreatedAt().format(formatter));

        // 计算得分相关信息
        double firstScore = firstReport.getTotalScore();
        double latestScore = lastReport.getTotalScore();
        double averageScore = sessionReportPairs.stream()
                .mapToDouble(pair -> ((InterviewReport) pair.get("report")).getTotalScore())
                .average()
                .orElse(0);
        double improvementRate = firstScore > 0 ? ((latestScore - firstScore) / firstScore) * 100 : 0;

        overview.setFirstTotalScore(firstScore);
        overview.setLatestTotalScore(latestScore);
        overview.setAverageScore(roundToTwoDecimalPlaces(averageScore));
        overview.setImprovementRate(roundToTwoDecimalPlaces(improvementRate));

        return overview;
    }

    private List<GrowthReportVO.ScoreTrendItemVO> generateScoreTrend(List<Map<String, Object>> sessionReportPairs, DateTimeFormatter formatter) {
        return sessionReportPairs.stream().map(pair -> {
            InterviewSession session = (InterviewSession) pair.get("session");
            InterviewReport report = (InterviewReport) pair.get("report");

            GrowthReportVO.ScoreTrendItemVO item = new GrowthReportVO.ScoreTrendItemVO();
            item.setSessionId(session.getSessionId());
            item.setDate(session.getCreatedAt().format(formatter));
            item.setTotalScore(report.getTotalScore());
            item.setJobName(session.getJobName());

            // 这里简化处理，实际项目中可能需要从报告内容中提取各维度得分
            // 只保留总分、技术、深度三项
            item.setTechDepthScore(estimateScoreFromText(report.getTechDepthEvaluation()));
            item.setAnswerDepthScore(estimateScoreFromText(report.getAnswerDepthEvaluation()));
            // 已移除: 逻辑表达和沟通能力评分

            return item;
        }).collect(Collectors.toList());
    }

    private Map<String, GrowthReportVO.AbilityGrowthVO> generateAbilityGrowth(List<GrowthReportVO.ScoreTrendItemVO> scoreTrend) {
        Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth = new HashMap<>();

        if (scoreTrend.size() < 2) return abilityGrowth;

        // 分析各个能力维度 - 只保留技术深度和回答深度
        String[] abilities = {"techDepth", "answerDepth"};
        String[] abilityNames = {"技术深度", "回答深度"};

        for (int i = 0; i < abilities.length; i++) {
            GrowthReportVO.AbilityGrowthVO growth = new GrowthReportVO.AbilityGrowthVO();
            String ability = abilities[i];
            String abilityName = abilityNames[i];

            // 获取该能力维度的所有得分
            List<Double> scores = new ArrayList<>();
            for (GrowthReportVO.ScoreTrendItemVO item : scoreTrend) {
                switch (ability) {
                    case "techDepth":
                        scores.add(item.getTechDepthScore());
                        break;
                    case "answerDepth":
                        scores.add(item.getAnswerDepthScore());
                        break;
                }
            }

            double firstScore = scores.get(0);
            double latestScore = scores.get(scores.size() - 1);
            double changeRate = firstScore > 0 ? ((latestScore - firstScore) / firstScore) * 100 : 0;

            growth.setFirstScore(firstScore);
            growth.setLatestScore(latestScore);
            growth.setChangeRate(roundToTwoDecimalPlaces(changeRate));

            // 确定趋势
            if (changeRate > 10) {
                growth.setTrend("显著提升");
            } else if (changeRate > 0) {
                growth.setTrend("有所提升");
            } else if (changeRate > -5) {
                growth.setTrend("保持稳定");
            } else {
                growth.setTrend("需要改进");
            }

            // 生成分析文本
            growth.setAnalysis(generateAbilityAnalysis(abilityName, growth.getTrend(), firstScore, latestScore));

            abilityGrowth.put(ability, growth);
        }

        return abilityGrowth;
    }

    private List<GrowthReportVO.StrengthVO> generateStrengths(List<Map<String, Object>> sessionReportPairs) {
        List<GrowthReportVO.StrengthVO> strengths = new ArrayList<>();

        // 简化实现：从所有报告的优势中提取最常出现的
        Map<String, Integer> strengthCount = new HashMap<>();

        for (Map<String, Object> pair : sessionReportPairs) {
            InterviewReport report = (InterviewReport) pair.get("report");
            if (report.getStrengths() != null && !report.getStrengths().isEmpty()) {
                String[] strengthItems = report.getStrengths().split("\n");
                for (String item : strengthItems) {
                    item = item.trim();
                    if (!item.isEmpty()) {
                        strengthCount.put(item, strengthCount.getOrDefault(item, 0) + 1);
                    }
                }
            }
        }

        // 排序并取前3个
        strengthCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    GrowthReportVO.StrengthVO strength = new GrowthReportVO.StrengthVO();
                    strength.setSkill(entry.getKey());
                    strength.setConsistency((int) ((double) entry.getValue() / sessionReportPairs.size() * 100));
                    strength.setAnalysis("在" + entry.getValue() + "次面试中表现出色，是您的核心优势");
                    strengths.add(strength);
                });

        return strengths;
    }

    private List<GrowthReportVO.ImprovementVO> generateImprovements(List<Map<String, Object>> sessionReportPairs) {
        List<GrowthReportVO.ImprovementVO> improvements = new ArrayList<>();

        // 简化实现：从所有报告的改进点中提取
        Map<String, List<String>> improvementMap = new HashMap<>();

        for (Map<String, Object> pair : sessionReportPairs) {
            InterviewReport report = (InterviewReport) pair.get("report");
            if (report.getImprovements() != null && !report.getImprovements().isEmpty()) {
                String[] improvementItems = report.getImprovements().split("\n");
                for (String item : improvementItems) {
                    item = item.trim();
                    if (!item.isEmpty()) {
                        improvementMap.computeIfAbsent(item, k -> new ArrayList<>()).add(item);
                    }
                }
            }
        }

        // 排序并取前3个
        improvementMap.entrySet().stream()
                .sorted(Map.Entry.<String, List<String>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                .limit(3)
                .forEach(entry -> {
                    GrowthReportVO.ImprovementVO improvement = new GrowthReportVO.ImprovementVO();
                    improvement.setArea(entry.getKey());
                    improvement.setProgress(calculateImprovementProgress(entry.getKey(), sessionReportPairs));
                    improvement.setAnalysis(generateImprovementAnalysis(entry.getKey(), improvement.getProgress()));
                    improvements.add(improvement);
                });

        return improvements;
    }

    private List<String> generateAISuggestions(Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth, List<GrowthReportVO.ImprovementVO> improvements) {
        try {
            // 准备提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append(AI_SUGGESTIONS_PROMPT_START);
            prompt.append("\n用户在以下方面需要改进：\n");
            
            improvements.forEach(improvement -> {
                prompt.append("- " + improvement.getArea() + "（改进进度：" + improvement.getProgress() + "%）\n");
            });
            
            prompt.append(AI_SUGGESTIONS_PROMPT_END);
            
            // 调用DeepSeek API
            String response = aiServiceUtils.callDeepSeekApi(prompt.toString());
            
            // 解析响应
            List<String> suggestions = new ArrayList<>();
            String[] lines = response.split("\\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    suggestions.add(trimmedLine);
                }
            }
            
            // 确保至少有9条建议，以便前端能正确提取推荐方向和三个时间维度的目标
            while (suggestions.size() < 9) {
                suggestions.add("持续学习和提升是职业发展的关键");
            }
            
            log.info("使用DeepSeek生成的AI建议: {}", suggestions);
            return suggestions;
        } catch (Exception e) {
            // 记录错误，但不影响正常流程
            log.error("调用DeepSeek API失败: {}", e.getMessage(), e);
            
            // 添加默认建议，确保符合前端预期的格式
            List<String> defaultSuggestions = new ArrayList<>();
            defaultSuggestions.add("推荐成为技术专家，专注后端架构和性能优化");
            defaultSuggestions.add("1. 系统学习分布式架构，提升技术深度");
            defaultSuggestions.add("2. 每日练习算法题，提高逻辑思维能力");
            defaultSuggestions.add("3. 总结面试经验，完善常见问题的回答");
            defaultSuggestions.add("4. 参与开源项目，积累实战经验");
            defaultSuggestions.add("5. 学习系统设计，提升架构能力");
            defaultSuggestions.add("6. 建立知识体系，形成个人技术博客");
            defaultSuggestions.add("7. 向技术管理方向发展，培养团队领导能力");
            defaultSuggestions.add("8. 拓展技术视野，关注行业前沿动态");
            defaultSuggestions.add("9. 构建个人影响力，参与技术分享");
            
            log.info("使用默认生成的结构化建议: {}", defaultSuggestions);
            return defaultSuggestions;
        }
    }
    
    private Map<String, Object> generateVisualizationData(List<GrowthReportVO.ScoreTrendItemVO> scoreTrend, Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth) {
        Map<String, Object> visualizationData = new HashMap<>();

        // 得分趋势数据
        List<Map<String, Object>> trendData = scoreTrend.stream().map(item -> {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", item.getDate());
            dataPoint.put("totalScore", item.getTotalScore());
            dataPoint.put("techDepth", item.getTechDepthScore());
            dataPoint.put("answerDepth", item.getAnswerDepthScore());
            // 已移除的字段：logicExpression和communication
            return dataPoint;
        }).collect(Collectors.toList());

        visualizationData.put("scoreTrend", trendData);

        // 能力对比数据
        Map<String, Object> abilityComparison = new HashMap<>();
        abilityGrowth.forEach((ability, growth) -> {
            Map<String, Object> abilityData = new HashMap<>();
            abilityData.put("firstScore", growth.getFirstScore());
            abilityData.put("latestScore", growth.getLatestScore());
            abilityData.put("changeRate", growth.getChangeRate());
            abilityComparison.put(ability, abilityData);
        });

        visualizationData.put("abilityComparison", abilityComparison);

        return visualizationData;
    }

    private GrowthReportVO convertToVO(GrowthReport growthReport, GrowthReportVO.ReportContentVO reportContent) {
        GrowthReportVO vo = new GrowthReportVO();
        vo.setReportId(growthReport.getId());
        vo.setUserId(growthReport.getUserId());
        vo.setReportContent(reportContent);
        vo.setGeneratedAt(growthReport.getGeneratedAt());
        vo.setInterviewCount(growthReport.getInterviewCount());
        vo.setStartDate(growthReport.getStartDate());
        vo.setEndDate(growthReport.getEndDate());
        return vo;
    }

    private double estimateScoreFromText(String text) {
        // 简化实现：根据文本长度和积极词汇估计得分
        if (text == null || text.isEmpty()) return 50;

        // 简单的得分估算逻辑
        int length = text.length();
        double baseScore = Math.min(100, 30 + (length / 10));

        // 积极词汇加分
        String[] positiveWords = {"优秀", "出色", "良好", "清晰", "深入", "扎实", "全面"};
        int positiveCount = 0;
        for (String word : positiveWords) {
            if (text.contains(word)) {
                positiveCount++;
            }
        }

        return roundToTwoDecimalPlaces(baseScore + (positiveCount * 2));
    }

    private String generateAbilityAnalysis(String abilityName, String trend, double firstScore, double latestScore) {
        StringBuilder analysis = new StringBuilder();
        analysis.append(abilityName).append("能力");

        if (trend.contains("提升")) {
            analysis.append("有")
                    .append(trend)
                    .append("，从首次面试的")
                    .append(firstScore)
                    .append("分提升到最近面试的")
                    .append(latestScore)
                    .append("分，进步明显。");
        } else if (trend.equals("保持稳定")) {
            analysis.append("保持稳定，得分在")
                    .append(Math.min(firstScore, latestScore))
                    .append("-").append(Math.max(firstScore, latestScore))
                    .append("分之间波动，建议继续保持。");
        } else {
            analysis.append("需要改进，得分从")
                    .append(firstScore)
                    .append("分变化到")
                    .append(latestScore)
                    .append("分，建议加强相关能力的培养。");
        }

        return analysis.toString();
    }

    private int calculateImprovementProgress(String area, List<Map<String, Object>> sessionReportPairs) {
        // 简化实现：根据该改进点在后期报告中出现的频率计算进度
        int totalReports = sessionReportPairs.size();
        int reportsWithoutArea = 0;

        // 只检查后一半的报告
        int startIndex = totalReports / 2;
        for (int i = startIndex; i < totalReports; i++) {
            Map<String, Object> pair = sessionReportPairs.get(i);
            InterviewReport report = (InterviewReport) pair.get("report");
            if (report.getImprovements() == null || !report.getImprovements().contains(area)) {
                reportsWithoutArea++;
            }
        }

        int remainingReports = totalReports - startIndex;
        return (int) ((double) reportsWithoutArea / remainingReports * 100);
    }

    private String generateImprovementAnalysis(String area, int progress) {
        if (progress >= 80) {
            return "在\"" + area + "\"方面已经有了显著进步，继续保持！";
        } else if (progress >= 50) {
            return "在\"" + area + "\"方面取得了一定进展，仍有提升空间。";
        } else {
            return "在\"" + area + "\"方面需要重点改进，建议制定针对性的提升计划。";
        }
    }

    private double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Override
    public Optional<GrowthReportVO> getLatestGrowthReport(Long userId) {
        try {
            Optional<GrowthReport> reportOptional = growthReportRepository.findFirstByUserIdOrderByGeneratedAtDesc(userId);
            if (reportOptional.isPresent()) {
                GrowthReport report = reportOptional.get();
                // 重新生成完整的报告内容
                return Optional.of(regenerateGrowthReport(report));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("获取最新成长报告失败: userId={}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<GrowthReportVO> getAllGrowthReports(Long userId) {
        try {
            List<GrowthReport> reports = growthReportRepository.findByUserIdOrderByGeneratedAtDesc(userId);
            return reports.stream().map(this::regenerateGrowthReport).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取所有成长报告失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<GrowthReportVO> getGrowthReportById(Long reportId) {
        try {
            Optional<GrowthReport> reportOptional = growthReportRepository.findById(reportId);
            if (reportOptional.isPresent()) {
                GrowthReport report = reportOptional.get();
                // 重新生成完整的报告内容
                return Optional.of(regenerateGrowthReport(report));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("获取成长报告失败: reportId={}", reportId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 重新生成完整的成长报告内容
     * @param growthReport 数据库中的成长报告记录
     * @return 完整的成长报告VO
     */
    private GrowthReportVO regenerateGrowthReport(GrowthReport growthReport) {
        try {
            // 首先尝试从存储的JSON数据中重建报告内容
            GrowthReportVO.ReportContentVO reportContent = rebuildReportContentFromJson(growthReport);
            
            // 如果JSON数据不存在或不完整，则重新生成报告内容
            if (reportContent == null || reportContent.getScoreTrend() == null || reportContent.getScoreTrend().isEmpty()) {
                Long userId = growthReport.getUserId();
                
                // 获取用户信息
                Optional<User> userOptional = userRepository.findById(userId);
                if (!userOptional.isPresent()) {
                    throw new IllegalArgumentException("用户不存在");
                }
                User user = userOptional.get();
                
                // 获取用户在报告时间范围内的所有面试会话（按时间顺序）
                List<InterviewSession> sessions = interviewSessionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                        userId, growthReport.getStartDate(), growthReport.getEndDate());
                
                // 为每个会话获取对应的面试报告
                List<Map<String, Object>> sessionReportPairs = new ArrayList<>();
                for (InterviewSession session : sessions) {
                    Optional<InterviewReport> reportOptional = interviewReportRepository.findBySessionId(session.getSessionId());
                    if (reportOptional.isPresent()) {
                        Map<String, Object> pair = new HashMap<>();
                        pair.put("session", session);
                        pair.put("report", reportOptional.get());
                        sessionReportPairs.add(pair);
                    }
                }
                
                if (sessionReportPairs.isEmpty()) {
                    throw new IllegalArgumentException("找不到对应的面试记录");
                }
                
                // 重新生成报告内容
                reportContent = generateReportContent(user, sessionReportPairs);
            }
            
            // 转换为VO并返回
            return convertToVO(growthReport, reportContent);
        } catch (Exception e) {
            log.error("重新生成成长报告失败: reportId={}", growthReport.getId(), e);
            // 如果重新生成失败，至少返回基础信息
            return convertToVO(growthReport);
        }
    }

    @Override
    public boolean deleteGrowthReport(Long reportId, Long userId) {
        try {
            Optional<GrowthReport> reportOptional = growthReportRepository.findById(reportId);
            if (reportOptional.isPresent() && reportOptional.get().getUserId().equals(userId)) {
                growthReportRepository.deleteById(reportId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("删除成长报告失败: reportId={}, userId={}", reportId, userId, e);
            return false;
        }
    }

    private GrowthReportVO convertToVO(GrowthReport growthReport) {
        try {
            // 构建概览信息
            GrowthReportVO.OverviewVO overview = new GrowthReportVO.OverviewVO();
            overview.setUserName(growthReport.getUserName());
            overview.setLatestJobName(growthReport.getLatestJobName());
            overview.setTimeRange(growthReport.getTimeRange());
            overview.setFirstTotalScore(growthReport.getFirstTotalScore());
            overview.setLatestTotalScore(growthReport.getLatestTotalScore());
            overview.setAverageScore(growthReport.getAverageScore());
            overview.setImprovementRate(growthReport.getImprovementRate());
            overview.setInterviewCount(growthReport.getInterviewCount());
            
            // 尝试从JSON数据中重建完整报告内容
            GrowthReportVO.ReportContentVO reportContent = rebuildReportContentFromJson(growthReport);
            if (reportContent != null) {
                reportContent.setOverview(overview);
            } else {
                // 如果JSON数据不可用，创建空的报告内容
                reportContent = new GrowthReportVO.ReportContentVO();
                reportContent.setOverview(overview);
            }
            
            return convertToVO(growthReport, reportContent);
        } catch (Exception e) {
            log.error("转换成长报告为VO失败: reportId={}", growthReport.getId(), e);
            throw new RuntimeException("转换成长报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 从数据库存储的JSON数据中重建报告内容
     */
    private GrowthReportVO.ReportContentVO rebuildReportContentFromJson(GrowthReport growthReport) {
        try {
            GrowthReportVO.ReportContentVO reportContent = new GrowthReportVO.ReportContentVO();
            
            // 反序列化各字段
            if (growthReport.getScoreTrendJson() != null) {
                reportContent.setScoreTrend(objectMapper.readValue(growthReport.getScoreTrendJson(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, GrowthReportVO.ScoreTrendItemVO.class)));
            }
            
            if (growthReport.getAbilityGrowthJson() != null) {
                reportContent.setAbilityGrowth(objectMapper.readValue(growthReport.getAbilityGrowthJson(), 
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, GrowthReportVO.AbilityGrowthVO.class)));
            }
            
            if (growthReport.getStrengthsJson() != null) {
                reportContent.setStrengths(objectMapper.readValue(growthReport.getStrengthsJson(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, GrowthReportVO.StrengthVO.class)));
            }
            
            if (growthReport.getImprovementsJson() != null) {
                reportContent.setImprovements(objectMapper.readValue(growthReport.getImprovementsJson(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, GrowthReportVO.ImprovementVO.class)));
            }
            
            if (growthReport.getAiSuggestionsJson() != null) {
                reportContent.setAiSuggestions(objectMapper.readValue(growthReport.getAiSuggestionsJson(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            }
            
            if (growthReport.getVisualizationDataJson() != null) {
                reportContent.setVisualizationData(objectMapper.readValue(growthReport.getVisualizationDataJson(), Map.class));
            }
            
            // 已在aiSuggestions中包含推荐职位和成长计划
            // 无需单独处理推荐职位和成长计划的JSON字段
            
            return reportContent;
        } catch (Exception e) {
            log.error("从JSON重建报告内容失败: reportId={}, {}", growthReport.getId(), e.getMessage(), e);
            return null;
        }
    }
}
