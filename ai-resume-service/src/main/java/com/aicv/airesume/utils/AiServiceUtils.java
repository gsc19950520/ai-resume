package com.aicv.airesume.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.aicv.airesume.entity.InterviewLog;
import com.aicv.airesume.repository.InterviewLogRepository;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Base64;

/**
 * AI服务工具类
 */
@Slf4j
@Component
public class AiServiceUtils {

    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekApiUrl;

    @Autowired
    private InterviewLogRepository interviewLogRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 使用OpenAI优化简历
     */
    public String optimizeResumeWithOpenAI(String originalContent, String jobType) {
        // 实现OpenAI API调用逻辑
        // 这里简化处理，实际项目中需要按照OpenAI API文档规范实现
        return "优化后的简历内容 (OpenAI)";
    }

    /**
     * 使用通义千问优化简历
     */
    public String optimizeResumeWithTongyi(String originalContent, String jobType) {
        // 实现通义千问API调用逻辑
        return "优化后的简历内容 (通义千问)";
    }

    /**
     * 使用文心一言优化简历
     */
    public String optimizeResumeWithWenxin(String originalContent, String jobType) {
        // 实现文心一言API调用逻辑
        return "优化后的简历内容 (文心一言)";
    }

    /**
     * 获取AI评分
     */
    public Integer getAiScore(String resumeContent, String jobType) {
        // 实现评分逻辑
        // 这里返回模拟分数，实际项目中需要通过AI评估
        return 85 + (int)(Math.random() * 10);
    }

    /**
     * 获取AI建议
     */
    public String getAiSuggestion(String resumeContent, String jobType) {
        // 实现建议生成逻辑
        Map<String, String> suggestions = new HashMap<>();
        suggestions.put("技术岗", "建议突出您的技术栈和项目经验，使用具体的数据成果来展示您的能力。");
        suggestions.put("市场岗", "建议强调您的市场分析能力和营销活动经验，突出您对市场趋势的把握。");
        suggestions.put("设计岗", "建议添加作品集链接，详细描述设计思路和使用的设计工具。");
        suggestions.put("销售岗", "建议量化您的销售业绩，使用具体的数字和百分比来展示您的成就。");
        suggestions.put("管理岗", "建议突出您的团队管理经验和项目交付能力，强调领导力和决策能力。");
        
        return suggestions.getOrDefault(jobType, "您的简历整体不错，建议进一步完善个人技能和项目经验的描述。");
    }
    
    /**
     * 计算文本的语义哈希
     * @param text 需要计算哈希的文本
     * @return 语义哈希值（MD5格式）
     */
    public String getSemanticHash(String text) {
        try {
            // 实际项目中，这里应该调用AI embedding接口获取语义向量
            // 然后对向量进行哈希计算
            // 这里使用简单的MD5实现作为模拟
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            // 如果计算失败，返回文本本身的哈希作为备选
            return text.hashCode() + "_fallback";
        }
    }

    /**
     * 调用DeepSeek API（同步方式）
     * @param prompt 提示词
     * @return API响应内容
     */
    public String callDeepSeekApi(String prompt) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("stream", false);
            
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            
            requestBody.put("messages", new Object[]{message});
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            // 构建HTTP头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepseekApiKey);

            // 创建HttpEntity
            HttpEntity<String> requestEntity = new HttpEntity<>(
                JSONObject.toJSONString(requestBody), headers);

            log.info("Sending request to DeepSeek API: {}", JSONObject.toJSONString(requestBody));
            
            // 发送同步请求
            ResponseEntity<String> response = restTemplate.exchange(
                deepseekApiUrl, HttpMethod.POST, requestEntity, String.class);
            log.info("Received response from DeepSeek API: {}", response.getBody());
            
            // 解析响应
            if (response.getStatusCodeValue() == 200) {
                JSONObject body = JSONObject.parseObject(response.getBody());
                if (body.containsKey("choices") && !body.getJSONArray("choices").isEmpty()) {
                    JSONObject choice = body.getJSONArray("choices").getJSONObject(0);
                    if (choice.containsKey("message")) {
                        return choice.getJSONObject("message").getString("content");
                    }
                }
            }
            return "";
        } catch (Exception e) {
            log.error("Error calling DeepSeek API: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 调用DeepSeek API（流式输出方式）
     * @param prompt 提示词
     * @param emitter SSE发射器，用于流式输出
     * @param onComplete 流结束回调函数
     * @param sessionId 会话ID，用于保存元数据
     */
    public void callDeepSeekApiStream(String prompt, SseEmitter emitter, Runnable onComplete, String sessionId) {
        // 默认使用"question"作为事件名称
        callDeepSeekApiStream(prompt, emitter, onComplete, sessionId, "question");
    }
    
    /**
     * 调用DeepSeek API（流式输出方式，支持自定义事件名称）
     * @param prompt 提示词
     * @param emitter SSE发射器，用于流式输出
     * @param onComplete 流结束回调函数
     * @param sessionId 会话ID，用于保存元数据
     * @param eventName 事件名称
     */
    public void callDeepSeekApiStream(String prompt, SseEmitter emitter, Runnable onComplete, String sessionId, String eventName) {
        executorService.submit(() -> {
            try {
                // 构建请求体
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "deepseek-chat");
                requestBody.put("stream", true); // 启用流式输出
                
                Map<String, String> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);
                
                requestBody.put("messages", new Object[]{message});
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 20000);

                log.info("Sending streaming request to DeepSeek API: {}", JSONObject.toJSONString(requestBody));
                
                // 元数据提取相关变量 - 使用数组包装以便在lambda中修改
                boolean[] isExtractingMetadata = {false};
                StringBuilder[] metadataBuilder = {new StringBuilder()};
                StringBuilder[] currentContentBuffer = {new StringBuilder()}; // 用于缓冲当前内容，处理标记被拆分的情况
                // 用于标记元数据是否已处理完成
                boolean[] metadataProcessed = {false};
                // 用于累积完整的问题文本
                StringBuilder[] fullQuestionBuffer = {new StringBuilder()};
                
                // 使用WebClient发送流式请求
                webClient.post()
                        .uri(deepseekApiUrl)
                        .header("Authorization", "Bearer " + deepseekApiKey)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .bodyValue(JSONObject.toJSONString(requestBody))
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .subscribe(
                                // 处理每一行流数据
                                line -> {
                                    try {
                                        log.debug("Received streaming line: {}", line);
                                        // 跳过空行
                                        if (line == null || line.isEmpty()) {
                                            return;
                                        }
                                        // 跳过结束标记
                                        if (line.equals("data: [DONE]") || line.equals("[DONE]")) {
                                            return;
                                        }
                                        // 提取JSON部分
                                        String jsonStr = line;
                                        // 如果line以"data: "开头，提取后面的JSON部分
                                        if (line.startsWith("data: ")) {
                                            jsonStr = line.substring(6);
                                        }
                                        // 去除可能的引号（如果line是被引号包裹的完整JSON字符串）
                                        if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"") && jsonStr.length() > 1) {
                                            jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                                        }
                                        
                                        // 解析JSON获取content
                                        JSONObject json = JSONObject.parseObject(jsonStr);
                                        if (json.containsKey("choices") && !json.getJSONArray("choices").isEmpty()) {
                                            JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
                                            if (choice.containsKey("delta")) {
                                                JSONObject delta = choice.getJSONObject("delta");
                                                if (delta.containsKey("content")) {
                                                    String content = delta.getString("content");
                                                    if (content != null && !content.isEmpty()) {
                                                        // 对于面试报告（eventName为"report"），直接流式输出内容，不需要等待元数据
                                        if ("report".equals(eventName)) {
                                            emitter.send(SseEmitter.event()
                                                    .name(eventName)
                                                    .data(content));
                                            // 同时将内容添加到完整问题缓冲区
                                            fullQuestionBuffer[0].append(content);
                                        }
                                        // 如果元数据已经处理完成，直接流式输出内容
                                        else if (metadataProcessed[0]) {
                                            emitter.send(SseEmitter.event()
                                                    .name(eventName)
                                                    .data(content));
                                            // 同时将内容添加到完整问题缓冲区
                                            fullQuestionBuffer[0].append(content);
                                        }
                                        // 否则继续累积内容，等待完整的元数据块
                                        else {
                                            // 将当前内容添加到缓冲区
                                            currentContentBuffer[0].append(content);
                                            String buffer = currentContentBuffer[0].toString();
                                                
                                            // 检查缓冲区中是否包含完整的元数据块（包括开始和结束标记）
                                            int startIndex = buffer.indexOf("# 元数据开始");
                                            int endIndex = buffer.indexOf("# 元数据结束");
                                                
                                            // 只有当缓冲区包含完整的元数据块时，才处理元数据
                                            if (startIndex != -1 && endIndex != -1) {
                                                // 提取完整的元数据内容（包括开始和结束标记之间的所有内容）
                                                String metadataContent = buffer.substring(startIndex, endIndex + "# 元数据结束".length());
                                                log.info("Extracted complete metadata block: {}", metadataContent);
                                                
                                                // 提取元数据JSON部分（只保留开始和结束标记之间的内容）
                                                String metadataJsonStr = buffer.substring(startIndex + "# 元数据开始".length(), endIndex).trim();
                                                if (!metadataJsonStr.isEmpty()) {
                                                    // 异步保存元数据到数据库
                                                    saveMetadataAsync(metadataJsonStr, sessionId);
                                                }
                                                
                                                // 如果元数据块后面有内容，开始流式输出问题
                                                if (endIndex + "# 元数据结束".length() < buffer.length()) {
                                                    String questionPart = buffer.substring(endIndex + "# 元数据结束".length());
                                                    if (!questionPart.isEmpty()) {
                                                        emitter.send(SseEmitter.event()
                                                                .name(eventName)
                                                                .data(questionPart));
                                                        // 同时将内容添加到完整问题缓冲区
                                                        fullQuestionBuffer[0].append(questionPart);
                                                    }
                                                }
                                                
                                                // 元数据已处理完成，设置标志
                                                metadataProcessed[0] = true;
                                                // 清空缓冲区，准备接收后续内容
                                                currentContentBuffer[0].setLength(0);
                                            }
                                            // 如果缓冲区还没有完整的元数据块，继续累积内容
                                            else {
                                                // 不发送任何内容，继续在缓冲区累积
                                            }
                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.error("Error processing streaming line: {}", e.getMessage(), e);
                                        emitter.completeWithError(e);
                                    }
                                },
                                // 处理错误
                                error -> {
                                    log.error("Error in streaming response: {}", error.getMessage(), error);
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .name("error")
                                                .data("流式响应错误: " + error.getMessage()));
                                    } catch (IOException ioException) {
                                        log.error("Failed to send error event: {}", ioException.getMessage(), ioException);
                                    }
                                    emitter.completeWithError(error);
                                },
                                // 流结束
                                () -> {
                                    log.info("Streaming response completed");
                                    
                                    // 保存完整的问题文本到数据库
                                    String fullQuestion = fullQuestionBuffer[0].toString().trim();
                                    if (!fullQuestion.isEmpty()) {
                                        // 异步保存完整问题到数据库
                                        saveQuestionAsync(fullQuestion, sessionId);
                                    }
                                    
                                    // 调用回调函数通知流结束
                                    if (onComplete != null) {
                                        onComplete.run();
                                    }
                                }
                        );
            } catch (Exception e) {
                log.error("Error setting up streaming request: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("设置流式请求错误: " + e.getMessage()));
                } catch (IOException ioException) {
                    log.error("Failed to send error event: {}", ioException.getMessage(), ioException);
                }
                emitter.completeWithError(e);
            }
        });
    }
    
    /**
     * 异步保存元数据到数据库
     *
     * @param metadataJsonStr 元数据JSON字符串
     * @param sessionId 会话ID
     */
    private void saveMetadataAsync(String metadataJsonStr, String sessionId) {
        executorService.submit(() -> {
            try {
                // 解析元数据
                JSONObject metadata = JSONObject.parseObject(metadataJsonStr);
                String depthLevel = metadata.getString("depthLevel");
                JSONArray expectedKeyPointsJson = metadata.getJSONArray("expectedKeyPoints");
                String relatedTech = metadata.getString("relatedTech");
                String relatedProjectPoint = metadata.getString("relatedProjectPoint");
                
                // 将JSONArray转换为List
                List<String> expectedKeyPoints = new ArrayList<>();
                for (int i = 0; i < expectedKeyPointsJson.size(); i++) {
                    expectedKeyPoints.add(expectedKeyPointsJson.getString(i));
                }
                
                // 获取最新的面试日志，更新元数据
                List<InterviewLog> logs = interviewLogRepository.findBySessionIdOrderByRoundNumberDesc(sessionId);
                if (!logs.isEmpty()) {
                    InterviewLog latestLog = logs.get(0);
                    latestLog.setDepthLevel(depthLevel);
                    latestLog.setExpectedKeyPoints(JSON.toJSONString(expectedKeyPoints));
                    latestLog.setRelatedTechItems(relatedTech);
                    latestLog.setRelatedProjectPoints(relatedProjectPoint);
                    
                    // 保存到数据库
                    interviewLogRepository.save(latestLog);
                    log.info("元数据保存成功，会话ID：{}", sessionId);
                }
            } catch (Exception e) {
                log.error("解析并保存元数据失败：{}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步保存完整问题文本到数据库
     *
     * @param questionText 完整的问题文本
     * @param sessionId 会话ID
     */
    private void saveQuestionAsync(String questionText, String sessionId) {
        executorService.submit(() -> {
            try {
                // 获取最新的面试日志，更新问题文本
                List<InterviewLog> logs = interviewLogRepository.findBySessionIdOrderByRoundNumberDesc(sessionId);
                if (!logs.isEmpty()) {
                    InterviewLog latestLog = logs.get(0);
                    latestLog.setQuestionText(questionText);
                    
                    // 保存到数据库
                    interviewLogRepository.save(latestLog);
                    log.info("完整问题保存成功，会话ID：{}，问题文本：{}", sessionId, questionText);
                }
            } catch (Exception e) {
                log.error("保存完整问题失败：{}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * 评估面试回答
     * @param questionText 问题文本
     * @param userAnswerText 用户回答文本
     * @param expectedKeyPoints 期望的关键点
     * @param depthLevel 当前问题的深度级别
     * @param relatedTech 相关技术点
     * @param persona 面试官风格
     * @return 评分结果，包含技术分、逻辑分、清晰度分、深度分
     */
    public Map<String, Double> assessInterviewAnswer(String questionText, String userAnswerText, List<String> expectedKeyPoints, String depthLevel, String relatedTech, String persona) {
        // 构建评分prompt，结合元数据进行更准确的评分
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为一个专业的面试官，请根据以下信息对候选人的回答进行评分：\n");
        prompt.append("\n1. 问题：");
        prompt.append(questionText);
        prompt.append("\n2. 问题深度级别：");
        prompt.append(depthLevel);
        prompt.append("\n3. 相关技术点：");
        prompt.append(relatedTech);
        prompt.append("\n4. 期望的关键点：");
        prompt.append(expectedKeyPoints != null ? expectedKeyPoints.toString() : "无");
        prompt.append("\n5. 候选人回答：");
        prompt.append(userAnswerText);
        prompt.append("\n6. 面试官风格：");
        prompt.append(persona);
        prompt.append("\n\n请按照以下格式返回评分结果，只返回JSON，不要添加任何其他说明：");
        prompt.append("\n{");
        prompt.append("\"tech\": 技术分（0-100）,");
        prompt.append("\"logic\": 逻辑分（0-100）,");
        prompt.append("\"clarity\": 清晰度分（0-100）,");
        prompt.append("\"depth\": 深度分（0-100）");
        prompt.append("}");
        
        try {
            // 调用DeepSeek API进行评分
            // 构建请求参数
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的技术面试官，擅长对候选人的回答进行评分。");
            messages.add(systemMessage);
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt.toString());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            log.info("调用deepseek计算评分请求参数：{}", JSONObject.toJSONString(requestBody));
            String response = webClient.post()
                    .uri(deepseekApiUrl)
                    .header("Authorization", "Bearer " + deepseekApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("调用deepseek计算评分响应结果：{}", JSONObject.toJSONString(response));
            // 解析评分结果
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();
            return objectMapper.readValue(content, new TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            log.error("Error assessing interview answer: {}", e.getMessage(), e);
            // 如果调用失败，返回默认评分
            Map<String, Double> defaultScores = new HashMap<>();
            defaultScores.put("tech", 75.0);
            defaultScores.put("logic", 75.0);
            defaultScores.put("clarity", 75.0);
            defaultScores.put("depth", 75.0);
            return defaultScores;
        }
    }
}