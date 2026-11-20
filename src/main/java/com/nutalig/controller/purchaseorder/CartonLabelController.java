package com.nutalig.controller.purchaseorder;

import com.nutalig.constant.ExportFileFormat;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.CartonLabelScanDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.CartonLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/purchase-orders")
public class CartonLabelController {
    private final CartonLabelService cartonLabelService;

    @GetMapping("/{purchaseOrderNo}/details/{detailId}/packages/{packageId}/carton-label")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_VIEW')")
    public ResponseEntity<DownloadDocumentDto> generateLabel(
            @PathVariable String purchaseOrderNo,
            @PathVariable Long detailId,
            @PathVariable Long packageId,
            @RequestParam(required = false) Long componentId,
            @RequestParam(defaultValue = "PDF") ExportFileFormat format
    ) throws Exception {
        return ResponseEntity.ok(cartonLabelService.generateLabel(
                purchaseOrderNo, detailId, packageId, componentId, format));
    }

    @GetMapping("/carton-label/scan")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_VIEW')")
    public GeneralResponse<CartonLabelScanDto> scan(@RequestParam String token)
            throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, cartonLabelService.scan(token));
    }
}
