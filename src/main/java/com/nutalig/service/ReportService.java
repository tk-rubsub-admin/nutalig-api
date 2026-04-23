package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.constant.ExportFileFormat;
import com.nutalig.constant.SystemConstant;
import com.nutalig.dto.SystemConfigDto;
import com.nutalig.dto.document.QuotationDocumentDto;
import com.nutalig.utils.JasperReportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
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
    private static final String SIGNATURE = "report/signature.jpg";
    private static final String INVOICE_TEMPLATE = "report/invoice.jrxml";
    private static final String QUOTATION_TEMPLATE = "report/quotation.jrxml";

    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    /* ======================= PUBLIC APIs ======================= */
    public Object getQuotationDocument(QuotationDocumentDto dto, ExportFileFormat format) throws Exception {
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

        parameters.put("bankName", dto.getBankName());
        parameters.put("accountName", dto.getAccountName());
        parameters.put("accountNo", dto.getAccountNo());

        JasperPrint jasperPrint = buildJasperPrint(
                QUOTATION_TEMPLATE,
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
            JRBeanCollectionDataSource dataSource
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
}
