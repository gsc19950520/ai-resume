package com.aicv.airesume.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI成长报告展示VO
 */
@Data
public class GrowthReportVO {
    private Long reportId;
    private Long userId;
    private ReportContentVO reportContent;
    private LocalDateTime generatedAt;
    private Integer interviewCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    /**
     * 报告内容内部类
     */
    @Data
    public static class ReportContentVO {
        private OverviewVO overview;
        private List<ScoreTrendItemVO> scoreTrend;
        private Map<String, AbilityGrowthVO> abilityGrowth;
        private List<StrengthVO> strengths;
        private List<ImprovementVO> improvements;
        private List<String> aiSuggestions;
        private Map<String, Object> visualizationData;
        private List<RecommendedJobVO> recommendedJobs;
        private List<GrowthPlanVO> growthPlans;
    }

    /**
     * 整体概览VO
     */
    @Data
    public static class OverviewVO {
        private String userName;
        private String latestJobName;
        private Integer interviewCount;
        private String timeRange;
        private Double firstTotalScore;
        private Double latestTotalScore;
        private Double averageScore;
        private Double improvementRate;
    }

    /**
     * 得分趋势项VO - 只保留总分、技术、深度三项
     */
    @Data
    public static class ScoreTrendItemVO {
        private String sessionId;
        private String date;
        private Double totalScore;
        private String jobName;
        private Double techDepthScore;
        private Double answerDepthScore;
        // 已移除: logicExpressionScore
        // 已移除: communicationScore
    }

    /**
     * 能力成长VO
     */
    @Data
    public static class AbilityGrowthVO {
        private String trend;
        private Double changeRate;
        private String analysis;
        private Double firstScore;
        private Double latestScore;
    }

    /**
     * 优势VO
     */
    @Data
    public static class StrengthVO {
        private String skill;
        private Integer consistency;
        private String analysis;
    }

    /**
     * 改进点VO
     */
    @Data
    public static class ImprovementVO {
        private String area;
        private Integer progress;
        private String analysis;
    }
    
    /**
     * 推荐职业方向VO
     */
    @Data
    public static class RecommendedJobVO {
        private String jobName;
        private String matchReason;
        private Integer matchScore;
    }
    
    /**
     * 未来成长规划VO
     */
    @Data
    public static class GrowthPlanVO {
        private String timeFrame;
        private List<String> goals;
        private List<String> actionSteps;
    }
}