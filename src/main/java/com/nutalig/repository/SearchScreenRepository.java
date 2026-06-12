package com.nutalig.repository;

import com.nutalig.entity.SearchScreenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchScreenRepository extends JpaRepository<SearchScreenEntity, String> {
}
