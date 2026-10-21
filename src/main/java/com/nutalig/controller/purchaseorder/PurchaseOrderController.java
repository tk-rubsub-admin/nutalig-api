package com.nutalig.controller.purchaseorder;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.constant.PurchaseOrderAttachmentDocumentType;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderPaymentRequest;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderPaymentInstructionRequest;
import com.nutalig.controller.purchaseorder.request.CompletePurchaseOrderProductionRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderDeliveryTransitRequest;
import com.nutalig.controller.purchaseorder.request.PurchaseOrderPaymentDecisionRequest;
import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.SearchPendingSupplierPaymentRequest;
import com.nutalig.controller.purchaseorder.request.PurchaseOrderCbmPreviewRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.StartPurchaseOrderRunRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderPaymentRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderMilestoneRequest;
import com.nutalig.controller.purchaseorder.response.CreatePurchaseOrderResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.dto.PurchaseOrderDto;
import com.nutalig.dto.PurchaseOrderCbmPreviewDto;
import com.nutalig.dto.PurchaseOrderPaymentDto;
import com.nutalig.dto.PurchaseOrderPaymentInstructionDto;
import com.nutalig.dto.PurchaseOrderPaymentScheduleDto;
import com.nutalig.dto.PendingSupplierPaymentDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.PurchaseOrderService;
import com.nutalig.service.PurchaseOrderPaymentService;
import com.nutalig.service.PurchaseOrderPaymentInstructionService;
import com.nutalig.service.PurchaseOrderPaymentScheduleService;
import com.nutalig.service.PendingSupplierPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final com.nutalig.service.PurchaseOrderCbmService purchaseOrderCbmService;
    private final PurchaseOrderPaymentService purchaseOrderPaymentService;
    private final PurchaseOrderPaymentInstructionService paymentInstructionService;
    private final PurchaseOrderPaymentScheduleService purchaseOrderPaymentScheduleService;
    private final PendingSupplierPaymentService pendingSupplierPaymentService;

    @GetMapping("/production-calendar")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_TRACKING')")
    public GeneralResponse<List<com.nutalig.dto.PurchaseOrderProductionCalendarDto>> productionCalendar(
            @RequestHeader("userId") String userId,
            @RequestParam(required = false) java.time.LocalDate start,
            @RequestParam(required = false) java.time.LocalDate end
    ) throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.getProductionCalendar(userId, start, end));
    }

    @GetMapping("/{id}/production-timeline")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_VIEW')")
    public GeneralResponse<com.nutalig.dto.PurchaseOrderTimelineDto> productionTimeline(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.getProductionTimeline(id, userId));
    }

    @PatchMapping("/{id}/milestones/{milestoneCode}/complete")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<PurchaseOrderDto> completeMilestone(
            @PathVariable("id") String id,
            @PathVariable com.nutalig.constant.PurchaseOrderMilestoneCode milestoneCode,
            @RequestBody(required = false) UpdatePurchaseOrderMilestoneRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        if (milestoneCode == com.nutalig.constant.PurchaseOrderMilestoneCode.PRODUCTION_COMPLETED) {
            throw new InvalidRequestException("Use the production-complete endpoint to provide delivery details.");
        }
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.updateMilestone(
                id,
                milestoneCode,
                request != null ? request.getPlannedDate() : null,
                request != null ? request.getNote() : null,
                userId
        ));
    }

    @PatchMapping("/{id}/milestones/{milestoneCode}/skip")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<com.nutalig.dto.PurchaseOrderTimelineDto> skipMilestone(
            @PathVariable("id") String id,
            @PathVariable com.nutalig.constant.PurchaseOrderMilestoneCode milestoneCode,
            @RequestBody UpdatePurchaseOrderMilestoneRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.skipProofMilestone(
                id,
                milestoneCode,
                request != null ? request.getNote() : null,
                userId
        ));
    }

    @PatchMapping("/{id}/production-complete")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<PurchaseOrderDto> productionComplete(
            @PathVariable("id") String id,
            @RequestBody CompletePurchaseOrderProductionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.completeProduction(id, request, userId));
    }

    @PatchMapping("/{id}/deliveries/{deliveryId}/in-transit")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<PurchaseOrderDto> markDeliveryInTransit(
            @PathVariable("id") String id,
            @PathVariable("deliveryId") Long deliveryId,
            @RequestBody UpdatePurchaseOrderDeliveryTransitRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.markDeliveryInTransit(
                id, deliveryId, request, userId));
    }

    @PatchMapping("/{id}/production-expected-end-date")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<PurchaseOrderDto> updateProductionExpectedEndDate(
            @PathVariable("id") String id,
            @RequestBody UpdatePurchaseOrderMilestoneRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.updateProductionExpectedEndDate(
                id,
                request != null ? request.getPlannedDate() : null,
                userId
        ));
    }

    @PostMapping("/cbm-preview")
    public GeneralResponse<PurchaseOrderCbmPreviewDto> previewCbm(
            @RequestBody PurchaseOrderCbmPreviewRequest request
    ) throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderCbmService.preview(request));
    }

    @PostMapping("/search")
    public GeneralResponse<Pageable<PurchaseOrderDto>> searchPurchaseOrders(
            @RequestBody(required = false) SearchPurchaseOrderRequest request,
            @Valid PageableRequest pageableRequest
    ) {
        Pageable<PurchaseOrderDto> response = purchaseOrderService.searchPurchaseOrders(request, pageableRequest);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/pending-payments/search")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_VIEW')")
    public GeneralResponse<Pageable<PendingSupplierPaymentDto>> searchPendingSupplierPayments(
            @RequestBody(required = false) SearchPendingSupplierPaymentRequest request,
            @Valid PageableRequest pageableRequest
    ) {
        return new GeneralResponse<>(SUCCESS, pendingSupplierPaymentService.search(request, pageableRequest));
    }

    @PostMapping
    public GeneralResponse<CreatePurchaseOrderResponse> createPurchaseOrder(
            @ModelAttribute CreatePurchaseOrderRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @RequestHeader("userId") String userId
    ) throws Exception {
        PurchaseOrderEntity entity = purchaseOrderService.createPurchaseOrder(request, attachments, userId);
        return new GeneralResponse<>(SUCCESS, new CreatePurchaseOrderResponse(entity.getPurchaseOrderNo()));
    }

    @GetMapping
    public GeneralResponse<PurchaseOrderDto> getPurchaseOrderById(@RequestParam(name = "id") String id)
            throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.getPurchaseOrderById(id));
    }

    @GetMapping("/document")
    public ResponseEntity<DownloadDocumentDto> getPurchaseOrderDocumentById(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "format") ExportFileFormat format,
            @RequestParam(name = "isOriginal") Boolean isOriginal,
            @RequestParam(name = "isCopy") Boolean isCopy
    ) throws Exception {
        DownloadDocumentDto doc = purchaseOrderService.getPurchaseOrderDocumentById(
                id,
                new DocumentRequest(format, isOriginal, isCopy)
        );

        if (doc == null || doc.getFiles() == null || doc.getFiles().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(doc);
    }

    @PatchMapping("/{id}")
    public GeneralResponse<PurchaseOrderDto> updatePurchaseOrder(
            @PathVariable("id") String id,
            @RequestBody UpdatePurchaseOrderRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.updatePurchaseOrder(id, request, userId));
    }

    @PostMapping("/{id}/start-run")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<PurchaseOrderDto> startRun(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId,
            @RequestBody(required = false) StartPurchaseOrderRunRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.startRun(
                id,
                userId,
                request != null ? request.getLateStartReason() : null,
                request != null ? request.getStartRunOverrideReason() : null
        ));
    }

    @PostMapping("/{id}/notify-sales-production-started")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<String> notifySalesProductionStarted(@PathVariable("id") String id)
            throws DataNotFoundException, InvalidRequestException {
        purchaseOrderService.notifySalesProductionStarted(id);
        return new GeneralResponse<>(SUCCESS, "ส่งการแจ้งเตือนการเริ่มผลิตแล้ว");
    }

    @GetMapping("/{id}/late-start-check")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<Boolean> checkLateStart( @PathVariable("id") String id) throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.isLateStart(id));
    }

    @PatchMapping("/{id}/cancel")
    public GeneralResponse<PurchaseOrderDto> cancelPurchaseOrder(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.cancelPurchaseOrder(id, userId));
    }

    @PatchMapping("/{id}/close")
    public GeneralResponse<PurchaseOrderDto> closePurchaseOrder(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.closePurchaseOrder(id, userId));
    }

    @PostMapping("/{id}/attachments")
    public GeneralResponse<PurchaseOrderDto> addAttachments(
            @PathVariable("id") String id,
            @RequestPart("attachments") List<MultipartFile> attachments,
            @RequestParam(value = "documentType", defaultValue = "OTHER")
            PurchaseOrderAttachmentDocumentType documentType,
            @RequestHeader("userId") String userId
    ) throws Exception {
        return new GeneralResponse<>(
                SUCCESS,
                purchaseOrderService.addAttachments(id, attachments, documentType, userId)
        );
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public GeneralResponse<PurchaseOrderDto> deleteAttachment(
            @PathVariable("id") String id,
            @PathVariable("attachmentId") Long attachmentId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.deleteAttachment(id, attachmentId, userId));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_VIEW')")
    public GeneralResponse<List<PurchaseOrderPaymentDto>> getPayments(@PathVariable("id") String id)
            throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderPaymentService.getPayments(id));
    }

    @GetMapping("/{id}/payment-schedules")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_VIEW')")
    public GeneralResponse<List<PurchaseOrderPaymentScheduleDto>> getPaymentSchedules(
            @PathVariable("id") String id
    ) throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderPaymentScheduleService.getSchedules(id));
    }

    @GetMapping("/{id}/payment-instructions")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_VIEW')")
    public GeneralResponse<List<PurchaseOrderPaymentInstructionDto>> getPaymentInstructions(
            @PathVariable("id") String id
    ) throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, paymentInstructionService.getInstructions(id));
    }

    @PostMapping("/{id}/payment-instructions")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_INSTRUCT')")
    public GeneralResponse<PurchaseOrderPaymentInstructionDto> createPaymentInstruction(
            @PathVariable("id") String id,
            @RequestBody CreatePurchaseOrderPaymentInstructionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, paymentInstructionService.createInstruction(id, request, userId));
    }

    @PostMapping("/{id}/payment-instructions/drafts")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<PurchaseOrderPaymentInstructionDto> createPaymentInstructionDraft(
            @PathVariable("id") String id,
            @RequestBody CreatePurchaseOrderPaymentInstructionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, paymentInstructionService.createDraft(id, request, userId));
    }

    @PostMapping("/{id}/payment-instructions/{instructionId}/submit")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_INSTRUCT')")
    public GeneralResponse<PurchaseOrderPaymentInstructionDto> submitPaymentInstructionDraft(
            @PathVariable("id") String id,
            @PathVariable("instructionId") Long instructionId,
            @RequestBody CreatePurchaseOrderPaymentInstructionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, paymentInstructionService.submitDraft(id, instructionId, request, userId));
    }

    @PatchMapping("/{id}/payment-instructions/{instructionId}")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_INSTRUCT')")
    public GeneralResponse<PurchaseOrderPaymentInstructionDto> updatePaymentInstruction(
            @PathVariable("id") String id,
            @PathVariable("instructionId") Long instructionId,
            @RequestBody CreatePurchaseOrderPaymentInstructionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,
                paymentInstructionService.updateInstruction(id, instructionId, request, userId));
    }

    @PostMapping("/{id}/payment-instructions/{instructionId}/cancel")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_INSTRUCT')")
    public GeneralResponse<PurchaseOrderPaymentInstructionDto> cancelPaymentInstruction(
            @PathVariable("id") String id,
            @PathVariable("instructionId") Long instructionId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,
                paymentInstructionService.cancelInstruction(id, instructionId, userId));
    }

    @PostMapping("/{id}/payment-instructions/{instructionId}/confirm-paid")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_CONFIRM')")
    public GeneralResponse<PurchaseOrderPaymentInstructionDto> confirmPaymentInstructionPaid(
            @PathVariable("id") String id,
            @PathVariable("instructionId") Long instructionId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,
                paymentInstructionService.confirmInstructionPaid(id, instructionId, userId));
    }

    @PostMapping("/{id}/payment-schedules/{scheduleId}/notify-super-admins")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_CREATE')")
    public GeneralResponse<String> notifySuperAdminsOfPaymentSchedule(
            @PathVariable("id") String id,
            @PathVariable("scheduleId") Long scheduleId
    ) throws DataNotFoundException, InvalidRequestException {
        purchaseOrderService.notifySuperAdminsOfPaymentSchedule(id, scheduleId);
        return new GeneralResponse<>(SUCCESS, "Notification sent");
    }

    @PostMapping("/{id}/payment-schedules/{scheduleId}/notify-accounting-admins")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_CONFIRM')")
    public GeneralResponse<String> notifyAccountingAdminsOfPaymentSchedule(
            @PathVariable("id") String id,
            @PathVariable("scheduleId") Long scheduleId
    ) throws DataNotFoundException, InvalidRequestException {
        purchaseOrderService.notifyAccountingAdminsOfPaymentSchedule(id, scheduleId);
        return new GeneralResponse<>(SUCCESS, "Accounting notification sent");
    }

    @PostMapping("/{id}/payment-schedules/{scheduleId}/notify-save-payment")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_CONFIRM')")
    public GeneralResponse<String> notifyAccountingAdminsForSavePayment(
            @PathVariable("id") String id,
            @PathVariable("scheduleId") Long scheduleId
    ) throws DataNotFoundException, InvalidRequestException {
        purchaseOrderService.notifyAccountingAdminsForSavePayment(id, scheduleId);
        return new GeneralResponse<>(SUCCESS, "Accounting notification sent");
    }

    @PostMapping(path = "/{id}/payments", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_CREATE')")
    public GeneralResponse<PurchaseOrderPaymentDto> createPayment(
            @PathVariable("id") String id,
            @ModelAttribute CreatePurchaseOrderPaymentRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @RequestHeader("userId") String userId
    ) throws Exception {
        return new GeneralResponse<>(SUCCESS,
                purchaseOrderPaymentService.createPayment(id, request, attachments, userId));
    }

    @PatchMapping(path = "/{id}/payments/{paymentId}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_CREATE')")
    public GeneralResponse<PurchaseOrderPaymentDto> updatePayment(
            @PathVariable("id") String id,
            @PathVariable("paymentId") Long paymentId,
            @ModelAttribute UpdatePurchaseOrderPaymentRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @RequestHeader("userId") String userId
    ) throws Exception {
        return new GeneralResponse<>(SUCCESS,
                purchaseOrderPaymentService.updatePayment(id, paymentId, request, attachments, userId));
    }

    @PostMapping("/{id}/payments/{paymentId}/approve")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_APPROVE')")
    public GeneralResponse<PurchaseOrderPaymentDto> approvePayment(
            @PathVariable("id") String id,
            @PathVariable("paymentId") Long paymentId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderPaymentService.approvePayment(id, paymentId, userId));
    }

    @PostMapping("/{id}/payments/{paymentId}/reject")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_APPROVE')")
    public GeneralResponse<PurchaseOrderPaymentDto> rejectPayment(
            @PathVariable("id") String id,
            @PathVariable("paymentId") Long paymentId,
            @RequestBody PurchaseOrderPaymentDecisionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,
                purchaseOrderPaymentService.rejectPayment(id, paymentId, request.getReason(), userId));
    }

    @PostMapping("/{id}/payments/{paymentId}/void")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_PAYMENT_VOID')")
    public GeneralResponse<PurchaseOrderPaymentDto> voidPayment(
            @PathVariable("id") String id,
            @PathVariable("paymentId") Long paymentId,
            @RequestBody PurchaseOrderPaymentDecisionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,
                purchaseOrderPaymentService.voidPayment(id, paymentId, request.getReason(), userId));
    }
}
