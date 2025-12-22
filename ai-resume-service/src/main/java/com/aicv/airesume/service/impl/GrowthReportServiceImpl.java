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
    // DeepSeek API 提示词常量
    private static final String RECOMMEND_JOBS_PROMPT_START = "请基于用户的面试表现，推荐3个最适合的职业方向。\n";
    private static final String RECOMMEND_JOBS_PROMPT_END = "\n请为每个推荐的职业方向提供：职业名称、匹配理由（30字以内）、匹配分数（0-100）。\n" +
            "格式要求：JSON格式，包含recommendations数组，每个元素包含jobName、matchReason、matchScore字段。";
    
    private static final String GROWTH_PLANS_PROMPT_START = "请基于用户的面试表现，制定短期（1-3个月）、中期（3-6个月）和长期（6-12个月）的成长规划。\n";
    private static final String GROWTH_PLANS_PROMPT_END = "\n请为每个时间阶段提供：目标（3-5条）和具体行动步骤（3-5条）。\n" +
            "格式要求：JSON格式，包含plans数组，每个元素包含timeFrame（'短期'/'中期'/'长期'）、goals数组、actionSteps数组。";
    
    private static final String AI_SUGGESTIONS_PROMPT_START = "请基于用户的面试表现分析，生成结构化的AI建议，包含以下部分：\n" +
            "1. 一条简短的推荐职业方向（30字以内）\n" +
            "2. 近期目标（3-6个月）的3条具体行动建议\n" +
            "3. 中期目标（6-12个月）的3条具体行动建议\n" +
            "4. 长期目标（1-3年）的3条具体行动建议\n";
    private static final String AI_SUGGESTIONS_PROMPT_END = "\n请提供具体、可操作的建议，每条建议30-50字。\n" +
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
                // 获取用户信息
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
                
                // 获取用户最近的面试会话和报告
                List<InterviewSession> sessions = interviewSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
                // 反转列表以获得正序
                Collections.reverse(sessions);
                List<Map<String, Object>> sessionReportPairs = new ArrayList<>();
                
                // 构建会话和报告的映射关系
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
                
                // 生成各种报告内容并流式输出
                StringBuilder currentChunk = new StringBuilder();
                
                // 1. 生成概览信息并流式输出
                GrowthReportVO.OverviewVO overview = generateOverview(user, sessionReportPairs, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String overviewJson = objectMapper.writeValueAsString(overview);
                currentChunk.append("概览信息：").append(overviewJson);
                
                // 超过20字时发送内容块
                if (currentChunk.length() > 20) {
                    record.addChunk(currentChunk.toString());
                    emitter.send(SseEmitter.event().name("report").data(currentChunk.toString()));
                    currentChunk.setLength(0);
                }
                
                // 2. 生成AI建议并流式输出
                List<GrowthReportVO.ScoreTrendItemVO> scoreTrend = generateScoreTrend(sessionReportPairs, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth = generateAbilityGrowth(scoreTrend);
                List<GrowthReportVO.StrengthVO> strengths = generateStrengths(sessionReportPairs);
                List<GrowthReportVO.ImprovementVO> improvements = generateImprovements(sessionReportPairs);
                List<String> aiSuggestions = generateAISuggestions(abilityGrowth, improvements);
                
                String suggestionsJson = objectMapper.writeValueAsString(aiSuggestions);
                currentChunk.append("\nAI建议：").append(suggestionsJson);
                
                // 发送AI建议内容块
                if (currentChunk.length() > 20) {
                    record.addChunk(currentChunk.toString());
                    emitter.send(SseEmitter.event().name("report").data(currentChunk.toString()));
                    currentChunk.setLength(0);
                }
                
                // 生成职业推荐并流式输出
                List<GrowthReportVO.RecommendedJobVO> recommendedJobs = generateRecommendedJobs(user, sessionReportPairs, strengths, improvements);
                String jobsJson = objectMapper.writeValueAsString(recommendedJobs);
                currentChunk.append("\n职业推荐：").append(jobsJson);
                
                record.addChunk(currentChunk.toString());
                emitter.send(SseEmitter.event().name("report").data(currentChunk.toString()));
                currentChunk.setLength(0);
                
                // 3. 生成成长规划并流式输出
                List<GrowthReportVO.GrowthPlanVO> growthPlans = generateGrowthPlans(user, sessionReportPairs, strengths, improvements);
                String plansJson = objectMapper.writeValueAsString(growthPlans);
                currentChunk.append("\n成长规划：").append(plansJson);
                record.addChunk(currentChunk.toString());
                emitter.send(SseEmitter.event().name("report").data(currentChunk.toString()));
                
                // 生成完整报告并保存
                GrowthReport report = new GrowthReport();
                report.setUserId(userId);
                report.setUserName(user.getName());
                
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
                
                // 组合报告内容
                StringBuilder fullReport = new StringBuilder();
                for (ReportGenerationService.ReportChunk chunk : record.getChunks()) {
                    fullReport.append(chunk.getContent());
                }
                
                // 将复杂数据转换为JSON并存储
                try {
                    report.setScoreTrendJson(objectMapper.writeValueAsString(scoreTrend));
                    report.setAbilityGrowthJson(objectMapper.writeValueAsString(abilityGrowth));
                    report.setStrengthsJson(objectMapper.writeValueAsString(strengths));
                    report.setImprovementsJson(objectMapper.writeValueAsString(improvements));
                    report.setAiSuggestionsJson(objectMapper.writeValueAsString(aiSuggestions));
                } catch (Exception e) {
                    log.error("JSON序列化失败: {}", e.getMessage(), e);
                }
                
                growthReportRepository.save(report);
                
                // 标记报告生成完成
                record.setStatus(ReportGenerationService.ReportStatus.COMPLETED);
                emitter.complete();
                
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
