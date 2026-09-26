package com.nutalig.controller.purchaseorder;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.constant.PurchaseOrderAttachmentDocumentType;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderPaymentRequest;
import com.nutalig.controller.purchaseorder.request.PurchaseOrderPaymentDecisionRequest;
import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.PurchaseOrderCbmPreviewRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.StartPurchaseOrderRunRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderPaymentRequest;
import com.nutalig.controller.purchaseorder.response.CreatePurchaseOrderResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.dto.PurchaseOrderDto;
import com.nutalig.dto.PurchaseOrderCbmPreviewDto;
import com.nutalig.dto.PurchaseOrderPaymentDto;
import com.nutalig.dto.PurchaseOrderPaymentScheduleDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.PurchaseOrderService;
import com.nutalig.service.PurchaseOrderPaymentService;
import com.nutalig.service.PurchaseOrderPaymentScheduleService;
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
    private final PurchaseOrderPaymentScheduleService purchaseOrderPaymentScheduleService;

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

    @PatchMapping("/{id}/production-complete")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_START_RUN')")
    public GeneralResponse<PurchaseOrderDto> productionComplete(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.completeProduction(id, userId));
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
                request != null ? request.getLateStartReason() : null
        ));
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
