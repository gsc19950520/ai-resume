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
import com.aicv.airesume.utils.AiServiceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI成长报告服务实现类
 */
@Service
@Slf4j
public class GrowthReportServiceImpl implements GrowthReportService {

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
            // 假设报告内容中包含这些信息，或者需要通过NLP分析报告文本
            item.setTechDepthScore(estimateScoreFromText(report.getTechDepthEvaluation()));
            item.setLogicExpressionScore(estimateScoreFromText(report.getLogicExpressionEvaluation()));
            item.setCommunicationScore(estimateScoreFromText(report.getCommunicationEvaluation()));
            item.setAnswerDepthScore(estimateScoreFromText(report.getAnswerDepthEvaluation()));

            return item;
        }).collect(Collectors.toList());
    }

    private Map<String, GrowthReportVO.AbilityGrowthVO> generateAbilityGrowth(List<GrowthReportVO.ScoreTrendItemVO> scoreTrend) {
        Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth = new HashMap<>();

        if (scoreTrend.size() < 2) return abilityGrowth;

        // 分析各个能力维度
        String[] abilities = {"techDepth", "logicExpression", "communication", "answerDepth"};
        String[] abilityNames = {"技术深度", "逻辑表达", "沟通能力", "回答深度"};

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
                    case "logicExpression":
                        scores.add(item.getLogicExpressionScore());
                        break;
                    case "communication":
                        scores.add(item.getCommunicationScore());
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
        // 1. 先使用规则生成基础建议（作为备选）
        List<String> ruleBasedSuggestions = generateRuleBasedSuggestions(abilityGrowth, improvements);
        
        try {
            // 2. 尝试使用DeepSeek生成更智能的建议
            String formattedData = formatDataForDeepSeek(abilityGrowth, improvements);
            
            // 构建DeepSeek提示词
            String prompt = String.format("你是一位专业的面试教练，请基于以下面试成长分析数据，为用户提供具体、实用的改进建议：\n\n%s\n\n请输出3-5条针对性强、可操作的建议，每条建议独立一行，不要添加任何序号或标记。", formattedData);
            
            // 调用DeepSeek API
            String deepSeekResponse = aiServiceUtils.callDeepSeekApi(prompt);
            
            if (deepSeekResponse != null && !deepSeekResponse.isEmpty()) {
                // 解析DeepSeek返回的建议
                List<String> aiSuggestions = parseDeepSeekSuggestions(deepSeekResponse);
                
                // 如果AI生成的建议不为空，则返回AI建议，否则返回规则生成的建议
                if (!aiSuggestions.isEmpty()) {
                    log.info("使用DeepSeek生成的AI建议: {}", aiSuggestions);
                    return aiSuggestions;
                }
            }
        } catch (Exception e) {
            // 记录错误，但不影响正常流程
            log.error("调用DeepSeek API失败: {}", e.getMessage(), e);
        }
        
        // 3. 如果DeepSeek调用失败或返回空结果，则使用基于规则的建议
        log.info("使用规则生成的建议: {}", ruleBasedSuggestions);
        return ruleBasedSuggestions;
    }
    
    /**
     * 使用规则生成基础建议
     */
    private List<String> generateRuleBasedSuggestions(Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth, List<GrowthReportVO.ImprovementVO> improvements) {
        List<String> suggestions = new ArrayList<>();

        // 基于能力成长提出建议
        abilityGrowth.forEach((ability, growth) -> {
            if (growth.getTrend().equals("需要改进") || growth.getChangeRate() < 0) {
                switch (ability) {
                    case "techDepth":
                        suggestions.add("加强技术知识的学习，特别是在" + (growth.getAnalysis().contains("技术深度") ? "相关领域" : "新技术") + "方面");
                        break;
                    case "logicExpression":
                        suggestions.add("练习结构化思考和表达，提高回答的条理性");
                        break;
                    case "communication":
                        suggestions.add("增强沟通能力，注意表达的清晰度和简洁性");
                        break;
                    case "answerDepth":
                        suggestions.add("培养深入分析问题的能力，尝试从多个角度思考问题");
                        break;
                }
            }
        });

        // 基于改进点提出建议
        improvements.forEach(improvement -> {
            if (improvement.getProgress() < 50) {
                suggestions.add("重点关注\"" + improvement.getArea() + "\"的提升，制定针对性的学习计划");
            }
        });

        // 添加通用建议
        suggestions.add("保持定期练习，通过模拟面试不断提升面试技巧");
        suggestions.add("总结每次面试的经验教训，形成自己的面试准备体系");

        return suggestions.stream().distinct().limit(5).collect(Collectors.toList());
    }
    
    /**
     * 解析DeepSeek返回的建议，格式化为列表
     */
    private List<String> parseDeepSeekSuggestions(String response) {
        List<String> suggestions = new ArrayList<>();
        
        // 去除首尾空白
        response = response.trim();
        
        // 如果是用分号分隔的
        if (response.contains(";")) {
            String[] parts = response.split(";");
            for (String part : parts) {
                String suggestion = part.trim();
                if (!suggestion.isEmpty()) {
                    // 移除可能的序号
                    suggestion = suggestion.replaceAll("^[0-9]+[\\.、][\\s]*", "");
                    suggestions.add(suggestion);
                }
            }
        } 
        // 如果是用换行分隔的
        else if (response.contains("\n")) {
            String[] lines = response.split("\n");
            for (String line : lines) {
                String suggestion = line.trim();
                if (!suggestion.isEmpty()) {
                    // 移除可能的序号
                    suggestion = suggestion.replaceAll("^[0-9]+[\\.、][\\s]*", "");
                    suggestions.add(suggestion);
                }
            }
        } 
        // 如果是单个建议
        else {
            suggestions.add(response);
        }
        
        // 限制返回数量为3-5条
        return suggestions.stream()
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 格式化数据为自然语言描述，用于发送给DeepSeek API
     */
    private String formatDataForDeepSeek(Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth, List<GrowthReportVO.ImprovementVO> improvements) {
        StringBuilder data = new StringBuilder();
        
        // 格式化能力成长数据
        data.append("【能力成长分析】\n");
        abilityGrowth.forEach((ability, growth) -> {
            String abilityName = "";
            switch (ability) {
                case "techDepth": abilityName = "技术深度"; break;
                case "logicExpression": abilityName = "逻辑表达"; break;
                case "communication": abilityName = "沟通能力"; break;
                case "answerDepth": abilityName = "回答深度"; break;
            }
            
            data.append(String.format("%s：%s，首次得分%.2f，最近得分%.2f，变化率%.2f%%\n",
                    abilityName, growth.getTrend(), growth.getFirstScore(), growth.getLatestScore(), growth.getChangeRate()));
            data.append(String.format("分析：%s\n\n", growth.getAnalysis()));
        });
        
        // 格式化改进点数据
        if (!improvements.isEmpty()) {
            data.append("【需要改进的方面】\n");
            improvements.forEach(improvement -> {
                data.append(String.format("%s：进度%d%%\n", improvement.getArea(), improvement.getProgress()));
                data.append(String.format("分析：%s\n\n", improvement.getAnalysis()));
            });
        }
        
        return data.toString();
    }

    private Map<String, Object> generateVisualizationData(List<GrowthReportVO.ScoreTrendItemVO> scoreTrend, Map<String, GrowthReportVO.AbilityGrowthVO> abilityGrowth) {
        Map<String, Object> visualizationData = new HashMap<>();

        // 得分趋势数据
        List<Map<String, Object>> trendData = scoreTrend.stream().map(item -> {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", item.getDate());
            dataPoint.put("totalScore", item.getTotalScore());
            dataPoint.put("techDepth", item.getTechDepthScore());
            dataPoint.put("logicExpression", item.getLogicExpressionScore());
            dataPoint.put("communication", item.getCommunicationScore());
            dataPoint.put("answerDepth", item.getAnswerDepthScore());
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
            GrowthReportVO.ReportContentVO reportContent = generateReportContent(user, sessionReportPairs);
            
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
            
            // 构建报告内容
            GrowthReportVO.ReportContentVO reportContent = new GrowthReportVO.ReportContentVO();
            reportContent.setOverview(overview);
            
            // 注意：对于其他复杂字段（如scoreTrend、abilityGrowth等），需要重新生成或从其他地方获取
            // 这里简化处理，仅设置概览信息
            
            return convertToVO(growthReport, reportContent);
        } catch (Exception e) {
            log.error("转换成长报告为VO失败: reportId={}", growthReport.getId(), e);
            throw new RuntimeException("转换成长报告失败: " + e.getMessage());
        }
    }
}
