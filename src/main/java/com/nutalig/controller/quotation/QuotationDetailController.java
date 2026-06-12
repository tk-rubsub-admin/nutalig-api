package com.nutalig.controller.quotation;

import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.QuotationDto;
import com.nutalig.dto.QuotationRequestDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.service.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QuotationDetailController {

    private final QuotationService quotationService;

    @GetMapping("/v1/quotation")
    public GeneralResponse<QuotationDto> getQuotationDetailById(@RequestParam(name = "id") String id)
            throws DataNotFoundException {
        log.info("=== Start get quotation by id ===");

        QuotationDto quotationDto = quotationService.getQuotationDetailById(id);

        log.info("=== End get quotation by id ===");
        return new GeneralResponse<>(SUCCESS, quotationDto);
    }

    @PatchMapping("/v1/quotation")
    public GeneralResponse<QuotationDto> updateQuotation(
            @RequestParam(name = "id") String id,
            @RequestBody QuotationRequestDto requestDto,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException {
        log.info("=== Start update quotation by id ===");

        QuotationDto quotationDto = quotationService.updateQuotation(id, requestDto, userId);

        log.info("=== End update quotation by id ===");
        return new GeneralResponse<>(SUCCESS, quotationDto);
    }
}
