package com.nutalig.dto;

import lombok.Data;
import com.nutalig.constant.OrderLineType;
import com.nutalig.constant.SampleRefundPolicy;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class RfqDetailDto {

    private Long id;
    private String optionName;
    private OrderLineType lineType;
    private SampleRefundPolicy sampleRefundPolicy;
    private BigDecimal sampleRefundPercent;
    private BigDecimal sampleRefundMinOrderQty;
    private String sampleRefundNote;
    private String sampleRefundConditionText;
    private String plan;
    private String spec;
    private Integer sortOrder;
    private String remark;
    private String internalRemark;
    private String recommend;
    private BigDecimal commission;
    private String packageDimension;
    private String packageWeight;
    private String packageCapacity;
    private SupplierDto supplier;
    private List<RfqTierDto> tiers;
    private List<RfqTierSplitDto> tierSplits;
    private ZonedDateTime createdDate;
    private ZonedDateTime updatedDate;
    private String createdBy;
    private String updatedBy;
}
