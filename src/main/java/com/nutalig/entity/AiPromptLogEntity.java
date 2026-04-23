package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(name = "ai_prompt_log")
@Entity(name = "AiPromptLog")
public class AiPromptLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt_code")
    private String promptCode;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_estimate")
    private Double costEstimate;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
