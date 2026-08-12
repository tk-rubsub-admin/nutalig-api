package com.nutalig.repository;

import com.nutalig.constant.RfqStatus;
import com.nutalig.entity.RfqStatusTimelineEntity;
import com.nutalig.entity.id.RfqStatusTimelineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RfqStatusTimelineRepository extends JpaRepository<RfqStatusTimelineEntity, RfqStatusTimelineId> {

    List<RfqStatusTimelineEntity> findAllByRfqHeader_IdOrderByStatusDatetimeAsc(String rfqId);

    List<RfqStatusTimelineEntity> findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(
            Collection<String> rfqIds,
            RfqStatus status
    );

    Optional<RfqStatusTimelineEntity> findByIdRfqIdAndIdStatus(String rfqId, RfqStatus status);
}
