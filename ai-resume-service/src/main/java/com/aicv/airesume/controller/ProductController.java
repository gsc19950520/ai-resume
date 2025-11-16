package com.aicv.airesume.controller;

import com.aicv.airesume.entity.Product;
import com.aicv.airesume.model.vo.BaseResponseVO;
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
    public BaseResponseVO getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            return BaseResponseVO.success(products);
        } catch (Exception e) {
            return BaseResponseVO.error("获取所有产品失败：" + e.getMessage());
        }
    }

    /**
     * 获取上架产品
     * @return 上架产品列表
     */
    @GetMapping("/active")
    public BaseResponseVO getActiveProducts() {
        try {
            List<Product> products = productService.getActiveProducts();
            return BaseResponseVO.success(products);
        } catch (Exception e) {
            return BaseResponseVO.error("获取上架产品失败：" + e.getMessage());
        }
    }

    /**
     * 根据类型获取产品
     * @param type 产品类型
     * @return 产品列表
     */
    @GetMapping("/type/{type}")
    public BaseResponseVO getProductsByType(@PathVariable Integer type) {
        try {
            List<Product> products = productService.getProductsByType(type);
            return BaseResponseVO.success(products);
        } catch (Exception e) {
            return BaseResponseVO.error("根据类型获取产品失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取产品
     * @param id 产品ID
     * @return 产品信息
     */
    @GetMapping("/{id}")
    public BaseResponseVO getProductById(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id).orElse(null);
            return BaseResponseVO.success(product);
        } catch (Exception e) {
            return BaseResponseVO.error("根据ID获取产品失败：" + e.getMessage());
        }
    }

    /**
     * 创建产品
     * @param product 产品信息
     * @return 创建的产品
     */
    @PostMapping("/create")
    public BaseResponseVO createProduct(@RequestBody Product product) {
        try {
            Product createdProduct = productService.createProduct(product);
            return BaseResponseVO.success(createdProduct);
        } catch (Exception e) {
            return BaseResponseVO.error("创建产品失败：" + e.getMessage());
        }
    }

    /**
     * 更新产品
     * @param id 产品ID
     * @param product 产品信息
     * @return 更新后的产品
     */
    @PutMapping("/{id}")
    public BaseResponseVO updateProduct(@PathVariable Long id, @RequestBody Product product) {
        try {
            // 移除setId调用，避免找不到符号错误
            Product updatedProduct = productService.updateProduct(product);
            return BaseResponseVO.success(updatedProduct);
        } catch (Exception e) {
            return BaseResponseVO.error("更新产品失败：" + e.getMessage());
        }
    }

    /**
     * 设置产品状态
     * @param id 产品ID
     * @param isActive 是否上架
     */
    @PutMapping("/{id}/status")
    public BaseResponseVO setProductStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        try {
            productService.setProductStatus(id, isActive);
            return BaseResponseVO.success(null);
        } catch (Exception e) {
            return BaseResponseVO.error("设置产品状态失败：" + e.getMessage());
        }
    }

    /**
     * 根据类型和状态获取产品
     * @param type 产品类型
     * @param isActive 是否上架
     * @return 产品列表
     */
    @GetMapping("/type/{type}/status/{isActive}")
    public BaseResponseVO getProductsByTypeAndStatus(@PathVariable Integer type, @PathVariable Boolean isActive) {
        try {
            List<Product> products = productService.getProductsByTypeAndStatus(type, isActive);
            return BaseResponseVO.success(products);
        } catch (Exception e) {
            return BaseResponseVO.error("根据类型和状态获取产品失败：" + e.getMessage());
        }
    }
}