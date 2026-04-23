package com.nutalig.repository;

import com.nutalig.entity.SlaConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaConfigRepository extends JpaRepository<SlaConfigEntity, String> {
}
