package com.aicv.airesume.model.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 保存报告请求DTO
 */
@Data
public class SaveReportRequestDTO {
    private String sessionId;
    private Map<String, Object> reportData;
}