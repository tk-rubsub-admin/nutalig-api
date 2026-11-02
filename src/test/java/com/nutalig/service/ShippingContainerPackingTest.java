package com.nutalig.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.controller.container.ShippingContainerRequest;
import com.nutalig.entity.*;
import com.nutalig.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ShippingContainerPackingTest {
    PurchaseOrderDeliveryEntity source() {
        var po=new PurchaseOrderEntity();po.setPurchaseOrderNo("PO-1");var product=new PurchaseOrderDetailEntity();product.setId(1L);
        var delivery=new PurchaseOrderDeliveryEntity();delivery.setId(1L);delivery.setDeliveryNo("D1");delivery.setPurchaseOrder(po);
        var item=new PurchaseOrderDeliveryDetailEntity();item.setId(10L);item.setPurchaseOrderDetail(product);item.setProductNameSnapshot("Bottle");item.setQuantity(new BigDecimal("30"));item.setCartonCount(new BigDecimal("3"));item.setPiecesPerCarton(BigDecimal.TEN);
        item.setNetWeightKg(BigDecimal.ONE);item.setGrossWeightKg(new BigDecimal("2"));item.setLengthCm(BigDecimal.TEN);item.setWidthCm(BigDecimal.TEN);item.setHeightCm(BigDecimal.TEN);item.setCbmM3(new BigDecimal("0.003"));delivery.addDetail(item);return delivery;
    }
    ShippingContainerRequest.Load request(int cartons) { var r=new ShippingContainerRequest.Load();r.setDeliveryId(1L);r.setDeliveryDetailId(10L);r.setCartonCount(BigDecimal.valueOf(cartons));return r; }
    @Test void partialBoxesRequireUniformPackagingAndTheLastAllocationKeepsExactTotals() throws Exception {
        var delivery=source();var first=request(1);
        assertThrows(InvalidRequestException.class,()->ShippingContainerPacking.allocate(delivery,first,List.of(),new ObjectMapper()));
        first.setConfirmUniformPackaging(true);List<ShippingContainerLoadEntity> allocated=new ArrayList<>();
        for(int i=0;i<3;i++)allocated.add(ShippingContainerPacking.allocate(delivery,first,allocated,new ObjectMapper()));
        assertEquals(0,BigDecimal.ONE.compareTo(allocated.stream().map(ShippingContainerLoadEntity::getNetWeightKg).reduce(BigDecimal.ZERO,BigDecimal::add)));
        assertEquals(new BigDecimal("10"),allocated.getFirst().getQuantity());
        assertThrows(InvalidRequestException.class,()->ShippingContainerPacking.allocate(delivery,first,allocated,new ObjectMapper()));
    }
    @Test void incompletePerCartonDataDisablesSplittingButAllowsWholeSourceForDraftReview() throws Exception {
        var delivery=source();delivery.getDetails().getFirst().setNetWeightKg(null);var part=request(1);part.setConfirmUniformPackaging(true);
        assertFalse(ShippingContainerPacking.canSplit(delivery.getDetails().getFirst()));
        assertThrows(InvalidRequestException.class,()->ShippingContainerPacking.allocate(delivery,part,List.of(),new ObjectMapper()));
        var full=ShippingContainerPacking.allocate(delivery,request(3),List.of(),new ObjectMapper());
        assertThrows(InvalidRequestException.class,()->ShippingContainerPacking.requireCompleteMetrics(full));
    }
    @Test void scrapBoxCannotBeSplitOrAllocatedTwiceAndIncludesAllComponentQuantities() throws Exception {
        var delivery=source();var box=new PurchaseOrderDeliveryScrapBoxEntity();box.setId(20L);delivery.addScrapBox(box);
        for(int i=0;i<2;i++){var item=new PurchaseOrderDeliveryDetailEntity();item.setId(30L+i);item.setPurchaseOrderDetail(delivery.getDetails().getFirst().getPurchaseOrderDetail());item.setScrapBox(box);item.setQuantity(BigDecimal.valueOf(i+1));delivery.addDetail(item);}
        var r=new ShippingContainerRequest.Load();r.setScrapBoxId(20L);r.setCartonCount(BigDecimal.ONE);
        var load=ShippingContainerPacking.allocate(delivery,r,List.of(),new ObjectMapper());assertEquals(new BigDecimal("3"),load.getQuantity());assertEquals(20L,load.getActiveScrapBoxId());
        assertThrows(InvalidRequestException.class,()->ShippingContainerPacking.allocate(delivery,r,List.of(load),new ObjectMapper()));
        r.setCartonCount(new BigDecimal("2"));assertThrows(InvalidRequestException.class,()->ShippingContainerPacking.allocate(delivery,r,List.of(),new ObjectMapper()));
        r.setCartonCount(BigDecimal.ONE);r.setScrapBoxId(null);r.setDeliveryDetailId(30L);assertThrows(InvalidRequestException.class,()->ShippingContainerPacking.allocate(delivery,r,List.of(),new ObjectMapper()));
    }
}
