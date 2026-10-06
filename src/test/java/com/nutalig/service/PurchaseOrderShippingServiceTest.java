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
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private UserMapper userMapper;
    @Spy private SupplierMapper supplierMapper = Mappers.getMapper(SupplierMapper.class);
    @Spy private ReportService reportService = new ReportService(new ObjectMapper());
    @InjectMocks private PurchaseOrderService service;

    private SupplierShippingEntity shipping;
    private CreatePurchaseOrderRequest request;

    @BeforeEach
    void setUp() {
        SalesOrderEntity salesOrder = new SalesOrderEntity();
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
}
