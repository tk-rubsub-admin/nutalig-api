package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "keyword_mapping_prompt")
public class KeywordMappingPromptEntity {
    @Id
    @Column(name = "keyword")
    private String keyword;

    @Column(name = "prompt_code")
    private String promptCode;
}
