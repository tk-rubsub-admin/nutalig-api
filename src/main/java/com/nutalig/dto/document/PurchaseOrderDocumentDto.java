package com.nutalig.dto.document;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseOrderDocumentDto extends DefaultDocumentDto {

    private String supplierName;
    private String supplierAddress;

    private String salesName;
    private String salesMobileNo;

    private String remark;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private BigDecimal freight;
    private BigDecimal subTotal;
    private BigDecimal vat;
    private BigDecimal grandTotal;
    private String thaiBahtText;

    private String coSalesId;
    private String salesId;
    private String shippingType;
    private String shippingLocation;
    private String shippingAddress;
    private String shippingRemark;
    private String carCode;
    private String procurementName;
    private String procurementMobileNo;
    private String leadTime;
    private String dueDate;
    private String salesOrderNo;

    private List<PurchaseOrderItemDocumentDto> items;
}
