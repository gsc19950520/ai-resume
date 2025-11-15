package com.aicv.airesume.model.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 创建简历请求DTO类
 */
@Data
public class CreateResumeDTO {
    @Valid
    @NotNull(message = "简历数据不能为空")
    private ResumeDataDTO resumeData;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}