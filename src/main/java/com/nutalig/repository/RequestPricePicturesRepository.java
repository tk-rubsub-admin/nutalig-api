package com.nutalig.repository;

import com.nutalig.entity.RfqPicturesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestPricePicturesRepository extends JpaRepository<RfqPicturesEntity, Long> {
}
