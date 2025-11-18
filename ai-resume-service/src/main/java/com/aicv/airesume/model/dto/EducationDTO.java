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
    
    // 手动添加getter方法以解决编译问题
    public String getSchool() {
        return school;
    }
    
    public String getDegree() {
        return degree;
    }
    
    public String getMajor() {
        return major;
    }
    
    public String getStartDate() {
        return startDate;
    }
    
    public String getEndDate() {
        return endDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getGpa() {
        return gpa;
    }
    
    public Integer getOrderIndex() {
        return orderIndex;
    }
}