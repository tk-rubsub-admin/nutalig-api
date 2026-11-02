package com.nutalig.controller.purchaseorder;

import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderDeliveryRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.dto.PurchaseOrderDeliverySearchDto;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.PurchaseOrderDeliverySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.nutalig.constant.ResponseStatus.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/purchase-orders/deliveries")
public class PurchaseOrderDeliveryController {
    private final PurchaseOrderDeliverySearchService service;

    @PostMapping("/search")
    @PreAuthorize("hasAnyAuthority('PERM_PURCHASE_ORDER_VIEW','PERM_CONTAINER_VIEW')")
    public GeneralResponse<Pageable<PurchaseOrderDeliverySearchDto>> search(
            @RequestBody(required = false) SearchPurchaseOrderDeliveryRequest request,
            PageableRequest pageable
    ) throws InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, service.search(request, pageable));
    }
}
