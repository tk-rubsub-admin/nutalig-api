package com.nutalig.mapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.dto.ShippingContainerDto;
import com.nutalig.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

@Component @RequiredArgsConstructor
public class ShippingContainerMapper {
    private final ObjectMapper json;
    public ShippingContainerDto toDto(ShippingContainerEntity entity) {
        var dto=new ShippingContainerDto();BeanUtils.copyProperties(entity,dto,"loads");
        dto.setSupplierShippingId(entity.getSupplierShipping()==null?null:entity.getSupplierShipping().getId());
        dto.setDestinationId(entity.getDestination()==null?null:entity.getDestination().getId());
        dto.setLoads(entity.getLoads().stream().map(load -> {
            var row=new ShippingContainerDto.Load();BeanUtils.copyProperties(load,row);
            row.setDeliveryId(load.getDelivery().getId());row.setDeliveryDetailId(load.getDeliveryDetail()==null?null:load.getDeliveryDetail().getId());
            row.setScrapBoxId(load.getScrapBox()==null?null:load.getScrapBox().getId());
            try { row.setContents(json.readValue(load.getContentsJson(),new TypeReference<List<ShippingContainerDto.Content>>(){})); }
            catch(Exception e) { throw new IllegalStateException("Cannot read container contents snapshot",e); }
            return row;
        }).toList());
        dto.setTotalCartons(total(entity,ShippingContainerLoadEntity::getCartonCount));
        dto.setTotalCbm(total(entity,ShippingContainerLoadEntity::getCbmM3));
        dto.setTotalNetWeightKg(total(entity,ShippingContainerLoadEntity::getNetWeightKg));
        dto.setTotalGrossWeightKg(total(entity,ShippingContainerLoadEntity::getGrossWeightKg));return dto;
    }
    private BigDecimal total(ShippingContainerEntity entity,Function<ShippingContainerLoadEntity,BigDecimal> field) {
        if(entity.getLoads().stream().anyMatch(load -> field.apply(load)==null))return null;
        return entity.getLoads().stream().map(field).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    public Map<String,Object> snapshot(ShippingContainerEntity entity) {
        var dto=toDto(entity);Map<String,Object> data=new LinkedHashMap<>();
        data.put("containerJobNo",dto.getContainerJobNo());data.put("containerNo",dto.getContainerNo());data.put("sealNo",dto.getSealNo());data.put("trackingNo",dto.getTrackingNo());
        data.put("status",dto.getStatus().name());data.put("containerSize",dto.getContainerSize());data.put("shippingMethod",dto.getShippingMethod());
        data.put("supplierShippingId",dto.getSupplierShippingId());data.put("destinationId",dto.getDestinationId());data.put("destinationName",dto.getDestinationName());
        data.put("sealedAt",Objects.toString(dto.getSealedAt(),null));data.put("departedAt",Objects.toString(dto.getDepartedAt(),null));data.put("receivedAt",Objects.toString(dto.getReceivedAt(),null));
        data.put("origin",dto.getOrigin());data.put("plannedDepartureDate",Objects.toString(dto.getPlannedDepartureDate(),null));data.put("plannedArrivalDate",Objects.toString(dto.getPlannedArrivalDate(),null));
        data.put("capacityCbm",dto.getCapacityCbm());data.put("maxCargoWeightKg",dto.getMaxCargoWeightKg());data.put("totalCbm",dto.getTotalCbm());data.put("totalCartons",dto.getTotalCartons());
        data.put("totalNetWeightKg",dto.getTotalNetWeightKg());data.put("totalGrossWeightKg",dto.getTotalGrossWeightKg());data.put("remark",dto.getRemark());data.put("loads",dto.getLoads());
        return data;
    }
}
