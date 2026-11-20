package com.nutalig.dto;

import lombok.Data;

@Data
public class CartonLabelScanDto {
    private String purchaseOrderNo;
    private Long purchaseOrderDetailId;
    private Long purchaseOrderDetailPackageId;
    private Long componentId;
    private String productName;
    private String spec;
    private String componentCode;
    private String componentName;
    private String componentSpecification;
    private String packageDimension;
    private String packageWeight;
    private String packageCapacity;
}
