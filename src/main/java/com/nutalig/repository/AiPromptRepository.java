package com.nutalig.repository;

import com.nutalig.entity.AiPromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPromptRepository extends JpaRepository<AiPromptEntity, Long> {

    Optional<AiPromptEntity> findByCodeAndActiveTrue(String code);

}
