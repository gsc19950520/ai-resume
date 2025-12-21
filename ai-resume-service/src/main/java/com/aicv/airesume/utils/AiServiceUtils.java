package com.aicv.airesume.utils;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.aicv.airesume.entity.InterviewLog;
import com.aicv.airesume.entity.InterviewSession;
import com.aicv.airesume.repository.InterviewLogRepository;
import com.aicv.airesume.repository.InterviewSessionRepository;
import com.aicv.airesume.service.config.DynamicConfigService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
    
    @Autowired
    private InterviewSessionRepository interviewSessionRepository;
    
    @Autowired
    private DynamicConfigService dynamicConfigService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
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
        return callDeepSeekApi(prompt, null);
    }
    
    /**
     * 调用DeepSeek API（同步方式，支持上下文）
     * @param prompt 提示词
     * @param sessionId 会话ID，用于获取对话历史
     * @return API响应内容
     */
    public String callDeepSeekApi(String prompt, String sessionId) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("stream", false);
            
            // 获取完整的对话历史
            List<Map<String, String>> messages = getConversationHistory(sessionId, prompt);
            
            requestBody.put("messages", messages);
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
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param emitter SSE发射器，用于流式输出
     * @param onComplete 流结束回调函数
     * @param sessionId 会话ID，用于保存元数据
     */
    public void callDeepSeekApiStream(String systemPrompt, String userPrompt, SseEmitter emitter, Runnable onComplete, String sessionId) {
        // 默认使用"question"作为事件名称
        callDeepSeekApiStream(systemPrompt, userPrompt, emitter, onComplete, sessionId, "question");
    }
    
    /**
     * 调用DeepSeek API（流式输出方式，支持自定义事件名称）
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param emitter SSE发射器，用于流式输出
     * @param onComplete 流结束回调函数
     * @param sessionId 会话ID，用于保存元数据
     * @param eventName 事件名称
     */
    public void callDeepSeekApiStream(String systemPrompt, String userPrompt, SseEmitter emitter, Runnable onComplete, String sessionId, String eventName) {
        callDeepSeekApiStream(systemPrompt, userPrompt, emitter, null, onComplete, sessionId, eventName);
    }
    
    /**
     * 获取对话历史
     * @param sessionId 会话ID
     * @param userPrompt 当前用户输入
     * @return 完整的对话历史消息列表
     */
    private List<Map<String, String>> getConversationHistory(String sessionId, String userPrompt) {
        return getConversationHistory(sessionId, null, userPrompt);
    }
    
    /**
     * 获取对话历史
     * @param sessionId 会话ID
     * @param systemPrompt 系统提示词
     * @param userPrompt 当前用户输入
     * @return 完整的对话历史消息列表
     */
    private List<Map<String, String>> getConversationHistory(String sessionId, String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 如果有sessionId，获取历史对话
        if (sessionId != null) {
            try {
                // 首先获取InterviewSession，添加系统级别的简历内容和面试要求
                Optional<InterviewSession> sessionOptional = interviewSessionRepository.findBySessionId(sessionId);
                if (sessionOptional.isPresent()) {
                    InterviewSession session = sessionOptional.get();
                    
                    // 添加简历内容作为系统消息（仅在会话开始时添加一次）
                    if (StringUtils.hasText(session.getResumeContent())) {
                        Map<String, String> resumeMessage = new HashMap<>();
                        resumeMessage.put("role", "system");
                        resumeMessage.put("content", "以下是候选人的完整简历内容：\n" + session.getResumeContent());
                        messages.add(resumeMessage);
                    }
                    
                    // 添加面试要求作为系统消息
                    String interviewRequirements = dynamicConfigService.getConfigValue("INTERVIEW", "INTERVIEW_REQUIREMENTS").orElse(null);
                    if (StringUtils.hasText(interviewRequirements)) {
                        Map<String, String> requirementsMessage = new HashMap<>();
                        requirementsMessage.put("role", "system");
                        requirementsMessage.put("content", interviewRequirements);
                        messages.add(requirementsMessage);
                    }
                    
                    // 添加通用规则作为系统消息
                    String generalRules = dynamicConfigService.getConfigValue("INTERVIEW", "GENERAL_RULES").orElse(null);
                    if (StringUtils.hasText(generalRules)) {
                        Map<String, String> rulesMessage = new HashMap<>();
                        rulesMessage.put("role", "system");
                        rulesMessage.put("content", generalRules);
                        messages.add(rulesMessage);
                    }
                    
                    // 添加自定义系统提示词（如果有）
                    if (StringUtils.hasText(systemPrompt)) {
                        Map<String, String> customSystemMessage = new HashMap<>();
                        customSystemMessage.put("role", "system");
                        customSystemMessage.put("content", systemPrompt);
                        messages.add(customSystemMessage);
                    }
                }
                
                // 从InterviewLog获取该sessionId下的所有面试记录
                List<InterviewLog> interviewLogs = interviewLogRepository.findBySessionIdOrderByRoundNumberAsc(sessionId);
                
                for (InterviewLog log : interviewLogs) {
                    // 添加AI问题作为assistant角色
                    if (log.getQuestionText() != null && !log.getQuestionText().isEmpty()) {
                        Map<String, String> assistantMessage = new HashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", log.getQuestionText());
                        messages.add(assistantMessage);
                    }
                    
                    // 添加用户回答作为user角色
                    if (log.getUserAnswerText() != null && !log.getUserAnswerText().isEmpty()) {
                        Map<String, String> userMessage = new HashMap<>();
                        userMessage.put("role", "user");
                        userMessage.put("content", log.getUserAnswerText());
                        messages.add(userMessage);
                    }
                    
                }
            } catch (Exception e) {
                log.error("获取对话历史失败: {}", e.getMessage(), e);
            }
        } else {
            // 如果没有sessionId，但有系统提示词，也添加为系统消息
            if (StringUtils.hasText(systemPrompt)) {
                Map<String, String> customSystemMessage = new HashMap<>();
                customSystemMessage.put("role", "system");
                customSystemMessage.put("content", systemPrompt);
                messages.add(customSystemMessage);
            }
        }
        
        // 添加当前用户输入
        Map<String, String> currentMessage = new HashMap<>();
        currentMessage.put("role", "user");
        currentMessage.put("content", userPrompt);
        messages.add(currentMessage);
        
        return messages;
    }
    
    /**
     * 调用DeepSeek API（流式输出方式，支持自定义事件名称）
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param emitter SSE发射器，用于流式输出
     * @param contentCallback 内容回调函数
     * @param onComplete 流结束回调函数
     * @param sessionId 会话ID，用于保存元数据和获取对话历史
     * @param eventName 事件名称
     */
    public void callDeepSeekApiStream(String systemPrompt, String userPrompt, SseEmitter emitter, Consumer<String> contentCallback, Runnable onComplete, String sessionId, String eventName) {
        // 标记 emitter 是否已经关闭
        AtomicBoolean emitterClosed = new AtomicBoolean(false);

        // 心跳线程，保持 SSE 连接活跃，防止云托管环境断开
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (!emitterClosed.get()) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data(" "));
                } catch (IOException ignored) {}
            }
        }, 0, 5, TimeUnit.SECONDS);

        executorService.submit(() -> {
            try {
                // ======= 原有请求体逻辑 =======
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "deepseek-chat");
                requestBody.put("stream", true);

                // 获取对话历史，报告生成时也需要system prompt
                List<Map<String, String>> messages;
                if ("report".equals(eventName)) {
                    // 报告生成：使用system prompt和当前user prompt
                    messages = new ArrayList<>();
                    
                    // 添加system prompt
                    if (StringUtils.hasText(systemPrompt)) {
                        Map<String, String> systemMessage = new HashMap<>();
                        systemMessage.put("role", "system");
                        systemMessage.put("content", systemPrompt);
                        messages.add(systemMessage);
                    }
                    
                    // 添加user prompt
                    Map<String, String> userMessage = new HashMap<>();
                    userMessage.put("role", "user");
                    userMessage.put("content", userPrompt);
                    messages.add(userMessage);
                } else {
                    // 其他情况：获取完整的对话历史
                    messages = getConversationHistory(sessionId, systemPrompt, userPrompt);
                }
                requestBody.put("messages", messages);

                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 5000);

                log.info("Sending streaming request to DeepSeek API: {}", JSONObject.toJSONString(requestBody));

                // ======= 流式请求变量 =======
                StringBuilder[] fullQuestionBuffer = {new StringBuilder()};
                // 移除了元数据处理相关变量

                // ======= WebClient 流式请求 =======
                webClient.post()
                        .uri(deepseekApiUrl)
                        .header("Authorization", "Bearer " + deepseekApiKey)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .bodyValue(JSONObject.toJSONString(requestBody))
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .subscribe(
                                line -> {
                                    try {
                                        if (emitterClosed.get()) return;

                                        if (line == null || line.isEmpty()) return;
                                        if ("data: [DONE]".equals(line) || "[DONE]".equals(line)) return;

                                        String jsonStr = line;
                                        if (jsonStr.startsWith("data: ")) {
                                            jsonStr = jsonStr.substring(6);
                                        }
                                        if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"") && jsonStr.length() > 1) {
                                            jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                                        }

                                        JSONObject json = JSONObject.parseObject(jsonStr);
                                        if (!json.containsKey("choices")) return;
                                        JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
                                        if (!choice.containsKey("delta")) return;

                                        JSONObject delta = choice.getJSONObject("delta");
                                        if (!delta.containsKey("content")) return;

                                        String content = delta.getString("content");
                                        if (content == null || content.isEmpty()) return;

                                        // ======= 保留原业务逻辑，只加 flush =======
                                        // 调用内容回调函数（如果不为null）
                                        if (contentCallback != null) {
                                            contentCallback.accept(content);
                                        }
                                        
                                        if (emitter != null) {
                                            // 直接流式输出内容，不进行元数据处理
                                            if (!emitterClosed.get()) {
                                                emitter.send(SseEmitter.event().name(eventName).data(content));
                                            }
                                            fullQuestionBuffer[0].append(content);
                                        }
                                    } catch (Exception e) {
                                        log.error("Error processing streaming line: {}", e.getMessage(), e);
                                        safeCompleteWithError(emitter, emitterClosed, e);
                                    }
                                },
                                error -> {
                                    log.error("Error in streaming response: {}", error.getMessage(), error);
                                    if (!emitterClosed.get()) {
                                        try {
                                            emitter.send(SseEmitter.event().name("error").data("流式响应错误: " + error.getMessage()));
                                        } catch (IOException ignored) {}
                                    }
                                    safeCompleteWithError(emitter, emitterClosed, error);
                                },
                                () -> {
                                    log.info("Streaming response completed");

                                    String fullQuestion = fullQuestionBuffer[0].toString().trim();
                                    if (!fullQuestion.isEmpty() && !"report".equals(eventName)) {
                                        saveQuestionAsync(fullQuestion, sessionId);
                                    }

                                    if (onComplete != null) onComplete.run();

                                    safeComplete(emitter, emitterClosed);
                                    scheduler.shutdown();
                                }
                        );

            } catch (Exception e) {
                log.error("Error setting up streaming request: {}", e.getMessage(), e);
                if (!emitterClosed.get()) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data("设置流式请求错误: " + e.getMessage()));
                    } catch (IOException ignored) {}
                }
                safeCompleteWithError(emitter, emitterClosed, e);
                scheduler.shutdown();
            }
        });
    }

    private void safeComplete(SseEmitter emitter, AtomicBoolean closedFlag) {
        if (closedFlag.compareAndSet(false, true)) {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        }
    }

    private void safeCompleteWithError(SseEmitter emitter, AtomicBoolean closedFlag, Throwable e) {
        if (closedFlag.compareAndSet(false, true)) {
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {}
        }
    }



    
    // 元数据处理逻辑已移除，现在直接流式输出内容
    

    
    /**
     * 异步保存完整问题文本到数据库
     *
     * @param questionText 完整的问题文本
     * @param sessionId 会话ID
     */
    /**
     * 顺序调用DeepSeek API（流式输出方式），确保每个prompt响应成功后再进行下一个
     * @param systemPrompt 系统提示词
     * @param prompts 用户提示词列表
     * @param emitter SSE发射器，用于流式输出
     * @param sessionId 会话ID，用于保存元数据和获取对话历史
     */
    public void callDeepSeekApiStreamSequential(String systemPrompt, List<String> prompts, SseEmitter emitter, String sessionId) {
        executorService.submit(() -> {
            try {
                // 遍历所有prompt，顺序执行
                for (int i = 0; i < prompts.size(); i++) {
                    final int promptIndex = i + 1;
                    String prompt = prompts.get(i);
                    // 为每个prompt创建一个CountDownLatch，用于等待响应完成
                    final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    
                    // 构建事件名称，包含prompt索引
                    String eventName = "prompt-" + promptIndex;
                    
                    // 发送开始事件，通知前端当前处理的是第几个prompt
                    try {
                        emitter.send(SseEmitter.event().name("prompt-start").data("{\"index\": " + promptIndex + ", \"total\": " + prompts.size() + ", \"content\": \"" + prompt + "\"}"));
                    } catch (IOException e) {
                        log.error("Error sending prompt-start event: {}", e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                    
                    // 调用流式API，传入回调函数
                    callDeepSeekApiStream(
                        systemPrompt,
                        prompt,
                        emitter,
                        content -> {
                            // 内容回调，这里可以添加额外处理逻辑
                        },
                        () -> {
                            // 这个prompt响应完成，计数减一
                            latch.countDown();
                            // 发送完成事件，通知前端当前prompt已完成
                            try {
                                emitter.send(SseEmitter.event().name("prompt-complete").data("{\"index\": " + promptIndex + "}"));
                            } catch (IOException e) {
                                log.error("Error sending prompt-complete event: {}", e.getMessage(), e);
                                throw new RuntimeException(e);
                            }
                        },
                        sessionId,
                        eventName
                    );
                    
                    // 等待当前prompt响应完成
                    latch.await();
                }
                
                // 所有prompt处理完成
                try {
                    emitter.send(SseEmitter.event().name("all-prompts-complete").data("{\"total\": " + prompts.size() + "}"));
                } catch (IOException e) {
                    log.error("Error sending all-prompts-complete event: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                log.error("Error in sequential prompt execution: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("顺序执行错误: " + e.getMessage()));
                } catch (IOException ignored) {}
            } finally {
                // 确保emitter最终关闭
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });
    }

    private void saveQuestionAsync(String questionText, String sessionId) {
        executorService.submit(() -> {
            try {
                // 获取最新的面试日志，更新问题文本
                // 找到轮次号最大且问题文本为空的记录（这是刚创建的新问题记录）
                List<InterviewLog> logs = interviewLogRepository.findBySessionIdOrderByRoundNumberDesc(sessionId);
                if (!logs.isEmpty()) {
                    InterviewLog latestLog = null;
                    for (InterviewLog log : logs) {
                        if (!StringUtils.hasText(log.getQuestionText())) {
                            latestLog = log;
                            break;
                        }
                    }
                    
                    // 如果没有找到问题文本为空的记录，就使用最新的记录
                    if (latestLog == null) {
                        latestLog = logs.get(0);
                    }
                    
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
}