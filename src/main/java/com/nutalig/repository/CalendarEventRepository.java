package com.nutalig.repository;

import com.nutalig.constant.CalendarEventType;
import com.nutalig.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {

    List<CalendarEventEntity> findAllByActiveTrueAndEventTypeNotOrderByStartAtAsc(
            CalendarEventType eventType
    );

    List<CalendarEventEntity> findAllByActiveTrueAndEventTypeNotAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
            CalendarEventType eventType,
            ZonedDateTime rangeEnd,
            ZonedDateTime rangeStart
    );

    List<CalendarEventEntity> findAllByActiveTrueAndEventTypeAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
            CalendarEventType eventType,
            ZonedDateTime rangeEnd,
            ZonedDateTime rangeStart
    );

    List<CalendarEventEntity> findAllByActiveTrueAndEventTypeAndCreatedByOrderByStartAtAsc(
            CalendarEventType eventType,
            String createdBy
    );

    List<CalendarEventEntity> findAllByActiveTrueAndEventTypeAndCreatedByAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
            CalendarEventType eventType,
            String createdBy,
            ZonedDateTime rangeEnd,
            ZonedDateTime rangeStart
    );

    java.util.Optional<CalendarEventEntity> findByIdAndActiveTrueAndEventTypeAndCreatedBy(
            Long id,
            CalendarEventType eventType,
            String createdBy
    );
}
