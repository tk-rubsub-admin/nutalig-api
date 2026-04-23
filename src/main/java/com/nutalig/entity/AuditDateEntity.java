package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditDateEntity {

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(name = "created_date")
    private ZonedDateTime createdDate;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(name = "updated_date")
    private ZonedDateTime updatedDate;
}
