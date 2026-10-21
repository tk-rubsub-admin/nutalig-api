package com.nutalig.controller.exchangerate;

import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.ExchangeRateDto;
import com.nutalig.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/exchange-rates")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/{baseCurrency}/{quoteCurrency}")
    public GeneralResponse<ExchangeRateDto> getExchangeRate(
            @PathVariable String baseCurrency,
            @PathVariable String quoteCurrency
    ) {
        return new GeneralResponse<>(SUCCESS, exchangeRateService.getExchangeRate(baseCurrency, quoteCurrency));
    }
}
