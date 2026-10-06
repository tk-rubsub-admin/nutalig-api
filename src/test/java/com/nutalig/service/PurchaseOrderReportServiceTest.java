package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.constant.ExportFileFormat;
import com.nutalig.dto.document.PurchaseOrderDocumentDto;
import com.nutalig.dto.document.PurchaseOrderItemDocumentDto;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderReportServiceTest {
    private static final int IMAGE_WIDTH = 24;
    private static final int IMAGE_HEIGHT = 16;
    private final ReportService service = new ReportService(new ObjectMapper());

    @ParameterizedTest(name = "{0}")
    @MethodSource("images")
    void exportsPdfWithReadableImagesAndOmitsUnreadableImages(String name, byte[] bytes, boolean hasImage)
            throws Exception {
        TrackingInputStream image = bytes == null ? null : new TrackingInputStream(bytes);
        PurchaseOrderDocumentDto dto = document(image);

        byte[] pdf = (byte[]) service.getPurchaseOrderDocument(dto, ExportFileFormat.PDF);

        try (PDDocument result = PDDocument.load(pdf)) {
            assertTrue(result.getNumberOfPages() > 0);
            String text = new PDFTextStripper().getText(result);
            assertTrue(text.contains("PO-IMAGE-TEST"));
            assertTrue(text.contains("Test product"));
            assertEquals(hasImage, containsProductImage(result));
        }
        if (image != null) {
            assertTrue(image.closed, "Source image stream should be closed");
        }
    }

    @Test
    void unreadableStreamDoesNotPreventPdfExport() throws Exception {
        InputStream image = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Image source cannot be read");
            }
        };

        byte[] pdf = (byte[]) service.getPurchaseOrderDocument(document(image), ExportFileFormat.PDF);

        try (PDDocument result = PDDocument.load(pdf)) {
            assertFalse(containsProductImage(result));
            assertTrue(new PDFTextStripper().getText(result).contains("Test product"));
        }
    }

    @Test
    void webpImageAlsoExportsAsJpg() throws Exception {
        TrackingInputStream image = new TrackingInputStream(webp("lossless"));

        Object exported = service.getPurchaseOrderDocument(document(image), ExportFileFormat.JPG);

        List<?> pages = assertInstanceOf(List.class, exported);
        assertFalse(pages.isEmpty());
        for (Object page : pages) {
            assertNotNull(ImageIO.read(new ByteArrayInputStream((byte[]) page)));
        }
        assertTrue(image.closed);
    }

    private static Stream<Arguments> images() throws IOException {
        return Stream.of(
                Arguments.of("PNG", raster("png"), true),
                Arguments.of("JPEG", raster("jpg"), true),
                Arguments.of("lossless WebP", webp("lossless"), true),
                Arguments.of("lossy WebP", webp("lossy"), true),
                Arguments.of("HTML instead of image", "<html>Not Found</html>".getBytes(StandardCharsets.UTF_8), false),
                Arguments.of("PDF instead of image", "%PDF-1.7\nNot an image".getBytes(StandardCharsets.UTF_8), false),
                Arguments.of("empty file", new byte[0], false),
                Arguments.of("truncated PNG", new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}, false),
                Arguments.of("no image", null, false)
        );
    }

    private static byte[] raster(String format) throws IOException {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFF0000);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output));
        return output.toByteArray();
    }

    private static byte[] webp(String variant) throws IOException {
        try (InputStream resource = PurchaseOrderReportServiceTest.class.getResourceAsStream(
                "/report/product-" + variant + ".webp")) {
            assertNotNull(resource);
            return resource.readAllBytes();
        }
    }

    private static boolean containsProductImage(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            for (COSName name : page.getResources().getXObjectNames()) {
                if (page.getResources().getXObject(name) instanceof PDImageXObject image
                        && image.getWidth() == IMAGE_WIDTH && image.getHeight() == IMAGE_HEIGHT) {
                    return true;
                }
            }
        }
        return false;
    }

    private static PurchaseOrderDocumentDto document(InputStream image) {
        PurchaseOrderItemDocumentDto item = new PurchaseOrderItemDocumentDto();
        item.setImage(image);
        item.setNo(1);
        item.setName("Test product");
        item.setPrice(BigDecimal.TEN);
        item.setQuantity(BigDecimal.ONE);
        item.setAmount(BigDecimal.TEN);

        PurchaseOrderDocumentDto dto = new PurchaseOrderDocumentDto();
        dto.setDocNo("PO-IMAGE-TEST");
        dto.setDocDate("06/10/2026");
        dto.setSupplierName("Test supplier");
        dto.setItems(List.of(item));
        dto.setTotalAmount(BigDecimal.TEN);
        dto.setDiscount(BigDecimal.ZERO);
        dto.setFreight(BigDecimal.ZERO);
        dto.setSubTotal(BigDecimal.TEN);
        dto.setVat(BigDecimal.ZERO);
        dto.setGrandTotal(BigDecimal.TEN);
        dto.setDepositAmount(BigDecimal.ZERO);
        return dto;
    }

    private static class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
