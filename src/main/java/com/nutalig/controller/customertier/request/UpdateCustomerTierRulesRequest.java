package com.nutalig.controller.customertier.request;

import com.nutalig.dto.CustomerTierRuleDto;
import lombok.Data;

import java.util.List;

@Data
public class UpdateCustomerTierRulesRequest {
    private List<CustomerTierRuleDto> rules;
}
