package com.nutalig.repository;

import com.nutalig.entity.RequestPricePicturesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestPricePicturesRepository extends JpaRepository<RequestPricePicturesEntity, Long> {
}
