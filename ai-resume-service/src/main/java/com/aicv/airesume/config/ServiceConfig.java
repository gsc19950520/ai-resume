package com.aicv.airesume.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 服务配置类
 */
@Configuration
@ComponentScan({
    "com.aicv.airesume.service",
    "com.aicv.airesume.controller",
    "com.aicv.airesume.repository"
})
public class ServiceConfig {
    // 服务配置
}
