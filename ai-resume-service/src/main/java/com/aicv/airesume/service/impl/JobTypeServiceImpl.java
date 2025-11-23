package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.JobType;
import com.aicv.airesume.model.vo.JobTypeVO;
import com.aicv.airesume.repository.JobTypeRepository;
import com.aicv.airesume.service.JobTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 职位类型服务实现类
 */
@Service
public class JobTypeServiceImpl implements JobTypeService {

    private static final Logger logger = LoggerFactory.getLogger(JobTypeServiceImpl.class);

    @Autowired
    private JobTypeRepository jobTypeRepository;

    @Override
    public List<JobTypeVO> getAllJobTypes() {
        try {
            logger.info("开始获取所有职位类型列表");
            
            // 从数据库获取所有职位类型
            List<JobType> jobTypes = jobTypeRepository.findAll();
            
            // 转换为VO对象
            List<JobTypeVO> jobTypeVOs = jobTypes.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
            
            logger.info("成功获取职位类型列表，共{}个职位类型", jobTypeVOs.size());
            return jobTypeVOs;
        } catch (Exception e) {
            logger.error("获取职位类型列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取职位类型列表失败", e);
        }
    }

    /**
     * 将JobType实体转换为JobTypeVO对象
     * @param jobType 职位类型实体
     * @return 职位类型VO对象
     */
    private JobTypeVO convertToVO(JobType jobType) {
        JobTypeVO vo = new JobTypeVO();
        vo.setId(jobType.getId());
        vo.setName(jobType.getJobName());
        return vo;
    }
}