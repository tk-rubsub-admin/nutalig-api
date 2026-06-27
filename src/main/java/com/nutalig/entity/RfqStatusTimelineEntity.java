package com.nutalig.entity;

import com.nutalig.entity.id.RfqStatusTimelineId;
import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Entity(name = "RfqStatusTimeline")
@Table(name = "rfq_status_timeline")
public class RfqStatusTimelineEntity {

    @EmbeddedId
    private RfqStatusTimelineId id;

    @MapsId("rfqId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", referencedColumnName = "id", nullable = false)
    private RfqHeaderEntity rfqHeader;

    @Column(name = "status_datetime", nullable = false)
    private ZonedDateTime statusDatetime;
}
