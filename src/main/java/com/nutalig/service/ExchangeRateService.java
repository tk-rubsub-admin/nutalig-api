package com.nutalig.service;

import com.nutalig.dto.ExchangeRateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final DateTimeFormatter RESPONSE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestClient restClient;

    @Value("${frankfurter.base-url}")
    private String frankfurterBaseUrl;

    public ExchangeRateDto getExchangeRate(String baseCurrency, String quoteCurrency) {
        String base = normalizeCurrency(baseCurrency, "baseCurrency");
        String quote = normalizeCurrency(quoteCurrency, "quoteCurrency");
        String url = StringUtils.stripEnd(frankfurterBaseUrl, "/") + "/" + base + "/" + quote;

        log.info("Get exchange rate from Frankfurter: {} -> {}", base, quote);
        ExchangeRateDto response = restClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new IllegalStateException("Frankfurter request failed with HTTP "
                            + clientResponse.getStatusCode().value());
                })
                .body(ExchangeRateDto.class);

        if (response == null) {
            throw new IllegalStateException("Frankfurter returned an empty exchange rate response");
        }
        if (StringUtils.isNotBlank(response.getDate())) {
            response.setDate(LocalDate.parse(response.getDate()).format(RESPONSE_DATE_FORMAT));
        }
        return response;
    }

    private String normalizeCurrency(String currency, String fieldName) {
        String normalized = StringUtils.trimToEmpty(currency).toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(fieldName + " must be a three-letter currency code");
        }
        return normalized;
    }
}
