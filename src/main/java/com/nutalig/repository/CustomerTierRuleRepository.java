package com.nutalig.repository;

import com.nutalig.entity.CustomerTierRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerTierRuleRepository extends JpaRepository<CustomerTierRuleEntity, String> {
    List<CustomerTierRuleEntity> findAllByOrderByMinimumOrderAmountDesc();
}
