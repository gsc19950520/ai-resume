package com.aicv.airesume.model.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 问题分析DTO
 */
@Data
public class QuestionAnalysisDTO {

    /**
     * 问题类别
     */
    private String category;

    /**
     * 问题列表
     */
    private List<QuestionItem> questions;

    /**
     * 分析总结
     */
    private String analysisSummary;

    /**
     * 建议关注点
     */
    private List<String> keyFocusPoints;

    /**
     * 问题项内部类
     */
    @Data
    public static class QuestionItem {
        private String question;
        private String type;
        private String difficulty;
        private String purpose;
        private List<String> evaluationCriteria;
        private Map<String, String> expectedAnswers;
        private String followUpStrategy;
    }
}