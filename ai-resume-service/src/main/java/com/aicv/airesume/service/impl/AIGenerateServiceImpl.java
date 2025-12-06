package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.AiTraceLog;
import com.aicv.airesume.entity.JobType;
import com.aicv.airesume.model.vo.SalaryRangeVO;
import com.aicv.airesume.repository.AiTraceLogRepository;
import com.aicv.airesume.repository.JobTypeRepository;
import com.aicv.airesume.service.AIGenerateService;
import com.aicv.airesume.utils.AiServiceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI生成服务实现类
 * 负责动态生成薪资信息和成长建议
 */
@Service
@Slf4j
public class AIGenerateServiceImpl implements AIGenerateService {

    @Autowired
    private AiServiceUtils aiServiceUtils;
    
    @Autowired
    private AiTraceLogRepository aiTraceLogRepository;
    
    @Autowired
    private JobTypeRepository jobTypeRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public SalaryRangeVO generateSalaryRange(String sessionId, String city, String jobType, 
                                           Map<String, Double> aggregatedScores, Map<String, Object> userPerformanceData) {
        try {
            log.info("AI动态生成薪资范围，会话ID: {}, 城市: {}, 职位: {}", sessionId, city, jobType);
            
            // 构建提示词模板
            String prompt = buildSalaryPrompt(city, jobType, aggregatedScores, userPerformanceData);
            
            // 调用AI服务生成薪资评估
            String aiResponse = aiServiceUtils.callDeepSeekApi(prompt);
            
            // 保存AI调用日志
            saveAiTraceLog(sessionId, "generate_salary", prompt, aiResponse);
            
            // 解析AI响应
            SalaryRangeVO vo = parseSalaryResponse(aiResponse, sessionId);
            
            // 基于评分和经验调整薪资建议
            adjustSalaryByScore(vo, aggregatedScores);
            
            return vo;
        } catch (Exception e) {
            log.error("AI生成薪资范围失败", e);
            // 返回默认值作为降级方案
            return getDefaultSalaryRange(sessionId, city, jobType, aggregatedScores);
        }
    }

    @Override
    public Map<String, Object> generateGrowthAdvice(String sessionId, String jobType, String domain,
                                                  Map<String, Double> aggregatedScores, List<String> weakSkills,
                                                  Map<String, Object> userPerformanceData) {
        try {
            log.info("AI动态生成成长建议，会话ID: {}, 职位: {}, 领域: {}", sessionId, jobType, domain);
            
            // 构建提示词模板
            String prompt = buildGrowthAdvicePrompt(jobType, domain, aggregatedScores, weakSkills, userPerformanceData);
            
            // 调用AI服务生成成长建议
            String aiResponse = aiServiceUtils.callDeepSeekApi(prompt);
            
            // 保存AI调用日志
            saveAiTraceLog(sessionId, "generate_growth_advice", prompt, aiResponse);
            
            // 解析AI响应
            return parseGrowthAdviceResponse(aiResponse);
        } catch (Exception e) {
            log.error("AI生成成长建议失败", e);
            // 返回默认建议作为降级方案
            return getDefaultGrowthAdvice(jobType, domain, weakSkills);
        }
    }

    @Override
    public String getJobDomain(Integer jobTypeId) {
        try {
            JobType jobType = jobTypeRepository.findById(jobTypeId)
                    .orElseThrow(() -> new RuntimeException("职位类型不存在"));
            // 使用domainId而不是不存在的domain对象
            return jobType.getDomainId() != null ? jobType.getDomainId().toString() : "未知领域";
        } catch (Exception e) {
            log.error("获取职位领域失败", e);
            return "未知领域";
        }
    }

    @Override
    public List<String> analyzeWeakSkills(String sessionId, List<Map<String, Object>> logs) {
        List<String> weakSkills = new ArrayList<>();
        
        try {
            // 从面试日志中分析技能短板
            Map<String, Double> avgScores = new HashMap<>();
            avgScores.put("tech", 0.0);
            avgScores.put("logic", 0.0);
            avgScores.put("clarity", 0.0);
            avgScores.put("depth", 0.0);
            
            int count = 0;
            for (Map<String, Object> log : logs) {
                if (log.containsKey("techScore")) {
                    avgScores.put("tech", avgScores.get("tech") + Double.parseDouble(log.get("techScore").toString()));
                    avgScores.put("logic", avgScores.get("logic") + Double.parseDouble(log.get("logicScore").toString()));
                    avgScores.put("clarity", avgScores.get("clarity") + Double.parseDouble(log.get("clarityScore").toString()));
                    avgScores.put("depth", avgScores.get("depth") + Double.parseDouble(log.get("depthScore").toString()));
                    count++;
                }
            }
            
            // 计算平均分
            if (count > 0) {
                final int finalCount = count;
                avgScores.replaceAll((k, v) -> v / finalCount);
                
                // 找出低于6分的维度作为短板
                if (avgScores.get("tech") < 6.0) weakSkills.add("专业技能");
                if (avgScores.get("logic") < 6.0) weakSkills.add("逻辑思维");
                if (avgScores.get("clarity") < 6.0) weakSkills.add("沟通表达");
                if (avgScores.get("depth") < 6.0) weakSkills.add("知识深度");
            }
            
            // 如果没有明显短板，从面试内容中提取
            if (weakSkills.isEmpty()) {
                // 从面试反馈中提取短板
                for (Map<String, Object> log : logs) {
                    if (log.containsKey("feedback")) {
                        String feedback = log.get("feedback").toString();
                        // 简单的关键词匹配，实际可以更复杂
                        if (feedback.contains("知识不足")) weakSkills.add("知识广度");
                        if (feedback.contains("实践经验")) weakSkills.add("项目实践");
                        if (feedback.contains("表达不清")) weakSkills.add("表达能力");
                    }
                }
            }
        } catch (Exception e) {
            log.error("分析技能短板失败", e);
        }
        
        return weakSkills;
    }
    
    /**
     * 构建薪资评估提示词
     */
    private String buildSalaryPrompt(String city, String jobType, Map<String, Double> aggregatedScores, 
                                    Map<String, Object> userPerformanceData) {
        // 基于merged_schema.sql中的示例提示模板
        return String.format("请基于以下信息为用户生成个性化薪资评估：\n" +
                           "职位类型：%s\n" +
                           "所在城市：%s\n" +
                           "专业技能评分：%.1f\n" +
                           "逻辑思维评分：%.1f\n" +
                           "沟通表达评分：%.1f\n" +
                           "创新潜力评分：%.1f\n" +
                           "总评分：%.1f\n" +
                           "估算工作经验：%s\n" +
                           "行业平均薪资趋势：%s\n" +
                           "\n请返回详细的薪资评估结果，包括：\n" +
                           "1. 薪资范围（格式：XXK-XXK/月）\n" +
                           "2. 薪资水平（如：低于平均/平均/高于平均）\n" +
                           "3. 薪资趋势（如：上升/平稳/下降）\n" +
                           "4. 置信度（0-100）\n" +
                           "5. 薪资建议值（如：XXK/月）",
                           jobType, city,
                           aggregatedScores.getOrDefault("tech", 0.0),
                           aggregatedScores.getOrDefault("logic", 0.0),
                           aggregatedScores.getOrDefault("clarity", 0.0),
                           aggregatedScores.getOrDefault("innovation", 0.0),
                           aggregatedScores.getOrDefault("total", 0.0),
                           userPerformanceData.getOrDefault("experienceYears", "未知"),
                           userPerformanceData.getOrDefault("industryTrend", "上升趋势"));
    }
    
    /**
     * 构建成长建议提示词
     */
    private String buildGrowthAdvicePrompt(String jobType, String domain, Map<String, Double> aggregatedScores,
                                          List<String> weakSkills, Map<String, Object> userPerformanceData) {
        // 基于merged_schema.sql中的示例提示模板
        return String.format("请基于以下信息为用户生成个性化的职业成长建议：\n" +
                           "职位类型：%s\n" +
                           "所在领域：%s\n" +
                           "专业技能评分：%.1f\n" +
                           "逻辑思维评分：%.1f\n" +
                           "沟通表达评分：%.1f\n" +
                           "创新潜力评分：%.1f\n" +
                           "总评分：%.1f\n" +
                           "技能短板：%s\n" +
                           "行业趋势：%s\n" +
                           "\n请生成：\n" +
                           "1. 推荐学习的技能（3-5项）\n" +
                           "2. 长期职业发展路径（3个阶段）\n" +
                           "3. 短期建议（1-3个月，具体可行的行动建议）\n" +
                           "4. 中期建议（3-6个月，能力提升方向）\n" +
                           "5. 长期建议（6-12个月，职业发展规划）\n" +
                           "\n建议语言风格要口语化、亲切自然，避免过于生硬的专业术语，让用户感觉像是一个经验丰富的前辈在给出建议。\n" +
                           "请严格按照指定格式返回，便于程序解析。",
                           jobType, domain,
                           aggregatedScores.getOrDefault("tech", 0.0),
                           aggregatedScores.getOrDefault("logic", 0.0),
                           aggregatedScores.getOrDefault("clarity", 0.0),
                           aggregatedScores.getOrDefault("innovation", 0.0),
                           aggregatedScores.getOrDefault("total", 0.0),
                           String.join(",", weakSkills),
                           userPerformanceData.getOrDefault("industryTrends", "技术更新迭代加速"));
    }
    
    /**
     * 解析薪资响应
     */
    private SalaryRangeVO parseSalaryResponse(String aiResponse, String sessionId) {
        SalaryRangeVO vo = new SalaryRangeVO();
            vo.setSessionId(Long.parseLong(sessionId));
            vo.setCurrency("CNY");
            vo.setPeriod("月");
        try {
            
            // 尝试JSON解析
            try {
                if (aiResponse.trim().startsWith("{") && aiResponse.trim().endsWith("}")) {
                    com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(aiResponse);
                    if (jsonNode.has("salary_range")) {
                        com.fasterxml.jackson.databind.JsonNode salaryNode = jsonNode.get("salary_range");
                        vo.setMinSalary(salaryNode.has("low") ? salaryNode.get("low").asInt() : 15);
                        // 设置平均薪资为minSalary和maxSalary的平均值
                        vo.setMaxSalary(salaryNode.has("high") ? salaryNode.get("high").asInt() : 30);
                        return vo;
                    }
                }
            } catch (Exception e) {
                // JSON解析失败，继续使用原有文本解析
            }
            
            // 原有文本解析逻辑作为备用
            if (aiResponse.contains("薪资范围")) {
                String rangePart = aiResponse.substring(aiResponse.indexOf("薪资范围"));
                if (rangePart.contains("-")) {
                    int start = rangePart.indexOf("：") + 1;
                    int end = rangePart.indexOf("/月");
                    if (start > 0 && end > start) {
                        String range = rangePart.substring(start, end).trim();
                        vo.setSalaryRange(range);
                        
                        // 提取最小值和最大值
                        if (range.contains("-")) {
                            String[] parts = range.split("-");
                            if (parts.length == 2) {
                                try {
                                    vo.setMinSalary(Integer.parseInt(parts[0].replaceAll("[^0-9]", "")));
                                    vo.setMaxSalary(Integer.parseInt(parts[1].replaceAll("[^0-9]", "")));
                                } catch (Exception e) {
                                    log.warn("解析薪资范围失败: {}", range, e);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析薪资响应失败", e);
            return getDefaultSalaryRange(sessionId, "", "", new HashMap<>());
        }
        
        // 设置默认值
        if (vo.getMinSalary() == null) vo.setMinSalary(15);
        if (vo.getMaxSalary() == null) vo.setMaxSalary(30);
        // 建议薪资设置已移除，因为SalaryRangeVO中没有该字段
        
        return vo;
    }
    
    /**
     * 解析成长建议响应
     */
    private Map<String, Object> parseGrowthAdviceResponse(String aiResponse) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 简单解析，实际可以更复杂地处理AI返回格式
            // 这里使用JSON格式解析，假设AI返回的是结构化数据
            Map<String, Object> adviceData = objectMapper.readValue(aiResponse, Map.class);
            
            result.put("recommended_skills", adviceData.getOrDefault("recommended_skills", new ArrayList<>()));
            result.put("long_term_path", adviceData.getOrDefault("long_term_path", new ArrayList<>()));
            result.put("short_term_advice", adviceData.getOrDefault("short_term_advice", ""));
            result.put("mid_term_advice", adviceData.getOrDefault("mid_term_advice", ""));
            result.put("long_term_advice", adviceData.getOrDefault("long_term_advice", ""));
        } catch (Exception e) {
            // 如果不是JSON格式，尝试纯文本解析
            log.warn("成长建议非JSON格式，尝试文本解析", e);
            
            // 简单的文本解析逻辑
            result.put("short_term_advice", extractSection(aiResponse, "短期建议", "中期建议"));
            result.put("mid_term_advice", extractSection(aiResponse, "中期建议", "长期建议"));
            result.put("long_term_advice", extractSection(aiResponse, "长期建议", null));
            result.put("recommended_skills", Arrays.asList("专业技能提升", "项目实践", "知识体系构建"));
            result.put("long_term_path", Arrays.asList("高级工程师", "技术专家", "技术管理"));
        }
        
        return result;
    }
    
    /**
     * 基于评分调整薪资
     */
    private void adjustSalaryByScore(SalaryRangeVO vo, Map<String, Double> aggregatedScores) {
        double techScore = aggregatedScores.getOrDefault("tech", 0.0);
        double totalScore = aggregatedScores.getOrDefault("total", 0.0);
        
        // 技术评分特别高时上调薪资
        if (techScore >= 9.0) {
            vo.setMaxSalary(Integer.valueOf((int) (vo.getMaxSalary() * 1.2)));
        } else if (techScore >= 8.0) {
            vo.setMaxSalary(Integer.valueOf((int) (vo.getMaxSalary() * 1.1)));
        }
        
        // 总评分较低时下调薪资
        if (totalScore < 5.0) {
            vo.setMinSalary(Integer.valueOf((int) (vo.getMinSalary() * 0.9)));
        }
        
        // 建议薪资计算已移除，因为SalaryRangeVO中没有该字段
    }
    
    /**
     * 获取默认薪资范围（降级方案）
     */
    private SalaryRangeVO getDefaultSalaryRange(String sessionId, String city, String jobType, 
                                              Map<String, Double> aggregatedScores) {
        SalaryRangeVO vo = new SalaryRangeVO();
        vo.setSessionId(Long.parseLong(sessionId));
        vo.setCurrency("CNY");
        vo.setPeriod("月");
        vo.setLevel("初级到高级");
        
        // 基于评分设置默认薪资范围
        double totalScore = aggregatedScores.getOrDefault("total", 0.0);
        if (totalScore >= 8.0) {
            vo.setMinSalary(Integer.valueOf(25));
            vo.setMaxSalary(Integer.valueOf(45));
        } else if (totalScore >= 6.0) {
            vo.setMinSalary(Integer.valueOf(15));
            vo.setMaxSalary(Integer.valueOf(30));
        } else {
            vo.setMinSalary(Integer.valueOf(8));
            vo.setMaxSalary(Integer.valueOf(20));
        }
        
        // 建议薪资设置已移除，因为SalaryRangeVO中没有该字段
        return vo;
    }
    
    /**
     * 获取默认成长建议（降级方案）
     */
    private Map<String, Object> getDefaultGrowthAdvice(String jobType, String domain, List<String> weakSkills) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("recommended_skills", Arrays.asList("专业技能提升", "项目实践", "沟通能力", "问题解决", "持续学习"));
        result.put("long_term_path", Arrays.asList("高级工程师", "技术专家", "技术管理"));
        result.put("short_term_advice", "最近可以多花些时间深入学习专业知识，尝试解决一些实际项目中的问题。遇到问题时，不要只是解决表面，多思考底层原因，这样进步会更快。");
        result.put("mid_term_advice", "工作中可以主动参与更多项目，积累实践经验。尝试学习一些新技术，拓宽自己的技术栈。多和团队成员交流，分享经验和想法。");
        result.put("long_term_advice", "随着经验积累，你可以选择一个感兴趣的技术方向深入研究，成为这个领域的专家。或者如果你对管理更感兴趣，也可以往技术管理方向发展。持续学习是保持竞争力的关键。");
        
        return result;
    }
    
    /**
     * 提取文本中的特定部分
     */
    private String extractSection(String text, String startTag, String endTag) {
        if (text == null || !text.contains(startTag)) {
            return "";
        }
        
        int startIndex = text.indexOf(startTag) + startTag.length();
        int endIndex = text.length();
        
        if (endTag != null && text.contains(endTag)) {
            endIndex = text.indexOf(endTag);
        }
        
        return text.substring(startIndex, endIndex).trim();
    }
    
    /**
     * 保存AI调用日志
     */
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
}