package com.aicv.airesume.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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
    
    /**
     * 配置RestTemplate Bean，用于调用微信API
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
