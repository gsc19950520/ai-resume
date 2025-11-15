package com.aicv.airesume.model.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 更新简历请求DTO
 */
@Data
public class UpdateResumeDTO {
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 简历数据
     */
    @Valid
    @NotNull(message = "简历数据不能为空")
    private ResumeDataDTO data;
}