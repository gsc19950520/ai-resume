package com.aicv.airesume.repository;

import com.aicv.airesume.entity.DynamicConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态配置数据访问接口
 */
@Repository
public interface DynamicConfigRepository extends JpaRepository<DynamicConfig, Long> {

    /**
     * 根据配置类型和键查询启用的配置
     */
    Optional<DynamicConfig> findByConfigTypeAndConfigKeyAndIsActiveTrue(String configType, String configKey);

    /**
     * 根据配置键查询启用的配置列表
     */
    List<DynamicConfig> findByConfigKeyAndIsActiveTrue(String configKey);

    /**
     * 根据配置类型查询启用的配置列表
     */
    List<DynamicConfig> findByConfigTypeAndIsActiveTrue(String configType);

    /**
     * 根据配置类型查询所有配置列表（包括禁用的）
     */
    List<DynamicConfig> findByConfigType(String configType);

    /**
     * 查询所有启用的配置
     */
    List<DynamicConfig> findByIsActiveTrue();
}