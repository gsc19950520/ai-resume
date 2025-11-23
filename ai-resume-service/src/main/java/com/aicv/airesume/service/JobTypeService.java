package com.aicv.airesume.service;

import com.aicv.airesume.model.vo.JobTypeVO;

import java.util.List;

/**
 * 职位类型服务接口
 */
public interface JobTypeService {

    /**
     * 获取所有职位类型列表
     * @return 职位类型VO列表
     */
    List<JobTypeVO> getAllJobTypes();
}