package com.nutalig.repository;

import com.nutalig.entity.AiPromptLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiPromptLogRepository extends JpaRepository<AiPromptLogEntity, Long> {
}
