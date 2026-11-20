package com.nutalig.service;

import com.nutalig.constant.PurchaseOrderReceiptStockMovementType;
import com.nutalig.entity.PurchaseOrderDeliveryDetailEntity;
import com.nutalig.entity.PurchaseOrderDetailEntity;
import com.nutalig.entity.PurchaseOrderReceiptDetailEntity;
import com.nutalig.entity.PurchaseOrderReceiptEntity;
import com.nutalig.entity.PurchaseOrderReceiptStockMovementEntity;
import com.nutalig.repository.PurchaseOrderReceiptStockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderReceiptStockMovementServiceTest {
    @Mock
    private PurchaseOrderReceiptStockMovementRepository movementRepository;

    @InjectMocks
    private PurchaseOrderReceiptStockMovementService service;

    @Test
    void recordsOnlyAcceptedQuantityAsReceiptMovement() {
        PurchaseOrderReceiptEntity receipt = receiptWithDetail(new BigDecimal("7"));

        service.recordReceipt(receipt, "USER-1");

        ArgumentCaptor<PurchaseOrderReceiptStockMovementEntity> captor =
                ArgumentCaptor.forClass(PurchaseOrderReceiptStockMovementEntity.class);
        verify(movementRepository).save(captor.capture());
        PurchaseOrderReceiptStockMovementEntity movement = captor.getValue();
        assertThat(movement.getMovementType()).isEqualTo(PurchaseOrderReceiptStockMovementType.RECEIPT);
        assertThat(movement.getQuantity()).isEqualByComparingTo("7");
        assertThat(movement.getCreatedBy()).isEqualTo("USER-1");
    }

    @Test
    void createsReverseMovementWhenReceiptIsCancelled() {
        PurchaseOrderReceiptEntity receipt = receiptWithDetail(new BigDecimal("3"));
        PurchaseOrderReceiptStockMovementEntity original = new PurchaseOrderReceiptStockMovementEntity();
        original.setReceipt(receipt);
        original.setReceiptDetail(receipt.getDetails().get(0));
        original.setDeliveryDetailId(11L);
        original.setWarehouseId(2L);
        original.setPurchaseOrderDetailId(5L);
        original.setQuantity(new BigDecimal("3"));
        original.setMovementType(PurchaseOrderReceiptStockMovementType.RECEIPT);
        original.setId(99L);
        when(movementRepository.findByReceipt_ReceiptNoAndMovementType(
                receipt.getReceiptNo(), PurchaseOrderReceiptStockMovementType.RECEIPT))
                .thenReturn(List.of(original));
        when(movementRepository.existsByReverseOf_Id(99L)).thenReturn(false);

        service.reverseReceipt(receipt, "USER-1");

        ArgumentCaptor<PurchaseOrderReceiptStockMovementEntity> captor =
                ArgumentCaptor.forClass(PurchaseOrderReceiptStockMovementEntity.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType())
                .isEqualTo(PurchaseOrderReceiptStockMovementType.REVERSE_RECEIPT);
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("-3");
        assertThat(captor.getValue().getReverseOf()).isSameAs(original);
    }

    private PurchaseOrderReceiptEntity receiptWithDetail(BigDecimal quantity) {
        PurchaseOrderReceiptEntity receipt = new PurchaseOrderReceiptEntity();
        receipt.setReceiptNo("NTL-GR20261100001");
        receipt.setWarehouseId(2L);
        PurchaseOrderDeliveryDetailEntity deliveryDetail = new PurchaseOrderDeliveryDetailEntity();
        deliveryDetail.setId(11L);
        deliveryDetail.setProductNameSnapshot("Test item");
        PurchaseOrderDetailEntity purchaseOrderDetail = new PurchaseOrderDetailEntity();
        purchaseOrderDetail.setId(5L);
        deliveryDetail.setPurchaseOrderDetail(purchaseOrderDetail);
        PurchaseOrderReceiptDetailEntity detail = new PurchaseOrderReceiptDetailEntity();
        detail.setReceipt(receipt);
        detail.setDeliveryDetail(deliveryDetail);
        detail.setReceivedQuantity(quantity);
        detail.setWarehouseLocationId(3L);
        receipt.addDetail(detail);
        return receipt;
    }
}
