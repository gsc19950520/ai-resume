package com.aicv.airesume.model.vo;

import com.aicv.airesume.service.ReportGenerationService.ReportChunk;
import lombok.Data;

import java.util.List;

/**
 * 报告内容块VO类
 * 用于封装报告生成状态和内容块数据
 */
@Data
public class ReportChunksVO {
    /**
     * 报告内容块列表
     */
    private List<ReportChunk> chunks;
    
    /**
     * 最新的内容块索引
     */
    private int lastIndex;
    
    /**
     * 报告是否已完成
     */
    private boolean completed;
    
    /**
     * 报告生成状态
     */
    private String status;
    
    /**
     * 错误信息（当状态为FAILED时）
     */
    private String errorMessage;
}