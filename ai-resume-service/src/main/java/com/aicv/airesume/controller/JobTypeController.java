package com.aicv.airesume.controller;

import com.aicv.airesume.model.vo.BaseResponseVO;
import com.aicv.airesume.model.vo.JobTypeVO;
import com.aicv.airesume.service.JobTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 职位类型控制器
 */
@RestController
@RequestMapping("/api/job-types")
public class JobTypeController {

    private static final Logger logger = LoggerFactory.getLogger(JobTypeController.class);

    @Autowired
    private JobTypeService jobTypeService;

    /**
     * 获取所有职位类型列表
     * @return 职位类型列表
     */
    @GetMapping
    public BaseResponseVO getJobTypes() {
        try {
            logger.info("请求获取职位类型列表");
            List<JobTypeVO> jobTypes = jobTypeService.getAllJobTypes();
            return BaseResponseVO.success(jobTypes);
        } catch (Exception e) {
            logger.error("获取职位类型列表失败: {}", e.getMessage(), e);
            return BaseResponseVO.error("获取职位类型列表失败");
        }
    }
}