package com.aicv.airesume.service;

import com.aicv.airesume.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * 产品服务接口
 */
public interface ProductService {
    
    /**
     * 获取所有产品列表
     */
    List<Product> getAllProducts();
    
    /**
     * 获取上架产品列表
     */
    List<Product> getActiveProducts();
    
    /**
     * 根据类型获取产品
     */
    List<Product> getProductsByType(Integer type);
    
    /**
     * 根据ID获取产品
     */
    Optional<Product> getProductById(Long id);
    
    /**
     * 创建产品
     */
    Product createProduct(Product product);
    
    /**
     * 更新产品
     */
    Product updateProduct(Product product);
    
    /**
     * 设置产品状态
     */
    Product setProductStatus(Long productId, Boolean isActive);
    
    /**
     * 根据类型和上架状态获取产品
     */
    List<Product> getProductsByTypeAndStatus(Integer type, Boolean isActive);
}