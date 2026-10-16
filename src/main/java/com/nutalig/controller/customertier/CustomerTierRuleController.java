package com.nutalig.controller.customertier;

import com.nutalig.controller.customertier.request.UpdateCustomerTierRulesRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.CustomerTierRuleDto;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.CustomerTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/customer-tier-rules")
public class CustomerTierRuleController {

    private final CustomerTierService customerTierService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public GeneralResponse<List<CustomerTierRuleDto>> getRules() {
        return new GeneralResponse<>(SUCCESS, customerTierService.getRules());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public GeneralResponse<List<CustomerTierRuleDto>> updateRules(
            @RequestBody UpdateCustomerTierRulesRequest request
    ) throws InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, customerTierService.updateRules(request));
    }
}
