package com.nutalig.entity;

import com.nutalig.constant.RfqSupplierInquiryStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
        name = "rfq_supplier_inquiry",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rfq_supplier_inquiry_rfq_version",
                columnNames = {"rfq_header_id", "version_no"}
        )
)
@Entity(name = "RfqSupplierInquiry")
public class RfqSupplierInquiryEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(generator = "rfqSupplierInquiryIdGenerator")
    @GenericGenerator(name = "rfqSupplierInquiryIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "prefix", value = "NTL-RFI"),
                    @org.hibernate.annotations.Parameter(name = "length", value = "%04d")
            },
            strategy = "com.nutalig.repository.jpa.IdGenerator")
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_header_id", referencedColumnName = "id", nullable = false)
    private RfqHeaderEntity requestPriceHeader;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RfqSupplierInquiryStatus status;

    @Column(name = "thai_message", columnDefinition = "LONGTEXT", nullable = false)
    private String thaiMessage;

    @Column(name = "chinese_message", columnDefinition = "LONGTEXT")
    private String chineseMessage;

    @Column(name = "source_snapshot", columnDefinition = "LONGTEXT")
    private String sourceSnapshot;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
