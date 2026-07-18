package com.nutalig.controller.freelancesale;

import com.nutalig.controller.freelancesale.request.CreateFreelanceSaleRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.FreelanceSaleDto;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.FreelanceSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/freelance-sales")
public class FreelanceSaleController {

    private final FreelanceSaleService freelanceSaleService;

    @PostMapping
    public GeneralResponse<FreelanceSaleDto> createFreelanceSale(
            @RequestBody CreateFreelanceSaleRequest request
    ) throws InvalidRequestException {
        log.info("=== Start create freelance sale {} ===", request == null ? null : request.getId());

        FreelanceSaleDto response = freelanceSaleService.createFreelanceSale(request);

        log.info("=== End create freelance sale {} ===", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping
    public GeneralResponse<List<FreelanceSaleDto>> getFreelanceSales() {
        log.info("=== Start get freelance sales ===");

        List<FreelanceSaleDto> response = freelanceSaleService.getFreelanceSales();

        log.info("=== End get freelance sales size {} ===", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }
}
