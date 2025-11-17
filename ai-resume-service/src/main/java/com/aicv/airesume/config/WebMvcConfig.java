package com.aicv.airesume.config;

import com.aicv.airesume.interceptor.TokenRefreshInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TokenRefreshInterceptor tokenRefreshInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("X-New-Token", "X-Token-Refreshed"); // 暴露新token头信息
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册token刷新拦截器，拦截所有需要认证的API接口
        registry.addInterceptor(tokenRefreshInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login", "/api/user/register", "/api/user/wechat/login"); // 排除登录相关的接口
    }
}