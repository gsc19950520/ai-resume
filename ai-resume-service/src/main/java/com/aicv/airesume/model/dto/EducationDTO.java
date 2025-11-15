package com.aicv.airesume.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 教育经历DTO类
 */
@Data
public class EducationDTO {
    @NotBlank(message = "学校名称不能为空")
    private String school;
    
    @NotBlank(message = "学历不能为空")
    private String degree;
    
    @NotBlank(message = "专业不能为空")
    private String major;
    
    @NotBlank(message = "开始日期不能为空")
    private String startDate;
    
    @NotBlank(message = "结束日期不能为空")
    private String endDate;
    
    private String description;
    private String gpa;
    private Integer orderIndex;
}