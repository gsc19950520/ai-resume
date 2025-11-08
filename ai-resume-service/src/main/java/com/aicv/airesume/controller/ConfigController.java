package com.aicv.airesume.controller;

import com.aicv.airesume.service.config.DynamicConfigService;
import com.aicv.airesume.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置相关接口控制器
 */
@RestController
@Slf4j
public class ConfigController {

    @Autowired
    private DynamicConfigService dynamicConfigService;

    /**
     * 获取面试配置
     * @return 面试相关配置信息
     */
    @GetMapping("/config/interview")
    public Map<String, Object> getInterviewConfig() {
        try {
            // 获取面试相关配置
            Map<String, Object> config = new HashMap<>();
            
            // 获取面试官风格配置（只返回启用的）
            dynamicConfigService.getInterviewPersonas().ifPresent(personas -> {
                config.put("personas", personas);
            });
            
            // 获取深度级别配置
            dynamicConfigService.getInterviewDepthLevels().ifPresent(depthLevels -> {
                config.put("depthLevels", depthLevels);
            });
            
            // 获取默认会话时长
            config.put("defaultSessionSeconds", dynamicConfigService.getDefaultSessionSeconds());
            
            // 获取默认面试官风格
            config.put("defaultPersona", dynamicConfigService.getDefaultPersona());
            
            return ResponseUtils.success(config);
        } catch (Exception e) {
            log.error("获取面试配置失败", e);
            return ResponseUtils.error("获取面试配置失败：" + e.getMessage());
        }
    }
}