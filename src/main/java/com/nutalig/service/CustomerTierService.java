package com.nutalig.service;

import com.nutalig.constant.SystemConstant;
import com.nutalig.controller.customertier.request.UpdateCustomerTierRulesRequest;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.CustomerTierRuleEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.dto.CustomerTierRuleDto;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.CustomerTierRuleRepository;
import com.nutalig.repository.CustomerRepository;
import com.nutalig.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerTierService {

    private final CustomerTierRuleRepository tierRuleRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public void applyTier(CustomerEntity customer) {
        if (customer == null) return;

        BigDecimal total = customer.getTotalSalesOrderAmount() == null
                ? BigDecimal.ZERO : customer.getTotalSalesOrderAmount().setScale(0, RoundingMode.DOWN);
        List<CustomerTierRuleEntity> rules = tierRuleRepository.findAllByOrderByMinimumOrderAmountDesc();
        for (CustomerTierRuleEntity rule : rules) {
            boolean meetsMinimum = total.compareTo(rule.getMinimumOrderAmount()) >= 0;
            boolean withinMaximum = rule.getMaximumOrderAmount() == null
                    || total.compareTo(rule.getMaximumOrderAmount()) <= 0;
            if (meetsMinimum && withinMaximum) {
                SystemConfigEntity tier = systemConfigRepository
                        .findByIdGroupCodeAndIdCode(SystemConstant.CUSTOMER_TIER, rule.getTierCode())
                        .orElse(null);
                if (tier != null) customer.setCustomerTier(tier);
                return;
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CustomerTierRuleDto> getRules() {
        Map<String, CustomerTierRuleEntity> rules = tierRuleRepository.findAll().stream()
                .collect(Collectors.toMap(CustomerTierRuleEntity::getTierCode, rule -> rule));
        return systemConfigRepository.findByIdGroupCodeOrderBySortAsc(SystemConstant.CUSTOMER_TIER).stream()
                .map(config -> toDto(config, rules.get(config.getId().getCode())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public List<CustomerTierRuleDto> updateRules(UpdateCustomerTierRulesRequest request)
            throws InvalidRequestException {
        List<SystemConfigEntity> tiers = systemConfigRepository
                .findByIdGroupCodeOrderBySortAsc(SystemConstant.CUSTOMER_TIER);
        if (tiers.isEmpty() || request == null || request.getRules() == null
                || request.getRules().size() != tiers.size()) {
            throw new InvalidRequestException("A rule is required for every customer tier.");
        }

        Map<String, CustomerTierRuleDto> requested = request.getRules().stream()
                .collect(Collectors.toMap(CustomerTierRuleDto::getTierCode, rule -> rule,
                        (first, duplicate) -> first));
        if (requested.size() != tiers.size()
                || tiers.stream().anyMatch(tier -> !requested.containsKey(tier.getId().getCode()))) {
            throw new InvalidRequestException("Rules must contain each customer tier exactly once.");
        }

        List<CustomerTierRuleDto> ordered = tiers.stream()
                .map(tier -> requested.get(tier.getId().getCode()))
                .sorted(Comparator.comparing(CustomerTierRuleDto::getMinimumOrderAmount,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        for (int i = 0; i < ordered.size(); i++) {
            CustomerTierRuleDto rule = ordered.get(i);
            if (rule.getMinimumOrderAmount() == null || rule.getMinimumOrderAmount().signum() < 0
                    || (rule.getMaximumOrderAmount() != null
                    && rule.getMaximumOrderAmount().compareTo(rule.getMinimumOrderAmount()) < 0)
                    || rule.getMinimumOrderAmount().stripTrailingZeros().scale() > 0
                    || (rule.getMaximumOrderAmount() != null
                    && rule.getMaximumOrderAmount().stripTrailingZeros().scale() > 0)) {
                throw new InvalidRequestException("Tier amount ranges must be valid and non-negative.");
            }
            if (i == 0 && rule.getMinimumOrderAmount().compareTo(BigDecimal.ZERO) != 0) {
                throw new InvalidRequestException("The lowest tier must start at 0.");
            }
            if (i < ordered.size() - 1) {
                CustomerTierRuleDto next = ordered.get(i + 1);
                if (rule.getMaximumOrderAmount() == null
                        || next.getMinimumOrderAmount().compareTo(
                        rule.getMaximumOrderAmount().add(BigDecimal.ONE)) != 0) {
                    throw new InvalidRequestException("Tier ranges must be continuous and non-overlapping.");
                }
            } else if (rule.getMaximumOrderAmount() != null) {
                throw new InvalidRequestException("The highest tier must have no maximum amount.");
            }
        }

        List<CustomerTierRuleEntity> entities = new ArrayList<>();
        for (SystemConfigEntity tier : tiers) {
            CustomerTierRuleDto value = requested.get(tier.getId().getCode());
            CustomerTierRuleEntity entity = tierRuleRepository.findById(tier.getId().getCode())
                    .orElseGet(CustomerTierRuleEntity::new);
            entity.setTierCode(tier.getId().getCode());
            entity.setMinimumOrderAmount(value.getMinimumOrderAmount());
            entity.setMaximumOrderAmount(value.getMaximumOrderAmount());
            entities.add(entity);
        }
        tierRuleRepository.saveAll(entities);
        refreshAllCustomerTiers();

        Map<String, CustomerTierRuleEntity> saved = entities.stream()
                .collect(Collectors.toMap(CustomerTierRuleEntity::getTierCode, rule -> rule));
        return tiers.stream().map(tier -> toDto(tier, saved.get(tier.getId().getCode()))).toList();
    }

    private void refreshAllCustomerTiers() {
        List<CustomerTierRuleEntity> rules = tierRuleRepository.findAllByOrderByMinimumOrderAmountDesc();
        Map<String, SystemConfigEntity> tiers = systemConfigRepository
                .findByIdGroupCodeOrderBySortAsc(SystemConstant.CUSTOMER_TIER).stream()
                .collect(Collectors.toMap(config -> config.getId().getCode(), config -> config));
        var customers = customerRepository.findAll();
        for (CustomerEntity customer : customers) {
            BigDecimal total = customer.getTotalSalesOrderAmount() == null
                    ? BigDecimal.ZERO : customer.getTotalSalesOrderAmount().setScale(0, RoundingMode.DOWN);
            for (CustomerTierRuleEntity rule : rules) {
                if (total.compareTo(rule.getMinimumOrderAmount()) >= 0
                        && (rule.getMaximumOrderAmount() == null
                        || total.compareTo(rule.getMaximumOrderAmount()) <= 0)) {
                    SystemConfigEntity tier = tiers.get(rule.getTierCode());
                    if (tier != null) customer.setCustomerTier(tier);
                    break;
                }
            }
        }
        customerRepository.saveAll(customers);
    }

    private CustomerTierRuleDto toDto(SystemConfigEntity config, CustomerTierRuleEntity rule) {
        CustomerTierRuleDto dto = new CustomerTierRuleDto();
        dto.setTierCode(config.getId().getCode());
        dto.setNameTh(config.getNameTh());
        dto.setNameEn(config.getNameEn());
        dto.setSort(config.getSort());
        if (rule != null) {
            dto.setMinimumOrderAmount(rule.getMinimumOrderAmount());
            dto.setMaximumOrderAmount(rule.getMaximumOrderAmount());
        }
        return dto;
    }
}
