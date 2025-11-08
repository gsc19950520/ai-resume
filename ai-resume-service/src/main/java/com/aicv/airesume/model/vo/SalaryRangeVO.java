package com.aicv.airesume.model.vo;

import lombok.Data;

/**
 * 薪资范围VO
 */
@Data
public class SalaryRangeVO {
    private Long id;
    private Long sessionId;
    private Integer minSalary;
    private Integer maxSalary;
    private String currency;
    private String period;
    private String level;
    private String salaryRange;
    private Integer suggestedSalary;
}
