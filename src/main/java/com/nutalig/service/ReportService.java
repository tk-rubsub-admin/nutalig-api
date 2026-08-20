package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.constant.ExportFileFormat;
import com.nutalig.constant.ReceiptType;
import com.nutalig.constant.TemplateLanguage;
import com.nutalig.dto.document.*;
import com.nutalig.utils.JasperReportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String DPK_LOGO = "report/img.png";
    private static final String NUTALIG_LOGO = "report/logo_nutalig.jpg";
    private static final String NUTALIG_STAMP = "report/stamp_nutalig.png";
    private static final String SIGNATURE = "report/signature.png";
    private static final String TERM_COND_TEMPLATE_TH = "report/termAndCondition_th.jrxml";
    private static final String TERM_COND_TEMPLATE_EN = "report/termAndCondition_en.jrxml";
    private static final String INVOICE_TEMPLATE = "report/invoice.jrxml";
    private static final String QUOTATION_TEMPLATE_TH = "report/quotation_th.jrxml";
    private static final String QUOTATION_TEMPLATE_EN = "report/quotation_en.jrxml";
    private static final String SALES_ORDER_TEMPLATE = "report/salesOrder.jrxml";
    private static final String PURCHASE_ORDER_TEMPLATE = "report/purchaseOrder.jrxml";
    private static final String DEPOSIT_RECEIPT_TEMPLATE = "report/depositReceipt.jrxml";
    private static final String DEPOSIT_RECEIPT_TAX_INVOICE_TEMPLATE = "report/depositReceiptTaxInvoice.jrxml";
    private static final String RECEIPT_TEMPLATE = "report/receipt.jrxml";
    private static final String RECEIPT_TAX_INVOICE_TEMPLATE = "report/receiptTaxInvoice.jrxml";

    private final ObjectMapper objectMapper;

    /* ======================= PUBLIC APIs ======================= */
    public Object getTermAndConditionDocument(TermAndConditionDocumentDto dto, ExportFileFormat format, TemplateLanguage language) throws Exception {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("salesName", dto.getSalesName());
        parameters.put("stamp", loadResource(NUTALIG_STAMP));
        parameters.put("signature", loadResource(SIGNATURE));
        parameters.put("bankName", dto.getBankName());
        parameters.put("branchName", dto.getBranchName());
        parameters.put("accountName", dto.getAccountName());
        parameters.put("accountNo", dto.getAccountNo());

        JasperPrint jasperPrint = buildJasperPrint(
                TemplateLanguage.EN.equals(language) ? TERM_COND_TEMPLATE_EN : TERM_COND_TEMPLATE_TH,
                parameters,
                new JREmptyDataSource(1)
        );

        if (format == ExportFileFormat.PDF) {
            return JasperReportUtil.exportJasperToPdf(jasperPrint);
        }

        if (format == ExportFileFormat.JPG) {
            return exportImages(jasperPrint);
        }

        return null;
    }

    public Object getQuotationDocument(QuotationDocumentDto dto, ExportFileFormat format, TemplateLanguage language) throws Exception {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("docNo", dto.getDocNo());
        parameters.put("docDate", dto.getDocDate());
        parameters.put("custName", dto.getCustName());
        parameters.put("custTaxId", dto.getCustTaxId());
        parameters.put("custAddress", dto.getCustAddress());
        parameters.put("custMobileNo", dto.getCustMobileNo());

        parameters.put("salesId", dto.getSalesId());
        parameters.put("salesName", dto.getSalesName());
        parameters.put("salesNickname", dto.getSalesNickname());
        parameters.put("salesMobileNo", dto.getSalesMobileNo());
        parameters.put("coSalesId", dto.getCoSalesId());

        parameters.put("subTotal", dto.getSubTotal());
        parameters.put("discount", dto.getDiscount());
        parameters.put("freight", dto.getFreight());
        parameters.put("vat", dto.getVat());
        parameters.put("grandTotal", dto.getGrandTotal());
        parameters.put("remark", dto.getRemark());
        parameters.put("thaiBahtText", dto.getThaiBahtText());
        parameters.put("logo", loadResource(NUTALIG_LOGO));
        parameters.put("shipping", dto.getShipping());

        parameters.put("bankName", dto.getBankName());
        parameters.put("accountName", dto.getAccountName());
        parameters.put("accountNo", dto.getAccountNo());
        parameters.put("branchName", dto.getBranchName());

        JasperPrint jasperPrint = buildJasperPrint(
                TemplateLanguage.EN.equals(language) ? QUOTATION_TEMPLATE_EN : QUOTATION_TEMPLATE_TH,
                parameters,
                new JRBeanCollectionDataSource(dto.getItems())
        );

        if (format == ExportFileFormat.PDF) {
            return JasperReportUtil.exportJasperToPdf(jasperPrint);
        }

        if (format == ExportFileFormat.JPG) {
            return exportImages(jasperPrint);
        }

        return null;
    }

    public Object getSalesOrderDocument(SalesOrderDocumentDto dto, ExportFileFormat format) throws Exception {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("docNo", dto.getDocNo());
        parameters.put("docDate", dto.getDocDate());
        parameters.put("custName", dto.getCustName());
        parameters.put("custTaxId", dto.getCustTaxId());
        parameters.put("custAddress", dto.getCustAddress());
        parameters.put("custMobileNo", dto.getCustMobileNo());
        parameters.put("quotationNo", dto.getQuotationNo());

        parameters.put("salesId", dto.getSalesId());
        parameters.put("salesName", dto.getSalesName());
        parameters.put("salesNickname", dto.getSalesNickname());
        parameters.put("salesMobileNo", dto.getSalesMobileNo());
        parameters.put("coSalesId", dto.getCoSalesId());

        parameters.put("subTotal", dto.getSubTotal());
        parameters.put("discount", dto.getDiscount());
        parameters.put("freight", dto.getFreight());
        parameters.put("vat", dto.getVat());
        parameters.put("grandTotal", dto.getGrandTotal());
        parameters.put("remark", dto.getRemark());
        parameters.put("thaiBahtText", dto.getThaiBahtText());
        parameters.put("logo", loadResource(NUTALIG_LOGO));
        parameters.put("shipping", dto.getShipping());

        parameters.put("bankName", dto.getBankName());
        parameters.put("accountName", dto.getAccountName());
        parameters.put("accountNo", dto.getAccountNo());
        parameters.put("branchName", dto.getBranchName());

        JasperPrint jasperPrint = buildJasperPrint(
                SALES_ORDER_TEMPLATE,
                parameters,
                new JRBeanCollectionDataSource(dto.getItems())
        );

        if (format == ExportFileFormat.PDF) {
            return JasperReportUtil.exportJasperToPdf(jasperPrint);
        }

        if (format == ExportFileFormat.JPG) {
            return exportImages(jasperPrint);
        }

        return null;
    }

    public Object getInvoiceDocument(InvoiceDocumentDto dto, ExportFileFormat format) throws Exception {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("docNo", dto.getDocNo());
        parameters.put("docDate", dto.getDocDate());
        parameters.put("custName", dto.getCustName());
        parameters.put("custTaxId", dto.getCustTaxId());
        parameters.put("custAddress", dto.getCustAddress());
        parameters.put("custMobileNo", dto.getCustMobileNo());
        parameters.put("dueDate", dto.getDueDate());
        parameters.put("salesOrderNo", dto.getSalesOrderNo());
        parameters.put("quotationNo", dto.getQuotationNo());

        parameters.put("salesId", dto.getSalesId());
        parameters.put("salesName", dto.getSalesName());
        parameters.put("salesNickname", dto.getSalesNickname());
        parameters.put("salesMobileNo", dto.getSalesMobileNo());
        parameters.put("coSalesId", dto.getCoSalesId());

        parameters.put("referenceNo", dto.getReferenceNo());
        parameters.put("amount", dto.getAmount());
        parameters.put("subTotal", dto.getSubTotal());
        parameters.put("discount", dto.getDiscount());
        parameters.put("freight", dto.getFreight());
        parameters.put("vat", dto.getVat());
        parameters.put("grandTotal", dto.getGrandTotal());
        parameters.put("remark", dto.getRemark());
        parameters.put("thaiBahtText", dto.getThaiBahtText());
        parameters.put("logo", loadResource(NUTALIG_LOGO));

        parameters.put("bankName", dto.getBankName());
        parameters.put("accountName", dto.getAccountName());
        parameters.put("accountNo", dto.getAccountNo());
        parameters.put("branchName", dto.getBranchName());

        JasperPrint jasperPrint = buildJasperPrint(
                INVOICE_TEMPLATE,
                parameters,
                new JRBeanCollectionDataSource(dto.getItems())
        );

        if (format == ExportFileFormat.PDF) {
            return JasperReportUtil.exportJasperToPdf(jasperPrint);
        }

        if (format == ExportFileFormat.JPG) {
            return exportImages(jasperPrint);
        }

        return null;
    }

    public Object getPurchaseOrderDocument(PurchaseOrderDocumentDto dto, ExportFileFormat format) throws Exception {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("logo", loadResource(NUTALIG_LOGO));
        parameters.put("supplierName", dto.getSupplierName());
        parameters.put("supplierAddress", dto.getSupplierAddress());
        parameters.put("docNo", dto.getDocNo());
        parameters.put("docDate", dto.getDocDate());
        parameters.put("salesName", dto.getSalesName());
        parameters.put("salesMobileNo", dto.getSalesMobileNo());
        parameters.put("accountName", dto.getAccountName());
        parameters.put("bankName", dto.getBankName());
        parameters.put("accountNo", dto.getAccountNo());
        parameters.put("remark", dto.getRemark());
        parameters.put("totalAmount", dto.getTotalAmount());
        parameters.put("discount", dto.getDiscount());
        parameters.put("freight", dto.getFreight());
        parameters.put("subTotal", dto.getSubTotal());
        parameters.put("vat", dto.getVat());
        parameters.put("grandTotal", dto.getGrandTotal());
        parameters.put("thaiBahtText", dto.getThaiBahtText());
        parameters.put("coSalesId", dto.getCoSalesId());
        parameters.put("salesId", dto.getSalesId());
        parameters.put("shippingType", dto.getShippingType());
        parameters.put("shippingLocation", dto.getShippingLocation());
        parameters.put("shippingAddress", dto.getShippingAddress());
        parameters.put("shippingRemark", dto.getShippingRemark());
        parameters.put("carCode", dto.getCarCode());
        parameters.put("procurementName", dto.getProcurementName());
        parameters.put("procurementMobileNo", dto.getProcurementMobileNo());
        parameters.put("leadTime", dto.getLeadTime());
        parameters.put("dueDate", dto.getDueDate());
        parameters.put("salesOrderNo", dto.getSalesOrderNo());

        JasperPrint jasperPrint = buildJasperPrint(
                PURCHASE_ORDER_TEMPLATE,
                parameters,
                new JRBeanCollectionDataSource(dto.getItems())
        );

        if (format == ExportFileFormat.PDF) {
            return JasperReportUtil.exportJasperToPdf(jasperPrint);
        }

        if (format == ExportFileFormat.JPG) {
            return exportImages(jasperPrint);
        }

        return null;
    }

    public Object getReceiptDocument(ReceiptDocumentDto dto, ExportFileFormat format) throws Exception {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("reportName", getReceiptName(dto.getReceiptType()));
        parameters.put("docNo", dto.getDocNo());
        parameters.put("docDate", dto.getDocDate());
        parameters.put("custName", dto.getCustName());
        parameters.put("custTaxId", dto.getCustTaxId());
        parameters.put("custAddress", dto.getCustAddress());

        parameters.put("salesId", dto.getSalesId());

        parameters.put("salesOrderNo", dto.getSalesOrderNo());
        parameters.put("invoiceNo", dto.getInvoiceNo());
        parameters.put("amount", dto.getAmount());
        parameters.put("subTotal", dto.getSubTotal());
        parameters.put("discount", dto.getDiscount());
        parameters.put("freight", dto.getFreight());
        parameters.put("vat", dto.getVat());
        parameters.put("grandTotal", dto.getGrandTotal());
        parameters.put("remark", dto.getRemark());
        parameters.put("thaiBahtText", dto.getThaiBahtText());
        parameters.put("logo", loadResource(NUTALIG_LOGO));

        parameters.put("bankName", dto.getBankName());
        parameters.put("accountName", dto.getAccountName());
        parameters.put("accountNo", dto.getAccountNo());

        parameters.put("paymentMethod", dto.getPaymentMethod().name());
        parameters.put("chequeNo", dto.getChequeNo());
        parameters.put("chequeBank", dto.getChequeBank());
        parameters.put("chequeBranch", dto.getChequeBranch());
        parameters.put("chequeDate", dto.getChequeDate());

        JasperPrint jasperPrint = buildJasperPrint(
                RECEIPT_TEMPLATE,
                parameters,
                new JRBeanCollectionDataSource(dto.getItems())
        );

        if (format == ExportFileFormat.PDF) {
            return JasperReportUtil.exportJasperToPdf(jasperPrint);
        }

        if (format == ExportFileFormat.JPG) {
            return exportImages(jasperPrint);
        }

        return null;
    }

    /* ======================= CORE METHODS ======================= */

    private JasperPrint buildJasperPrint(
            String templatePath,
            Map<String, Object> parameters,
            JRDataSource dataSource
    ) throws JRException {

        InputStream template = loadResource(templatePath);
        JasperDesign design = JRXmlLoader.load(template);
        JasperReport report = JasperCompileManager.compileReport(design);

        return JasperFillManager.fillReport(report, parameters, dataSource);
    }

    public byte[] exportPdf(JasperPrint jasperPrint) throws JRException {
        return JasperReportUtil.exportJasperToPdf(jasperPrint);
    }

    public List<byte[]> exportImages(JasperPrint jasperPrint) throws IOException, JRException {
        int pageCount = jasperPrint.getPages().size();
        List<byte[]> images = new ArrayList<>(pageCount);

        float zoom = 300f / 72f; // 300 DPI

        for (int i = 0; i < pageCount; i++) {
            Image image = JasperPrintManager.printPageToImage(jasperPrint, i, zoom);
            BufferedImage bufferedImage = toBufferedImage(image);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(bufferedImage, "jpg", baos);
                images.add(baos.toByteArray());
            }
        }
        return images;
    }

    private Object exportByFormat(JasperPrint jasperPrint, ExportFileFormat format)
            throws JRException, IOException {

        return switch (format) {
            case PDF -> JasperReportUtil.exportJasperToPdf(jasperPrint);
            case JPG -> exportImages(jasperPrint);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    /* ======================= UTILITIES ======================= */

    private InputStream loadResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    private BufferedImage toBufferedImage(Image image) {
        BufferedImage bufferedImage = new BufferedImage(
                image.getWidth(null),
                image.getHeight(null),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return bufferedImage;
    }


    private String getReceiptName(ReceiptType receiptType) {
        return switch (receiptType) {
            case RECEIPT -> "ใบเสร็จรับเงิน/ RECEIPT";
            case DEPOSIT_RECEIPT -> "ใบรับเงินมัดจำ/ DEPOSIT RECEIPT";
            case RECEIPT_TAX_INVOICE -> "ใบเสร็จรับเงิน/ใบกำกับภาษี / RECEIPT/ TAX INVOICE";
            case DEPOSIT_TAX_INVOICE -> "ใบรับเงินมัดจำ/ใบกำกับภาษี / DEPOSIT RECEIPT/ TAX INVOICE";
        };
    }
}
