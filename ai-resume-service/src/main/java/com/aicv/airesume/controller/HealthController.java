package com.aicv.airesume.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * 提供简单的健康检查接口，用于云托管平台检测应用状态
 */
@RestController
public class HealthController {

    /**
     * 健康检查接口
     * @return 健康状态信息
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    /**
     * 根路径检查接口
     * @return 应用状态信息
     */
    @GetMapping("/")
    public String index() {
        return "AI Resume Service is running";
    }
}
