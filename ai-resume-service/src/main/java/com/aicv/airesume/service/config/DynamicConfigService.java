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
     * 获取完整的面试动态配置
     * @return 面试动态配置对象，如果不存在则返回空
     */
    public Optional<Map<String, Object>> getInterviewDynamicConfig() {
        Optional<String> configJson = getConfigValue("config", "interview_dynamic_config");
        if (configJson.isPresent()) {
            try {
                return Optional.of(objectMapper.readValue(configJson.get(), Map.class));
            } catch (Exception e) {
                logger.error("解析面试动态配置失败", e);
            }
        }
        return Optional.empty();
    }

    /**
     * 获取所有面试官风格配置（只返回启用状态的风格）
     * 优先从interview_dynamic_config读取，如不存在则从interview_personas读取，最后尝试从persona类型的配置读取
     * @return 面试官风格列表的Optional包装，与原有项目调用方式兼容
     */
    public Optional<List<Map<String, Object>>> getInterviewPersonas() {
        // 先尝试从interview_dynamic_config读取
        Optional<List<Map<String, Object>>> personasFromDynamic = getPersonasFromConfig("interview_dynamic_config", true);
        if (personasFromDynamic.isPresent()) {
            return personasFromDynamic;
        }
        
        // 如果interview_dynamic_config不存在或没有personas，尝试从interview_personas读取
        Optional<List<Map<String, Object>>> personasFromPersonasConfig = getPersonasFromConfig("interview_personas", true);
        if (personasFromPersonasConfig.isPresent()) {
            return personasFromPersonasConfig;
        }
        
        // 最后尝试从persona类型的配置读取
        return getPersonasFromPersonaTypeConfigs(true);
    }
    
    /**
     * 获取所有面试官风格配置（包括禁用的）
     * 优先从interview_dynamic_config读取，如不存在则从interview_personas读取，最后尝试从persona类型的配置读取
     * @return 所有面试官风格列表
     */
    public Optional<List<Map<String, Object>>> getAllInterviewPersonas() {
        // 先尝试从interview_dynamic_config读取
        Optional<List<Map<String, Object>>> personasFromDynamic = getPersonasFromConfig("interview_dynamic_config", false);
        if (personasFromDynamic.isPresent()) {
            return personasFromDynamic;
        }
        
        // 如果interview_dynamic_config不存在或没有personas，尝试从interview_personas读取
        Optional<List<Map<String, Object>>> personasFromPersonasConfig = getPersonasFromConfig("interview_personas", false);
        if (personasFromPersonasConfig.isPresent()) {
            return personasFromPersonasConfig;
        }
        
        // 最后尝试从persona类型的配置读取
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
            List<DynamicConfig> personaConfigs = dynamicConfigRepository.findByConfigTypeAndIsActiveTrue("persona");
            if (personaConfigs.isEmpty()) {
                return Optional.empty();
            }
            
            List<Map<String, Object>> personas = new ArrayList<>();
            for (DynamicConfig config : personaConfigs) {
                String configValue = config.getConfigValue();
                // 解析配置值为JSON
                JsonNode personaNode = objectMapper.readTree(configValue);
                
                Map<String, Object> persona = new HashMap<>();
                persona.put("id", config.getConfigKey());
                persona.put("name", personaNode.get("name").asText());
                persona.put("description", personaNode.get("description").asText());
                persona.put("prompt", personaNode.get("prompt").asText());
                persona.put("enabled", true); // 因为查询条件已经是isActiveTrue
                
                personas.add(persona);
            }
            
            return Optional.of(personas);
        } catch (Exception e) {
            logger.error("从persona类型配置获取面试官风格失败", e);
        }
        return Optional.empty();
    }
    
    /**
     * 从指定配置中读取面试官风格
     * @param configKey 配置键名
     * @param filterEnabled 是否只返回启用的风格
     * @return 面试官风格列表
     */
    private Optional<List<Map<String, Object>>> getPersonasFromConfig(String configKey, boolean filterEnabled) {
        try {
            String configValue = getConfigValue("config", configKey).orElse("{}");
            JsonNode configNode = objectMapper.readTree(configValue);
            
            // 不同配置的结构可能不同，需要适配
            JsonNode personasNode;
            if (configKey.equals("interview_personas") && configNode.has("personas")) {
                // interview_personas配置中，personas是一个对象的属性
                personasNode = configNode.get("personas");
            } else if (configKey.equals("interview_dynamic_config") && configNode.has("personas")) {
                // interview_dynamic_config配置中，personas也是一个对象的属性
                personasNode = configNode.get("personas");
            } else {
                return Optional.empty();
            }
            
            List<Map<String, Object>> personas = new ArrayList<>();
            for (JsonNode personaNode : personasNode) {
                // 如果需要过滤，且enabled为false，则跳过
                if (filterEnabled && personaNode.has("enabled") && !personaNode.get("enabled").asBoolean()) {
                    continue;
                }
                
                Map<String, Object> persona = new HashMap<>();
                persona.put("id", personaNode.get("id").asText());
                persona.put("name", personaNode.get("name").asText());
                persona.put("description", personaNode.get("description").asText());
                
                // 添加其他可能存在的字段
                if (personaNode.has("example")) {
                    persona.put("example", personaNode.get("example").asText());
                }
                if (personaNode.has("emoji")) {
                    persona.put("emoji", personaNode.get("emoji").asText());
                }
                if (personaNode.has("enabled")) {
                    persona.put("enabled", personaNode.get("enabled").asBoolean());
                } else if (!filterEnabled) {
                    // 如果不需要过滤且没有enabled字段，默认设为true
                    persona.put("enabled", true);
                }
                
                personas.add(persona);
            }
            return Optional.of(personas);
        } catch (Exception e) {
            logger.error("从配置[{}]获取面试官风格失败", configKey, e);
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