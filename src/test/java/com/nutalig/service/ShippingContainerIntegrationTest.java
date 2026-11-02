package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.constant.*;
import com.nutalig.controller.container.ShippingContainerRequest;
import com.nutalig.dto.ShippingContainerDto;
import com.nutalig.entity.*;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.ShippingContainerMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import jakarta.persistence.Entity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShippingContainerIntegrationTest {
    static SessionFactory factory;
    record Fixture(Long deliveryId,Long normalId,Long scrapId,Long shippingId,Long destinationId,String suffix) {}
    interface Work<T>{T run(Session session,ShippingContainerService service)throws Exception;}
    final ActivityHistoryService history=mock(ActivityHistoryService.class);
    final PurchaseOrderMilestoneService milestones=mock(PurchaseOrderMilestoneService.class);
    @BeforeAll static void start() throws Exception {
        var cfg=new Configuration();var scan=new ClassPathScanningCandidateComponentProvider(false);scan.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for(var c:scan.findCandidateComponents("com.nutalig.entity"))cfg.addAnnotatedClass(Class.forName(c.getBeanClassName()));
        cfg.setProperty("hibernate.connection.url","jdbc:h2:mem:containers_"+UUID.randomUUID()+";MODE=MySQL;NON_KEYWORDS=USER,VALUE,KEY;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");cfg.setProperty("hibernate.connection.driver_class","org.h2.Driver");cfg.setProperty("hibernate.hbm2ddl.auto","create-drop");factory=cfg.buildSessionFactory();
    }
    @AfterAll static void stop(){if(factory!=null)factory.close();}
    <T>T tx(Work<T> work)throws Exception {
        try(var session=factory.openSession()){
            var t=session.beginTransaction();var repos=new JpaRepositoryFactory(session);var json=new ObjectMapper();
            var service=new ShippingContainerService(repos.getRepository(ShippingContainerRepository.class),repos.getRepository(ShippingContainerLoadRepository.class),repos.getRepository(PurchaseOrderDeliveryRepository.class),repos.getRepository(SupplierShippingRepository.class),new ShippingContainerMapper(json),json,history,milestones);
            try{T result=work.run(session,service);t.commit();return result;}catch(Exception e){if(t.isActive())t.rollback();throw e;}
        }
    }
    Fixture fixture()throws Exception{return tx((s,service)->{
        String code=UUID.randomUUID().toString().substring(0,8);var supplier=new SupplierEntity();supplier.setSupplierName("Factory "+code);s.persist(supplier);
        var so=new SalesOrderEntity();so.setSalesOrderNo("SO-"+code);s.persist(so);
        var shipping=new SupplierShippingEntity();shipping.setShippingMethod(ShippingMethod.SEA);shipping.setShippingMode(ShippingMode.FCL);shipping.setShippingName("Carrier");s.persist(shipping);
        var dest=new SupplierShippingDestinationEntity();dest.setDestinationName("Warehouse");dest.setActive(true);shipping.addDestination(dest);s.persist(dest);
        var po=new PurchaseOrderEntity();po.setPurchaseOrderNo("PO-"+code);po.setSalesOrder(so);po.setSupplier(supplier);po.setSupplierShipping(shipping);po.setDocDate(LocalDate.now());po.setStatus(PurchaseOrderStatus.PRODUCTION_RUNNING);
        var product=new PurchaseOrderDetailEntity();product.setName("Bottle");product.setQuantity(new BigDecimal("100"));po.addItem(product);
        var a=new PurchaseOrderDetailComponentEntity();a.setComponentName("Cap");a.setQuantityPerItem(BigDecimal.ONE);a.setUnit("pcs");product.addComponent(a);
        var b=new PurchaseOrderDetailComponentEntity();b.setComponentName("Tube");b.setQuantityPerItem(BigDecimal.ONE);b.setUnit("pcs");product.addComponent(b);s.persist(po);
        var d=new PurchaseOrderDeliveryEntity();d.setPurchaseOrder(po);d.setDeliveryNo("D-"+code);d.setShippingMethod("SEA_FCL_20GP");d.setShippingLocation(dest);d.setShippedAt(LocalDate.now().atStartOfDay(DateUtil.getTimeZone()));d.setStatus(PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER);
        var normal=new PurchaseOrderDeliveryDetailEntity();normal.setPurchaseOrderDetail(product);normal.setPurchaseOrderDetailComponent(a);normal.setProductNameSnapshot("Bottle");normal.setComponentNameSnapshot("Cap");normal.setQuantity(new BigDecimal("90"));normal.setCartonCount(new BigDecimal("9"));normal.setPiecesPerCarton(BigDecimal.TEN);normal.setNetWeightKg(new BigDecimal("9"));normal.setGrossWeightKg(new BigDecimal("18"));normal.setLengthCm(BigDecimal.TEN);normal.setWidthCm(BigDecimal.TEN);normal.setHeightCm(BigDecimal.TEN);normal.setCbmM3(new BigDecimal("0.009"));d.addDetail(normal);
        var box=new PurchaseOrderDeliveryScrapBoxEntity();box.setNetWeightKg(new BigDecimal("5"));box.setGrossWeightKg(new BigDecimal("6"));box.setLengthCm(new BigDecimal("40"));box.setWidthCm(new BigDecimal("30"));box.setHeightCm(new BigDecimal("20"));box.setCbmM3(new BigDecimal("0.024"));d.addScrapBox(box);
        for(var c:List.of(a,b)){var line=new PurchaseOrderDeliveryDetailEntity();line.setPurchaseOrderDetail(product);line.setPurchaseOrderDetailComponent(c);line.setProductNameSnapshot("Bottle");line.setComponentNameSnapshot(c.getComponentName());line.setQuantity(c==a?BigDecimal.TEN:new BigDecimal("100"));line.setScrapBox(box);d.addDetail(line);}s.persist(d);s.flush();
        return new Fixture(d.getId(),normal.getId(),box.getId(),shipping.getId(),dest.getId(),code);
    });}
    ShippingContainerRequest metadata(Fixture f,String name){var r=new ShippingContainerRequest();r.setContainerSize("20GP");r.setContainerNo(f.suffix()+name);r.setSealNo("SEAL");r.setSupplierShippingId(f.shippingId());r.setDestinationId(f.destinationId());r.setCapacityCbm(new BigDecimal("33"));r.setMaxCargoWeightKg(new BigDecimal("1000"));r.setPlannedDepartureDate(LocalDate.now());r.setPlannedArrivalDate(LocalDate.now().plusDays(4));return r;}
    ShippingContainerDto create(Fixture f,String name)throws Exception{return tx((s,service)->service.create(metadata(f,name),"USER"));}
    ShippingContainerDto action(ShippingContainerDto c,String action)throws Exception{return tx((s,service)->{var r=new ShippingContainerRequest.Action();r.setVersion(c.getVersion());r.setReason("test reason");return service.action(c.getId(),action,r,"USER");});}
    ShippingContainerDto add(ShippingContainerDto c,Fixture f,boolean scrap,int cartons)throws Exception{return tx((s,service)->{var r=new ShippingContainerRequest.Load();r.setVersion(c.getVersion());r.setDeliveryId(f.deliveryId());if(scrap)r.setScrapBoxId(f.scrapId());else r.setDeliveryDetailId(f.normalId());r.setCartonCount(BigDecimal.valueOf(cartons));r.setConfirmUniformPackaging(true);return service.addLoad(c.getId(),r,"USER");});}
    PurchaseOrderDeliveryStatus status(Fixture f)throws Exception{return tx((s,service)->s.find(PurchaseOrderDeliveryEntity.class,f.deliveryId()).getStatus());}
    void assignment(Fixture f,DeliveryContainerStatus expected,Long... ids)throws Exception {
        tx((s,service)->{
            var delivery=s.find(PurchaseOrderDeliveryEntity.class,f.deliveryId());
            assertEquals(expected,delivery.getContainerStatus());
            var dto=com.nutalig.mapper.PurchaseOrderDeliveryMapper.toDto(delivery);
            assertEquals(expected,dto.getContainerStatus());
            assertEquals(new HashSet<>(Arrays.asList(ids)),new HashSet<>(dto.getContainers().stream().map(com.nutalig.dto.PurchaseOrderDeliveryDto.ContainerReference::getId).toList()));
            return null;
        });
    }
    @Test void assignmentTracksPartialFullRemovalCancellationAndNullableNonFcl()throws Exception {
        var f=fixture();assignment(f,DeliveryContainerStatus.NOT_ASSIGNED);
        var a=add(create(f,"A"),f,false,4);assignment(f,DeliveryContainerStatus.PARTIALLY_ASSIGNED,a.getId());
        var b=add(create(f,"B"),f,false,5);b=add(b,f,true,1);assignment(f,DeliveryContainerStatus.ASSIGNED,a.getId(),b.getId());
        var current=a;
        a=tx((s,service)->service.removeLoad(current.getId(),current.getLoads().getFirst().getId(),current.getVersion(),"USER"));
        assignment(f,DeliveryContainerStatus.PARTIALLY_ASSIGNED,b.getId());
        action(b,"CANCEL");assignment(f,DeliveryContainerStatus.NOT_ASSIGNED);
        tx((s,service)->{
            for(String method:List.of("LAND","SEA")) {
                var delivery=new PurchaseOrderDeliveryEntity();delivery.setPurchaseOrder(s.find(PurchaseOrderDeliveryEntity.class,f.deliveryId()).getPurchaseOrder());
                delivery.setDeliveryNo(method+UUID.randomUUID());delivery.setShippingMethod(method);s.persist(delivery);s.flush();
                assertNull(delivery.getContainerStatus());assertTrue(delivery.getContainers().isEmpty());
            }
            return null;
        });
    }
    ShippingContainerRequest.Load loadRequest(Fixture f,boolean scrap,int cartons) {
        var load=new ShippingContainerRequest.Load();load.setDeliveryId(f.deliveryId());load.setCartonCount(BigDecimal.valueOf(cartons));load.setConfirmUniformPackaging(true);
        if(scrap)load.setScrapBoxId(f.scrapId());else load.setDeliveryDetailId(f.normalId());return load;
    }
    @Test void previewDoesNotCreateOrReserveAndBulkCreateAllocatesEverythingTogether()throws Exception {
        var f=fixture();long count=tx((s,service)->s.createQuery("select count(c) from ShippingContainerEntity c",Long.class).getSingleResult());
        var available=tx((s,service)->service.previewSources("20GP",f.shippingId(),f.destinationId(),f.deliveryId()));
        assertEquals(2,available.size());assignment(f,DeliveryContainerStatus.NOT_ASSIGNED);
        assertEquals(count,this.<Long>tx((s,service)->s.createQuery("select count(c) from ShippingContainerEntity c",Long.class).getSingleResult()).longValue());
        var request=metadata(f,"BULK");request.setLoads(List.of(loadRequest(f,false,9),loadRequest(f,true,1)));
        var created=tx((s,service)->service.create(request,"USER"));
        assertEquals(ShippingContainerStatus.DRAFT,created.getStatus());assertEquals(2,created.getLoads().size());
        assertEquals(0,BigDecimal.TEN.compareTo(created.getTotalCartons()));assignment(f,DeliveryContainerStatus.ASSIGNED,created.getId());
        assertThrows(InvalidRequestException.class,()->tx((s,service)->service.previewSources("40HQ",f.shippingId(),f.destinationId(),f.deliveryId())));
    }
    @Test void conflictingBulkCreateLeavesNoContainerOrPartialAllocations()throws Exception {
        var f=fixture();var existing=add(create(f,"EXISTING"),f,true,1);
        long count=tx((s,service)->s.createQuery("select count(c) from ShippingContainerEntity c",Long.class).getSingleResult());
        var request=metadata(f,"FAILED");request.setLoads(List.of(loadRequest(f,false,9),loadRequest(f,true,1)));
        assertThrows(InvalidRequestException.class,()->tx((s,service)->service.create(request,"USER")));
        assertEquals(count,this.<Long>tx((s,service)->s.createQuery("select count(c) from ShippingContainerEntity c",Long.class).getSingleResult()).longValue());
        var source=tx((s,service)->service.previewSources("20GP",f.shippingId(),f.destinationId(),f.deliveryId())).stream().filter(row->Objects.equals(row.getDeliveryDetailId(),f.normalId())).findFirst().orElseThrow();
        assertEquals(0,new BigDecimal("9").compareTo(source.getAvailableCartons()));assignment(f,DeliveryContainerStatus.PARTIALLY_ASSIGNED,existing.getId());
        var fresh=fixture();var duplicate=metadata(fresh,"DUPLICATE");
        duplicate.setLoads(List.of(loadRequest(fresh,false,9),loadRequest(fresh,true,1),loadRequest(fresh,true,1)));
        assertThrows(InvalidRequestException.class,()->tx((s,service)->service.create(duplicate,"USER")));
        duplicate.setLoads(List.of(loadRequest(fresh,false,6),loadRequest(fresh,false,6)));
        assertThrows(InvalidRequestException.class,()->tx((s,service)->service.create(duplicate,"USER")));
        assignment(fresh,DeliveryContainerStatus.NOT_ASSIGNED);
        assertEquals(count,this.<Long>tx((s,service)->s.createQuery("select count(c) from ShippingContainerEntity c",Long.class).getSingleResult()).longValue());
    }
    @Test void simultaneousBulkCreatesCannotReserveTheSameCartons()throws Exception {
        var f=fixture();long before=tx((s,service)->s.createQuery("select count(c) from ShippingContainerEntity c",Long.class).getSingleResult());
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> futures=new ArrayList<>();
            for(String name:List.of("A","B"))futures.add(pool.submit(()->{start.await();var request=metadata(f,name);request.setLoads(List.of(loadRequest(f,false,6)));
                try{tx((s,service)->service.create(request,"USER"));return true;}catch(InvalidRequestException expected){return false;}}));
            start.countDown();int accepted=0;for(var future:futures)if(future.get(20,TimeUnit.SECONDS))accepted++;assertEquals(1,accepted);
            assertEquals(before+1,this.<Long>tx((s,service)->s.createQuery("select count(c) from ShippingContainerEntity c",Long.class).getSingleResult()).longValue());
        }finally{pool.shutdownNow();}
    }
    @Test void shipmentMovesOnlyAfterAllItsBoxesDepartAndReceivesOnlyAfterAllContainersReceive()throws Exception {
        var f=fixture();var a=action(create(f,"A"),"START_LOADING");var b=action(create(f,"B"),"START_LOADING");
        a=add(a,f,false,4);b=add(b,f,false,5);b=add(b,f,true,1);
        assertEquals(0,new BigDecimal("0.033").compareTo(a.getTotalCbm().add(b.getTotalCbm())));
        a=action(action(a,"SEAL"),"DEPART");assertEquals(PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER,status(f));
        b=action(action(b,"SEAL"),"DEPART");assertEquals(PurchaseOrderDeliveryStatus.IN_TRANSIT,status(f));
        a=action(a,"RECEIVE");assertEquals(PurchaseOrderDeliveryStatus.IN_TRANSIT,status(f));
        b=action(b,"RECEIVE");assertEquals(PurchaseOrderDeliveryStatus.RECEIVED_AT_WAREHOUSE,status(f));
        assertNotNull(b.getReceivedAt());verify(milestones,times(1)).notifySalesDeliveryInTransitSafely(any(),any());
    }
    @Test void cancellationReleasesSourcesAndReopeningRequiresReasonAndKeepsAuditHistory()throws Exception {
        var f=fixture();var c=add(action(create(f,"C"),"START_LOADING"),f,true,1);c=action(c,"SEAL");
        var sealed=c;assertThrows(InvalidRequestException.class,()->tx((s,service)->{var r=new ShippingContainerRequest.Action();r.setVersion(sealed.getVersion());return service.action(sealed.getId(),"REOPEN",r,"USER");}));
        c=action(c,"REOPEN");assertNull(c.getSealedAt());c=action(c,"CANCEL");assertFalse(c.getLoads().getFirst().getActive());
        var replacement=add(create(f,"R"),f,true,1);assertEquals(1,replacement.getLoads().size());
        var audit=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(history,atLeastOnce()).record(eq(ActivityEntityType.SHIPPING_CONTAINER),anyString(),eq("USER"),eq(ActivityActorType.USER),any(),eq(ActivitySource.API),contains("ยกเลิก"),audit.capture());
        assertEquals("test reason",((Map<?,?>)audit.getValue().get("after")).get("changeReason"));
    }
    @Test void preventsStaleEditsClosedPackingAndAllowsSealWithoutOptionalCapacityFields()throws Exception {
        var f=fixture();var created=create(f,"C");var loading=action(created,"START_LOADING");
        assertThrows(InvalidRequestException.class,()->add(created,f,true,1));
        loading=add(loading,f,true,1);var request=metadata(f,"C");request.setVersion(loading.getVersion());request.setCapacityCbm(null);request.setMaxCargoWeightKg(null);var id=loading.getId();
        var enough=tx((s,service)->service.update(id,request,"USER"));var sealed=action(enough,"SEAL");assertThrows(InvalidRequestException.class,()->add(sealed,f,false,1));
    }
    @Test void physicalContainerNumberCannotBeActiveInTwoJobs()throws Exception {
        var f=fixture();var a=create(f,"SAME");var b=create(f,"SAME");a=action(a,"START_LOADING");var second=b;assertThrows(InvalidRequestException.class,()->action(second,"START_LOADING"));
        action(a,"CANCEL");b=action(b,"START_LOADING");assertEquals(ShippingContainerStatus.LOADING,b.getStatus());
    }
    @Test void concurrentAllocationsCannotOverbookNormalCartons()throws Exception {
        var f=fixture();var a=create(f,"A");var b=create(f,"B");var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> futures=new ArrayList<>();
            for(var c:List.of(a,b))futures.add(pool.submit(()->{start.await();try{add(c,f,false,6);return true;}catch(InvalidRequestException expected){return false;}}));
            start.countDown();int accepted=0;for(var future:futures)if(future.get(20,TimeUnit.SECONDS))accepted++;assertEquals(1,accepted);
            var remaining=tx((s,service)->service.sources(a.getId(),f.deliveryId())).stream().filter(source->Objects.equals(source.getDeliveryDetailId(),f.normalId())).findFirst().orElseThrow();assertEquals(0,new BigDecimal("3").compareTo(remaining.getAvailableCartons()));
        } finally {pool.shutdownNow();}
    }
}
