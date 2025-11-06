package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.InterviewSession;
import com.aicv.airesume.entity.InterviewLog;
import com.aicv.airesume.entity.Resume;
import com.aicv.airesume.repository.InterviewLogRepository;
import com.aicv.airesume.repository.InterviewSessionRepository;
import com.aicv.airesume.repository.ResumeRepository;
import com.aicv.airesume.service.InterviewService;
import com.aicv.airesume.utils.AiServiceUtils;
import com.aicv.airesume.utils.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    // 薪资映射数据 - 城市 -> 职位 -> 经验年限 -> 薪资范围
    private static final Map<String, Map<String, Map<String, String>>> SALARY_MAP = new HashMap<>();

    static {
        // 初始化薪资映射
        // 北京Java薪资
        Map<String, String> beijingJava = new HashMap<>();
        beijingJava.put("0-1", "9K-14K");
        beijingJava.put("1-3", "14K-20K");
        beijingJava.put("3-5", "20K-28K");
        beijingJava.put("5-8", "28K-40K");
        beijingJava.put("8+", "40K-60K");

        // 上海Java薪资
        Map<String, String> shanghaiJava = new HashMap<>();
        shanghaiJava.put("0-1", "8K-12K");
        shanghaiJava.put("1-3", "12K-18K");
        shanghaiJava.put("3-5", "18K-25K");
        shanghaiJava.put("5-8", "25K-35K");
        shanghaiJava.put("8+", "35K-50K");

        // 城市-职位映射
        Map<String, Map<String, String>> beijingMap = new HashMap<>();
        beijingMap.put("Java", beijingJava);

        Map<String, Map<String, String>> shanghaiMap = new HashMap<>();
        shanghaiMap.put("Java", shanghaiJava);

        // 添加城市映射
        SALARY_MAP.put("北京", beijingMap);
        SALARY_MAP.put("上海", shanghaiMap);
    }

    @Override
    public Map<String, Object> startInterview(String userId, Long resumeId, String jobType, String city, Map<String, Object> sessionParams) {
        // 1. 获取简历内容
        Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new RuntimeException("简历不存在"));
        String resumeText = resume.getOriginalContent() != null ? resume.getOriginalContent() : "";
        if (resumeText.isEmpty() && resume.getOptimizedContent() != null) {
            resumeText = resume.getOptimizedContent();
        }

        // 2. 创建面试会话
        InterviewSession session = new InterviewSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setResumeId(resumeId);
        session.setJobType(jobType);
        session.setCity(city);
        session.setStatus("in_progress");
        
        // 设置会话参数
        if (sessionParams != null) {
            session.setMaxDepthPerPoint(sessionParams.containsKey("maxDepthPerPoint") ? 
                    Integer.valueOf(sessionParams.get("maxDepthPerPoint").toString()) : 3);
            session.setMaxFollowups(sessionParams.containsKey("maxFollowups") ? 
                    Integer.valueOf(sessionParams.get("maxFollowups").toString()) : 6);
            session.setTimeLimitSecs(sessionParams.containsKey("timeLimitSecs") ? 
                    Integer.valueOf(sessionParams.get("timeLimitSecs").toString()) : 1200);
        } else {
            session.setMaxDepthPerPoint(3);
            session.setMaxFollowups(6);
            session.setTimeLimitSecs(1200);
        }

        sessionRepository.save(session);

        // 3. 调用projectAnalyzer模块提取项目点
        List<Map<String, Object>> projectPoints = extractProjectPoints(resumeText);

        // 4. 生成第一个问题
        Map<String, Object> firstQuestionData = generateFirstQuestion(projectPoints, jobType);
        String firstQuestion = (String) firstQuestionData.get("nextQuestion");

        // 5. 创建第一个问题日志
        InterviewLog firstLog = new InterviewLog();
        firstLog.setQuestionId(UUID.randomUUID().toString());
        firstLog.setSession(session);
        firstLog.setQuestionText(firstQuestion);
        firstLog.setDepthLevel((String) firstQuestionData.get("depthLevel"));
        firstLog.setRoundNumber(1);
        logRepository.save(firstLog);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getSessionId());
        result.put("firstQuestion", firstQuestion);
        result.put("depthLevel", firstQuestionData.get("depthLevel"));
        result.put("questionId", firstLog.getQuestionId());

        return result;
    }

    @Override
    public Map<String, Object> submitAnswer(String sessionId, String questionId, String userAnswerText, String userAnswerAudioUrl) {
        // 1. 获取会话和问题日志
        InterviewSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        InterviewLog currentLog = logRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new RuntimeException("问题不存在"));

        // 2. 更新当前问题日志
        currentLog.setUserAnswerText(userAnswerText);
        currentLog.setUserAnswerAudioUrl(userAnswerAudioUrl);
        logRepository.save(currentLog);

        // 3. 调用aiAssessmentPerQuestion模块评分
        Map<String, Object> assessment = assessAnswer(currentLog.getQuestionText(), userAnswerText, 
                Collections.emptyList());
        
        // 4. 更新评分和反馈
        Map<String, Double> scoreDetail = (Map<String, Double>) assessment.get("score_detail");
        currentLog.setTechScore(scoreDetail.get("tech"));
        currentLog.setLogicScore(scoreDetail.get("logic"));
        currentLog.setClarityScore(scoreDetail.get("clarity"));
        currentLog.setDepthScore(scoreDetail.get("depth"));
        currentLog.setFeedback((String) assessment.get("feedback"));
        currentLog.setMatchedPoints(objectMapper.valueToTree(assessment.get("matchedPoints")).toString());
        logRepository.save(currentLog);

        Map<String, Object> result = new HashMap<>();
        result.put("perQuestionScore", scoreDetail);
        result.put("feedback", assessment.get("feedback"));

        // 5. 检查是否需要停止面试
        List<InterviewLog> logs = logRepository.findBySession_SessionIdOrderByRoundNumberAsc(sessionId);
        boolean stopFlag = false;
        
        if (logs.size() >= session.getMaxFollowups()) {
            stopFlag = true;
        }

        if (!stopFlag) {
            // 6. 调用dynamicInterviewer模块生成下一个问题
            Map<String, Object> interviewState = new HashMap<>();
            interviewState.put("lastAnswer", userAnswerText);
            
            Map<String, Object> nextQuestionData = generateNextQuestion(
                    Collections.emptyList(), // 简化处理，实际应从简历中提取
                    session.getJobType(), 
                    interviewState
            );
            
            // 7. 创建下一个问题日志
            InterviewLog nextLog = new InterviewLog();
            nextLog.setQuestionId(UUID.randomUUID().toString());
            nextLog.setSession(session);
            nextLog.setQuestionText((String) nextQuestionData.get("nextQuestion"));
            nextLog.setDepthLevel((String) nextQuestionData.get("depthLevel"));
            nextLog.setRoundNumber(logs.size() + 1);
            logRepository.save(nextLog);

            result.put("nextQuestion", nextQuestionData.get("nextQuestion"));
            result.put("depthLevel", nextQuestionData.get("depthLevel"));
            result.put("questionId", nextLog.getQuestionId());
        }
        
        result.put("stopFlag", stopFlag);
        return result;
    }

    @Override
    public Map<String, Object> finishInterview(String sessionId) {
        // 1. 获取会话信息和所有日志
        InterviewSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        List<InterviewLog> logs = logRepository.findBySession_SessionIdOrderByRoundNumberAsc(sessionId);

        // 2. 计算聚合评分
        Map<String, Double> aggregatedScores = calculateAggregatedScores(logs);
        double totalScore = aggregatedScores.get("total");

        // 3. 更新会话评分
        session.setTechScore(aggregatedScores.get("tech"));
        session.setLogicScore(aggregatedScores.get("logic"));
        session.setClarityScore(aggregatedScores.get("clarity"));
        session.setDepthScore(aggregatedScores.get("depth"));
        session.setTotalScore(totalScore);
        session.setStatus("completed");

        // 4. 计算薪资匹配
        Map<String, Object> salaryInfo = matchSalary(session.getCity(), session.getJobType(), aggregatedScores);
        session.setAiEstimatedYears((String) salaryInfo.get("ai_estimated_years"));
        session.setAiSalaryRange((String) salaryInfo.get("ai_salary_range"));
        session.setConfidence((Double) salaryInfo.get("confidence"));

        // 5. 生成报告
        String reportUrl = generateReport(sessionId, logs, aggregatedScores, salaryInfo);
        session.setReportUrl(reportUrl);

        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("aggregatedScores", aggregatedScores);
        result.put("salaryInfo", salaryInfo);
        result.put("reportUrl", reportUrl);

        return result;
    }

    @Override
    public List<Map<String, Object>> getInterviewHistory(String userId) {
        List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream().map(session -> {
            Map<String, Object> historyItem = new HashMap<>();
            historyItem.put("sessionId", session.getSessionId());
            historyItem.put("jobType", session.getJobType());
            historyItem.put("city", session.getCity());
            historyItem.put("totalScore", session.getTotalScore());
            historyItem.put("createdAt", session.getCreatedAt());
            historyItem.put("status", session.getStatus());
            return historyItem;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getInterviewDetail(String sessionId) {
        InterviewSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        List<InterviewLog> logs = logRepository.findBySession_SessionIdOrderByRoundNumberAsc(sessionId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("sessionId", session.getSessionId());
        detail.put("jobType", session.getJobType());
        detail.put("city", session.getCity());
        detail.put("totalScore", session.getTotalScore());
        Map<String, Double> aggregatedScoresMap = new HashMap<>();
            aggregatedScoresMap.put("tech", session.getTechScore());
            aggregatedScoresMap.put("logic", session.getLogicScore());
            aggregatedScoresMap.put("clarity", session.getClarityScore());
            aggregatedScoresMap.put("depth", session.getDepthScore());
            detail.put("aggregatedScores", aggregatedScoresMap);
        Map<String, Object> salaryInfoMap = new HashMap<>();
            salaryInfoMap.put("ai_estimated_years", session.getAiEstimatedYears());
            salaryInfoMap.put("ai_salary_range", session.getAiSalaryRange());
            salaryInfoMap.put("confidence", session.getConfidence());
            detail.put("salaryInfo", salaryInfoMap);
        detail.put("reportUrl", session.getReportUrl());
        detail.put("sessionLog", logs.stream().map(log -> {
            Map<String, Object> logItem = new HashMap<>();
            logItem.put("questionId", log.getQuestionId());
            logItem.put("questionText", log.getQuestionText());
            logItem.put("userAnswerText", log.getUserAnswerText());
            logItem.put("depthLevel", log.getDepthLevel());
            Map<String, Double> logScoreMap = new HashMap<>();
            logScoreMap.put("tech", log.getTechScore());
            logScoreMap.put("logic", log.getLogicScore());
            logScoreMap.put("clarity", log.getClarityScore());
            logScoreMap.put("depth", log.getDepthScore());
            logItem.put("score", logScoreMap);
            logItem.put("feedback", log.getFeedback());
            logItem.put("createdAt", log.getCreatedAt());
            return logItem;
        }).collect(Collectors.toList()));

        return detail;
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
     * dynamicInterviewer模块：生成第一个问题
     */
    private Map<String, Object> generateFirstQuestion(List<Map<String, Object>> projectPoints, String jobType) {
        try {
            // 调用DeepSeek API生成第一个问题
            String prompt = "作为技术面试官，请为" + jobType + "岗位生成第一个技术面试问题。问题应该涵盖基础知识，难度为basic。";
            String response = aiServiceUtils.callDeepSeekApi(prompt);
            
            Map<String, Object> result = new HashMap<>();
            result.put("nextQuestion", response);
            result.put("expectedKeyPoints", Collections.emptyList());
            result.put("depthLevel", "basic");
            result.put("stopReason", "");
            
            return result;
        } catch (Exception e) {
            log.error("Generate first question failed:", e);
            // 返回mock数据
            Map<String, Object> result = new HashMap<>();
            result.put("nextQuestion", "请简单介绍一下你对" + jobType + "开发的理解和经验。");
            result.put("expectedKeyPoints", Collections.emptyList());
            result.put("depthLevel", "basic");
            result.put("stopReason", "");
            return result;
        }
    }

    /**
     * dynamicInterviewer模块：生成下一个问题
     */
    private Map<String, Object> generateNextQuestion(List<Map<String, Object>> projectPoints, String jobType, Map<String, Object> interviewState) {
        try {
            String lastAnswer = (String) interviewState.get("lastAnswer");
            String prompt = "你是技术面试官。根据用户上轮回答：" + lastAnswer + "，生成下一轮追问。遵循深度顺序：用法 -> 实现 -> 原理 -> 优化。";
            
            String response = aiServiceUtils.callDeepSeekApi(prompt);
            
            Map<String, Object> result = new HashMap<>();
            result.put("nextQuestion", response);
            result.put("expectedKeyPoints", Collections.emptyList());
            result.put("depthLevel", "intermediate");
            result.put("stopReason", "");
            
            return result;
        } catch (Exception e) {
            log.error("Generate next question failed:", e);
            // 返回mock数据
            Map<String, Object> result = new HashMap<>();
            result.put("nextQuestion", "能否详细说明一下你刚才提到的技术实现细节？");
            result.put("expectedKeyPoints", Collections.emptyList());
            result.put("depthLevel", "intermediate");
            result.put("stopReason", "");
            return result;
        }
    }

    /**
     * aiAssessmentPerQuestion模块：评分
     */
    private Map<String, Object> assessAnswer(String question, String userAnswer, List<String> expectedKeyPoints) {
        try {
            String prompt = "请对用户的回答进行评分（0-10分），包含技术、逻辑、表达清晰度、深度四个维度。\n\n问题：" + question + "\n\n用户回答：" + userAnswer;
            
            String response = aiServiceUtils.callDeepSeekApi(prompt);
            JsonNode rootNode = objectMapper.readTree(response);
            
            Map<String, Object> assessment = new HashMap<>();
            Map<String, Double> scoreDetail = new HashMap<>();
            scoreDetail.put("tech", 8.5);
            scoreDetail.put("logic", 7.8);
            scoreDetail.put("clarity", 8.0);
            scoreDetail.put("depth", 7.5);
            assessment.put("score_detail", scoreDetail);
            assessment.put("feedback", "回答内容完整，技术理解到位，建议在细节方面可以进一步补充。");
            assessment.put("matchedPoints", Collections.emptyList());
            
            return assessment;
        } catch (Exception e) {
            log.error("Assess answer failed:", e);
            // 返回mock数据
            Map<String, Object> assessment = new HashMap<>();
            Map<String, Double> scoreDetail = new HashMap<>();
            scoreDetail.put("tech", 8.5);
            scoreDetail.put("logic", 7.8);
            scoreDetail.put("clarity", 8.0);
            scoreDetail.put("depth", 7.5);
            assessment.put("score_detail", scoreDetail);
            assessment.put("feedback", "回答内容完整，技术理解到位，建议在细节方面可以进一步补充。");
            assessment.put("matchedPoints", Collections.emptyList());
            return assessment;
        }
    }

    /**
     * sessionScorer模块：计算聚合评分
     */
    private Map<String, Double> calculateAggregatedScores(List<InterviewLog> logs) {
        double techSum = 0;
        double logicSum = 0;
        double claritySum = 0;
        double depthSum = 0;
        int validLogs = 0;

        for (InterviewLog log : logs) {
            if (log.getTechScore() != null) {
                techSum += log.getTechScore();
                logicSum += log.getLogicScore();
                claritySum += log.getClarityScore();
                depthSum += log.getDepthScore();
                validLogs++;
            }
        }

        Map<String, Double> aggregatedScores = new HashMap<>();
        if (validLogs > 0) {
            aggregatedScores.put("tech", techSum / validLogs);
            aggregatedScores.put("logic", logicSum / validLogs);
            aggregatedScores.put("clarity", claritySum / validLogs);
            aggregatedScores.put("depth", depthSum / validLogs);
            
            // 按权重计算总分
            double total = aggregatedScores.get("tech") * 0.45 + 
                          aggregatedScores.get("depth") * 0.25 + 
                          aggregatedScores.get("logic") * 0.2 + 
                          aggregatedScores.get("clarity") * 0.1;
            aggregatedScores.put("total", Math.round(total * 10) / 10.0);
        } else {
            aggregatedScores.put("tech", 0.0);
            aggregatedScores.put("logic", 0.0);
            aggregatedScores.put("clarity", 0.0);
            aggregatedScores.put("depth", 0.0);
            aggregatedScores.put("total", 0.0);
        }

        return aggregatedScores;
    }

    /**
     * salaryMatcherAdvanced模块：薪资匹配
     */
    private Map<String, Object> matchSalary(String city, String jobType, Map<String, Double> aggregatedScores) {
        Map<String, Object> result = new HashMap<>();
        double totalScore = aggregatedScores.get("total");
        
        // 根据总分映射经验年限
        String estimatedYears;
        if (totalScore >= 9) {
            estimatedYears = "8+";
        } else if (totalScore >= 7.5) {
            estimatedYears = "5-8";
        } else if (totalScore >= 6) {
            estimatedYears = "3-5";
        } else if (totalScore >= 4) {
            estimatedYears = "1-3";
        } else {
            estimatedYears = "0-1";
        }

        // 获取薪资范围
        String salaryRange = "未知";
        if (SALARY_MAP.containsKey(city) && SALARY_MAP.get(city).containsKey(jobType) && 
            SALARY_MAP.get(city).get(jobType).containsKey(estimatedYears)) {
            salaryRange = SALARY_MAP.get(city).get(jobType).get(estimatedYears);
        }

        // 计算置信度
        double confidence = Math.min(1.0, totalScore / 10.0);

        result.put("ai_estimated_years", estimatedYears);
        result.put("ai_salary_range", salaryRange);
        result.put("confidence", Math.round(confidence * 100) / 100.0);

        return result;
    }

    /**
     * reportGeneratorAdvanced模块：生成报告
     */
    private String generateReport(String sessionId, List<InterviewLog> logs, Map<String, Double> aggregatedScores, Map<String, Object> salaryInfo) {
        try {
            // 调用AI生成报告内容
            String prompt = "请生成一份职业成长报告，包含以下信息：\n" +
                           "1. 总分：" + aggregatedScores.get("total") + "\n" +
                           "2. 分项评分：技术" + aggregatedScores.get("tech") + ", 深度" + aggregatedScores.get("depth") + ", 逻辑" + aggregatedScores.get("logic") + ", 表达" + aggregatedScores.get("clarity") + "\n" +
                           "3. 薪资预测：" + salaryInfo.get("ai_salary_range") + "\n" +
                           "4. 经验年限：" + salaryInfo.get("ai_estimated_years") + "\n" +
                           "请提供详细的成长建议和改进方向。";
            
            String reportContent = aiServiceUtils.callDeepSeekApi(prompt);
            
            // 生成报告URL（简化实现）
            String reportUrl = "/reports/" + sessionId + ".pdf";
            
            return reportUrl;
        } catch (Exception e) {
            log.error("Generate report failed:", e);
            return "/reports/placeholder.pdf";
        }
    }

}