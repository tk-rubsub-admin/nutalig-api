package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PurchaseOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseOrderDto {
    private String purchaseOrderNo;
    private String salesOrderNo;
    private String docDate;
    private Integer productionLeadTimeDay;
    private Integer shippingLeadTimeDay;
    private PurchaseOrderStatus status;
    private Currency currency;
    private BigDecimal exchangeRate;
    private SupplierDto supplier;
    private SupplierShippingDto supplierShipping;
    private BigDecimal subTotal;
    private BigDecimal subTotalThb;
    private BigDecimal grandTotal;
    private BigDecimal grandTotalThb;
    private String remark;
    private Integer revNo;
    private String supplierNameSnapshot;
    private String supplierAddressSnapshot;
    private String supplierContactSnapshot;
    private String supplierPhoneSnapshot;
    private UserDto createdBy;
    private UserDto updatedBy;
    private List<PurchaseOrderAttachmentDto> attachments;
    private List<PurchaseOrderDetailDto> items;
}
