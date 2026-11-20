package com.nutalig.controller.purchaseorder;

import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.PurchaseOrderReceiptDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.PurchaseOrderReceivingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/purchase-order-receipts")
public class PurchaseOrderReceiptController {
    private final PurchaseOrderReceivingService purchaseOrderReceivingService;

    @GetMapping("/{receiptNo}")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_RECEIVE_VIEW')")
    public GeneralResponse<PurchaseOrderReceiptDto> get(@PathVariable String receiptNo)
            throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderReceivingService.getReceipt(receiptNo));
    }

    @PatchMapping("/{receiptNo}/cancel")
    @PreAuthorize("hasAuthority('PERM_PURCHASE_ORDER_RECEIVE_CANCEL')")
    public GeneralResponse<PurchaseOrderReceiptDto> cancel(
            @PathVariable String receiptNo,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,
                purchaseOrderReceivingService.cancelReceipt(receiptNo, userId));
    }
}
