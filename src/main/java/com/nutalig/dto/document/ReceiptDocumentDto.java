package com.nutalig.dto.document;

import com.nutalig.constant.PaymentMethod;
import com.nutalig.constant.ReceiptType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReceiptDocumentDto extends DefaultDocumentDto {

    private ReceiptType receiptType;
    private String salesOrderNo;
    private String invoiceNo;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal grandTotal;
    private BigDecimal freight;
    private BigDecimal subTotal;
    private BigDecimal vat;
    private String remark;
    private String thaiBahtText;
    private PaymentMethod paymentMethod;

    // Customer
    private String custName;
    private String custTaxId;
    private String custAddress;

    // Sales Account
    private String salesId;

    // Cheque
    private String chequeNo;
    private String chequeBank;
    private String chequeDate;
    private String chequeBranch;

    // Items
    private List<ReceiptItemDocumentDto> items;
}
