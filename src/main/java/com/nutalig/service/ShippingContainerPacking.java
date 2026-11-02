package com.nutalig.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.controller.container.ShippingContainerRequest;
import com.nutalig.dto.ShippingContainerDto;
import com.nutalig.entity.*;
import com.nutalig.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;

/** Packing calculations use physical boxes; a mixed box is always indivisible. */
public final class ShippingContainerPacking {
    private ShippingContainerPacking() {}
    public static boolean wholePositive(BigDecimal value) {
        return value != null && value.signum() > 0 && value.stripTrailingZeros().scale() <= 0;
    }
    public static BigDecimal cartonsUsed(List<ShippingContainerLoadEntity> active, Long detailId) {
        return active.stream().filter(l -> l.getDeliveryDetail() != null && Objects.equals(l.getDeliveryDetail().getId(),detailId))
                .map(ShippingContainerLoadEntity::getCartonCount).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    private static boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private static boolean completeMetrics(BigDecimal net,BigDecimal gross,BigDecimal cbm) {
        return net != null && net.signum() >= 0 && positive(gross) && gross.compareTo(net) >= 0 && positive(cbm);
    }
    public static boolean canSplit(PurchaseOrderDeliveryDetailEntity source) {
        return wholePositive(source.getCartonCount()) && positive(source.getPiecesPerCarton()) && positive(source.getQuantity())
                && source.getQuantity().compareTo(source.getPiecesPerCarton().multiply(source.getCartonCount())) == 0
                && positive(source.getLengthCm()) && positive(source.getWidthCm()) && positive(source.getHeightCm())
                && completeMetrics(source.getNetWeightKg(),source.getGrossWeightKg(),source.getCbmM3());
    }
    public static void requireCompleteMetrics(ShippingContainerLoadEntity load) throws InvalidRequestException {
        if (!completeMetrics(load.getNetWeightKg(),load.getGrossWeightKg(),load.getCbmM3()))
            throw new InvalidRequestException("ข้อมูล Net/Gross Weight หรือ CBM ของกล่องไม่ครบหรือไม่ถูกต้อง: " + load.getDeliveryNo());
    }
    public static ShippingContainerLoadEntity allocate(PurchaseOrderDeliveryEntity delivery,
            ShippingContainerRequest.Load request,List<ShippingContainerLoadEntity> active,ObjectMapper json)
            throws InvalidRequestException {
        if ((request.getDeliveryDetailId() == null) == (request.getScrapBoxId() == null))
            throw new InvalidRequestException("ต้องเลือกกล่องปกติหรือกล่องเศษอย่างใดอย่างหนึ่ง");
        ShippingContainerLoadEntity load=new ShippingContainerLoadEntity();
        load.setDelivery(delivery);load.setDeliveryNo(delivery.getDeliveryNo());
        load.setPurchaseOrderNo(delivery.getPurchaseOrder().getPurchaseOrderNo());
        List<ShippingContainerDto.Content> contents=new ArrayList<>();
        if (request.getScrapBoxId() != null) {
            if (request.getCartonCount() != null && request.getCartonCount().compareTo(BigDecimal.ONE) != 0)
                throw new InvalidRequestException("กล่องเศษต้องจัดลงตู้ทั้งกล่อง จำนวน 1 กล่อง");
            var source=delivery.getScrapBoxes().stream().filter(b -> Objects.equals(b.getId(),request.getScrapBoxId())).findFirst()
                    .orElseThrow(() -> new InvalidRequestException("ไม่พบกล่องเศษในเที่ยวส่งที่เลือก"));
            if (active.stream().anyMatch(l -> l.getScrapBox()!=null && Objects.equals(l.getScrapBox().getId(),source.getId())))
                throw new InvalidRequestException("กล่องเศษนี้ถูกจัดลงตู้แล้ว");
            load.setScrapBox(source);load.setActiveScrapBoxId(source.getId());load.setCartonCount(BigDecimal.ONE);
            load.setComponentName("กล่องเศษ");load.setNetWeightKg(source.getNetWeightKg());load.setGrossWeightKg(source.getGrossWeightKg());
            load.setCbmM3(source.getCbmM3());load.setLengthCm(source.getLengthCm());load.setWidthCm(source.getWidthCm());load.setHeightCm(source.getHeightCm());
            delivery.getDetails().stream().filter(d -> d.getScrapBox()!=null && Objects.equals(d.getScrapBox().getId(),source.getId()))
                    .forEach(d -> contents.add(content(d,d.getQuantity())));
            if(contents.isEmpty()) throw new InvalidRequestException("กล่องเศษไม่มีรายการอะไหล่");
            load.setQuantity(contents.stream().map(ShippingContainerDto.Content::getQuantity).reduce(BigDecimal.ZERO,BigDecimal::add));
        } else {
            var source=delivery.getDetails().stream().filter(d -> Objects.equals(d.getId(),request.getDeliveryDetailId())).findFirst()
                    .orElseThrow(() -> new InvalidRequestException("ไม่พบรายการกล่องในเที่ยวส่งที่เลือก"));
            if(source.getScrapBox()!=null) throw new InvalidRequestException("ต้องเลือกกล่องเศษทั้งกล่อง ไม่สามารถแยกอะไหล่ภายในได้");
            BigDecimal total=source.getCartonCount();BigDecimal count=request.getCartonCount();
            if(!wholePositive(total)||!wholePositive(count)) throw new InvalidRequestException("จำนวนกล่องต้องเป็นจำนวนเต็มมากกว่า 0 และมีข้อมูลจำนวนกล่องต้นทางครบ");
            BigDecimal used=cartonsUsed(active,source.getId());BigDecimal remaining=total.subtract(used);
            if(count.compareTo(remaining)>0) throw new InvalidRequestException("จำนวนกล่องเกินยอดคงเหลือที่ยังไม่ได้จัดตู้");
            if(!positive(source.getQuantity())) throw new InvalidRequestException("ไม่พบจำนวนชิ้นต้นทาง");
            boolean partial=count.compareTo(total)<0;
            if(partial && (!Boolean.TRUE.equals(request.getConfirmUniformPackaging()) || !canSplit(source)))
                throw new InvalidRequestException("การแบ่งกล่องต้องมีข้อมูลต่อกล่องครบ และยืนยันว่าจำนวนชิ้น น้ำหนัก และขนาดเท่ากันทุกกล่อง");
            load.setDeliveryDetail(source);load.setCartonCount(count);load.setUniformPackagingConfirmed(partial);
            load.setComponentName(source.getComponentNameSnapshot()==null?source.getProductNameSnapshot():source.getComponentNameSnapshot());
            load.setQuantity(partial?source.getPiecesPerCarton().multiply(count):source.getQuantity());
            var usedLoads=active.stream().filter(l -> l.getDeliveryDetail()!=null && Objects.equals(l.getDeliveryDetail().getId(),source.getId())).toList();
            boolean last=count.compareTo(remaining)==0;
            load.setNetWeightKg(allocated(source.getNetWeightKg(),count,total,usedLoads,ShippingContainerLoadEntity::getNetWeightKg,last));
            load.setGrossWeightKg(allocated(source.getGrossWeightKg(),count,total,usedLoads,ShippingContainerLoadEntity::getGrossWeightKg,last));
            load.setCbmM3(allocated(source.getCbmM3(),count,total,usedLoads,ShippingContainerLoadEntity::getCbmM3,last));
            load.setLengthCm(source.getLengthCm());load.setWidthCm(source.getWidthCm());load.setHeightCm(source.getHeightCm());
            contents.add(content(source,load.getQuantity()));
        }
        if(!positive(load.getQuantity())) throw new InvalidRequestException("จำนวนชิ้นในกล่องต้องมากกว่า 0");
        try { load.setContentsJson(json.writeValueAsString(contents)); }
        catch(JsonProcessingException e) { throw new IllegalStateException("Cannot snapshot container contents",e); }
        return load;
    }
    private static BigDecimal allocated(BigDecimal source,BigDecimal count,BigDecimal total,List<ShippingContainerLoadEntity> used,
            Function<ShippingContainerLoadEntity,BigDecimal> metric,boolean last) throws InvalidRequestException {
        if(source==null)return null;
        BigDecimal value;
        if(last) {
            BigDecimal reserved=BigDecimal.ZERO;
            for(var load:used) {
                if(metric.apply(load)==null) throw new InvalidRequestException("ข้อมูลกล่องที่จัดลงตู้ก่อนหน้านี้ไม่ครบ");
                reserved=reserved.add(metric.apply(load));
            }
            value=source.subtract(reserved);
        } else value=source.multiply(count).divide(total,6,RoundingMode.HALF_UP);
        if(value.signum()<0) throw new InvalidRequestException("ยอดข้อมูลกล่องต้นทางไม่สอดคล้องกับยอดที่จัดตู้แล้ว");
        return value;
    }
    private static ShippingContainerDto.Content content(PurchaseOrderDeliveryDetailEntity source,BigDecimal quantity) {
        var item=new ShippingContainerDto.Content();item.setPurchaseOrderDetailId(source.getPurchaseOrderDetail().getId());
        item.setComponentId(source.getPurchaseOrderDetailComponent()==null?null:source.getPurchaseOrderDetailComponent().getId());
        item.setProductName(source.getProductNameSnapshot());item.setComponentName(source.getComponentNameSnapshot());item.setQuantity(quantity);return item;
    }
    public static List<ShippingContainerDto.Source> sources(PurchaseOrderDeliveryEntity delivery,List<ShippingContainerLoadEntity> active) {
        List<ShippingContainerDto.Source> result=new ArrayList<>();
        for(var item:delivery.getDetails()) {
            if(item.getScrapBox()!=null)continue;
            var dto=new ShippingContainerDto.Source();dto.setDeliveryId(delivery.getId());dto.setDeliveryDetailId(item.getId());
            dto.setName(item.getComponentNameSnapshot()==null?item.getProductNameSnapshot():item.getComponentNameSnapshot());
            dto.setTotalCartons(item.getCartonCount());dto.setAvailableCartons(wholePositive(item.getCartonCount())?item.getCartonCount().subtract(cartonsUsed(active,item.getId())).max(BigDecimal.ZERO):null);
            dto.setQuantity(item.getQuantity());dto.setCbmM3(item.getCbmM3());dto.setNetWeightKg(item.getNetWeightKg());dto.setGrossWeightKg(item.getGrossWeightKg());dto.setCanSplit(canSplit(item));dto.setContents(List.of(content(item,item.getQuantity())));result.add(dto);
        }
        for(var item:delivery.getScrapBoxes()) {
            var dto=new ShippingContainerDto.Source();dto.setDeliveryId(delivery.getId());dto.setScrapBoxId(item.getId());dto.setName("กล่องเศษ #"+item.getId());dto.setTotalCartons(BigDecimal.ONE);
            dto.setAvailableCartons(active.stream().anyMatch(l -> l.getScrapBox()!=null && Objects.equals(l.getScrapBox().getId(),item.getId()))?BigDecimal.ZERO:BigDecimal.ONE);
            dto.setCbmM3(item.getCbmM3());dto.setNetWeightKg(item.getNetWeightKg());dto.setGrossWeightKg(item.getGrossWeightKg());dto.setCanSplit(false);
            var contents=delivery.getDetails().stream().filter(row->row.getScrapBox()!=null&&Objects.equals(row.getScrapBox().getId(),item.getId())).map(row->content(row,row.getQuantity())).toList();
            dto.setContents(contents);dto.setQuantity(contents.stream().map(ShippingContainerDto.Content::getQuantity).reduce(BigDecimal.ZERO,BigDecimal::add));result.add(dto);
        }
        return result;
    }
}
