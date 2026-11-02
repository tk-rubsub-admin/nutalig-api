package com.nutalig.repository;
import com.nutalig.entity.ShippingContainerEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface ShippingContainerRepository extends JpaRepository<ShippingContainerEntity,Long>, JpaSpecificationExecutor<ShippingContainerEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ShippingContainerEntity c where c.id = :id")
    Optional<ShippingContainerEntity> findByIdForUpdate(@Param("id") Long id);
}
