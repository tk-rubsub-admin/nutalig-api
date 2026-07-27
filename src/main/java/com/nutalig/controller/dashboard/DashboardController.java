package com.nutalig.controller.dashboard;

import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.DashboardDataDto;
import com.nutalig.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public GeneralResponse<DashboardDataDto> getDashboard(
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo,
            @RequestParam(value = "salesId", required = false) String salesId,
            @RequestParam(value = "procurementId", required = false) String procurementId
    ) {
        log.info("=== Start get dashboard dateFrom {} dateTo {} salesId {} procurementId {} ===",
                dateFrom, dateTo, salesId, procurementId);

        DashboardDataDto response = dashboardService.getDashboard(dateFrom, dateTo, salesId, procurementId);

        log.info("=== End get dashboard ===");
        return new GeneralResponse<>(SUCCESS, response);
    }
}
