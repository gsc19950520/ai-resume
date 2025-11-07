package com.aicv.airesume.repository;

import com.aicv.airesume.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 领域数据访问接口
 */
@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    // 可以根据需要添加更多查询方法
}