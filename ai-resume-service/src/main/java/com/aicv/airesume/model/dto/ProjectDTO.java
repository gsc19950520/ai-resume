package com.aicv.airesume.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 项目经验DTO类
 */
@Data
public class ProjectDTO {
    @NotBlank(message = "项目名称不能为空")
    private String projectName;
    
    @NotBlank(message = "开始日期不能为空")
    private String startDate;
    
    @NotBlank(message = "结束日期不能为空")
    private String endDate;
    
    private String role;
    private String description;
    private String achievements;
    private String technologies;
    private Integer orderIndex;
}