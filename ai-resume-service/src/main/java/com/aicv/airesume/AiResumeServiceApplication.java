package com.aicv.airesume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * AI简历优化小程序后端应用主类
 */
@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
		org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class
})
public class AiResumeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiResumeServiceApplication.class, args);
    }

    /**
     * 配置跨域请求
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
