package com.nutalig.repository;

import com.nutalig.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {

    List<CalendarEventEntity> findAllByActiveTrueOrderByStartAtAsc();

    List<CalendarEventEntity> findAllByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
            ZonedDateTime rangeEnd,
            ZonedDateTime rangeStart
    );
}
