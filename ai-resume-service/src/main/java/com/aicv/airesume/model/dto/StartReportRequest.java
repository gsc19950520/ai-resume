package com.aicv.airesume.model.dto;

import lombok.Data;

/**
 * 开始生成报告请求DTO
 * 用于接收/start-report接口的请求参数
 */
@Data
public class StartReportRequest {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 最后一题的回答内容（可选）
     */
    private String lastAnswer = "";
    
}