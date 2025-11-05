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
import com.aicv.airesume.utils.RetryUtils;

/**
 * 产品服务实现类
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private RetryUtils retryUtils;

    @Override
    public List<Product> getAllProducts() {
        return retryUtils.executeWithDefaultRetrySupplier(() -> productRepository.findAll());
    }

    @Override
    public List<Product> getActiveProducts() {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 使用Repository提供的方法查询上架产品
            return productRepository.findByActiveOrderByUpdateTimeDesc(true);
        });
    }

    @Override
    public List<Product> getProductsByType(Integer type) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 使用Repository提供的方法查询指定类型的上架产品
            return productRepository.findByTypeAndActiveOrderByUpdateTimeDesc(type.toString(), true);
        });
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> productRepository.findById(id));
    }

    @Override
    public Product createProduct(Product product) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 简化实现，直接保存
            return productRepository.save(product);
        });
    }

    @Override
    public Product updateProduct(Product product) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 简化实现，直接保存
            return productRepository.save(product);
        });
    }

    @Override
    public Product setProductStatus(Long productId, Boolean isActive) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 简化实现，直接返回查询结果
            return productRepository.findById(productId).orElse(null);
        });
    }

    @Override
    public List<Product> getProductsByTypeAndStatus(Integer type, Boolean isActive) {
        return retryUtils.executeWithDefaultRetrySupplier(() -> {
            // 如果指定了类型，使用Repository的方法；否则返回所有
            if (type != null) {
                return productRepository.findByTypeAndActiveOrderByUpdateTimeDesc(type.toString(), isActive);
            }
            if (isActive != null) {
                return productRepository.findByActiveOrderByUpdateTimeDesc(isActive);
            }
            return productRepository.findAll();
        });
    }
}