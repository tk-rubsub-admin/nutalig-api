package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "generated_id_sequence")
@Entity(name = "generatedIdSequence")
public class GeneratedIdSequenceEntity {
    @Id
    private String prefix;
    @Column
    private Integer nextId;
    @Version
    private Long version;

}
