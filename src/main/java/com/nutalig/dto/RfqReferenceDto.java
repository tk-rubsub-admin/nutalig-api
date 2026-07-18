package com.nutalig.dto;

import com.nutalig.constant.RfqStatus;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RfqReferenceDto {

    private String id;
    private ZonedDateTime requestedDate;
    private RfqStatus status;
    private SystemConfigDto rfqType;
    private ProductFamilyDto productFamily;
    private ProductSubtype1Dto productSubtype1;
    private ProductSubtype2Dto productSubType2;
    private ProductMaterialDto material;
}
