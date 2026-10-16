package com.nutalig.controller.dashboard;

import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.SalesMonthlyPerformanceDto;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.SalesPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sales-performance")
public class SalesPerformanceController {

    private final SalesPerformanceService salesPerformanceService;

    @GetMapping("/monthly")
    public GeneralResponse<List<SalesMonthlyPerformanceDto>> getMonthlySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String salesId
    ) throws InvalidRequestException {
        if (dateFrom.isAfter(dateTo)) throw new InvalidRequestException("dateFrom must not be after dateTo.");
        return new GeneralResponse<>(SUCCESS,
                salesPerformanceService.getMonthlySummary(dateFrom, dateTo, salesId));
    }
}
