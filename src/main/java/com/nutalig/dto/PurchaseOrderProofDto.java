package com.nutalig.dto;

import com.nutalig.constant.PurchaseOrderProofStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseOrderProofDto {
    private Long id;
    private String purchaseOrderNo;
    private String salesOrderNo;
    private String customerName;
    private SystemConfigDto proofType;
    private Boolean required;
    private Integer currentRevision;
    private PurchaseOrderProofStatus status;
    private String assignedSalesUserId;
    private String assignedSalesName;
    private Boolean canApprove;
    private Boolean canResubmit;
    private Boolean canCancel;
    private List<PurchaseOrderProofRevisionDto> revisions = new ArrayList<>();
}
