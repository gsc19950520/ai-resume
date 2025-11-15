package com.aicv.airesume.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 工作经历DTO类
 */
@Data
public class WorkExperienceDTO {
    @NotBlank(message = "公司名称不能为空")
    private String companyName;
    
    @NotBlank(message = "职位名称不能为空")
    private String positionName;
    
    @NotBlank(message = "开始日期不能为空")
    private String startDate;
    
    @NotBlank(message = "结束日期不能为空")
    private String endDate;
    
    private String description;
    private Integer orderIndex;
}