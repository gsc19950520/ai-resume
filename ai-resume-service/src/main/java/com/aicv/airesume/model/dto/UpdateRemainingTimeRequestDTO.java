package com.aicv.airesume.model.dto;

import lombok.Data;

/**
 * 更新面试剩余时间请求DTO
 */
@Data
public class UpdateRemainingTimeRequestDTO {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 剩余时间（秒）
     */
    private Integer sessionTimeRemaining;
    
}