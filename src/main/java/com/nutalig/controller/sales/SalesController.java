package com.nutalig.controller.sales;

import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.sales.request.CreateSalesRequest;
import com.nutalig.controller.sales.request.SearchSalesRequest;
import com.nutalig.controller.sales.request.UpdateSalesRequest;
import com.nutalig.dto.SalesAccountDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sales")
public class SalesController {

    private final SalesService salesService;

    @PostMapping
    public GeneralResponse<SalesAccountDto> createSales(@RequestBody CreateSalesRequest request)
            throws InvalidRequestException {
        log.info("=== Start create sales {} ===", request.getSalesId());

        SalesAccountDto response = salesService.createSales(request);

        log.info("=== End create sales {} ===", response.getSalesId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping
    public GeneralResponse<com.nutalig.controller.response.Pageable<SalesAccountDto>> searchSales(
            SearchSalesRequest searchRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search sales page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        com.nutalig.controller.response.Pageable<SalesAccountDto> response =
                salesService.searchSales(searchRequest, pageableRequest);

        log.info("=== End search sales page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PutMapping("/{salesId}")
    public GeneralResponse<SalesAccountDto> updateSales(
            @PathVariable("salesId") String salesId,
            @RequestBody UpdateSalesRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start update sales {} ===", salesId);

        SalesAccountDto response = salesService.updateSales(salesId, request);

        log.info("=== End update sales {} ===", salesId);
        return new GeneralResponse<>(SUCCESS, response);
    }
}
