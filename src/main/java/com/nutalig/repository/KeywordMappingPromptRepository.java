package com.nutalig.repository;

import com.nutalig.entity.KeywordMappingPromptEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordMappingPromptRepository extends CrudRepository<KeywordMappingPromptEntity, String> {
}
