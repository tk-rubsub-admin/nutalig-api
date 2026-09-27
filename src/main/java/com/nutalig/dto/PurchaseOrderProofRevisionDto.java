package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderProofStatus;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseOrderProofRevisionDto {
    private Long id;
    private Integer revisionNo;
    private String title;
    private String procurementNote;
    private PurchaseOrderProofStatus status;
    private String requestedByUserId;
    private String requestedByName;
    private ZonedDateTime requestedAt;
    private ZonedDateTime dueDate;
    private String actedByUserId;
    private String actedByName;
    private ZonedDateTime actedAt;
    private String salesComment;
    private String changeReason;
    private Long approvalRequestId;
    private List<PurchaseOrderProofAttachmentDto> attachments = new ArrayList<>();
}
