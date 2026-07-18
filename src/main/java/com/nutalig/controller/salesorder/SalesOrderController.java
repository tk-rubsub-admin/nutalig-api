package com.nutalig.controller.salesorder;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.salesorder.request.CreateSalesOrderRequest;
import com.nutalig.controller.salesorder.request.SearchSalesOrderRequest;
import com.nutalig.controller.salesorder.request.UpdateSalesOrderRequest;
import com.nutalig.controller.salesorder.response.CreateSalesOrderResponse;
import com.nutalig.dto.SalesOrderDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.SalesOrderEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sales-orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @PostMapping("/search")
    public GeneralResponse<Pageable<SalesOrderDto>> searchSalesOrders(
            @RequestBody(required = false) SearchSalesOrderRequest request,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search sales orders ===");

        Pageable<SalesOrderDto> response = salesOrderService.searchSalesOrders(request, pageableRequest);

        log.info("=== End search sales orders ===");
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping
    public GeneralResponse<CreateSalesOrderResponse> createSalesOrder(
            @RequestBody CreateSalesOrderRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start create sales order ===");

        SalesOrderEntity salesOrder = salesOrderService.createSalesOrder(request, userId);

        log.info("=== End create sales order {} ===", salesOrder.getSalesOrderNo());
        return new GeneralResponse<>(SUCCESS, new CreateSalesOrderResponse(salesOrder.getSalesOrderNo()));
    }

    @GetMapping
    public GeneralResponse<SalesOrderDto> getSalesOrderById(@RequestParam(name = "id") String id) throws DataNotFoundException {
        log.info("=== Start get sales order by id {} ===", id);

        SalesOrderDto response = salesOrderService.getSalesOrderById(id);

        log.info("=== End get sales order by id {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}")
    public GeneralResponse<SalesOrderDto> updateSalesOrder(
            @PathVariable(name = "id") String id,
            @RequestBody UpdateSalesOrderRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start update sales order {} ===", id);

        SalesOrderDto response = salesOrderService.updateSalesOrder(id, request, userId);

        log.info("=== End update sales order {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping(path = "/{id}/attachments", consumes = "multipart/form-data")
    public GeneralResponse<SalesOrderDto> addAttachments(
            @PathVariable(name = "id") String id,
            @RequestPart("attachments") java.util.List<MultipartFile> attachments,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start add sales order attachments {} ===", id);

        SalesOrderDto response = salesOrderService.addAttachments(id, attachments, userId);

        log.info("=== End add sales order attachments {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public GeneralResponse<SalesOrderDto> deleteAttachment(
            @PathVariable(name = "id") String id,
            @PathVariable(name = "attachmentId") Long attachmentId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start delete sales order attachment {} from {} ===", attachmentId, id);

        SalesOrderDto response = salesOrderService.deleteAttachment(id, attachmentId, userId);

        log.info("=== End delete sales order attachment {} from {} ===", attachmentId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/document")
    public ResponseEntity<DownloadDocumentDto> getSalesOrderDocumentById(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "format") ExportFileFormat format,
            @RequestParam(name = "isOriginal") Boolean isOriginal,
            @RequestParam(name = "isCopy") Boolean isCopy
    ) throws Exception {
        log.info("=== Start download sales order document ===");

        DownloadDocumentDto doc = salesOrderService.getSalesOrderDocumentById(id, new DocumentRequest(format, isOriginal, isCopy));

        if (doc == null || doc.getFiles() == null || doc.getFiles().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        log.info("=== End download quotation document ===");
        return ResponseEntity.ok(doc);
    }

}
