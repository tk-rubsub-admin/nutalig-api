package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.constant.*;
import com.nutalig.controller.container.ShippingContainerRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.ShippingContainerDto;
import com.nutalig.entity.*;
import com.nutalig.exception.*;
import com.nutalig.mapper.ShippingContainerMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
@Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
public class ShippingContainerService {
    private final ShippingContainerRepository containers;
    private final ShippingContainerLoadRepository loads;
    private final PurchaseOrderDeliveryRepository deliveries;
    private final SupplierShippingRepository shippings;
    private final ShippingContainerMapper mapper;
    private final ObjectMapper json;
    private final ActivityHistoryService history;
    private final PurchaseOrderMilestoneService milestoneService;

    @Transactional(readOnly=true)
    public Pageable<ShippingContainerDto> search(String keyword,ShippingContainerStatus status,String size,PageableRequest pagination)
            throws InvalidRequestException {
        if(pagination==null)pagination=new PageableRequest();
        int page=pagination.getPage()==null?1:pagination.getPage();int limit=pagination.getSize()==null?10:pagination.getSize();
        if(page<1||limit<1||limit>100)throw new InvalidRequestException("page ต้องเริ่มที่ 1 และ size ต้องอยู่ระหว่าง 1 ถึง 100");
        if(StringUtils.isNotBlank(size)&&!Set.of("20GP","40HQ").contains(size))throw new InvalidRequestException("ขนาดตู้ไม่ถูกต้อง");
        Specification<ShippingContainerEntity> criteria=(root,query,cb) -> {
            List<Predicate> predicates=new ArrayList<>();
            if(status!=null)predicates.add(cb.equal(root.get("status"),status));
            if(StringUtils.isNotBlank(size))predicates.add(cb.equal(root.get("containerSize"),size));
            if(StringUtils.isNotBlank(keyword)) {
                String pattern="%"+keyword.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("containerJobNo")),pattern,'\\'),cb.like(cb.lower(root.get("containerNo")),pattern,'\\'),
                        cb.like(cb.lower(root.get("sealNo")),pattern,'\\'),cb.like(cb.lower(root.get("destinationName")),pattern,'\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var found=containers.findAll(criteria,PageRequest.of(page-1,limit,Sort.by(Sort.Direction.DESC,"updatedDate","id")));
        Pageable<ShippingContainerDto> result=new Pageable<>();result.setRecords(found.getContent().stream().map(mapper::toDto).toList());result.setPagination(Pagination.build(found));return result;
    }
    @Transactional(readOnly=true)
    public ShippingContainerDto get(Long id) throws DataNotFoundException { return mapper.toDto(find(id)); }
    public ShippingContainerDto create(ShippingContainerRequest request,String user) throws InvalidRequestException {
        var entity=new ShippingContainerEntity();entity.setContainerJobNo("TMP-"+UUID.randomUUID());
        applyMetadata(entity,request);entity.setCreatedBy(user);entity.setCreatedDate(now());entity.setUpdatedDate(now());
        var requested=request.getLoads()==null?List.<ShippingContainerRequest.Load>of():request.getLoads();
        if(requested.size()>1000||requested.stream().anyMatch(row->row==null||row.getDeliveryId()==null))
            throw new InvalidRequestException("รายการจัดตู้ไม่ถูกต้อง หรือเกิน 1000 รายการ");
        var ids=requested.stream().map(ShippingContainerRequest.Load::getDeliveryId).distinct().sorted().toList();
        var locked=ids.isEmpty()?List.<PurchaseOrderDeliveryEntity>of():deliveries.findAllByIdForUpdate(ids);
        if(locked.size()!=ids.size())throw new InvalidRequestException("ไม่พบเที่ยวส่งที่เลือก กรุณาตรวจสอบรายการใหม่");
        Map<Long,List<ShippingContainerLoadEntity>> allocated=new HashMap<>();
        for(var source:locked) {
            validateRoute(entity,source);
            allocated.put(source.getId(),new ArrayList<>(loads.findActiveByDeliveryId(source.getId())));
        }
        for(var row:requested) {
            var source=locked.stream().filter(d->Objects.equals(d.getId(),row.getDeliveryId())).findFirst().orElseThrow();
            var active=allocated.get(source.getId());
            var load=ShippingContainerPacking.allocate(source,row,active,json);
            load.setCreatedDate(now());load.setUpdatedDate(now());entity.addLoad(load);active.add(load);
        }
        save(entity);
        entity.setContainerJobNo("NTL-CT"+now().format(DateTimeFormatter.ofPattern("yyyyMM"))+String.format("%06d",entity.getId()));
        var result=finish(entity,user,null,"สร้างตู้คอนเทนเนอร์",ActivityAction.CREATE,null);
        for(var source:locked)syncContainerAssignment(source,user);
        return result;
    }
    public ShippingContainerDto update(Long id,ShippingContainerRequest request,String user) throws DataNotFoundException,InvalidRequestException {
        if(request.getLoads()!=null&&!request.getLoads().isEmpty())throw new InvalidRequestException("กรุณาใช้ API จัดกล่องสำหรับตู้ที่สร้างแล้ว");
        var entity=lock(id);checkVersion(entity,request.getVersion());editable(entity);var before=mapper.snapshot(entity);
        applyMetadata(entity,request);return finish(entity,user,before,"แก้ไขข้อมูลตู้คอนเทนเนอร์",ActivityAction.UPDATE,null);
    }
    @Transactional(readOnly=true)
    public List<ShippingContainerDto.Source> previewSources(String size,Long shippingId,Long destinationId,Long deliveryId)
            throws DataNotFoundException,InvalidRequestException {
        var request=new ShippingContainerRequest();request.setContainerSize(size);request.setSupplierShippingId(shippingId);request.setDestinationId(destinationId);
        var target=new ShippingContainerEntity();applyMetadata(target,request);
        var source=deliveries.findById(deliveryId).orElseThrow(()->new DataNotFoundException("ไม่พบเที่ยวส่ง"));
        validateRoute(target,source);
        return ShippingContainerPacking.sources(source,loads.findActiveByDeliveryId(deliveryId));
    }
    @Transactional(readOnly=true)
    public List<ShippingContainerDto.Source> sources(Long id,Long deliveryId) throws DataNotFoundException,InvalidRequestException {
        var entity=find(id);editable(entity);var source=deliveries.findById(deliveryId).orElseThrow(()->new DataNotFoundException("ไม่พบเที่ยวส่ง"));
        validateRoute(entity,source);return ShippingContainerPacking.sources(source,loads.findActiveByDeliveryId(deliveryId));
    }
    public ShippingContainerDto addLoad(Long id,ShippingContainerRequest.Load request,String user) throws DataNotFoundException,InvalidRequestException {
        var entity=lock(id);checkVersion(entity,request.getVersion());editable(entity);
        if(request.getDeliveryId()==null)throw new InvalidRequestException("กรุณาเลือกเที่ยวส่ง");
        var source=lockDeliveries(List.of(request.getDeliveryId())).getFirst();validateRoute(entity,source);
        var before=mapper.snapshot(entity);
        var allocated=ShippingContainerPacking.allocate(source,request,loads.findActiveByDeliveryId(source.getId()),json);
        allocated.setCreatedDate(now());allocated.setUpdatedDate(now());entity.addLoad(allocated);
        var result=finish(entity,user,before,"จัดกล่องลงตู้คอนเทนเนอร์",ActivityAction.UPDATE,null);
        syncContainerAssignment(source,user);
        return result;
    }
    public ShippingContainerDto removeLoad(Long id,Long loadId,Long version,String user) throws DataNotFoundException,InvalidRequestException {
        var entity=lock(id);checkVersion(entity,version);editable(entity);
        var target=entity.getLoads().stream().filter(row->Objects.equals(row.getId(),loadId)).findFirst().orElseThrow(()->new DataNotFoundException("ไม่พบรายการจัดตู้"));
        var source=lockDeliveries(List.of(target.getDelivery().getId())).getFirst();var before=mapper.snapshot(entity);entity.getLoads().remove(target);
        var result=finish(entity,user,before,"นำกล่องออกจากตู้คอนเทนเนอร์",ActivityAction.UPDATE,null);
        syncContainerAssignment(source,user);
        return result;
    }
    public ShippingContainerDto action(Long id,String action,ShippingContainerRequest.Action request,String user) throws DataNotFoundException,InvalidRequestException {
        var entity=lock(id);checkVersion(entity,request.getVersion());
        var before=mapper.snapshot(entity);String reason=StringUtils.trimToNull(request.getReason());
        var sourceIds=entity.getLoads().stream().map(load->load.getDelivery().getId()).distinct().sorted().toList();
        var locked=sourceIds.isEmpty()?List.<PurchaseOrderDeliveryEntity>of():lockDeliveries(sourceIds);
        String title;
        switch(action) {
            case "START_LOADING" -> { requireStatus(entity,ShippingContainerStatus.DRAFT);entity.setStatus(ShippingContainerStatus.LOADING);title="เริ่มจัดตู้คอนเทนเนอร์"; }
            case "SEAL" -> {
                requireStatus(entity,ShippingContainerStatus.LOADING);validateSeal(entity);
                for(var delivery:locked)validateRoute(entity,delivery);
                entity.setStatus(ShippingContainerStatus.SEALED);entity.setSealedAt(now());title="ปิดตู้คอนเทนเนอร์";
            }
            case "DEPART" -> {
                requireStatus(entity,ShippingContainerStatus.SEALED);
                for(var delivery:locked)validateRoute(entity,delivery);
                entity.setStatus(ShippingContainerStatus.IN_TRANSIT);entity.setDepartedAt(now());title="ส่งตู้คอนเทนเนอร์ออกเดินทาง";
            }
            case "RECEIVE" -> { requireStatus(entity,ShippingContainerStatus.IN_TRANSIT);entity.setStatus(ShippingContainerStatus.RECEIVED);entity.setReceivedAt(now());title="รับตู้คอนเทนเนอร์เข้าคลัง"; }
            case "REOPEN" -> {
                requireStatus(entity,ShippingContainerStatus.SEALED);requireReason(reason);entity.setStatus(ShippingContainerStatus.LOADING);entity.setSealedAt(null);title="เปิดตู้คอนเทนเนอร์เพื่อแก้ไข";
            }
            case "CANCEL" -> {
                if(!Set.of(ShippingContainerStatus.DRAFT,ShippingContainerStatus.LOADING,ShippingContainerStatus.SEALED).contains(entity.getStatus()))throw new InvalidRequestException("ไม่สามารถยกเลิกตู้ที่ออกเดินทางหรือรับเข้าคลังแล้ว");
                requireReason(reason);entity.setStatus(ShippingContainerStatus.CANCELLED);
                entity.getLoads().forEach(load->{load.setActive(false);load.setActiveScrapBoxId(null);});title="ยกเลิกตู้คอนเทนเนอร์";
            }
            default -> throw new InvalidRequestException("ไม่รองรับการเปลี่ยนสถานะนี้");
        }
        var result=finish(entity,user,before,title,ActivityAction.STATUS_CHANGE,reason);
        if("CANCEL".equals(action))for(var delivery:locked)syncContainerAssignment(delivery,user);
        if("DEPART".equals(action)||"RECEIVE".equals(action))for(var delivery:locked)syncDelivery(delivery,entity,user);
        return result;
    }
    private void syncContainerAssignment(PurchaseOrderDeliveryEntity delivery,String user) {
        var active=loads.findActiveByDeliveryId(delivery.getId());
        var previous=delivery.getContainerStatus();
        var previousIds=delivery.getContainers().stream().map(ShippingContainerEntity::getId).sorted().toList();
        delivery.getContainers().clear();
        active.forEach(load->delivery.getContainers().add(load.getContainer()));
        var next=!delivery.requiresContainer()?null:active.isEmpty()?DeliveryContainerStatus.NOT_ASSIGNED:
                fullyAllocated(delivery,active)?DeliveryContainerStatus.ASSIGNED:DeliveryContainerStatus.PARTIALLY_ASSIGNED;
        delivery.setContainerStatus(next);delivery.setUpdatedBy(user);delivery.setUpdatedDate(now());
        deliveries.save(delivery);
        var ids=delivery.getContainers().stream().map(ShippingContainerEntity::getId).sorted().toList();
        if(previous!=next||!previousIds.equals(ids)) {
            Map<String,Object> changes=new LinkedHashMap<>();changes.put("deliveryId",delivery.getId());
            changes.put("previousContainerStatus",previous);changes.put("containerStatus",next);changes.put("containerIds",ids);
            history.record(ActivityEntityType.PURCHASE_ORDER,delivery.getPurchaseOrder().getPurchaseOrderNo(),user,
                    ActivityActorType.USER,ActivityAction.UPDATE,ActivitySource.API,"อัปเดตการจัดตู้เที่ยวส่ง "+delivery.getDeliveryNo(),changes);
        }
    }
    private void syncDelivery(PurchaseOrderDeliveryEntity delivery,ShippingContainerEntity current,String user) {
        var active=loads.findActiveByDeliveryId(delivery.getId());
        if(delivery.getStatus()==PurchaseOrderDeliveryStatus.CANCELLED||active.isEmpty()||!fullyAllocated(delivery,active))return;
        boolean received=active.stream().allMatch(load->load.getContainer().getStatus()==ShippingContainerStatus.RECEIVED);
        boolean departed=active.stream().allMatch(load->Set.of(ShippingContainerStatus.IN_TRANSIT,ShippingContainerStatus.RECEIVED).contains(load.getContainer().getStatus()));
        PurchaseOrderDeliveryStatus next=received?PurchaseOrderDeliveryStatus.RECEIVED_AT_WAREHOUSE:departed?PurchaseOrderDeliveryStatus.IN_TRANSIT:null;
        if(next==null||next==delivery.getStatus())return;
        delivery.setStatus(next);delivery.setUpdatedBy(user);delivery.setUpdatedDate(now());
        if(received)delivery.setReceivedAt(now());else delivery.setInTransitDate(now().toLocalDate());deliveries.save(delivery);
        history.record(ActivityEntityType.PURCHASE_ORDER,delivery.getPurchaseOrder().getPurchaseOrderNo(),user,ActivityActorType.USER,ActivityAction.STATUS_CHANGE,ActivitySource.API,
                "อัปเดตเที่ยวส่ง "+delivery.getDeliveryNo()+" จากตู้ "+current.getContainerJobNo()+" เป็น"+(received?"รับเข้าคลังแล้ว":"อยู่ระหว่างขนส่ง"),
                Map.of("deliveryId",delivery.getId(),"containerId",current.getId(),"status",next.name()));
        if(!received)milestoneService.notifySalesDeliveryInTransitSafely(delivery.getPurchaseOrder(),delivery);
    }
    private boolean fullyAllocated(PurchaseOrderDeliveryEntity delivery,List<ShippingContainerLoadEntity> active) {
        for(var detail:delivery.getDetails()) {
            if(detail.getScrapBox()!=null)continue;
            if(!ShippingContainerPacking.wholePositive(detail.getCartonCount())||ShippingContainerPacking.cartonsUsed(active,detail.getId()).compareTo(detail.getCartonCount())!=0)return false;
        }
        for(var box:delivery.getScrapBoxes())if(active.stream().noneMatch(load->load.getScrapBox()!=null&&Objects.equals(load.getScrapBox().getId(),box.getId())))return false;
        return true;
    }
    private void applyMetadata(ShippingContainerEntity entity,ShippingContainerRequest request) throws InvalidRequestException {
        if(!Set.of("20GP","40HQ").contains(Objects.toString(request.getContainerSize(),"")))throw new InvalidRequestException("กรุณาระบุขนาดตู้ 20GP หรือ 40HQ");
        if(!entity.getLoads().isEmpty()&&(!Objects.equals(entity.getContainerSize(),request.getContainerSize())
                ||!Objects.equals(entity.getSupplierShipping()==null?null:entity.getSupplierShipping().getId(),request.getSupplierShippingId())
                ||!Objects.equals(entity.getDestination()==null?null:entity.getDestination().getId(),request.getDestinationId())))
            throw new InvalidRequestException("ต้องนำกล่องออกก่อนเปลี่ยนขนาดตู้ ผู้ให้บริการขนส่ง หรือปลายทาง");
        if(request.getPlannedDepartureDate()!=null&&request.getPlannedArrivalDate()!=null&&request.getPlannedArrivalDate().isBefore(request.getPlannedDepartureDate()))throw new InvalidRequestException("วันที่ถึงต้องไม่ก่อนวันที่ออกเดินทาง");
        if(request.getCapacityCbm()!=null&&request.getCapacityCbm().signum()<0||request.getMaxCargoWeightKg()!=null&&request.getMaxCargoWeightKg().signum()<0)throw new InvalidRequestException("ความจุและน้ำหนักต้องไม่ติดลบ");
        SupplierShippingEntity shipping=null;SupplierShippingDestinationEntity destination=null;
        if(request.getSupplierShippingId()!=null) {
            shipping=shippings.findByIdAndActiveTrue(request.getSupplierShippingId()).orElseThrow(()->new InvalidRequestException("ไม่พบผู้ให้บริการขนส่งที่ใช้งานอยู่"));
            if(shipping.getShippingMethod()!=ShippingMethod.SEA||shipping.getShippingMode()!=ShippingMode.FCL)throw new InvalidRequestException("ต้องเลือกขนส่งทางเรือแบบปิดตู้");
            if(request.getDestinationId()!=null) destination=shipping.getDestinations().stream().filter(d->Objects.equals(d.getId(),request.getDestinationId())&&Boolean.TRUE.equals(d.getActive())).findFirst().orElseThrow(()->new InvalidRequestException("ปลายทางไม่ได้อยู่ในขนส่งที่เลือก"));
        } else if(request.getDestinationId()!=null)throw new InvalidRequestException("กรุณาเลือกผู้ให้บริการขนส่งก่อนเลือกปลายทาง");
        entity.setContainerSize(request.getContainerSize());entity.setShippingMethod("SEA_FCL_"+request.getContainerSize());entity.setSupplierShipping(shipping);entity.setDestination(destination);
        entity.setCarrierName(shipping==null?null:shipping.getShippingName());entity.setDestinationName(destination==null?null:destination.getDestinationName());
        entity.setContainerNo(StringUtils.upperCase(StringUtils.trimToNull(request.getContainerNo()),Locale.ROOT));entity.setSealNo(StringUtils.trimToNull(request.getSealNo()));
        entity.setTrackingNo(StringUtils.trimToNull(request.getTrackingNo()));
        entity.setOrigin(StringUtils.trimToNull(request.getOrigin()));entity.setPlannedDepartureDate(request.getPlannedDepartureDate());entity.setPlannedArrivalDate(request.getPlannedArrivalDate());
        entity.setCapacityCbm(request.getCapacityCbm());entity.setMaxCargoWeightKg(request.getMaxCargoWeightKg());entity.setRemark(StringUtils.trimToNull(request.getRemark()));
    }
    private void validateRoute(ShippingContainerEntity container,PurchaseOrderDeliveryEntity source) throws InvalidRequestException {
        if(source.getStatus()!=PurchaseOrderDeliveryStatus.HANDED_OVER_TO_CARRIER||source.getPurchaseOrder().getStatus()==PurchaseOrderStatus.CANCELLED)throw new InvalidRequestException("เที่ยวส่งต้องอยู่ในสถานะส่งมอบให้ขนส่งแล้ว และ PO ไม่ถูกยกเลิก");
        var shipping=source.getPurchaseOrder().getSupplierShipping();
        if(container.getSupplierShipping()==null||container.getDestination()==null||shipping==null||source.getShippingLocation()==null
                ||!Boolean.TRUE.equals(container.getSupplierShipping().getActive())||!Boolean.TRUE.equals(container.getDestination().getActive())
                ||!Objects.equals(container.getSupplierShipping().getId(),shipping.getId())||!Objects.equals(container.getDestination().getId(),source.getShippingLocation().getId())
                ||!StringUtils.equalsIgnoreCase(container.getShippingMethod(),source.getShippingMethod()))throw new InvalidRequestException("เที่ยวส่งต้องมีขนาดตู้ ผู้ให้บริการ และปลายทางตรงกับตู้ที่เลือก");
    }
    private void validateSeal(ShippingContainerEntity entity) throws InvalidRequestException {
        if(StringUtils.isBlank(entity.getContainerNo())||StringUtils.isBlank(entity.getSealNo())||entity.getSupplierShipping()==null||entity.getDestination()==null
                ||entity.getPlannedDepartureDate()==null||entity.getPlannedArrivalDate()==null||entity.getLoads().isEmpty())throw new InvalidRequestException("กรุณาระบุหมายเลขตู้ ซีล ขนส่ง ปลายทาง กำหนดวัน และรายการกล่องให้ครบก่อนปิดตู้");
        for(var load:entity.getLoads())ShippingContainerPacking.requireCompleteMetrics(load);
        var dto=mapper.toDto(entity);
        if(entity.getCapacityCbm()!=null && entity.getCapacityCbm().signum()>0 && dto.getTotalCbm().compareTo(entity.getCapacityCbm())>0)
            throw new InvalidRequestException("CBM หรือน้ำหนักสินค้าเกินความจุตู้");
        if(entity.getMaxCargoWeightKg()!=null && entity.getMaxCargoWeightKg().signum()>0 && dto.getTotalGrossWeightKg().compareTo(entity.getMaxCargoWeightKg())>0)
            throw new InvalidRequestException("CBM หรือน้ำหนักสินค้าเกินความจุตู้");
    }
    private ShippingContainerDto finish(ShippingContainerEntity entity,String user,Map<String,Object> before,String title,ActivityAction action,String reason) throws InvalidRequestException {
        entity.setUpdatedBy(user);entity.setUpdatedDate(now());
        entity.setActiveContainerNo(Set.of(ShippingContainerStatus.LOADING,ShippingContainerStatus.SEALED,ShippingContainerStatus.IN_TRANSIT).contains(entity.getStatus())?entity.getContainerNo():null);
        save(entity);
        Map<String,Object> detail=new LinkedHashMap<>();detail.put("before",before);Map<String,Object> after=mapper.snapshot(entity);if(reason!=null)after.put("changeReason",reason);detail.put("after",after);detail.put("reason",reason);
        history.record(ActivityEntityType.SHIPPING_CONTAINER,entity.getId().toString(),user,ActivityActorType.USER,action,ActivitySource.API,title+" "+entity.getContainerJobNo(),detail);
        return mapper.toDto(entity);
    }
    private void save(ShippingContainerEntity entity) throws InvalidRequestException {
        try { containers.saveAndFlush(entity); }
        catch(DataIntegrityViolationException | org.hibernate.exception.ConstraintViolationException e) { throw new InvalidRequestException("หมายเลขตู้ถูกใช้งานอยู่ หรือกล่องถูกจัดตู้แล้ว กรุณาโหลดข้อมูลใหม่"); }
    }
    private ShippingContainerEntity find(Long id) throws DataNotFoundException { return containers.findById(id).orElseThrow(()->new DataNotFoundException("ไม่พบตู้คอนเทนเนอร์")); }
    private ShippingContainerEntity lock(Long id) throws DataNotFoundException { return containers.findByIdForUpdate(id).orElseThrow(()->new DataNotFoundException("ไม่พบตู้คอนเทนเนอร์")); }
    private List<PurchaseOrderDeliveryEntity> lockDeliveries(List<Long> ids) throws DataNotFoundException {
        var locked=deliveries.findAllByIdForUpdate(ids);if(locked.size()!=new HashSet<>(ids).size())throw new DataNotFoundException("ไม่พบเที่ยวส่ง");return locked;
    }
    private void checkVersion(ShippingContainerEntity entity,Long version) throws InvalidRequestException { if(version==null||!Objects.equals(entity.getVersion(),version))throw new InvalidRequestException("ข้อมูลตู้มีการเปลี่ยนแปลง กรุณาโหลดใหม่ก่อนบันทึก"); }
    private void editable(ShippingContainerEntity entity) throws InvalidRequestException { if(entity.getStatus()!=ShippingContainerStatus.DRAFT&&entity.getStatus()!=ShippingContainerStatus.LOADING)throw new InvalidRequestException("ตู้ถูกปิดแล้ว ไม่สามารถแก้ไขรายการได้"); }
    private void requireStatus(ShippingContainerEntity entity,ShippingContainerStatus expected) throws InvalidRequestException { if(entity.getStatus()!=expected)throw new InvalidRequestException("ไม่สามารถดำเนินการจากสถานะปัจจุบันได้"); }
    private void requireReason(String reason) throws InvalidRequestException { if(reason==null)throw new InvalidRequestException("กรุณาระบุเหตุผล"); }
    private ZonedDateTime now() { return ZonedDateTime.now(DateUtil.getTimeZone()); }
}
