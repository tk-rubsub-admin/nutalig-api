package com.nutalig.dto;

import com.nutalig.constant.ApprovalLifecycleStatus;
import com.nutalig.constant.CommercialOutcomeStatus;
import com.nutalig.constant.DocumentLifecycleStatus;
import com.nutalig.constant.PaymentLifecycleStatus;
import lombok.Data;

@Data
public class DocumentStatusProfileDto {
    private DocumentLifecycleStatus documentLifecycle;
    private CommercialOutcomeStatus commercialOutcome;
    private PaymentLifecycleStatus paymentLifecycle;
    private ApprovalLifecycleStatus approvalLifecycle;
}
