package com.aicv.airesume.repository;

import com.aicv.airesume.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 产品数据访问接口
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * 根据产品类型查询上架产品
     */
    List<Product> findByTypeAndActiveOrderByUpdateTimeDesc(String type, Boolean active);
    
    /**
     * 查询所有上架产品
     */
    List<Product> findByActiveOrderByUpdateTimeDesc(Boolean active);
}