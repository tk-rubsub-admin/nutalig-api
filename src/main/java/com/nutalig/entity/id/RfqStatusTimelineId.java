package com.nutalig.entity.id;

import com.nutalig.constant.RfqStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class RfqStatusTimelineId implements Serializable {

    @Column(name = "rfq_id", length = 255)
    private String rfqId;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private RfqStatus status;
}
