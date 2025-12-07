package com.aicv.airesume.service.config;

import com.aicv.airesume.entity.DynamicConfig;
import com.aicv.airesume.repository.DynamicConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 动态配置服务类，用于读取和管理系统中的动态配置数据
 */
@Service
public class DynamicConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfigService.class);

    @Autowired
    private DynamicConfigRepository dynamicConfigRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, String> configCache = new HashMap<>();

    /**
     * 根据配置类型和键获取配置值
     * @param configType 配置类型
     * @param configKey 配置键
     * @return 配置值，如果不存在则返回空
     */
    public Optional<String> getConfigValue(String configType, String configKey) {
        String cacheKey = configType + ":" + configKey;
        
        // 检查缓存
        if (configCache.containsKey(cacheKey)) {
            return Optional.ofNullable(configCache.get(cacheKey));
        }
        
        // 从数据库查询
        try {
            Optional<DynamicConfig> configOptional = dynamicConfigRepository.findByConfigTypeAndConfigKeyAndIsActiveTrue(configType, configKey);
            if (configOptional.isPresent()) {
                String value = configOptional.get().getConfigValue();
                configCache.put(cacheKey, value);
                return Optional.of(value);
            }
        } catch (Exception e) {
            logger.error("获取动态配置失败: type={}, key={}", configType, configKey, e);
        }
        
        return Optional.empty();
    }



    /**
     * 获取所有面试官风格配置（只返回启用状态的风格）
     * 直接从persona类型的配置读取
     * @return 面试官风格列表的Optional包装，与原有项目调用方式兼容
     */
    public Optional<List<Map<String, Object>>> getInterviewPersonas() {
        return getPersonasFromPersonaTypeConfigs(true);
    }
    
    /**
     * 获取所有面试官风格配置（只返回启用状态的风格）
     * 直接返回DynamicConfig实体类对象列表，用于优化性能和类型安全
     * @return 面试官风格的DynamicConfig列表的Optional包装
     */
    public Optional<List<DynamicConfig>> getInterviewPersonasAsDynamicConfigs() {
        try {
            // 直接从persona类型的配置读取，使用最新的配置方式
            List<DynamicConfig> personaConfigs = dynamicConfigRepository.findByConfigTypeAndIsActiveTrue("persona");
            if (!personaConfigs.isEmpty()) {
                return Optional.of(personaConfigs);
            }
        } catch (Exception e) {
            logger.error("从persona类型配置获取面试官风格失败", e);
        }
        return Optional.empty();
    }
    
    /**
     * 获取所有面试官风格配置（包括禁用的）
     * 直接从persona类型的配置读取
     * @return 所有面试官风格列表
     */
    public Optional<List<Map<String, Object>>> getAllInterviewPersonas() {
        return getPersonasFromPersonaTypeConfigs(false);
    }
    
    /**
     * 从persona类型的配置中读取所有面试官风格
     * @param filterEnabled 是否只返回启用的风格
     * @return 面试官风格列表
     */
    private Optional<List<Map<String, Object>>> getPersonasFromPersonaTypeConfigs(boolean filterEnabled) {
        try {
            // 从数据库查询所有persona类型的配置
            List<DynamicConfig> personaConfigs;
            if (filterEnabled) {
                personaConfigs = dynamicConfigRepository.findByConfigTypeAndIsActiveTrue("persona");
            } else {
                // 获取所有persona类型配置，包括禁用的
                personaConfigs = dynamicConfigRepository.findByConfigType("persona");
            }
            
            if (personaConfigs.isEmpty()) {
                return Optional.empty();
            }
            
            List<Map<String, Object>> personas = new ArrayList<>();
            for (DynamicConfig config : personaConfigs) {
                Map<String, Object> persona = new HashMap<>();
                persona.put("id", config.getConfigKey());
                persona.put("name", config.getDescription()); // description存储风格名称
                persona.put("description", config.getDescription()); // 保持字段一致性
                persona.put("prompt", config.getConfigValue()); // config_value直接存储prompt字符串
                persona.put("enabled", config.getIsActive()); // 直接使用配置中的is_active字段
                
                personas.add(persona);
            }
            
            return Optional.of(personas);
        } catch (Exception e) {
            logger.error("从persona类型配置获取面试官风格失败", e);
        }
        return Optional.empty();
    }
    


    /**
     * 获取默认会话时长
     * @return 默认会话时长（秒），默认为900秒
     */
    public int getDefaultSessionSeconds() {
        // 保持与原有项目一致的默认值900秒
        return getConfigValue("system", "default_session_seconds")
                .map(Integer::parseInt)
                .orElse(900);
    }

    /**
     * 获取报告生成的系统提示词
     * @return 报告生成系统提示词，如果不存在则返回空
     */
    public Optional<String> getReportGenerationSystemPrompt() {
        return getConfigValue("REPORT", "REPORT_GENERATION_SYSTEM_PROMPT");
    }

    /**
     * 刷新配置缓存
     */
    public void refreshCache() {
        configCache.clear();
        logger.info("动态配置缓存已刷新");
    }
}