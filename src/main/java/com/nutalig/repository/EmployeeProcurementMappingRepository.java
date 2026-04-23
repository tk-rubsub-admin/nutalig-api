package com.nutalig.repository;

import com.nutalig.entity.EmployeeProcurementMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeProcurementMappingRepository
        extends JpaRepository<EmployeeProcurementMappingEntity, Long> {

    List<EmployeeProcurementMappingEntity> findBySalesEmployee_EmployeeId(
            String salesEmployeeId
    );

    List<EmployeeProcurementMappingEntity> findByProcurementEmployee_EmployeeId(
            String procurementEmployeeId
    );

    boolean existsBySalesEmployee_EmployeeIdAndProcurementEmployee_EmployeeId(
            String salesEmployeeId,
            String procurementEmployeeId
    );
}
