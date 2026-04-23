package com.nutalig.repository;

import com.nutalig.constant.ActivityEntityType;
import com.nutalig.entity.ActivityHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityHistoryRepository extends JpaRepository<ActivityHistoryEntity, Long> {

    List<ActivityHistoryEntity> findByEntityTypeAndReferenceIdOrderByActionAtDesc(ActivityEntityType entityType, String referenceId);
}
