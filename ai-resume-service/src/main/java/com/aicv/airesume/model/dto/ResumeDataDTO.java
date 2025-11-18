package com.aicv.airesume.model.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 简历数据DTO类
 */
@Data
public class ResumeDataDTO {
    @Valid
    private PersonalInfoDTO personalInfo;
    
    @Valid
    @NotEmpty(message = "请至少填写一个完整的教育经历")
    private List<EducationDTO> education;
    
    @Valid
    @NotEmpty(message = "请至少填写一个完整的工作经历")
    private List<WorkExperienceDTO> workExperience;
    
    @Valid
    @NotEmpty(message = "请至少填写一个完整的项目经验")
    private List<ProjectDTO> projects;
    
    @Valid
    @NotEmpty(message = "请至少填写1个专业技能")
    private List<SkillWithLevelDTO> skillsWithLevel;
    
    // 手动添加getter方法以解决编译问题
    public PersonalInfoDTO getPersonalInfo() {
        return personalInfo;
    }
    
    public List<EducationDTO> getEducation() {
        return education;
    }
    
    public List<WorkExperienceDTO> getWorkExperience() {
        return workExperience;
    }
    
    public List<ProjectDTO> getProjects() {
        return projects;
    }
    
    public List<SkillWithLevelDTO> getSkillsWithLevel() {
        return skillsWithLevel;
    }
}