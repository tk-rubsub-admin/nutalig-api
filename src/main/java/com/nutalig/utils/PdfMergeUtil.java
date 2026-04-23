package com.nutalig.utils;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class PdfMergeUtil {

    public static byte[] merge(List<byte[]> pdfBytesList) throws Exception {
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        merger.setDestinationStream(outputStream);

        for (byte[] pdf : pdfBytesList) {
            merger.addSource(new ByteArrayInputStream(pdf));
        }

        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

        return outputStream.toByteArray();
    }
}
