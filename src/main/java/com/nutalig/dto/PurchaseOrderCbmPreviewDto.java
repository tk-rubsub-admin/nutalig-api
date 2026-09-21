package com.nutalig.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseOrderCbmPreviewDto {
    private BigDecimal totalCbm = BigDecimal.ZERO;
    private Integer unavailableItemCount = 0;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long salesOrderDetailId;
        private Long supplierQuoteTierId;
        private BigDecimal quantity;
        private BigDecimal totalCbm;
        private Boolean available;
        private String reason;
        private List<PurchaseOrderPackageSnapshotDto> packages = new ArrayList<>();
    }
}
