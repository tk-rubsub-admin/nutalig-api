package com.nutalig.entity;

import jakarta.persistence.*;
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
@Table(name = "rfq_pictures")
public class RfqPicturesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_header_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude
    private RfqHeaderEntity requestPriceHeader;

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
