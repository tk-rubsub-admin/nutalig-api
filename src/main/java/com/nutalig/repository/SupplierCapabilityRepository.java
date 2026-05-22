package com.nutalig.repository;

import com.nutalig.constant.Status;
import com.nutalig.entity.SupplierCapabilityEntity;
import com.nutalig.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierCapabilityRepository extends JpaRepository<SupplierCapabilityEntity, String> {

    List<SupplierCapabilityEntity> findAllBySupplier_IdAndStatusOrderByProductFamilyCodeAscProductMaterialCodeAsc(
            String supplierId,
            Status status
    );

    boolean existsBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeIsNullAndStatus(
            String supplierId,
            String productFamilyCode,
            Status status
    );

    boolean existsBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeAndStatus(
            String supplierId,
            String productFamilyCode,
            String productMaterialCode,
            Status status
    );

    boolean existsBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeIsNotNullAndStatus(
            String supplierId,
            String productFamilyCode,
            Status status
    );

    Optional<SupplierCapabilityEntity> findBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeIsNullAndStatus(
            String supplierId,
            String productFamilyCode,
            Status status
    );

    Optional<SupplierCapabilityEntity> findBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeAndStatus(
            String supplierId,
            String productFamilyCode,
            String productMaterialCode,
            Status status
    );

    @Query("""
            select distinct capability.supplier
            from SupplierCapability capability
            where capability.status = :capabilityStatus
              and capability.supplier.status = :supplierStatus
              and capability.productFamilyCode = :productFamilyCode
              and (
                    :productMaterialCode is null
                    or capability.productMaterialCode is null
                    or capability.productMaterialCode = :productMaterialCode
              )
            """)
    List<SupplierEntity> findSuggestedSuppliers(
            @Param("productFamilyCode") String productFamilyCode,
            @Param("productMaterialCode") String productMaterialCode,
            @Param("capabilityStatus") Status capabilityStatus,
            @Param("supplierStatus") Status supplierStatus
    );
}
