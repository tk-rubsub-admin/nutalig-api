package com.nutalig.service;
import com.nutalig.constant.PurchaseOrderDeliveryStatus;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderDeliveryTransitRequest;
import com.nutalig.entity.*;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@ExtendWith(MockitoExtension.class)
class PurchaseOrderContainerTransitGuardTest {
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock PurchaseOrderDeliveryRepository purchaseOrderDeliveryRepository;
    @Mock ShippingContainerLoadRepository shippingContainerLoadRepository;
    @Mock UserRepository userRepository;
    @Mock PurchaseOrderMilestoneService purchaseOrderMilestoneService;
    @InjectMocks PurchaseOrderService service;
    @Test void manualTransitLocksTheSourceAndRejectsContainerAllocatedDeliveries()throws Exception {
        var po=new PurchaseOrderEntity();po.setPurchaseOrderNo("PO");
        var role=new UserRoleEntity();role.setRoleCode("ADMIN");var user=new UserEntity();user.setUserRoleEntity(role);
        var delivery=new PurchaseOrderDeliveryEntity();delivery.setId(1L);delivery.setPurchaseOrder(po);delivery.setStatus(PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER);
        when(purchaseOrderRepository.findByIdForUpdate("PO")).thenReturn(Optional.of(po));when(userRepository.findById("USER")).thenReturn(Optional.of(user));
        when(purchaseOrderDeliveryRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(delivery));
        when(shippingContainerLoadRepository.existsByDelivery_IdAndActiveTrue(1L)).thenReturn(true);
        var request=new UpdatePurchaseOrderDeliveryTransitRequest();request.setTrackingNumber("TRACK");request.setInTransitDate(LocalDate.now());
        assertThrows(InvalidRequestException.class,()->service.markDeliveryInTransit("PO",1L,request,"USER"));
        assertEquals(PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER,delivery.getStatus());verify(purchaseOrderDeliveryRepository,never()).save(any());verifyNoInteractions(purchaseOrderMilestoneService);
    }
}
