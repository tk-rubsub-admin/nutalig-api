package com.nutalig.repository;

import com.nutalig.entity.GeneratedIdSequenceEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratedIdSequenceRepository extends CrudRepository<GeneratedIdSequenceEntity, String> {
}
