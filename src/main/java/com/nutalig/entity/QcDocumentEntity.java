package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.nutalig.constant.QcTypes.*;

@Getter
@Setter
@Entity
@Table(name = "qc_document")
public class QcDocumentEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "document_no")
    private String documentNo;

    @Column(name = "qc_route_code")
    private String qcRouteCode;

    @Column(name = "name_th")
    private String nameTh;

    @Column(name = "name_en")
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;

    @Column(name = "current_revision_id")
    private Long currentRevisionId;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_revision_id", insertable = false, updatable = false)
    private QcDocumentRevisionEntity currentRevision;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
