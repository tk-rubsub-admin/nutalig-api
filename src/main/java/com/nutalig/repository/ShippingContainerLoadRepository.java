package com.nutalig.repository;
import com.nutalig.entity.ShippingContainerLoadEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ShippingContainerLoadRepository extends JpaRepository<ShippingContainerLoadEntity,Long> {
    @Query("select l from ShippingContainerLoadEntity l join fetch l.container where l.delivery.id = :deliveryId and l.active = true order by l.id")
    List<ShippingContainerLoadEntity> findActiveByDeliveryId(@Param("deliveryId") Long deliveryId);
    boolean existsByDelivery_IdAndActiveTrue(Long deliveryId);
}
