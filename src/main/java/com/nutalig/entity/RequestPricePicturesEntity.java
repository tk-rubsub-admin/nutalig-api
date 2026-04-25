package com.nutalig.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.ZonedDateTime;

@Getter
@Setter
@ToString
@Entity
@Table(name = "request_price_pictures")
public class RequestPricePicturesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_price_header_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private RequestPriceHeaderEntity requestPriceHeader;

    @Column(name = "pic_url", length = 1000)
    private String pictureUrl;

    @Column(name = "sort")
    private Integer sort;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(name = "updated_date")
    private ZonedDateTime updatedDate;

    @Column(name = "updated_by")
    @EqualsAndHashCode.Exclude
    private String updatedBy;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;
}
