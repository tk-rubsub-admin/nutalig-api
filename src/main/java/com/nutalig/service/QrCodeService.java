package com.nutalig.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class QrCodeService {
    public InputStream generatePng(String content) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                320,
                320,
                Map.of(
                        EncodeHintType.CHARACTER_SET, "UTF-8",
                        EncodeHintType.MARGIN, 1,
                        EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M
                )
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);
        return new ByteArrayInputStream(output.toByteArray());
    }
}
