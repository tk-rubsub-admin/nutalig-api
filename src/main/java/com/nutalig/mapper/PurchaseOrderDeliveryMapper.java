package com.nutalig.mapper;

import com.nutalig.dto.*;
import com.nutalig.entity.PurchaseOrderDeliveryEntity;
import org.apache.commons.lang3.StringUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

public final class PurchaseOrderDeliveryMapper {
    private PurchaseOrderDeliveryMapper() {}

    public static PurchaseOrderDeliveryDto toDto(PurchaseOrderDeliveryEntity delivery) {
        return populate(new PurchaseOrderDeliveryDto(), delivery);
    }

    private static <T extends PurchaseOrderDeliveryDto> T populate(T deliveryDto, PurchaseOrderDeliveryEntity delivery) {
        deliveryDto.setId(delivery.getId());
        deliveryDto.setDeliveryNo(delivery.getDeliveryNo());
        deliveryDto.setPurchaseOrderNo(delivery.getPurchaseOrder().getPurchaseOrderNo());
        deliveryDto.setShippedAt(delivery.getShippedAt());
        deliveryDto.setExpectedWarehouseDate(delivery.getExpectedWarehouseDate());
        deliveryDto.setReceivedAt(delivery.getReceivedAt());
        deliveryDto.setShippingMethod(delivery.getShippingMethod());
        deliveryDto.setTrackingNumber(delivery.getTrackingNumber());
        deliveryDto.setInTransitDate(delivery.getInTransitDate());
        deliveryDto.setShippingLocationId(delivery.getShippingLocation() == null
                ? null : delivery.getShippingLocation().getId());
        deliveryDto.setShippingLocationName(delivery.getShippingLocationNameSnapshot());
        deliveryDto.setStatus(delivery.getStatus());
        deliveryDto.setContainerStatus(delivery.getContainerStatus());
        deliveryDto.setContainers(delivery.getContainers().stream()
                .sorted(java.util.Comparator.comparing(com.nutalig.entity.ShippingContainerEntity::getId))
                .map(container -> {
                    var ref = new PurchaseOrderDeliveryDto.ContainerReference();
                    ref.setId(container.getId());
                    ref.setContainerJobNo(container.getContainerJobNo());
                    ref.setContainerNo(container.getContainerNo());
                    return ref;
                }).toList());
        deliveryDto.setRemark(delivery.getRemark());
        deliveryDto.setCreatedBy(delivery.getCreatedBy());
        deliveryDto.setUpdatedBy(delivery.getUpdatedBy());
        deliveryDto.setCreatedDate(delivery.getCreatedDate());
        deliveryDto.setUpdatedDate(delivery.getUpdatedDate());
        deliveryDto.setScrapBoxes(delivery.getScrapBoxes().stream().map(box -> {
            PurchaseOrderDeliveryScrapBoxDto boxDto = new PurchaseOrderDeliveryScrapBoxDto();
            boxDto.setId(box.getId());
            boxDto.setSortOrder(box.getSortOrder());
            boxDto.setNetWeightKg(box.getNetWeightKg());
            boxDto.setGrossWeightKg(box.getGrossWeightKg());
            boxDto.setLengthCm(box.getLengthCm());
            boxDto.setWidthCm(box.getWidthCm());
            boxDto.setHeightCm(box.getHeightCm());
            boxDto.setCbmM3(box.getCbmM3());
            return boxDto;
        }).toList());
        deliveryDto.setDetails(delivery.getDetails().stream().map(detail -> {
            PurchaseOrderDeliveryDetailDto detailDto = new PurchaseOrderDeliveryDetailDto();
            detailDto.setId(detail.getId());
            detailDto.setScrapBoxId(detail.getScrapBox() == null ? null : detail.getScrapBox().getId());
            detailDto.setPurchaseOrderDetailId(detail.getPurchaseOrderDetail().getId());
            detailDto.setPurchaseOrderDetailComponentId(detail.getPurchaseOrderDetailComponent() == null
                    ? null : detail.getPurchaseOrderDetailComponent().getId());
            detailDto.setProductName(detail.getProductNameSnapshot());
            detailDto.setComponentName(detail.getComponentNameSnapshot());
            detailDto.setQuantity(detail.getQuantity());
            detailDto.setCartonCount(detail.getCartonCount());
            detailDto.setPiecesPerCarton(detail.getPiecesPerCarton());
            detailDto.setNetWeightKg(detail.getNetWeightKg());
            detailDto.setGrossWeightKg(detail.getGrossWeightKg());
            detailDto.setLengthCm(detail.getLengthCm());
            detailDto.setWidthCm(detail.getWidthCm());
            detailDto.setHeightCm(detail.getHeightCm());
            detailDto.setCbmM3(detail.getCbmM3());
            detailDto.setSortOrder(detail.getSortOrder());
            return detailDto;
        }).toList());
        return deliveryDto;
    }

    public static PurchaseOrderDeliverySearchDto toSearchDto(PurchaseOrderDeliveryEntity delivery) {
        PurchaseOrderDeliverySearchDto dto = populate(new PurchaseOrderDeliverySearchDto(), delivery);
        var po = delivery.getPurchaseOrder();
        dto.setSalesOrderNo(po.getSalesOrder() == null ? null : po.getSalesOrder().getSalesOrderNo());
        dto.setSupplierId(po.getSupplier() == null ? null : po.getSupplier().getId());
        dto.setSupplierName(StringUtils.defaultIfBlank(po.getSupplierNameSnapshot(),
                po.getSupplier() == null ? null : po.getSupplier().getSupplierName()));
        dto.setTotalQuantity(dto.getDetails().stream().map(PurchaseOrderDeliveryDetailDto::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalCartons(sumIfComplete(Stream.concat(
                dto.getDetails().stream().filter(item -> item.getScrapBoxId() == null)
                        .map(PurchaseOrderDeliveryDetailDto::getCartonCount),
                dto.getScrapBoxes().stream().map(box -> BigDecimal.ONE)).toList()));
        dto.setTotalCbm(sumIfComplete(Stream.concat(
                dto.getDetails().stream().filter(item -> item.getScrapBoxId() == null)
                        .map(PurchaseOrderDeliveryDetailDto::getCbmM3),
                dto.getScrapBoxes().stream().map(PurchaseOrderDeliveryScrapBoxDto::getCbmM3)).toList()));
        return dto;
    }

    private static BigDecimal sumIfComplete(List<BigDecimal> values) {
        return values.stream().anyMatch(value -> value == null) ? null
                : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
