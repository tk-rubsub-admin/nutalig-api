package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.controller.purchaseorder.request.CompletePurchaseOrderProductionRequest;
import com.nutalig.controller.purchaseorder.request.CompletePurchaseOrderProductionRequest.Detail;
import com.nutalig.controller.purchaseorder.request.CompletePurchaseOrderProductionRequest.ScrapBox;
import com.nutalig.dto.SupplierShippingDto;
import com.nutalig.entity.*;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.*;
import com.nutalig.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderScrapBoxServiceTest {
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock PurchaseOrderDeliveryRepository purchaseOrderDeliveryRepository;
    @Mock UserRepository userRepository;
    @Mock RequestPriceDetailRepository requestPriceDetailRepository;
    @Mock SupplierMapper supplierMapper;
    @Mock SystemConfigMapper systemConfigMapper;
    @Mock UserMapper userMapper;
    @Mock PurchaseOrderMilestoneService purchaseOrderMilestoneService;
    @Mock PurchaseOrderCbmService purchaseOrderCbmService;
    @Mock ActivityHistoryService activityHistoryService;
    @Captor ArgumentCaptor<Map<String, Object>> historyDetail;
    @InjectMocks PurchaseOrderService service;
    PurchaseOrderEntity po;
    CompletePurchaseOrderProductionRequest request;
    List<PurchaseOrderDeliveryEntity> saved;

    @BeforeEach void setup() {
        po = new PurchaseOrderEntity(); po.setPurchaseOrderNo("PO-SCRAP");
        po.setStatus(PurchaseOrderStatus.PRODUCTION_RUNNING); po.setShippingMethodSnapshot("LAND");
        SupplierShippingEntity shipping = new SupplierShippingEntity(); shipping.setShippingMethod(ShippingMethod.LAND);
        SupplierShippingDestinationEntity destination = new SupplierShippingDestinationEntity();
        destination.setId(1L); destination.setActive(true); destination.setDestinationName("Warehouse");
        shipping.addDestination(destination); po.setSupplierShipping(shipping);
        PurchaseOrderDetailEntity product = new PurchaseOrderDetailEntity();
        product.setId(10L); product.setName("Bottle"); product.setQuantity(new BigDecimal("100"));
        for (long id : new long[]{11,12}) {
            PurchaseOrderDetailComponentEntity component = new PurchaseOrderDetailComponentEntity();
            component.setId(id); component.setComponentName("Component " + id); component.setQuantityPerItem(BigDecimal.ONE);
            product.addComponent(component);
        }
        po.addItem(product);
        UserEntity user = new UserEntity(); UserRoleEntity role = new UserRoleEntity(); role.setRoleCode("ADMIN"); user.setUserRoleEntity(role);
        when(userRepository.findById("USER")).thenReturn(Optional.of(user));
        when(purchaseOrderRepository.findByIdForUpdate("PO-SCRAP")).thenReturn(Optional.of(po));
        saved = new ArrayList<>();
        when(purchaseOrderDeliveryRepository.findAllByPurchaseOrder_PurchaseOrderNo("PO-SCRAP")).thenAnswer(i -> new ArrayList<>(saved));
        lenient().when(purchaseOrderDeliveryRepository.save(any())).thenAnswer(i -> {
            PurchaseOrderDeliveryEntity delivery = i.getArgument(0); delivery.setId(1L);
            delivery.getScrapBoxes().forEach(box -> box.setId(20L)); saved.add(delivery); return delivery;
        });
        lenient().when(supplierMapper.toDto(shipping)).thenReturn(new SupplierShippingDto());
        lenient().when(supplierMapper.toDto(destination)).thenReturn(new com.nutalig.dto.SupplierShippingDestinationDto());
        request = new CompletePurchaseOrderProductionRequest();
        var delivery = new CompletePurchaseOrderProductionRequest.PurchaseOrderDelivery();
        delivery.setShippedDate(LocalDate.of(2026,10,24)); delivery.setExpectedWarehouseDate(LocalDate.of(2026,10,28));
        delivery.setShippingMethod("LAND"); delivery.setShippingLocationId(1L);
        ScrapBox box = new ScrapBox(); box.setLengthCm(new BigDecimal("40")); box.setWidthCm(new BigDecimal("30"));
        box.setHeightCm(new BigDecimal("20")); box.setNetWeightKg(new BigDecimal("5"));
        delivery.setScrapBoxes(List.of(box)); request.setDelivery(delivery);
    }
    Detail detail(long component, int quantity, Integer boxIndex) {
        Detail d = new Detail(); d.setPurchaseOrderDetailId(10L); d.setPurchaseOrderDetailComponentId(component);
        d.setQuantity(BigDecimal.valueOf(quantity)); d.setScrapBoxIndex(boxIndex); return d;
    }
    @Test void savesAndReturnsOneSharedBoxForTwoComponents() throws Exception {
        request.getDelivery().setDetails(List.of(detail(11,10,0), detail(12,20,0)));
        var result = service.completeProduction("PO-SCRAP", request, "USER");
        var delivery = saved.getFirst();
        assertEquals(1, delivery.getScrapBoxes().size());
        assertEquals(new BigDecimal("0.024000"), delivery.getScrapBoxes().getFirst().getCbmM3());
        assertSame(delivery.getDetails().get(0).getScrapBox(), delivery.getDetails().get(1).getScrapBox());
        assertNull(delivery.getDetails().getFirst().getCbmM3());
        assertNull(delivery.getDetails().getFirst().getNetWeightKg());
        assertEquals(20L, result.getDeliveries().getFirst().getDetails().getFirst().getScrapBoxId());
        assertEquals(1, result.getDeliveries().getFirst().getScrapBoxes().size());
        verifyNoInteractions(purchaseOrderMilestoneService);
        verify(activityHistoryService).record(eq(ActivityEntityType.PURCHASE_ORDER), eq("PO-SCRAP"),
                eq("USER"), eq(ActivityActorType.USER), eq(ActivityAction.UPDATE), eq(ActivitySource.API),
                contains("ยังส่งไม่ครบ"), historyDetail.capture());
        Map<String, Object> history = historyDetail.getValue();
        assertEquals(false, history.get("allQuantitiesDelivered"));
        assertEquals(delivery.getDeliveryNo(), history.get("deliveryNo"));
        assertEquals("2026-10-24", history.get("shippedDate"));
        assertEquals(2, ((List<?>) history.get("details")).size());
        List<?> historyBoxes = (List<?>) history.get("scrapBoxes");
        assertEquals(1, historyBoxes.size());
        assertEquals(new BigDecimal("0.024000"), ((Map<?, ?>) historyBoxes.getFirst()).get("cbmM3"));
    }
    @Test void combinesNormalAndScrapQuantitiesForCompletion() throws Exception {
        request.getDelivery().setDetails(List.of(detail(11,90,null), detail(11,10,0), detail(12,100,0)));
        service.completeProduction("PO-SCRAP", request, "USER");
        verify(purchaseOrderMilestoneService).completeMilestone(eq(po), any(), isNull(), isNull(), eq("USER"));
        verify(activityHistoryService).record(eq(ActivityEntityType.PURCHASE_ORDER), eq("PO-SCRAP"),
                eq("USER"), eq(ActivityActorType.USER), eq(ActivityAction.UPDATE), eq(ActivitySource.API),
                contains("ส่งครบแล้ว"), historyDetail.capture());
        assertEquals(true, historyDetail.getValue().get("allQuantitiesDelivered"));
    }
    @Test void rejectsCombinedQuantityAboveRemaining() {
        request.getDelivery().setDetails(List.of(detail(11,95,null), detail(11,10,0)));
        assertThrows(InvalidRequestException.class, () -> service.completeProduction("PO-SCRAP", request, "USER"));
        verify(purchaseOrderDeliveryRepository, never()).save(any());
        verifyNoInteractions(activityHistoryService);
    }
    @Test void rejectsInvalidBoxReferenceOrForeignComponent() {
        request.getDelivery().setDetails(List.of(detail(11,1,5)));
        assertThrows(InvalidRequestException.class, () -> service.completeProduction("PO-SCRAP", request, "USER"));
        request.getDelivery().setDetails(List.of(detail(999,1,0)));
        assertThrows(InvalidRequestException.class, () -> service.completeProduction("PO-SCRAP", request, "USER"));
        verify(purchaseOrderDeliveryRepository, never()).save(any());
        verifyNoInteractions(activityHistoryService);
    }
    @Test void rejectsBoxesWithNoContents() {
        request.getDelivery().setDetails(List.of(detail(11,1,null)));
        assertThrows(InvalidRequestException.class, () -> service.completeProduction("PO-SCRAP", request, "USER"));
        verify(purchaseOrderDeliveryRepository, never()).save(any());
        verifyNoInteractions(activityHistoryService);
    }
    @Test void recordsNormalPartialDeliveryWithoutCompletingProduction() throws Exception {
        request.setNote(" บางส่วน ");
        request.getDelivery().setScrapBoxes(List.of());
        Detail normal = detail(11, 10, null);
        normal.setCartonCount(BigDecimal.ONE);
        normal.setCbmM3(new BigDecimal("0.12"));
        request.getDelivery().setDetails(List.of(normal));
        service.completeProduction("PO-SCRAP", request, "USER");
        verify(activityHistoryService).record(eq(ActivityEntityType.PURCHASE_ORDER), eq("PO-SCRAP"),
                eq("USER"), eq(ActivityActorType.USER), eq(ActivityAction.UPDATE), eq(ActivitySource.API),
                contains("ยังส่งไม่ครบ"), historyDetail.capture());
        assertEquals("บางส่วน", historyDetail.getValue().get("note"));
        assertTrue(((List<?>) historyDetail.getValue().get("scrapBoxes")).isEmpty());
        Map<?, ?> item = (Map<?, ?>) ((List<?>) historyDetail.getValue().get("details")).getFirst();
        assertEquals(BigDecimal.ONE, item.get("cartonCount"));
        assertEquals(new BigDecimal("0.12"), item.get("cbmM3"));
        verifyNoInteractions(purchaseOrderMilestoneService);
    }

}
