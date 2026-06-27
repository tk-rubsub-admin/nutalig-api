package com.nutalig.dto;

import com.nutalig.constant.RfqStatus;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqStatusTimelineDto {

    private String rfqId;
    private RfqStatus status;
    private ZonedDateTime statusDatetime;
}
