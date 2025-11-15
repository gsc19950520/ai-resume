package com.aicv.airesume.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 专业技能DTO类
 */
@Data
public class SkillWithLevelDTO {
    @NotBlank(message = "技能名称不能为空")
    private String name;
    
    private Integer level;
    private String category;
    private Integer orderIndex;
}