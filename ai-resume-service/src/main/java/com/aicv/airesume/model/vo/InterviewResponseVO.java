package com.aicv.airesume.model.vo;

import lombok.Data;

/**
 * 面试响应VO类
 * 用于@PostMapping("/start")接口的返回值，包含前端需要的简化字段
 */
@Data
public class InterviewResponseVO {
    private String sessionId;     // 面试会话ID
    private String question;      // 当前问题
    private String questionType;  // 问题类型
    private String feedback;      // 反馈信息
    private String nextQuestion;  // 下一个问题
    private Boolean isCompleted;  // 是否完成面试
    private String industryJobTag; // 行业职位标签
}