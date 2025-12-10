package com.aicv.airesume.model.vo;

import com.aicv.airesume.entity.InterviewLog;
import lombok.Data;
import java.time.format.DateTimeFormatter;

/**
 * 面试历史记录项VO，用于前端展示
 */
@Data
public class InterviewHistoryItemVO {

    private Long id;
    private String type;
    private String content;
    private String formattedTime;
    private String feedback;
    private Double techScore;
    private Double logicScore;
    private Double clarityScore;
    private Double depthScore;
    private Integer roundNumber;
    private String matchedPoints;

    /**
     * 将InterviewLog转换为InterviewHistoryItemVO
     * @param log 面试日志
     * @return 面试历史记录项VO
     */
    public static InterviewHistoryItemVO fromInterviewLog(InterviewLog log) {
        InterviewHistoryItemVO vo = new InterviewHistoryItemVO();
        vo.setId(log.getId());
        
        // 格式化时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        vo.setFormattedTime(log.getCreatedAt().format(formatter));
        
        // 设置轮次
        vo.setRoundNumber(log.getRoundNumber());
        
        
        return vo;
    }

    /**
     * 创建问题类型的历史记录项
     * @param log 面试日志
     * @return 问题类型的面试历史记录项VO
     */
    public static InterviewHistoryItemVO createQuestionItem(InterviewLog log) {
        InterviewHistoryItemVO vo = fromInterviewLog(log);
        vo.setType("question");
        vo.setContent(log.getQuestionText());
        return vo;
    }

    /**
     * 创建回答类型的历史记录项
     * @param log 面试日志
     * @return 回答类型的面试历史记录项VO
     */
    public static InterviewHistoryItemVO createAnswerItem(InterviewLog log) {
        InterviewHistoryItemVO vo = fromInterviewLog(log);
        vo.setType("answer");
        vo.setContent(log.getUserAnswerText());
        return vo;
    }
}