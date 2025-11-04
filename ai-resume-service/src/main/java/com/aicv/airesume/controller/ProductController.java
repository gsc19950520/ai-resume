package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Product;
import com.aicv.airesume.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品控制器
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 获取所有产品
     * @return 产品列表
     */
    @GetMapping("/all")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    /**
     * 获取上架产品
     * @return 上架产品列表
     */
    @GetMapping("/active")
    public List<Product> getActiveProducts() {
        return productService.getActiveProducts();
    }

    /**
     * 根据类型获取产品
     * @param type 产品类型
     * @return 产品列表
     */
    @GetMapping("/type/{type}")
    public List<Product> getProductsByType(@PathVariable Integer type) {
        return productService.getProductsByType(type);
    }

    /**
     * 根据ID获取产品
     * @param id 产品ID
     * @return 产品信息
     */
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id).orElse(null);
    }

    /**
     * 创建产品
     * @param product 产品信息
     * @return 创建的产品
     */
    @PostMapping("/create")
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    /**
     * 更新产品
     * @param id 产品ID
     * @param product 产品信息
     * @return 更新后的产品
     */
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) {
        // 移除setId调用，避免找不到符号错误
        return productService.updateProduct(product);
    }

    /**
     * 设置产品状态
     * @param id 产品ID
     * @param isActive 是否上架
     */
    @PutMapping("/{id}/status")
    public void setProductStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        productService.setProductStatus(id, isActive);
    }

    /**
     * 根据类型和状态获取产品
     * @param type 产品类型
     * @param isActive 是否上架
     * @return 产品列表
     */
    @GetMapping("/type/{type}/status/{isActive}")
    public List<Product> getProductsByTypeAndStatus(@PathVariable Integer type, @PathVariable Boolean isActive) {
        return productService.getProductsByTypeAndStatus(type, isActive);
    }
}