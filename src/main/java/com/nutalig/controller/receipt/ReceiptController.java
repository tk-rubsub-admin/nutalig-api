package com.nutalig.controller.receipt;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.controller.receipt.request.CreateReceiptRequest;
import com.nutalig.controller.receipt.request.SearchReceiptRequest;
import com.nutalig.controller.receipt.response.CreateReceiptResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.dto.ReceiptDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.ReceiptEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.ReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/search")
    public GeneralResponse<Pageable<ReceiptDto>> searchReceipts(
            @RequestBody(required = false) SearchReceiptRequest request,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search receipts ===");
        Pageable<ReceiptDto> response = receiptService.searchReceipts(request, pageableRequest);
        log.info("=== End search receipts ===");
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping
    public GeneralResponse<CreateReceiptResponse> createReceipt(
            @RequestBody CreateReceiptRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start create receipt ===");
        ReceiptEntity entity = receiptService.createReceipt(request, userId);
        log.info("=== End create receipt {} ===", entity.getReceiptNo());
        return new GeneralResponse<>(SUCCESS, new CreateReceiptResponse(entity.getReceiptNo()));
    }

    @GetMapping
    public GeneralResponse<ReceiptDto> getReceiptById(@RequestParam(name = "id") String id)
            throws DataNotFoundException {
        log.info("=== Start get receipt by id {} ===", id);
        ReceiptDto response = receiptService.getReceiptById(id);
        log.info("=== End get receipt by id {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/void")
    public GeneralResponse<ReceiptDto> voidReceipt(
            @PathVariable(name = "id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start void receipt {} ===", id);
        ReceiptDto response = receiptService.voidReceipt(id, userId);
        log.info("=== End void receipt {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/document")
    public ResponseEntity<DownloadDocumentDto> getReceiptDocumentById(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "format") ExportFileFormat format,
            @RequestParam(name = "isOriginal") Boolean isOriginal,
            @RequestParam(name = "isCopy") Boolean isCopy
    ) throws Exception {
        log.info("=== Start download receipt document ===");

        DownloadDocumentDto doc = receiptService.getReceiptDocumentById(id, new DocumentRequest(format, isOriginal, isCopy));

        if (doc == null || doc.getFiles() == null || doc.getFiles().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        log.info("=== End download receipt document ===");
        return ResponseEntity.ok(doc);
    }
}
