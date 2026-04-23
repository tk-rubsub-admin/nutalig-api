package com.nutalig.controller.quotation;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.controller.quotation.request.SearchQuotationRequest;
import com.nutalig.controller.quotation.response.SearchQuotationResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.QuotationRequestDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.QuotationEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.service.QuotationService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    @PostMapping("/search")
    public GeneralResponse<SearchQuotationResponse> searchQuotation(
            @RequestBody SearchQuotationRequest searchQuotationRequest,
            @Valid PageableRequest pageableRequest) {
        log.info("=== Start search quotation ===");

        SearchQuotationResponse response = quotationService.searchQuotation(searchQuotationRequest, pageableRequest);

        log.info("=== End search quotation ===");
        return new GeneralResponse<>(SUCCESS, response);
    }
    @PostMapping
    public GeneralResponse createQuotation(@RequestBody QuotationRequestDto requestDto, @RequestHeader("userId") String userId) throws DataNotFoundException {
        log.info("=== Start create quotation ===");

        QuotationEntity quotationEntity = quotationService.createQuotation(requestDto, userId);
        record CreateQuotationResponse(String id) {

        }

        log.info("=== End create quotation");
        return new GeneralResponse<>(SUCCESS, new CreateQuotationResponse(quotationEntity.getQuotationNo()));
    }

    @GetMapping("/document")
    public ResponseEntity<DownloadDocumentDto> getQuotationDocumentById(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "format") ExportFileFormat format,
            @RequestParam(name = "isOriginal") Boolean isOriginal,
            @RequestParam(name = "isCopy") Boolean isCopy
    ) throws Exception {
        log.info("=== Start download quotation document ===");

        DownloadDocumentDto doc = quotationService.getQuotationDocumentById(id, new DocumentRequest(format, isOriginal, isCopy));

        if (doc == null || doc.getFiles() == null || doc.getFiles().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        log.info("=== End download quotation document ===");
        return ResponseEntity.ok(doc);
    }

}
