package com.nutalig.utils;

import com.nutalig.constant.ApprovalLifecycleStatus;
import com.nutalig.constant.CommercialOutcomeStatus;
import com.nutalig.constant.DocumentLifecycleStatus;
import com.nutalig.constant.InvoicePaymentStatus;
import com.nutalig.constant.InvoiceStatus;
import com.nutalig.constant.PaymentLifecycleStatus;
import com.nutalig.constant.PurchaseOrderStatus;
import com.nutalig.constant.QuotationStatus;
import com.nutalig.constant.ReceiptStatus;
import com.nutalig.constant.SalesOrderStatus;
import com.nutalig.dto.DocumentStatusProfileDto;
import com.nutalig.entity.InvoicePaymentEntity;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class DocumentStatusResolver {
    private DocumentStatusResolver() {
    }

    public static DocumentStatusProfileDto resolveQuotation(QuotationStatus status) {
        DocumentStatusProfileDto profile = baseProfile();
        profile.setDocumentLifecycle(resolveQuotationLifecycle(status));
        profile.setCommercialOutcome(resolveQuotationCommercialOutcome(status));
        return profile;
    }

    public static DocumentStatusProfileDto resolveSalesOrder(SalesOrderStatus status) {
        DocumentStatusProfileDto profile = baseProfile();
        profile.setDocumentLifecycle(resolveSalesOrderLifecycle(status));
        profile.setCommercialOutcome(resolveSalesOrderCommercialOutcome(status));
        return profile;
    }

    public static DocumentStatusProfileDto resolveInvoice(InvoiceStatus status, Collection<InvoicePaymentEntity> payments) {
        DocumentStatusProfileDto profile = baseProfile();
        profile.setDocumentLifecycle(resolveInvoiceLifecycle(status));
        profile.setPaymentLifecycle(resolveInvoicePaymentLifecycle(status));
        profile.setApprovalLifecycle(resolveInvoiceApprovalLifecycle(status, payments));
        return profile;
    }

    public static DocumentStatusProfileDto resolveReceipt(ReceiptStatus status) {
        DocumentStatusProfileDto profile = baseProfile();
        profile.setDocumentLifecycle(resolveReceiptLifecycle(status));
        profile.setPaymentLifecycle(status == ReceiptStatus.ISSUED ? PaymentLifecycleStatus.PAID : PaymentLifecycleStatus.NONE);
        return profile;
    }

    public static DocumentStatusProfileDto resolvePurchaseOrder(PurchaseOrderStatus status) {
        DocumentStatusProfileDto profile = baseProfile();
        profile.setDocumentLifecycle(resolvePurchaseOrderLifecycle(status));
        return profile;
    }

    private static DocumentStatusProfileDto baseProfile() {
        DocumentStatusProfileDto profile = new DocumentStatusProfileDto();
        profile.setDocumentLifecycle(DocumentLifecycleStatus.OPEN);
        profile.setCommercialOutcome(CommercialOutcomeStatus.NONE);
        profile.setPaymentLifecycle(PaymentLifecycleStatus.NONE);
        profile.setApprovalLifecycle(ApprovalLifecycleStatus.NONE);
        return profile;
    }

    private static DocumentLifecycleStatus resolveQuotationLifecycle(QuotationStatus status) {
        if (status == null) {
            return DocumentLifecycleStatus.OPEN;
        }
        return switch (status) {
            case DRAFT -> DocumentLifecycleStatus.DRAFT;
            case ACCEPTED, REJECTED -> DocumentLifecycleStatus.FINALIZED;
            case CANCELLED -> DocumentLifecycleStatus.CANCELLED;
            case ISSUED, SENT -> DocumentLifecycleStatus.OPEN;
        };
    }

    private static CommercialOutcomeStatus resolveQuotationCommercialOutcome(QuotationStatus status) {
        if (status == null) {
            return CommercialOutcomeStatus.NONE;
        }
        return switch (status) {
            case SENT -> CommercialOutcomeStatus.SENT;
            case ACCEPTED -> CommercialOutcomeStatus.ACCEPTED;
            case REJECTED -> CommercialOutcomeStatus.REJECTED;
            case DRAFT, ISSUED -> CommercialOutcomeStatus.PENDING;
            case CANCELLED -> CommercialOutcomeStatus.NONE;
        };
    }

    private static DocumentLifecycleStatus resolveSalesOrderLifecycle(SalesOrderStatus status) {
        if (status == null) {
            return DocumentLifecycleStatus.OPEN;
        }
        return switch (status) {
            case DRAFT -> DocumentLifecycleStatus.DRAFT;
            case ACCEPTED, REJECTED -> DocumentLifecycleStatus.FINALIZED;
            case CANCELLED -> DocumentLifecycleStatus.CANCELLED;
            case CREATED, ISSUED, SENT -> DocumentLifecycleStatus.OPEN;
        };
    }

    private static CommercialOutcomeStatus resolveSalesOrderCommercialOutcome(SalesOrderStatus status) {
        if (status == null) {
            return CommercialOutcomeStatus.NONE;
        }
        return switch (status) {
            case SENT -> CommercialOutcomeStatus.SENT;
            case ACCEPTED -> CommercialOutcomeStatus.ACCEPTED;
            case REJECTED -> CommercialOutcomeStatus.REJECTED;
            case DRAFT, CREATED, ISSUED -> CommercialOutcomeStatus.PENDING;
            case CANCELLED -> CommercialOutcomeStatus.NONE;
        };
    }

    private static DocumentLifecycleStatus resolveInvoiceLifecycle(InvoiceStatus status) {
        if (status == null) {
            return DocumentLifecycleStatus.OPEN;
        }
        return switch (status) {
            case DRAFT -> DocumentLifecycleStatus.DRAFT;
            case PAID -> DocumentLifecycleStatus.FINALIZED;
            case CANCELLED -> DocumentLifecycleStatus.CANCELLED;
            case VOID -> DocumentLifecycleStatus.VOID;
            case ISSUED, AWAITING_VALIDATION, PARTIALLY_PAID -> DocumentLifecycleStatus.OPEN;
        };
    }

    private static PaymentLifecycleStatus resolveInvoicePaymentLifecycle(InvoiceStatus status) {
        if (status == null) {
            return PaymentLifecycleStatus.NONE;
        }
        return switch (status) {
            case DRAFT, CANCELLED, VOID -> PaymentLifecycleStatus.NONE;
            case ISSUED, AWAITING_VALIDATION -> PaymentLifecycleStatus.UNPAID;
            case PARTIALLY_PAID -> PaymentLifecycleStatus.PARTIALLY_PAID;
            case PAID -> PaymentLifecycleStatus.PAID;
        };
    }

    private static ApprovalLifecycleStatus resolveInvoiceApprovalLifecycle(InvoiceStatus status, Collection<InvoicePaymentEntity> payments) {
        if (status == InvoiceStatus.AWAITING_VALIDATION) {
            return ApprovalLifecycleStatus.AWAITING_VALIDATION;
        }

        Optional<InvoicePaymentStatus> latestPaymentStatus = Optional.ofNullable(payments)
                .orElseGet(List::of)
                .stream()
                .max(Comparator.comparing(InvoicePaymentEntity::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(InvoicePaymentEntity::getStatus);

        if (latestPaymentStatus.isEmpty()) {
            return ApprovalLifecycleStatus.NONE;
        }

        return switch (latestPaymentStatus.get()) {
            case PENDING -> ApprovalLifecycleStatus.AWAITING_VALIDATION;
            case APPROVE -> ApprovalLifecycleStatus.APPROVED;
            case REJECT -> ApprovalLifecycleStatus.REJECTED;
        };
    }

    private static DocumentLifecycleStatus resolveReceiptLifecycle(ReceiptStatus status) {
        if (status == null) {
            return DocumentLifecycleStatus.OPEN;
        }
        return switch (status) {
            case ISSUED -> DocumentLifecycleStatus.FINALIZED;
            case CANCELLED -> DocumentLifecycleStatus.CANCELLED;
            case VOID -> DocumentLifecycleStatus.VOID;
        };
    }

    private static DocumentLifecycleStatus resolvePurchaseOrderLifecycle(PurchaseOrderStatus status) {
        if (status == null) {
            return DocumentLifecycleStatus.OPEN;
        }
        return switch (status) {
            case CREATED, AWAITING_PAYMENT, PAID -> DocumentLifecycleStatus.OPEN;
            case CLOSED -> DocumentLifecycleStatus.FINALIZED;
            case CANCELLED -> DocumentLifecycleStatus.CANCELLED;
        };
    }
}
