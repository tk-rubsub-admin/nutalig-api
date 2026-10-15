package com.nutalig.repository;

import com.nutalig.entity.QcDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Collection;
import java.util.List;

public interface QcDocumentRepository extends JpaRepository<QcDocumentEntity, Long> {

    @EntityGraph(attributePaths = "currentRevision")
    List<QcDocumentEntity> findAllByQcRouteCodeInAndIsActiveTrueOrderByIdAsc(Collection<String> routeCodes);
}
