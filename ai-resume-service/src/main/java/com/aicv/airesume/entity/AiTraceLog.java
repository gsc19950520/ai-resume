package com.aicv.airesume.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI运行日志实体类
 */
@Data
@Entity
@Table(name = "ai_trace_log")
public class AiTraceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // generate_question / score_answer / give_advice

    @Column(name = "prompt_input", columnDefinition = "longtext")
    private String promptInput;

    @Column(name = "ai_response", columnDefinition = "longtext")
    private String aiResponse;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}