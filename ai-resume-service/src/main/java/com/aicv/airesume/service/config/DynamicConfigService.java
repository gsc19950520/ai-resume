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
     * 优先从interview_dynamic_config读取，如不存在则从interview_personas读取
     * @return 面试官风格列表的Optional包装，与原有项目调用方式兼容
     */
    public Optional<List<Map<String, Object>>> getInterviewPersonas() {
        // 先尝试从interview_dynamic_config读取
        Optional<List<Map<String, Object>>> personasFromDynamic = getPersonasFromConfig("interview_dynamic_config", true);
        if (personasFromDynamic.isPresent()) {
            return personasFromDynamic;
        }
        
        // 如果interview_dynamic_config不存在或没有personas，尝试从interview_personas读取
        return getPersonasFromConfig("interview_personas", true);
    }
    
    /**
     * 获取所有面试官风格配置（包括禁用的）
     * 优先从interview_dynamic_config读取，如不存在则从interview_personas读取
     * @return 所有面试官风格列表
     */
    public Optional<List<Map<String, Object>>> getAllInterviewPersonas() {
        // 先尝试从interview_dynamic_config读取
        Optional<List<Map<String, Object>>> personasFromDynamic = getPersonasFromConfig("interview_dynamic_config", false);
        if (personasFromDynamic.isPresent()) {
            return personasFromDynamic;
        }
        
        // 如果interview_dynamic_config不存在或没有personas，尝试从interview_personas读取
        return getPersonasFromConfig("interview_personas", false);
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
     * 获取问题深度级别配置
     * 优先从interview_dynamic_config读取，如不存在则从interview_depth_levels读取
     * @return 深度级别配置列表，如果不存在则返回空
     */
    public Optional<List<Map<String, Object>>> getInterviewDepthLevels() {
        try {
            // 优先从interview_dynamic_config配置中读取深度级别
            String configValue = getConfigValue("config", "interview_dynamic_config").orElse("{}");
            JsonNode configNode = objectMapper.readTree(configValue);
            if (configNode.has("depthLevels")) {
                List<Map<String, Object>> depthLevels = new ArrayList<>();
                for (JsonNode levelNode : configNode.get("depthLevels")) {
                    Map<String, Object> level = new HashMap<>();
                    // 映射字段
                    if (levelNode.has("id")) level.put("id", levelNode.get("id").asText());
                    if (levelNode.has("name")) level.put("name", levelNode.get("name").asText());
                    if (levelNode.has("text")) level.put("text", levelNode.get("text").asText());
                    if (levelNode.has("description")) level.put("description", levelNode.get("description").asText());
                    depthLevels.add(level);
                }
                return Optional.of(depthLevels);
            }
        } catch (Exception e) {
            logger.error("从interview_dynamic_config获取深度级别配置失败", e);
        }
        
        // 如果interview_dynamic_config中没有，尝试从interview_depth_levels读取
        try {
            String configValue = getConfigValue("config", "interview_depth_levels").orElse("[]");
            // interview_depth_levels可能直接是数组格式
            JsonNode rootNode = objectMapper.readTree(configValue);
            if (rootNode.isArray()) {
                List<Map<String, Object>> depthLevels = new ArrayList<>();
                for (JsonNode levelNode : rootNode) {
                    Map<String, Object> level = new HashMap<>();
                    if (levelNode.has("id")) level.put("id", levelNode.get("id").asText());
                    if (levelNode.has("name")) level.put("name", levelNode.get("name").asText());
                    if (levelNode.has("text")) level.put("text", levelNode.get("text").asText());
                    if (levelNode.has("description")) level.put("description", levelNode.get("description").asText());
                    depthLevels.add(level);
                }
                return Optional.of(depthLevels);
            }
        } catch (Exception e) {
            logger.error("从interview_depth_levels获取深度级别配置失败", e);
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
     * 获取默认面试官风格
     * @return 默认面试官风格ID，使用原有项目的默认值"friendly"
     */
    public String getDefaultPersona() {
        // 使用原有项目的默认面试官风格，保持兼容性
        return getConfigValue("config", "default_persona").orElse("friendly");
    }

    /**
     * 刷新配置缓存
     */
    public void refreshCache() {
        configCache.clear();
        logger.info("动态配置缓存已刷新");
    }
}