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
    private DocumentStatusProfileDto statusProfile;
    private Currency currency;
    private SupplierDto supplier;
    private SupplierShippingDto supplierShipping;
    private SystemConfigDto paymentTerm;
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
    private String supplierContactNoSnapshot;
    private String shippingMethodSnapshot;
    private String containerSizeSnapshot;
    private UserDto createdBy;
    private UserDto updatedBy;
    private List<PurchaseOrderAttachmentDto> attachments;
    private List<PurchaseOrderDetailDto> items;
}
