package com.nutalig.service;

import com.nutalig.controller.purchaseorder.request.PurchaseOrderCbmPreviewRequest;
import com.nutalig.entity.*;
import com.nutalig.repository.RfqSupplierQuoteTierRepository;
import com.nutalig.repository.SalesOrderRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderCbmServiceTest {
    @Mock SalesOrderRepository salesOrderRepository;
    @Mock RfqSupplierQuoteTierRepository supplierQuoteTierRepository;
    @InjectMocks PurchaseOrderCbmService service;

    @ParameterizedTest
    @CsvSource({"500,3,0.054834", "3000,21,0.383838", "280,2,0.036556", "139,0,0.000000"})
    void previewAndNewPoSnapshotsRoundCartonsDown(String quantity, long cartons, String totalCbm) throws Exception {
        RfqSupplierQuoteEntity quote = new RfqSupplierQuoteEntity();
        RfqSupplierQuotePackageEntity sourcePackage = new RfqSupplierQuotePackageEntity();
        sourcePackage.setId(754L);
        sourcePackage.setPackageName("ขวด");
        sourcePackage.setPackageDimension("37 x 26 x 19 cm.");
        sourcePackage.setPackageCapacity("140 pcs.");
        quote.addPackage(sourcePackage);
        RfqSupplierQuoteDetailEntity quoteDetail = new RfqSupplierQuoteDetailEntity();
        quoteDetail.setSupplierQuote(quote);
        RfqSupplierQuoteTierEntity tier = new RfqSupplierQuoteTierEntity();
        tier.setId(1454L);
        tier.setQuoteDetail(quoteDetail);
        SalesOrderEntity so = new SalesOrderEntity();
        SalesOrderDetailEntity soDetail = new SalesOrderDetailEntity();
        soDetail.setId(105L);
        soDetail.setSupplierQuoteTierId(1454L);
        soDetail.setQuantity(new BigDecimal("500"));
        so.addItem(soDetail);
        when(salesOrderRepository.findById("SO-1")).thenReturn(Optional.of(so));
        when(supplierQuoteTierRepository.findAllByIdIn(any())).thenReturn(List.of(tier));
        when(supplierQuoteTierRepository.findById(1454L)).thenReturn(Optional.of(tier));
        PurchaseOrderCbmPreviewRequest request = new PurchaseOrderCbmPreviewRequest();
        request.setSalesOrderNo("SO-1");
        PurchaseOrderCbmPreviewRequest.Item requestedItem = new PurchaseOrderCbmPreviewRequest.Item();
        requestedItem.setSalesOrderDetailId(105L);
        requestedItem.setQuantity(new BigDecimal(quantity));
        request.setItems(List.of(requestedItem));

        var preview = service.preview(request);
        assertEquals(cartons, preview.getItems().getFirst().getPackages().getFirst().getCartonCount());
        assertEquals(new BigDecimal(totalCbm), preview.getTotalCbm());
        assertEquals(true, preview.getItems().getFirst().getAvailable());

        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setSupplierQuoteTierId(1454L);
        detail.setQuantity(new BigDecimal(quantity));
        service.snapshotFromSupplierQuote(detail);
        assertEquals(cartons, detail.getPackages().getFirst().getCartonCount());
        assertEquals(new BigDecimal(totalCbm), detail.getPackages().getFirst().getTotalCbm());
    }

    @ParameterizedTest
    @CsvSource({"500,3,0.054834", "3000,21,0.383838", "280,2,0.036556", "139,0,0.000000"})
    void recalculatesExistingPoPackagesWithRoundingDown(String quantity, long cartons, String totalCbm) {
        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
        detail.setSupplierQuoteTierId(1454L);
        detail.setQuantity(new BigDecimal(quantity));
        PurchaseOrderDetailPackageEntity snapshot = new PurchaseOrderDetailPackageEntity();
        snapshot.setWidthCm(new BigDecimal("37"));
        snapshot.setLengthCm(new BigDecimal("26"));
        snapshot.setHeightCm(new BigDecimal("19"));
        snapshot.setCapacityQty(new BigDecimal("140"));
        snapshot.setSelectedForCalculation(true);
        snapshot.setCartonCount(22L);
        snapshot.setTotalCbm(new BigDecimal("0.402116"));
        detail.addPackage(snapshot);
        PurchaseOrderEntity po = new PurchaseOrderEntity();
        po.addItem(detail);

        service.recalculateTotal(po);

        assertEquals(cartons, snapshot.getCartonCount());
        assertEquals(new BigDecimal("0.018278"), snapshot.getCbmPerCarton());
        assertEquals(new BigDecimal(totalCbm), snapshot.getTotalCbm());
        assertEquals(new BigDecimal(totalCbm), po.getTotalCbm());
        verifyNoInteractions(salesOrderRepository, supplierQuoteTierRepository);
    }
}
