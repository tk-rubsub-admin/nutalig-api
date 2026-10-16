package com.nutalig.repository;

import com.nutalig.entity.CustomerAnnualSalesEntity;
import com.nutalig.entity.id.CustomerAnnualSalesId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAnnualSalesRepository extends JpaRepository<CustomerAnnualSalesEntity, CustomerAnnualSalesId> {
}
