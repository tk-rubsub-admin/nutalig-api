package com.nutalig.repository;

import com.nutalig.entity.SearchFieldEntity;
import com.nutalig.entity.id.SearchFieldId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchFieldRepository extends JpaRepository<SearchFieldEntity, SearchFieldId> {
    List<SearchFieldEntity> findByScreenCodeAndActiveTrueOrderBySortOrderAscFieldCodeAsc(String screenCode);
}
