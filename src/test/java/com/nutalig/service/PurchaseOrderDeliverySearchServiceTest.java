package com.nutalig.service;

import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderDeliveryRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.entity.*;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.PurchaseOrderDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderDeliverySearchServiceTest {
    @Mock PurchaseOrderDeliveryRepository repository;
    @InjectMocks PurchaseOrderDeliverySearchService service;

    private PurchaseOrderDeliveryEntity delivery() {
        PurchaseOrderEntity po = new PurchaseOrderEntity(); po.setPurchaseOrderNo("PO-1"); po.setSupplierNameSnapshot("Factory");
        SalesOrderEntity so = new SalesOrderEntity(); so.setSalesOrderNo("SO-1"); po.setSalesOrder(so);
        PurchaseOrderDetailEntity product = new PurchaseOrderDetailEntity(); product.setId(10L);
        PurchaseOrderDeliveryEntity delivery = new PurchaseOrderDeliveryEntity(); delivery.setId(1L); delivery.setDeliveryNo("PO-1-D001"); delivery.setPurchaseOrder(po);
        PurchaseOrderDeliveryScrapBoxEntity box = new PurchaseOrderDeliveryScrapBoxEntity(); box.setId(99L); box.setCbmM3(new BigDecimal("0.024")); delivery.addScrapBox(box);
        for (int i=0;i<3;i++) {
            PurchaseOrderDeliveryDetailEntity line = new PurchaseOrderDeliveryDetailEntity(); line.setPurchaseOrderDetail(product);
            line.setQuantity(BigDecimal.TEN);
            if(i==0) { line.setCartonCount(new BigDecimal("2")); line.setCbmM3(new BigDecimal("0.12")); }
            else line.setScrapBox(box);
            delivery.addDetail(line);
        }
        return delivery;
    }
    @Test void returnsPaginationAndCountsSharedScrapBoxOnce() throws Exception {
        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenAnswer(call ->
                new PageImpl<>(List.of(delivery()), call.getArgument(1), 48));
        var result = service.search(null, new PageableRequest().setPage(2).setSize(10));
        assertEquals(2, result.getPagination().getPage()); assertEquals(5, result.getPagination().getTotalPage());
        assertEquals(48L, result.getPagination().getTotalRecords());
        var dto=result.getRecords().getFirst();
        assertEquals("SO-1",dto.getSalesOrderNo()); assertEquals("Factory",dto.getSupplierName());
        assertEquals(new BigDecimal("30"), dto.getTotalQuantity());
        assertEquals(new BigDecimal("3"), dto.getTotalCartons());
        assertEquals(new BigDecimal("0.144"),dto.getTotalCbm());
        assertEquals(3,dto.getDetails().size()); assertEquals(1,dto.getScrapBoxes().size());
        assertEquals(99L,dto.getDetails().get(1).getScrapBoxId());
    }
    @Test void unknownBoxMetricsAreNotReportedAsZeroAndDefaultPaginationIsStable() throws Exception {
        var delivery=delivery(); delivery.getDetails().getFirst().setCartonCount(null); delivery.getScrapBoxes().getFirst().setCbmM3(null);
        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class))).thenAnswer(call -> {
            org.springframework.data.domain.Pageable page=call.getArgument(1);
            assertEquals(0,page.getPageNumber()); assertEquals(10,page.getPageSize());
            assertEquals(Sort.Direction.DESC,page.getSort().getOrderFor("shippedAt").getDirection());
            assertNotNull(page.getSort().getOrderFor("id"));
            return new PageImpl<>(List.of(delivery),page,1);
        });
        var dto=service.search(null,null).getRecords().getFirst();
        assertNull(dto.getTotalCartons()); assertNull(dto.getTotalCbm());
    }
    @Test void invalidDateRangePaginationAndSortFailBeforeQuery() {
        var criteria=new SearchPurchaseOrderDeliveryRequest(); criteria.setShippedDateStart(LocalDate.of(2026,10,25)); criteria.setShippedDateEnd(LocalDate.of(2026,10,24));
        assertThrows(InvalidRequestException.class,()->service.search(criteria,null));
        assertThrows(InvalidRequestException.class,()->service.search(null,new PageableRequest().setPage(0)));
        assertThrows(InvalidRequestException.class,()->service.search(null,new PageableRequest().setSize(101)));
        assertThrows(InvalidRequestException.class,()->service.search(null,new PageableRequest().setSortBy("purchaseOrder.items")));
        verifyNoInteractions(repository);
    }
}
