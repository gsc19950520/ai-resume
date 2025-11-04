package com.aicv.airesume.service.impl;

import com.aicv.airesume.entity.Product;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import com.aicv.airesume.repository.ProductRepository;
import com.aicv.airesume.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 产品服务实现类
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getActiveProducts() {
        // 使用Repository提供的方法查询上架产品
        return productRepository.findByIsActiveOrderBySortWeightDesc(1);
    }

    @Override
    public List<Product> getProductsByType(Integer type) {
        // 使用Repository提供的方法查询指定类型的上架产品
        return productRepository.findByTypeAndIsActiveOrderBySortWeightDesc(type, 1);
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Product createProduct(Product product) {
        // 简化实现，直接保存
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Product product) {
        // 简化实现，直接保存
        return productRepository.save(product);
    }

    @Override
    public Product setProductStatus(Long productId, Boolean isActive) {
        // 简化实现，直接返回查询结果
        return productRepository.findById(productId).orElse(null);
    }

    @Override
    public List<Product> getProductsByTypeAndStatus(Integer type, Boolean isActive) {
        // 如果指定了类型，使用Repository的方法；否则返回所有
        if (type != null) {
            return productRepository.findByTypeAndIsActiveOrderBySortWeightDesc(type, isActive ? 1 : 0);
        }
        if (isActive != null) {
            return productRepository.findByIsActiveOrderBySortWeightDesc(isActive ? 1 : 0);
        }
        return productRepository.findAll();
    }
}