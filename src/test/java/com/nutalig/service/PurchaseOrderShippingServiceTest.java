package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.constant.*;
import com.nutalig.constant.Currency;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderRequest;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.entity.*;
import com.nutalig.entity.id.SystemConfigId;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.mapper.SystemConfigMapper;
import com.nutalig.mapper.UserMapper;
import com.nutalig.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderShippingServiceTest {
    @Mock private SalesOrderRepository salesOrderRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private SupplierShippingRepository supplierShippingRepository;
    @Mock private UserRepository userRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private RequestPriceDetailRepository requestPriceDetailRepository;
    @Mock private GeneratedIdSequenceService generatedIdSequenceService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private PurchaseOrderPaymentService purchaseOrderPaymentService;
    @Mock private PurchaseOrderPaymentScheduleService purchaseOrderPaymentScheduleService;
    @Mock private PurchaseOrderCbmService purchaseOrderCbmService;
    @Mock private PurchaseOrderMilestoneService purchaseOrderMilestoneService;
    @Mock private ActivityHistoryService activityHistoryService;
    @Mock private SalesOrderItemShippingService salesOrderItemShippingService;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private UserMapper userMapper;
    @Spy private SupplierMapper supplierMapper = Mappers.getMapper(SupplierMapper.class);
    @Spy private ReportService reportService = new ReportService(new ObjectMapper());
    @InjectMocks private PurchaseOrderService service;

    private SupplierShippingEntity shipping;
    private CreatePurchaseOrderRequest request;
    private SalesOrderEntity salesOrder;

    @BeforeEach
    void setUp() {
        salesOrder = new SalesOrderEntity();
        salesOrder.setSalesOrderNo("SO-TEST");
        salesOrder.setProcurementStatus(ProcurementStatus.READY_FOR_PO);
        SupplierEntity supplier = new SupplierEntity();
        supplier.setId("supplier");
        shipping = new SupplierShippingEntity();
        shipping.setId(1L);
        shipping.setShippingMethod(ShippingMethod.SEA);
        shipping.setShippingMode(ShippingMode.FCL);
        shipping.setCarCode("TZ001");
        when(salesOrderRepository.findById("SO-TEST")).thenReturn(Optional.of(salesOrder));
        when(supplierRepository.findById("supplier")).thenReturn(Optional.of(supplier));
        when(supplierShippingRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(shipping));

        request = new CreatePurchaseOrderRequest();
        request.setSalesOrderNo("SO-TEST");
        request.setSupplierId("supplier");
        request.setSupplierShippingId(1L);
        request.setPaymentTerm("DEP_30");
        CreatePurchaseOrderRequest.Item item = new CreatePurchaseOrderRequest.Item();
        item.setName("Test product");
        item.setQuantity(BigDecimal.ONE);
        item.setSupplierUnitPrice(BigDecimal.TEN);
        item.setSupplierCurrency(Currency.THB);
        request.setItems(List.of(item));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SEA_FCL_20GP", "SEA_FCL_40HQ", "SEA_SHARE_FCL_20GP", "SEA_SHARE_FCL_40HQ"})
    void createsClosedContainerPoWithTzSnapshot(String method) throws Exception {
        prepareSuccessfulCreate();
        request.setShippingMethodSnapshot(method);

        PurchaseOrderEntity created = service.createPurchaseOrder(request, List.of(), "user");

        assertEquals("TZ001", created.getCarCodeSnapshot());
        assertEquals(method, created.getShippingMethodSnapshot());
        assertEquals(PurchaseOrderStatus.AWAITING_PAYMENT, created.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SEA", "LAND", "SEA_FCL_UNKNOWN", "AIR"})
    void rejectsIncompatibleMethodBeforeSaving(String method) {
        request.setShippingMethodSnapshot(method);
        assertThrows(InvalidRequestException.class, () -> service.createPurchaseOrder(request, List.of(), "user"));
        verifyNoInteractions(purchaseOrderRepository, userRepository);
    }

    @Test
    void rejectsTbShippingForClosedContainers() {
        shipping.setShippingMode(ShippingMode.STANDARD);
        shipping.setCarCode("TB001");
        request.setShippingMethodSnapshot("SEA_FCL_20GP");
        assertThrows(InvalidRequestException.class, () -> service.createPurchaseOrder(request, List.of(), "user"));
        verifyNoInteractions(purchaseOrderRepository);
    }

    @Test
    void omittedMethodStillSupportsLegacyStandardRequests() throws Exception {
        prepareSuccessfulCreate();
        shipping.setShippingMode(ShippingMode.STANDARD);
        shipping.setCarCode("TB001");

        PurchaseOrderEntity created = service.createPurchaseOrder(request, List.of(), "user");

        assertEquals("SEA", created.getShippingMethodSnapshot());
        assertEquals("TB001", created.getCarCodeSnapshot());
    }

    @Test
    void masterChangesDoNotAlterDetailOrOriginalAndCopyPdfCodes() throws Exception {
        prepareSuccessfulCreate();
        request.setShippingMethodSnapshot("SEA_FCL_20GP");
        PurchaseOrderEntity created = service.createPurchaseOrder(request, List.of(), "user");
        when(purchaseOrderRepository.findById(created.getPurchaseOrderNo())).thenReturn(Optional.of(created));
        shipping.setCarCode("TZ999");

        assertEquals("TZ001", service.getPurchaseOrderById(created.getPurchaseOrderNo()).getCarCodeSnapshot());
        DownloadDocumentDto document = service.getPurchaseOrderDocumentById(created.getPurchaseOrderNo(),
                new DocumentRequest(ExportFileFormat.PDF, true, true));
        try (PDDocument pdf = PDDocument.load(Base64.getDecoder().decode(document.getFiles().getFirst().getBase64()))) {
            assertTrue(pdf.getNumberOfPages() >= 2);
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.split("TZ001", -1).length - 1 >= 2, "Both original and copy should retain the snapshot code");
            assertFalse(text.contains("TZ999"));
        }
    }

    private void prepareSuccessfulCreate() {
        when(userRepository.findById("user")).thenReturn(Optional.of(new UserEntity()));
        when(generatedIdSequenceService.getNextIdWithMonth(anyString(), eq(4))).thenReturn("PO-TEST");
        when(purchaseOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SystemConfigEntity paymentTerm = new SystemConfigEntity();
        SystemConfigId id = new SystemConfigId();
        id.setGroupCode(SystemConstant.SUPPLIER_PAYMENT_TERM);
        id.setCode("DEP_30");
        paymentTerm.setId(id);
        when(systemConfigService.getConfigEntity(SystemConstant.SUPPLIER_PAYMENT_TERM, "DEP_30")).thenReturn(paymentTerm);
    }

    @Test
    void legacyRequestWithoutSelectedIdsStillCreatesAllMatchingSourceItems() throws Exception {
        prepareSuccessfulCreate();
        request.setShippingMethodSnapshot("SEA_FCL_40HQ");
        request.setItems(null);
        SupplierEntity supplier = supplierRepository.findById("supplier").orElseThrow();
        for (long id : List.of(1L, 2L)) {
            SalesOrderDetailEntity item = new SalesOrderDetailEntity();
            item.setId(id);
            item.setSupplier(supplier);
            item.setShippingMethod(id == 1 ? "SEA" : "SEA_FCL_40HQ");
            item.setQuantity(BigDecimal.TEN);
            item.setSupplierCurrency(Currency.THB);
            item.setSupplierUnitPrice(BigDecimal.ONE);
            item.setSupplierTotalUnitCost(BigDecimal.ONE);
            salesOrder.addItem(item);
        }
        assertEquals(2, service.createPurchaseOrder(request, List.of(), "user").getItems().size());
        verifyNoInteractions(salesOrderItemShippingService);
    }

    @Test
    void emptyExplicitSelectionCreatesOnlyManualItemsWithoutReaddingSalesOrderItems() throws Exception {
        prepareSuccessfulCreate();
        request.setShippingMethodSnapshot("SEA_FCL_40HQ");
        request.setSalesOrderDetailIds(List.of());
        SalesOrderDetailEntity excluded = new SalesOrderDetailEntity();
        excluded.setId(1L);
        excluded.setName("Removed SO item");
        excluded.setSupplier(supplierRepository.findById("supplier").orElseThrow());
        excluded.setShippingMethod("SEA");
        salesOrder.addItem(excluded);

        PurchaseOrderEntity created = service.createPurchaseOrder(request, List.of(), "user");
        assertEquals(1, created.getItems().size());
        assertEquals("Test product", created.getItems().iterator().next().getName());
        assertEquals(new BigDecimal("10.00"), created.getSubTotal());
        verifyNoInteractions(salesOrderItemShippingService);
        assertEquals(1, salesOrder.getItems().size());
    }

    @Test
    void explicitNormalSupplierSelectionStoresTheChosenTransportForEveryItem() throws Exception {
        prepareSuccessfulCreate();
        request.setShippingMethodSnapshot("SEA_SHARE_FCL_40HQ");
        request.setSalesOrderDetailIds(List.of(1L, 2L));
        request.setItems(null);
        SupplierEntity supplier = supplierRepository.findById("supplier").orElseThrow();
        for (long id : List.of(1L, 2L)) {
            SalesOrderDetailEntity item = new SalesOrderDetailEntity();
            item.setId(id);
            item.setSupplier(supplier);
            item.setName("Normal item " + id);
            item.setShippingMethod(id == 1 ? "LAND" : "SEA");
            item.setQuantity(BigDecimal.TEN);
            item.setSupplierCurrency(Currency.THB);
            item.setSupplierUnitPrice(BigDecimal.ONE);
            item.setSupplierShippingCost(BigDecimal.ZERO);
            item.setSupplierTotalUnitCost(BigDecimal.ONE);
            salesOrder.addItem(item);
        }
        when(salesOrderItemShippingService.selectItems(salesOrder.getItems(), List.of(1L, 2L), "supplier", "SEA_SHARE_FCL_40HQ"))
                .thenCallRealMethod();
        PurchaseOrderEntity created = service.createPurchaseOrder(request, List.of(), "user");
        assertEquals(2, created.getItems().size());
        assertEquals("SEA_SHARE_FCL_40HQ", created.getShippingMethodSnapshot());
        created.getItems().forEach(item -> {
            assertEquals("SEA_SHARE_FCL_40HQ", item.getShippingMethod());
            assertNull(item.getRfqTierSplitId());
        });
        assertEquals(List.of("LAND", "SEA"), salesOrder.getItems().stream().map(SalesOrderDetailEntity::getShippingMethod).toList());
    }

    @Test
    void explicitSelectionCreatesOnlyItsSplitAndKeepsMetadataWhenLegacyEditOmitsSplitId() throws Exception {
        prepareSuccessfulCreate();
        request.setShippingMethodSnapshot("SEA_SHARE_FCL_40HQ");
        request.setSalesOrderDetailIds(List.of(183L));
        SalesOrderDetailEntity source = new SalesOrderDetailEntity();
        source.setId(183L);
        source.setName("Split SEA");
        source.setQuantity(new BigDecimal("25000"));
        source.setSupplierCurrency(Currency.THB);
        source.setSupplierUnitPrice(new BigDecimal("6.25"));
        source.setSupplierShippingCost(new BigDecimal("0.75"));
        source.setSupplierTotalUnitCost(new BigDecimal("7.00"));
        source.setRfqDetailId(689L);
        source.setRfqTierSplitId(10L);
        source.setShippingMethod("SEA");
        when(salesOrderItemShippingService.selectItems(salesOrder.getItems(), List.of(183L), "supplier", "SEA_SHARE_FCL_40HQ"))
                .thenReturn(new SalesOrderItemShippingService.Selection(List.of(source), java.util.Map.of(183L, "SEA_SHARE_FCL_40HQ")));
        CreatePurchaseOrderRequest.Item requested = new CreatePurchaseOrderRequest.Item();
        requested.setSalesOrderDetailId(183L);
        request.setItems(List.of(requested));

        PurchaseOrderEntity created = service.createPurchaseOrder(request, List.of(), "user");
        assertEquals(1, created.getItems().size());
        PurchaseOrderDetailEntity detail = created.getItems().iterator().next();
        assertEquals(10L, detail.getRfqTierSplitId());
        assertEquals("SEA_SHARE_FCL_40HQ", detail.getShippingMethod());
        assertEquals(new BigDecimal("175000.00"), created.getSubTotal());
        assertNull(detail.getSupplierQuoteTierId());
        detail.setId(1L);
        when(purchaseOrderRepository.findById(created.getPurchaseOrderNo())).thenReturn(Optional.of(created));
        com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderDetailRequest edit = new com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderDetailRequest();
        edit.setId(1L);
        edit.setName(detail.getName());
        edit.setQuantity(detail.getQuantity());
        edit.setSupplierCurrency(Currency.THB);
        edit.setSupplierUnitPrice(detail.getSupplierUnitPrice());
        edit.setSupplierShippingCost(detail.getSupplierShippingCost());
        edit.setRfqDetailId(689L);
        edit.setShippingMethod(detail.getShippingMethod());
        com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderRequest update = new com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderRequest();
        update.setItems(List.of(edit));
        assertEquals(10L, service.updatePurchaseOrder(created.getPurchaseOrderNo(), update, "user").getItems().getFirst().getRfqTierSplitId());
    }
}
