package com.nutalig.repository;

import com.nutalig.constant.Status;
import com.nutalig.dto.CustomerDto;
import com.nutalig.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, String>, JpaSpecificationExecutor<CustomerEntity> {

    Optional<CustomerEntity> findFirstByCompanyNameContainingIgnoreCase(String companyName);

    Optional<CustomerEntity> findFirstByCustomerNameContainingIgnoreCase(String customerName);

    Optional<CustomerEntity> findFirstByTaxIdIgnoreCase(String taxId);

    Optional<CustomerEntity> findFirstByCustomerNameIgnoreCaseAndBranchNumberIgnoreCaseAndCompanyNameIgnoreCase(
            String customerName,
            String branchNumber,
            String companyName
    );

    @Query("SELECT new com.nutalig.dto.CustomerDto(c.id, c.customerName) FROM Customer c WHERE c.status = 'ACTIVE' ORDER BY c.createdDate DESC")
    List<CustomerDto> findAllBasicCustomer();

    List<CustomerEntity> findAllByStatusOrderByCreatedDateDesc(Status status);

    List<CustomerEntity> findAllByStatusAndSalesAccountOrderByCreatedDateDesc(Status status, String salesAccount);

}
