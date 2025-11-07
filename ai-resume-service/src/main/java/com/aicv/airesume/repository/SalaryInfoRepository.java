package com.aicv.airesume.repository;

import com.aicv.airesume.entity.SalaryInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 薪资信息数据访问接口
 */
@Repository
public interface SalaryInfoRepository extends JpaRepository<SalaryInfo, Long> {

    /**
     * 根据城市和职位类型ID查询薪资信息
     * @param city 城市
     * @param jobTypeId 职位类型ID
     * @return 薪资信息列表
     */
    List<SalaryInfo> findByCityAndJobTypeId(String city, Long jobTypeId);

    /**
     * 根据城市、职位类型ID和经验年限查询薪资信息
     * @param city 城市
     * @param jobTypeId 职位类型ID
     * @param experience 经验年限
     * @return 薪资信息
     */
    Optional<SalaryInfo> findByCityAndJobTypeIdAndExperience(String city, Long jobTypeId, String experience);

    /**
     * 根据城市查询薪资信息列表
     * @param city 城市
     * @return 薪资信息列表
     */
    List<SalaryInfo> findByCity(String city);

    /**
     * 根据职位类型ID查询薪资信息列表
     * @param jobTypeId 职位类型ID
     * @return 薪资信息列表
     */
    List<SalaryInfo> findByJobTypeId(Long jobTypeId);

    /**
     * 根据城市和经验年限查询薪资信息列表
     * @param city 城市
     * @param experience 经验年限
     * @return 薪资信息列表
     */
    List<SalaryInfo> findByCityAndExperience(String city, String experience);
}
