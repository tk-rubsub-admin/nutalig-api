package com.nutalig.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RfqDetailHistorySnapshotDto {
    private Integer detailSetNo;
    private String archivedBy;
    private ZonedDateTime archivedAt;
    private Long sourceDetailId;
    private String optionName;
    private String spec;
    private Integer sortOrder;
    private String remark;
    private String recommend;
    private BigDecimal commission;
    private String packageDimension;
    private String packageWeight;
    private String packageCapacity;
    private String supplierId;
    private List<RfqDetailHistoryTierDto> tiers;
    private List<RfqDetailHistoryTierSplitDto> tierSplits;
}
