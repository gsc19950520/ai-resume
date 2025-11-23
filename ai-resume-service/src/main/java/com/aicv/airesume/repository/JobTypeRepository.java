package com.aicv.airesume.repository;

import com.aicv.airesume.entity.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 职位类型数据访问接口
 */
@Repository
public interface JobTypeRepository extends JpaRepository<JobType, Integer> {

    /**
     * 根据职位名称查询职位类型
     * @param name 职位名称
     * @return 职位类型对象
     */
    Optional<JobType> findByJobName(String name);
    

    /**
     * 根据领域ID查询职位类型列表
     * @param domainId 领域ID
     * @return 职位类型列表
     */
    java.util.List<JobType> findByDomainId(Long domainId);
    
}