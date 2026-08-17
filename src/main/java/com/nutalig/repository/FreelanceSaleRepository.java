package com.nutalig.repository;

import com.nutalig.entity.FreelanceSaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FreelanceSaleRepository extends JpaRepository<FreelanceSaleEntity, String>, JpaSpecificationExecutor<FreelanceSaleEntity> {
}
