package com.nutalig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.nutalig.constant.QcTypes.*;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "qc_document_revision")
public class QcDocumentRevisionEntity extends AuditDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "qc_document_id")
    private Long qcDocumentId;

    @Column(name = "revision_no")
    private String revisionNo;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "sha256")
    private String sha256;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RevisionStatus status;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
