package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "ai_prompt")
@Entity(name = "AiPrompt")
public class AiPromptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "model")
    private String model;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt_template", columnDefinition = "TEXT")
    private String userPromptTemplate;

    @Column(name = "active")
    private Boolean active;
}
