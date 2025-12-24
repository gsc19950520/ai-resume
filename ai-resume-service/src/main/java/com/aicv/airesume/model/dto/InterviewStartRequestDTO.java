package com.aicv.airesume.model.dto;

import lombok.Data;

/**
 * 开始面试请求DTO
 */
@Data
public class InterviewStartRequestDTO {
    private Long userId;
    private Long resumeId;
    private String persona;
    private Integer sessionSeconds;
    private String analysisDepth = "intermediate";
    private Integer jobTypeId;
    private Boolean forceNew = false; // 是否强制创建新会话
    private String payOrderNo; // 支付订单编号
}