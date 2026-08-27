package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.entity.ApprovalRequestEntity;
import com.nutalig.entity.RfqHeaderEntity;
import com.nutalig.entity.InvoiceEntity;
import com.nutalig.entity.SalesOrderEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.entity.UserTodoEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.repository.RequestPriceHeaderRepository;
import com.nutalig.repository.InvoiceRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.repository.SalesOrderRepository;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalBusinessService {

    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final UserRepository userRepository;
    private final ActivityHistoryService activityHistoryService;
    private final UserTodoService userTodoService;
    private final UserProfileService userProfileService;

    @Transactional
    public void handleApproved(ApprovalRequestEntity approvalRequest, String actorUserId, ApprovalSource source)
            throws DataNotFoundException {
        if (approvalRequest.getRequestType() == ApprovalRequestType.URGENT_RFQ) {
            handleUrgentRfqApproved(approvalRequest, actorUserId, source);
            return;
        }
        if (approvalRequest.getRequestType() == ApprovalRequestType.URGENT_READY_PO) {
            handleUrgentReadyPoApproved(approvalRequest, actorUserId, source);
            return;
        }
        if (approvalRequest.getRequestType() == ApprovalRequestType.INVOICE_PAYMENT_TERM) {
            handleInvoicePaymentTermApproved(approvalRequest, actorUserId, source);
            return;
        }

        log.info("No business approval handler for requestType={}", approvalRequest.getRequestType());
    }

    @Transactional
    public void handleRejected(
            ApprovalRequestEntity approvalRequest,
            String actorUserId,
            ApprovalSource source,
            String reason
    ) throws DataNotFoundException {
        if (approvalRequest.getRequestType() == ApprovalRequestType.URGENT_RFQ) {
            handleUrgentRfqRejected(approvalRequest, actorUserId, source, reason);
            return;
        }
        if (approvalRequest.getRequestType() == ApprovalRequestType.URGENT_READY_PO) {
            handleUrgentReadyPoRejected(approvalRequest, actorUserId, source, reason);
            return;
        }
        if (approvalRequest.getRequestType() == ApprovalRequestType.INVOICE_PAYMENT_TERM) {
            handleInvoicePaymentTermRejected(approvalRequest, actorUserId, source, reason);
            return;
        }

        log.info("No business rejection handler for requestType={}", approvalRequest.getRequestType());
    }

    public String buildTargetPath(ApprovalRequestEntity approvalRequest) {
        if (approvalRequest.getRequestType() == ApprovalRequestType.URGENT_RFQ) {
            return "/price-inquiry/" + approvalRequest.getReferenceId();
        }
        if (approvalRequest.getRequestType() == ApprovalRequestType.URGENT_READY_PO) {
            return "/sales-order/" + approvalRequest.getReferenceId();
        }
        if (approvalRequest.getRequestType() == ApprovalRequestType.INVOICE_PAYMENT_TERM) {
            return "/invoice/" + approvalRequest.getReferenceId();
        }
        return null;
    }

    private void handleInvoicePaymentTermApproved(
            ApprovalRequestEntity approvalRequest, String actorUserId, ApprovalSource source
    ) throws DataNotFoundException {
        InvoiceEntity invoice = invoiceRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new DataNotFoundException("Invoice " + approvalRequest.getReferenceId() + " not found."));
        UserEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new DataNotFoundException("User " + actorUserId + " not found."));
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String actor = userProfileService.getNameFromId(actorUserId);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setRequiredApproveStatus(UrgentRequestStatus.APPROVED);
        invoice.setApprovedBy(actor);
        invoice.setApprovedDate(now);
        invoice.setRejectedBy(null);
        invoice.setRejectedDate(null);
        invoice.setRejectReason(null);
        invoice.setUpdatedBy(user);
        invoice.setUpdatedDate(now);
        invoiceRepository.save(invoice);
        activityHistoryService.record(ActivityEntityType.INVOICE, invoice.getInvoiceNo(), actorUserId,
                ActivityActorType.USER, ActivityAction.APPROVE,
                source == ApprovalSource.LINE_POSTBACK ? ActivitySource.LINE : ActivitySource.API,
                "อนุมัติใบแจ้งหนี้เลขที่ " + invoice.getInvoiceNo(),
                Map.of("approvalRequestId", approvalRequest.getId(), "approvalRequestNo", approvalRequest.getRequestNo(),
                        "paymentTerm", invoice.getCustomerPaymentTerm() == null ? "" : invoice.getCustomerPaymentTerm().getId().getCode()));
        completeApprovalTodos(approvalRequest.getId(), actorUserId);
    }

    private void handleInvoicePaymentTermRejected(
            ApprovalRequestEntity approvalRequest, String actorUserId, ApprovalSource source, String reason
    ) throws DataNotFoundException {
        InvoiceEntity invoice = invoiceRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new DataNotFoundException("Invoice " + approvalRequest.getReferenceId() + " not found."));
        UserEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new DataNotFoundException("User " + actorUserId + " not found."));
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String actor = userProfileService.getNameFromId(actorUserId);
        invoice.setRequiredApproveStatus(UrgentRequestStatus.REJECTED);
        invoice.setRejectedBy(actor);
        invoice.setRejectedDate(now);
        invoice.setRejectReason(StringUtils.trimToNull(reason));
        invoice.setUpdatedBy(user);
        invoice.setUpdatedDate(now);
        invoiceRepository.save(invoice);
        activityHistoryService.record(ActivityEntityType.INVOICE, invoice.getInvoiceNo(), actorUserId,
                ActivityActorType.USER, ActivityAction.REJECT,
                source == ApprovalSource.LINE_POSTBACK ? ActivitySource.LINE : ActivitySource.API,
                "ไม่อนุมัติใบแจ้งหนี้เลขที่ " + invoice.getInvoiceNo(),
                Map.of("approvalRequestId", approvalRequest.getId(), "approvalRequestNo", approvalRequest.getRequestNo(),
                        "reason", StringUtils.defaultString(reason), "paymentTerm", invoice.getCustomerPaymentTerm() == null ? "" : invoice.getCustomerPaymentTerm().getId().getCode()));
        completeApprovalTodos(approvalRequest.getId(), actorUserId);
    }

    private void handleUrgentRfqApproved(ApprovalRequestEntity approvalRequest, String actorUserId, ApprovalSource source)
            throws DataNotFoundException {
        RfqHeaderEntity entity = requestPriceHeaderRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new DataNotFoundException("RFQ " + approvalRequest.getReferenceId() + " not found."));
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String actor = userProfileService.getNameFromId(actorUserId);

        entity.setUrgentRequestStatus(UrgentRequestStatus.APPROVED);
        entity.setUrgentApprovedBy(actor);
        entity.setUrgentApprovedDate(now);
        entity.setUrgentRejectedBy(null);
        entity.setUrgentRejectedDate(null);
        entity.setUrgentRejectReason(null);
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(now);
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("urgentRequestStatus", entity.getUrgentRequestStatus());
        detail.put("urgentApprovedBy", entity.getUrgentApprovedBy());
        detail.put("urgentApprovedDate", entity.getUrgentApprovedDate());
        detail.put("approvalRequestId", approvalRequest.getId());
        detail.put("approvalRequestNo", approvalRequest.getRequestNo());
        detail.put("approvalSource", source);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                actorUserId,
                ActivityActorType.USER,
                ActivityAction.APPROVE,
                source == ApprovalSource.LINE_POSTBACK ? ActivitySource.LINE : ActivitySource.API,
                "อนุมัติคำขอเร่งด่วนของคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        completeApprovalTodos(approvalRequest.getId(), actorUserId);
    }

    private void handleUrgentRfqRejected(
            ApprovalRequestEntity approvalRequest,
            String actorUserId,
            ApprovalSource source,
            String reason
    ) throws DataNotFoundException {
        RfqHeaderEntity entity = requestPriceHeaderRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new DataNotFoundException("RFQ " + approvalRequest.getReferenceId() + " not found."));
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String actor = userProfileService.getNameFromId(actorUserId);

        entity.setUrgentRequestStatus(UrgentRequestStatus.REJECTED);
        entity.setUrgentRejectedBy(actor);
        entity.setUrgentRejectedDate(now);
        entity.setUrgentRejectReason(StringUtils.trimToNull(reason));
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(now);
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("urgentRequestStatus", entity.getUrgentRequestStatus());
        detail.put("urgentRejectedBy", entity.getUrgentRejectedBy());
        detail.put("urgentRejectedDate", entity.getUrgentRejectedDate());
        detail.put("urgentRejectReason", entity.getUrgentRejectReason());
        detail.put("approvalRequestId", approvalRequest.getId());
        detail.put("approvalRequestNo", approvalRequest.getRequestNo());
        detail.put("approvalSource", source);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                actorUserId,
                ActivityActorType.USER,
                ActivityAction.REJECT,
                source == ApprovalSource.LINE_POSTBACK ? ActivitySource.LINE : ActivitySource.API,
                "ไม่อนุมัติคำขอเร่งด่วนของคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        completeApprovalTodos(approvalRequest.getId(), actorUserId);
    }

    private void handleUrgentReadyPoApproved(
            ApprovalRequestEntity approvalRequest,
            String actorUserId,
            ApprovalSource source
    ) throws DataNotFoundException {
        SalesOrderEntity entity = salesOrderRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new DataNotFoundException("Sales order " + approvalRequest.getReferenceId() + " not found."));
        UserEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new DataNotFoundException("User " + actorUserId + " not found."));
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String actor = userProfileService.getNameFromId(actorUserId);

        entity.setUrgentRequestStatus(UrgentRequestStatus.APPROVED);
        entity.setUrgentApprovedBy(actor);
        entity.setUrgentApprovedDate(now);
        entity.setUrgentRejectedBy(null);
        entity.setUrgentRejectedDate(null);
        entity.setUrgentRejectReason(null);
        entity.setProcurementStatus(ProcurementStatus.READY_FOR_PO_OVERRIDE);
        entity.setUpdatedBy(user);
        entity.setUpdatedDate(now);
        salesOrderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("urgentRequestStatus", entity.getUrgentRequestStatus());
        detail.put("urgentApprovedBy", entity.getUrgentApprovedBy());
        detail.put("urgentApprovedDate", entity.getUrgentApprovedDate());
        detail.put("procurementStatus", entity.getProcurementStatus());
        detail.put("approvalRequestId", approvalRequest.getId());
        detail.put("approvalRequestNo", approvalRequest.getRequestNo());
        detail.put("approvalSource", source);

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                entity.getSalesOrderNo(),
                actorUserId,
                ActivityActorType.USER,
                ActivityAction.APPROVE,
                source == ApprovalSource.LINE_POSTBACK ? ActivitySource.LINE : ActivitySource.API,
                "อนุมัติคำขอสร้างใบสั่งซื้อเลขที่ " + entity.getSalesOrderNo(),
                detail
        );

        completeApprovalTodos(approvalRequest.getId(), actorUserId);
    }

    private void handleUrgentReadyPoRejected(
            ApprovalRequestEntity approvalRequest,
            String actorUserId,
            ApprovalSource source,
            String reason
    ) throws DataNotFoundException {
        SalesOrderEntity entity = salesOrderRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new DataNotFoundException("Sales order " + approvalRequest.getReferenceId() + " not found."));
        UserEntity user = userRepository.findById(actorUserId)
                .orElseThrow(() -> new DataNotFoundException("User " + actorUserId + " not found."));
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String actor = userProfileService.getNameFromId(actorUserId);

        entity.setUrgentRequestStatus(UrgentRequestStatus.REJECTED);
        entity.setUrgentRejectedBy(actor);
        entity.setUrgentRejectedDate(now);
        entity.setUrgentRejectReason(StringUtils.trimToNull(reason));
        entity.setUpdatedBy(user);
        entity.setUpdatedDate(now);
        salesOrderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("urgentRequestStatus", entity.getUrgentRequestStatus());
        detail.put("urgentRejectedBy", entity.getUrgentRejectedBy());
        detail.put("urgentRejectedDate", entity.getUrgentRejectedDate());
        detail.put("urgentRejectReason", entity.getUrgentRejectReason());
        detail.put("approvalRequestId", approvalRequest.getId());
        detail.put("approvalRequestNo", approvalRequest.getRequestNo());
        detail.put("approvalSource", source);

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                entity.getSalesOrderNo(),
                actorUserId,
                ActivityActorType.USER,
                ActivityAction.REJECT,
                source == ApprovalSource.LINE_POSTBACK ? ActivitySource.LINE : ActivitySource.API,
                "ไม่อนุมัติคำขอสร้างใบสั่งซื้อเลขที่ " + entity.getSalesOrderNo(),
                detail
        );

        completeApprovalTodos(approvalRequest.getId(), actorUserId);
    }

    private void completeApprovalTodos(Long approvalRequestId, String userId) {
        List<UserTodoEntity> todos = userTodoService.findActiveTodosByTarget(
                ActivityEntityType.APPROVAL_REQUEST.name(),
                String.valueOf(approvalRequestId),
                List.of(UserTodoStatus.TODO, UserTodoStatus.IN_PROGRESS)
        );
        userTodoService.markTodosAsDone(todos, userId);
    }
}
