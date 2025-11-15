package com.aicv.airesume.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 个人信息DTO类
 */
@Data
public class PersonalInfoDTO {
    @NotBlank(message = "职位不能为空")
    private String jobTitle;
    
    @NotBlank(message = "期望薪资不能为空")
    private String expectedSalary;
    
    @NotBlank(message = "到岗时间不能为空")
    private String startTime;
    
    private String jobTypeId;
    
    @NotEmpty(message = "兴趣爱好不能为空")
    private List<String> interests;
    
    @NotBlank(message = "自我评价不能为空")
    private String selfEvaluation;
}