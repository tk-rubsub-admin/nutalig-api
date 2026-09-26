package com.nutalig.dto;

import com.nutalig.constant.Currency;
import com.nutalig.constant.PurchaseOrderStatus;
import com.nutalig.constant.PurchaseOrderPaymentLifecycleStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

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
    private PurchaseOrderPaymentLifecycleStatus paymentStatus;
    private BigDecimal paidTotal;
    private BigDecimal paidTotalThb;
    private BigDecimal outstandingTotal;
    private BigDecimal outstandingTotalThb;
    private BigDecimal totalCbm;
    private String remark;
    private String lateStartReason;
    private LocalDate productionStartedDate;
    private LocalDate productionExpectedEndDate;
    private LocalDate productionCompletedDate;
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
