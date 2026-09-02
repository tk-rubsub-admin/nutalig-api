package com.nutalig.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqDetailHistoryDto {
    private Long id;
    private String rfqId;
    private Integer detailSetNo;
    private Long sourceDetailId;
    private String optionName;
    private String plan;
    private String spec;
    private Integer sortOrder;
    private String remark;
    private String internalRemark;
    private String recommend;
    private RfqDetailHistorySnapshotDto snapshot;
    private String archivedBy;
    private ZonedDateTime archivedAt;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
}
