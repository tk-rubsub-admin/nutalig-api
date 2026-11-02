package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderDeliveryRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.entity.*;
import com.nutalig.repository.PurchaseOrderDeliveryRepository;
import com.nutalig.utils.DateUtil;
import jakarta.persistence.Entity;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderDeliverySearchIntegrationTest {
    @Test void searchesRealDeliveriesWithInclusiveDatesCombinedFiltersEscapedKeywordAndPagination() throws Exception {
        Configuration config=new Configuration();
        var scanner=new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for(var candidate:scanner.findCandidateComponents("com.nutalig.entity")) config.addAnnotatedClass(Class.forName(candidate.getBeanClassName()));
        config.setProperty("hibernate.connection.url","jdbc:h2:mem:delivery_search_"+UUID.randomUUID()+";MODE=MySQL;NON_KEYWORDS=USER,VALUE,KEY");
        config.setProperty("hibernate.connection.driver_class","org.h2.Driver");
        config.setProperty("hibernate.hbm2ddl.auto","create-drop");
        try(var factory=config.buildSessionFactory();var session=factory.openSession()) {
            var tx=session.beginTransaction();
            SupplierEntity supplier=new SupplierEntity(); supplier.setSupplierName("Factory One"); session.persist(supplier);
            SalesOrderEntity so=new SalesOrderEntity();so.setSalesOrderNo("SO-A");session.persist(so);
            PurchaseOrderEntity po=new PurchaseOrderEntity();po.setPurchaseOrderNo("PO-A");po.setSalesOrder(so);po.setSupplier(supplier);po.setSupplierNameSnapshot("Factory One");po.setDocDate(LocalDate.of(2026,10,1));po.setStatus(PurchaseOrderStatus.PRODUCTION_RUNNING);session.persist(po);
            var shipping=new SupplierShippingEntity();shipping.setShippingName("Carrier");shipping.setShippingMethod(ShippingMethod.SEA);session.persist(shipping);
            var location=new SupplierShippingDestinationEntity();location.setDestinationName("Warehouse");shipping.addDestination(location);session.persist(location);
            for(int i=0;i<3;i++) {
                PurchaseOrderDeliveryEntity delivery=new PurchaseOrderDeliveryEntity();delivery.setPurchaseOrder(po);delivery.setDeliveryNo("PO-A-D00"+(i+1));
                delivery.setShippedAt(LocalDate.of(2026,10,24+i).atTime(23,59,59,999000000).atZone(DateUtil.getTimeZone()));
                delivery.setExpectedWarehouseDate(LocalDate.of(2026,10,28+i)); delivery.setShippingMethod(i==1?"LAND":"SEA_FCL_20GP");
                delivery.setStatus(i==2?PurchaseOrderDeliveryStatus.CANCELLED:PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER);
                if(i==2)delivery.setContainerStatus(DeliveryContainerStatus.ASSIGNED);
                if(i==0)delivery.setShippingLocation(location);
                delivery.setTrackingNumber(i==0?"TRACK_100%":"TRACKX100Y"); session.persist(delivery);
            }
            session.flush();session.clear();
            var repository=new JpaRepositoryFactory(session).getRepository(PurchaseOrderDeliveryRepository.class);
            var service=new PurchaseOrderDeliverySearchService(repository);
            var request=new SearchPurchaseOrderDeliveryRequest();request.setPurchaseOrderNo("PO-A");request.setSalesOrderNo("SO-A");request.setSupplierName("factory");request.setSupplierId(supplier.getId());request.setShippingMethod("SEA");request.setStatus(PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER);
            request.setShippedDateStart(LocalDate.of(2026,10,24));request.setShippedDateEnd(LocalDate.of(2026,10,24));
            var result=service.search(request,new PageableRequest().setPage(1).setSize(10));
            assertEquals(1L,result.getPagination().getTotalRecords());assertEquals("PO-A-D001",result.getRecords().getFirst().getDeliveryNo());
            request=new SearchPurchaseOrderDeliveryRequest();request.setKeyword("_100%");
            assertEquals(1L,service.search(request,null).getPagination().getTotalRecords());
            request=new SearchPurchaseOrderDeliveryRequest();request.setExpectedWarehouseDateStart(LocalDate.of(2026,10,29));request.setExpectedWarehouseDateEnd(LocalDate.of(2026,10,29));
            assertEquals("PO-A-D002",service.search(request,null).getRecords().getFirst().getDeliveryNo());
            result=service.search(null,new PageableRequest().setPage(2).setSize(1));
            assertEquals(3L,result.getPagination().getTotalRecords());assertEquals("PO-A-D002",result.getRecords().getFirst().getDeliveryNo());
            request=new SearchPurchaseOrderDeliveryRequest();request.setContainerStatus(DeliveryContainerStatus.NOT_ASSIGNED);
            result=service.search(request,null);
            assertEquals(1L,result.getPagination().getTotalRecords());
            assertEquals("PO-A-D001",result.getRecords().getFirst().getDeliveryNo());
            request.setContainerStatus(DeliveryContainerStatus.ASSIGNED);
            result=service.search(request,null);
            assertEquals(1L,result.getPagination().getTotalRecords());
            assertEquals("PO-A-D003",result.getRecords().getFirst().getDeliveryNo());
            request.setContainerStatus(DeliveryContainerStatus.PARTIALLY_ASSIGNED);
            assertEquals(0L,service.search(request,null).getPagination().getTotalRecords());
            var assigned=repository.findAll().stream().filter(d->"PO-A-D003".equals(d.getDeliveryNo())).findFirst().orElseThrow();
            assigned.setContainerStatus(DeliveryContainerStatus.PARTIALLY_ASSIGNED);session.flush();
            result=service.search(request,null);
            assertEquals(1L,result.getPagination().getTotalRecords());
            assertEquals("PO-A-D003",result.getRecords().getFirst().getDeliveryNo());
            request.setStatus(PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER);
            assertEquals(0L,service.search(request,null).getPagination().getTotalRecords());
            request=new SearchPurchaseOrderDeliveryRequest();request.setShippingLocationId(location.getId());
            result=service.search(request,null);
            assertEquals(1L,result.getPagination().getTotalRecords());
            assertEquals("PO-A-D001",result.getRecords().getFirst().getDeliveryNo());
            request.setContainerStatus(DeliveryContainerStatus.PARTIALLY_ASSIGNED);
            assertEquals(0L,service.search(request,null).getPagination().getTotalRecords());
            request.setContainerStatus(null);request.setShippingLocationId(Long.MAX_VALUE);
            assertEquals(0L,service.search(request,null).getPagination().getTotalRecords());
            tx.rollback();
        }
    }
}
