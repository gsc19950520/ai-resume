package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI成长报告实体类
 */
@Data
@Entity
@Table(name = "growth_report")
public class GrowthReport {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 用户姓名
     */
    @Column(name = "user_name", length = 100)
    private String userName;
    
    /**
     * 最新职位名称
     */
    @Column(name = "latest_job_name", length = 200)
    private String latestJobName;
    
    /**
     * 报告时间范围描述
     */
    @Column(name = "time_range", length = 100)
    private String timeRange;
    
    /**
     * 第一次面试总分
     */
    @Column(name = "first_total_score")
    private Double firstTotalScore;
    
    /**
     * 最近一次面试总分
     */
    @Column(name = "latest_total_score")
    private Double latestTotalScore;
    
    /**
     * 平均得分
     */
    @Column(name = "average_score")
    private Double averageScore;
    
    /**
     * 能力提升百分比
     */
    @Column(name = "improvement_rate")
    private Double improvementRate;

    /**
     * 报告生成时间
     */
    @CreationTimestamp
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    /**
     * 面试总次数
     */
    @Column(name = "interview_count", nullable = false)
    private Integer interviewCount;

    /**
     * 报告统计的起始时间
     */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /**
     * 报告统计的结束时间
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    /**
     * 得分趋势数据，JSON格式存储，包含总分、技术和深度三项得分的历史变化
     */
    @Column(name = "score_trend_json", columnDefinition = "JSON")
    private String scoreTrendJson;
    
    /**
     * 能力成长数据，JSON格式存储，包含各能力维度的成长情况
     */
    @Column(name = "ability_growth_json", columnDefinition = "JSON")
    private String abilityGrowthJson;
    
    /**
     * 优势项数据，JSON格式存储
     */
    @Column(name = "strengths_json", columnDefinition = "JSON")
    private String strengthsJson;
    
    /**
     * 改进项数据，JSON格式存储
     */
    @Column(name = "improvements_json", columnDefinition = "JSON")
    private String improvementsJson;
    
    /**
     * AI建议数据，JSON格式存储，包含推荐方向和成长计划
     */
    @Column(name = "ai_suggestions_json", columnDefinition = "JSON")
    private String aiSuggestionsJson;
    
    /**
     * 可视化数据，JSON格式存储，用于前端图表展示
     */
    @Column(name = "visualization_data_json", columnDefinition = "JSON")
    private String visualizationDataJson;
    
}