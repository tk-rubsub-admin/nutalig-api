package com.nutalig.service;

import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import com.nutalig.constant.PurchaseOrderReceiptStatus;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderReceiptRequest;
import com.nutalig.entity.PurchaseOrderDeliveryDetailEntity;
import com.nutalig.entity.PurchaseOrderDeliveryEntity;
import com.nutalig.entity.PurchaseOrderEntity;
import com.nutalig.entity.PurchaseOrderReceiptEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.repository.PurchaseOrderDeliveryRepository;
import com.nutalig.repository.PurchaseOrderReceiptRepository;
import com.nutalig.repository.PurchaseOrderRepository;
import com.nutalig.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderReceivingServiceTest {
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private PurchaseOrderDeliveryRepository deliveryRepository;
    @Mock private PurchaseOrderReceiptRepository receiptRepository;
    @Mock private UserRepository userRepository;
    @Mock private GeneratedIdSequenceService generatedIdSequenceService;
    @Mock private PurchaseOrderStatusService purchaseOrderStatusService;
    @Mock private ActivityHistoryService activityHistoryService;
    @Mock private PurchaseOrderReceiptStockMovementService stockMovementService;

    @InjectMocks private PurchaseOrderReceivingService service;

    @Test
    void rejectsReceiptWhenDeliveryIsNotInTransit() {
        PurchaseOrderEntity po = purchaseOrder();
        PurchaseOrderDeliveryEntity delivery = delivery(po, PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER, 10);
        givenBase(po, delivery);

        assertThatThrownBy(() -> service.createReceipt("PO-1", 10L, request(1, 10), "USER-1"))
                .isInstanceOf(com.nutalig.exception.InvalidRequestException.class)
                .hasMessageContaining("in transit");
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void createsFullReceiptAndMovesDeliveryToWarehouseReceived() throws Exception {
        PurchaseOrderEntity po = purchaseOrder();
        PurchaseOrderDeliveryEntity delivery = delivery(po, PurchaseOrderDeliveryStatus.IN_TRANSIT, 10);
        givenBase(po, delivery);
        when(userRepository.findById("USER-1")).thenReturn(Optional.of(user()));
        when(generatedIdSequenceService.getNextIdWithMonth(anyString(), anyInt())).thenReturn("NTL-GR20261100001");
        AtomicReference<PurchaseOrderReceiptEntity> saved = new AtomicReference<>();
        when(receiptRepository.findByDelivery_IdOrderByCreatedDateDesc(10L))
                .thenAnswer(invocation -> saved.get() == null ? List.of() : List.of(saved.get()));
        when(receiptRepository.save(any())).thenAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return saved.get();
        });

        var result = service.createReceipt("PO-1", 10L, request(1, 10), "USER-1");

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderReceiptStatus.CONFIRMED);
        assertThat(result.getReceiptNo()).isEqualTo("NTL-GR20261100001");
        assertThat(delivery.getStatus()).isEqualTo(PurchaseOrderDeliveryStatus.RECEIVED_AT_WAREHOUSE);
        verify(stockMovementService).recordReceipt(any(PurchaseOrderReceiptEntity.class), eq("USER-1"));
        verify(activityHistoryService).record(any(), eq("PO-1"), eq("USER-1"), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsReceiptWhenQuantityExceedsRemainingQuantity() {
        PurchaseOrderEntity po = purchaseOrder();
        PurchaseOrderDeliveryEntity delivery = delivery(po, PurchaseOrderDeliveryStatus.IN_TRANSIT, 10);
        givenBase(po, delivery);
        when(userRepository.findById("USER-1")).thenReturn(Optional.of(user()));
        when(generatedIdSequenceService.getNextIdWithMonth(anyString(), anyInt())).thenReturn("NTL-GR20261100001");

        assertThatThrownBy(() -> service.createReceipt("PO-1", 10L, request(1, 11), "USER-1"))
                .isInstanceOf(com.nutalig.exception.InvalidRequestException.class)
                .hasMessageContaining("exceeds");
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void cancellationReversesStockAndRefreshesDelivery() throws Exception {
        PurchaseOrderEntity po = purchaseOrder();
        PurchaseOrderDeliveryEntity delivery = delivery(po, PurchaseOrderDeliveryStatus.RECEIVED_AT_WAREHOUSE, 10);
        PurchaseOrderReceiptEntity receipt = new PurchaseOrderReceiptEntity();
        receipt.setReceiptNo("NTL-GR20261100001");
        receipt.setPurchaseOrder(po);
        receipt.setDelivery(delivery);
        receipt.setStatus(PurchaseOrderReceiptStatus.CONFIRMED);
        when(receiptRepository.findById(receipt.getReceiptNo())).thenReturn(Optional.of(receipt));
        when(userRepository.findById("USER-1")).thenReturn(Optional.of(user()));
        when(receiptRepository.findByDelivery_IdOrderByCreatedDateDesc(10L)).thenReturn(List.of(receipt));

        var result = service.cancelReceipt(receipt.getReceiptNo(), "USER-1");

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderReceiptStatus.CANCELLED);
        verify(stockMovementService).reverseReceipt(receipt, "USER-1");
        verify(purchaseOrderStatusService).refresh(po);
    }

    private void givenBase(PurchaseOrderEntity po, PurchaseOrderDeliveryEntity delivery) {
        when(purchaseOrderRepository.findById("PO-1")).thenReturn(Optional.of(po));
        when(deliveryRepository.findById(10L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(delivery));
        lenient().when(receiptRepository.findByDelivery_IdOrderByCreatedDateDesc(10L)).thenReturn(List.of());
    }

    private PurchaseOrderEntity purchaseOrder() {
        PurchaseOrderEntity po = new PurchaseOrderEntity();
        po.setPurchaseOrderNo("PO-1");
        po.setSupplierNameSnapshot("Supplier");
        return po;
    }

    private PurchaseOrderDeliveryEntity delivery(PurchaseOrderEntity po, PurchaseOrderDeliveryStatus status,
                                                  int quantity) {
        PurchaseOrderDeliveryEntity delivery = new PurchaseOrderDeliveryEntity();
        delivery.setId(10L);
        delivery.setPurchaseOrder(po);
        delivery.setDeliveryNo("D-1");
        delivery.setStatus(status);
        PurchaseOrderDeliveryDetailEntity detail = new PurchaseOrderDeliveryDetailEntity();
        detail.setId(20L);
        detail.setPurchaseOrderDelivery(delivery);
        detail.setProductNameSnapshot("Item");
        detail.setQuantity(BigDecimal.valueOf(quantity));
        com.nutalig.entity.PurchaseOrderDetailEntity purchaseOrderDetail = new com.nutalig.entity.PurchaseOrderDetailEntity();
        purchaseOrderDetail.setId(30L);
        detail.setPurchaseOrderDetail(purchaseOrderDetail);
        delivery.setDetails(new ArrayList<>(List.of(detail)));
        return delivery;
    }

    private CreatePurchaseOrderReceiptRequest request(long warehouseId, int quantity) {
        CreatePurchaseOrderReceiptRequest request = new CreatePurchaseOrderReceiptRequest();
        request.setWarehouseId(warehouseId);
        CreatePurchaseOrderReceiptRequest.Detail detail = new CreatePurchaseOrderReceiptRequest.Detail();
        detail.setDeliveryDetailId(20L);
        detail.setReceivedQuantity(BigDecimal.valueOf(quantity));
        detail.setWarehouseLocationId(99L);
        request.setDetails(List.of(detail));
        return request;
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId("USER-1");
        return user;
    }
}
