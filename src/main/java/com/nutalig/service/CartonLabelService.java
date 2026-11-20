package com.nutalig.service;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.dto.CartonLabelScanDto;
import com.nutalig.dto.document.CartonLabelDocumentDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.PurchaseOrderDetailComponentEntity;
import com.nutalig.entity.PurchaseOrderDetailEntity;
import com.nutalig.entity.PurchaseOrderDetailPackageEntity;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartonLabelService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final QrCodeService qrCodeService;
    private final ReportService reportService;

    @Value("${app.frontend.base-url:https://app.nutalig.com}")
    private String frontendBaseUrl;

    @Value("${app.carton-label.token-secret:change-this-carton-label-secret}")
    private String tokenSecret;

    @Transactional(readOnly = true)
    public DownloadDocumentDto generateLabel(
            String purchaseOrderNo,
            Long detailId,
            Long packageId,
            Long componentId,
            ExportFileFormat format
    ) throws Exception {
        CartonLabelScanDto scan = resolveReference(purchaseOrderNo, detailId, packageId, componentId);
        String token = createToken(scan);
        String qrUrl = buildQrUrl(token);

        CartonLabelDocumentDto document = new CartonLabelDocumentDto();
        document.setPurchaseNo(scan.getPurchaseOrderNo());
        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        document.setSalesOrderNo(purchaseOrder.getSalesOrder().getSalesOrderNo());
        document.setProductName(scan.getProductName());
        document.setSpec(scan.getSpec());
        document.setComponent(scan.getComponentName());
        document.setDimension(scan.getPackageDimension());
        document.setWeight(scan.getPackageWeight());
        document.setQrCode(qrCodeService.generatePng(qrUrl));

        Object exported = reportService.getCartonLabelDocument(document, format);
        String baseName = purchaseOrderNo + "-detail-" + detailId + "-package-" + packageId + "-carton-label";
        if (format == ExportFileFormat.PDF) {
            return new DownloadDocumentDto(baseName, format, List.of(new DownloadDocumentDto.FileItem(
                    baseName + ".pdf",
                    Base64.getEncoder().encodeToString((byte[]) exported),
                    "application/pdf")));
        }

        @SuppressWarnings("unchecked")
        List<byte[]> pages = (List<byte[]>) exported;
        List<DownloadDocumentDto.FileItem> files = new ArrayList<>();
        for (int index = 0; index < pages.size(); index++) {
            files.add(new DownloadDocumentDto.FileItem(
                    baseName + "-page-" + (index + 1) + ".jpg",
                    Base64.getEncoder().encodeToString(pages.get(index)),
                    "image/jpeg"));
        }
        return new DownloadDocumentDto(baseName, format, files);
    }

    @Transactional(readOnly = true)
    public CartonLabelScanDto scan(String token) throws InvalidRequestException, DataNotFoundException {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2) throw new InvalidRequestException("Invalid carton label token.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] expectedSignature = sign(payload);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!java.security.MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new InvalidRequestException("Invalid carton label token signature.");
            }
            String[] values = payload.split("\\|", -1);
            if (values.length != 5 || !"v1".equals(values[0])) {
                throw new InvalidRequestException("Invalid carton label token payload.");
            }
            return resolveReference(
                    values[1],
                    Long.valueOf(values[2]),
                    Long.valueOf(values[3]),
                    "0".equals(values[4]) ? null : Long.valueOf(values[4])
            );
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new InvalidRequestException("Invalid carton label token.");
        }
    }

    private CartonLabelScanDto resolveReference(
            String purchaseOrderNo, Long detailId, Long packageId, Long componentId
    ) throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        PurchaseOrderDetailEntity detail = purchaseOrder.getItems().stream()
                .filter(item -> Objects.equals(item.getId(), detailId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Purchase order detail " + detailId + " not found."));
        PurchaseOrderDetailPackageEntity packageEntity = detail.getPackages().stream()
                .filter(item -> Objects.equals(item.getId(), packageId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Purchase order detail package " + packageId + " not found."));

        PurchaseOrderDetailComponentEntity component = null;
        if (componentId != null) {
            component = detail.getComponents().stream()
                    .filter(item -> Objects.equals(item.getId(), componentId))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException("Component does not belong to purchase order detail."));
        }

        CartonLabelScanDto dto = new CartonLabelScanDto();
        dto.setPurchaseOrderNo(purchaseOrderNo);
        dto.setPurchaseOrderDetailId(detailId);
        dto.setPurchaseOrderDetailPackageId(packageId);
        dto.setComponentId(componentId);
        dto.setProductName(detail.getName());
        dto.setSpec(detail.getSpec());
        dto.setPackageDimension(packageEntity.getPackageDimension());
        dto.setPackageWeight(packageEntity.getPackageWeight());
        dto.setPackageCapacity(packageEntity.getPackageCapacity());
        if (component != null) {
            dto.setComponentCode(component.getComponentCode());
            dto.setComponentName(component.getComponentName());
            dto.setComponentSpecification(component.getSpecification());
        } else if (detail.getComponents() != null) {
            dto.setComponentName(detail.getComponents().stream()
                    .map(PurchaseOrderDetailComponentEntity::getComponentName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", ")));
        }
        return dto;
    }

    private String createToken(CartonLabelScanDto scan) throws GeneralSecurityException {
        String payload = String.join("|", "v1", scan.getPurchaseOrderNo(),
                String.valueOf(scan.getPurchaseOrderDetailId()),
                String.valueOf(scan.getPurchaseOrderDetailPackageId()),
                scan.getComponentId() == null ? "0" : String.valueOf(scan.getComponentId()));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
    }

    private byte[] sign(String payload) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String buildQrUrl(String token) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/purchase-order-receive/scan")
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
