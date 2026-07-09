package com.nutalig.controller.purchaseorder;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.response.CreatePurchaseOrderResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.dto.PurchaseOrderDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
            @RequestPart("attachments") List<MultipartFile> attachments,
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
            @RequestHeader("userId") String userId
    ) throws Exception {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.addAttachments(id, attachments, userId));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public GeneralResponse<PurchaseOrderDto> deleteAttachment(
            @PathVariable("id") String id,
            @PathVariable("attachmentId") Long attachmentId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderService.deleteAttachment(id, attachmentId, userId));
    }
}
