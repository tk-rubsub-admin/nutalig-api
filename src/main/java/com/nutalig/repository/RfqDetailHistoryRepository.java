package com.nutalig.repository;

import com.nutalig.entity.RfqDetailHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RfqDetailHistoryRepository extends JpaRepository<RfqDetailHistoryEntity, Long> {

    @Query("select coalesce(max(h.detailSetNo), 0) from RfqDetailHistoryEntity h where h.rfqId = :rfqId")
    Integer findMaxDetailSetNoByRfqId(@Param("rfqId") String rfqId);

    List<RfqDetailHistoryEntity> findByRfqIdOrderByDetailSetNoDescIdDesc(String rfqId);
}
