package com.nutalig.controller.invoice;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.constant.PaymentMethod;
import com.nutalig.controller.invoice.request.CreateInvoiceRequest;
import com.nutalig.controller.invoice.request.SearchInvoiceRequest;
import com.nutalig.controller.invoice.response.CreateInvoiceResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.dto.InvoiceDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.InvoiceEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/search")
    public GeneralResponse<Pageable<InvoiceDto>> searchInvoices(
            @RequestBody(required = false) SearchInvoiceRequest request,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search invoices ===");

        Pageable<InvoiceDto> response = invoiceService.searchInvoices(request, pageableRequest);

        log.info("=== End search invoices ===");
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping
    public GeneralResponse<CreateInvoiceResponse> createInvoice(
            @RequestBody CreateInvoiceRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start create invoice ===");

        InvoiceEntity entity = invoiceService.createInvoice(request, userId);

        log.info("=== End create invoice {} ===", entity.getInvoiceNo());
        return new GeneralResponse<>(SUCCESS, new CreateInvoiceResponse(entity.getInvoiceNo()));
    }

    @GetMapping
    public GeneralResponse<InvoiceDto> getInvoiceById(@RequestParam(name = "id") String id)
            throws DataNotFoundException {
        log.info("=== Start get invoice by id {} ===", id);

        InvoiceDto response = invoiceService.getInvoiceById(id);

        log.info("=== End get invoice by id {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/sales-orders/{salesOrderId}")
    public GeneralResponse<java.util.List<InvoiceDto>> getInvoicesBySalesOrderId(
            @PathVariable(name = "salesOrderId") String salesOrderId
    ) {
        log.info("=== Start get invoices by sales order id {} ===", salesOrderId);

        java.util.List<InvoiceDto> response = invoiceService.getInvoicesBySalesOrderId(salesOrderId);

        log.info("=== End get invoices by sales order id {}, size {} ===", salesOrderId, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/document")
    public ResponseEntity<DownloadDocumentDto> getInvoiceDocumentById(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "format") ExportFileFormat format,
            @RequestParam(name = "isOriginal") Boolean isOriginal,
            @RequestParam(name = "isCopy") Boolean isCopy
    ) throws Exception {
        log.info("=== Start download invoice document ===");

        DownloadDocumentDto doc = invoiceService.getInvoiceDocumentById(id, new DocumentRequest(format, isOriginal, isCopy));

        if (doc == null || doc.getFiles() == null || doc.getFiles().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        log.info("=== End download invoice document ===");
        return ResponseEntity.ok(doc);
    }

    @PostMapping(path = "/{id}/payments", consumes = "multipart/form-data")
    public GeneralResponse<InvoiceDto> receivePayment(
            @PathVariable(name = "id") String id,
            @RequestParam(name = "paymentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.ZonedDateTime paymentDate,
            @RequestParam(name = "amount") java.math.BigDecimal amount,
            @RequestParam(name = "paymentMethod") PaymentMethod paymentMethod,
            @RequestParam(name = "chequeBank", required = false) String chequeBank,
            @RequestParam(name = "chequeNo", required = false) String chequeNo,
            @RequestParam(name = "chequeDate", required = false) java.time.LocalDate chequeDate,
            @RequestParam(name = "chequeBranch", required = false) String chequeBranch,
            @RequestPart(name = "slipFile", required = false) MultipartFile slipFile,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start receive invoice payment {} ===", id);

        InvoiceDto response = invoiceService.receivePayment(
                id,
                paymentDate,
                amount,
                paymentMethod,
                chequeBank,
                chequeNo,
                chequeDate,
                chequeBranch,
                slipFile,
                userId
        );

        log.info("=== End receive invoice payment {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }
}
