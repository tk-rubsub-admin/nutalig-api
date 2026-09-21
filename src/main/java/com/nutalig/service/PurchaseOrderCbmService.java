package com.nutalig.service;

import com.nutalig.controller.purchaseorder.request.PurchaseOrderCbmPreviewRequest;
import com.nutalig.dto.PurchaseOrderCbmPreviewDto;
import com.nutalig.dto.PurchaseOrderPackageSnapshotDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.repository.RfqSupplierQuoteTierRepository;
import com.nutalig.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderCbmService {
    private static final BigDecimal CUBIC_CENTIMETERS_PER_CBM = new BigDecimal("1000000");

    private final SalesOrderRepository salesOrderRepository;
    private final RfqSupplierQuoteTierRepository supplierQuoteTierRepository;

    @Transactional(readOnly = true)
    public PurchaseOrderCbmPreviewDto preview(PurchaseOrderCbmPreviewRequest request) throws DataNotFoundException {
        SalesOrderEntity salesOrder = salesOrderRepository.findById(request.getSalesOrderNo())
                .orElseThrow(() -> new DataNotFoundException("Sales order " + request.getSalesOrderNo() + " not found."));
        Map<Long, SalesOrderDetailEntity> sourceById = salesOrder.getItems().stream()
                .collect(Collectors.toMap(SalesOrderDetailEntity::getId, Function.identity()));
        List<PurchaseOrderCbmPreviewRequest.Item> requestedItems = Optional.ofNullable(request.getItems()).orElse(List.of());
        Set<Long> tierIds = requestedItems.stream()
                .map(PurchaseOrderCbmPreviewRequest.Item::getSalesOrderDetailId)
                .map(sourceById::get)
                .filter(Objects::nonNull)
                .map(SalesOrderDetailEntity::getSupplierQuoteTierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, RfqSupplierQuoteTierEntity> tierById = tierIds.isEmpty()
                ? Map.of()
                : supplierQuoteTierRepository.findAllByIdIn(tierIds).stream()
                        .collect(Collectors.toMap(
                                RfqSupplierQuoteTierEntity::getId,
                                Function.identity(),
                                (first, duplicate) -> first
                        ));
        Map<Long, BigDecimal> quantityByTierId = requestedItems.stream()
                .map(item -> new RequestedItemContext(item, sourceById.get(item.getSalesOrderDetailId())))
                .filter(context -> context.source() != null && context.source().getSupplierQuoteTierId() != null)
                .filter(context -> context.request().getQuantity() != null)
                .collect(Collectors.toMap(
                        context -> context.source().getSupplierQuoteTierId(),
                        context -> context.request().getQuantity(),
                        BigDecimal::max
                ));
        Map<Long, Calculation> calculationByTierId = new HashMap<>();
        Set<Long> emittedTierIds = new HashSet<>();

        PurchaseOrderCbmPreviewDto response = new PurchaseOrderCbmPreviewDto();
        BigDecimal total = BigDecimal.ZERO;
        int unavailable = 0;
        for (PurchaseOrderCbmPreviewRequest.Item requestedItem : requestedItems) {
            PurchaseOrderCbmPreviewDto.Item item = new PurchaseOrderCbmPreviewDto.Item();
            item.setSalesOrderDetailId(requestedItem.getSalesOrderDetailId());
            item.setQuantity(requestedItem.getQuantity());
            SalesOrderDetailEntity source = sourceById.get(requestedItem.getSalesOrderDetailId());
            Long tierId = source == null ? null : source.getSupplierQuoteTierId();
            item.setSupplierQuoteTierId(tierId);
            Calculation calculation = tierId == null
                    ? calculate(null, requestedItem.getQuantity())
                    : calculationByTierId.computeIfAbsent(tierId, id -> calculate(
                            tierById.get(id),
                            quantityByTierId.get(id)
                    ));
            boolean isFirstItemInTier = tierId == null || emittedTierIds.add(tierId);
            item.setPackages(isFirstItemInTier ? calculation.packages() : List.of());
            item.setAvailable(calculation.totalCbm() != null);
            item.setTotalCbm(isFirstItemInTier ? calculation.totalCbm() : BigDecimal.ZERO.setScale(6));
            item.setReason(isFirstItemInTier
                    ? calculation.reason()
                    : calculation.totalCbm() == null
                            ? calculation.reason()
                            : "รวม CBM ไว้ที่รายการแรกของ Supplier Quote Tier เดียวกัน");
            if (calculation.totalCbm() == null && isFirstItemInTier) unavailable++;
            else if (isFirstItemInTier) total = total.add(calculation.totalCbm());
            response.getItems().add(item);
        }
        response.setTotalCbm(total.setScale(6, RoundingMode.HALF_UP));
        response.setUnavailableItemCount(unavailable);
        return response;
    }

    @Transactional(readOnly = true)
    public void snapshotFromSupplierQuote(PurchaseOrderDetailEntity detail) {
        detail.getPackages().clear();
        if (detail.getSupplierQuoteTierId() == null) return;
        supplierQuoteTierRepository.findById(detail.getSupplierQuoteTierId()).ifPresent(tier -> {
            Calculation calculation = calculate(tier, detail.getQuantity());
            calculation.packages().forEach(dto -> detail.addPackage(toEntity(dto)));
        });
    }

    public void copyAndRecalculateSnapshots(PurchaseOrderDetailEntity source, PurchaseOrderDetailEntity target) {
        if (source != null) {
            source.getPackages().forEach(item -> target.addPackage(copy(item)));
        }
        recalculateDetail(target);
    }

    public void recalculateDetail(PurchaseOrderDetailEntity detail) {
        detail.getPackages().forEach(item -> {
            clearCalculatedFields(item);
            if (Boolean.TRUE.equals(item.getSelectedForCalculation())) calculateSnapshot(item, detail.getQuantity());
        });
    }

    public void recalculateTotal(PurchaseOrderEntity purchaseOrder) {
        BigDecimal total = BigDecimal.ZERO;
        Map<Long, List<PurchaseOrderDetailEntity>> detailByTierId = purchaseOrder.getItems().stream()
                .filter(detail -> detail.getSupplierQuoteTierId() != null)
                .collect(Collectors.groupingBy(
                        PurchaseOrderDetailEntity::getSupplierQuoteTierId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Set<PurchaseOrderDetailEntity> groupedDetails = Collections.newSetFromMap(new IdentityHashMap<>());

        for (List<PurchaseOrderDetailEntity> tierDetails : detailByTierId.values()) {
            groupedDetails.addAll(tierDetails);
            PurchaseOrderDetailEntity owner = tierDetails.stream()
                    .filter(detail -> !detail.getPackages().isEmpty())
                    .findFirst()
                    .orElse(tierDetails.get(0));
            BigDecimal tierQuantity = tierDetails.stream()
                    .map(PurchaseOrderDetailEntity::getQuantity)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            for (PurchaseOrderDetailEntity detail : tierDetails) {
                boolean isOwner = detail == owner;
                detail.getPackages().forEach(item -> {
                    item.setSelectedForCalculation(isOwner);
                    clearCalculatedFields(item);
                    if (isOwner) calculateSnapshot(item, tierQuantity);
                });
            }
            BigDecimal tierTotal = sumPackageTotal(owner.getPackages());
            if (tierTotal != null) total = total.add(tierTotal);
        }

        for (PurchaseOrderDetailEntity detail : purchaseOrder.getItems()) {
            if (groupedDetails.contains(detail)) continue;
            recalculateDetail(detail);
            BigDecimal detailTotal = sumPackageTotal(detail.getPackages());
            if (detailTotal != null) total = total.add(detailTotal);
        }
        purchaseOrder.setTotalCbm(total.setScale(6, RoundingMode.HALF_UP));
    }

    private Calculation calculate(RfqSupplierQuoteTierEntity tier, BigDecimal quantity) {
        if (tier == null || tier.getQuoteDetail() == null) return new Calculation(null, "ไม่พบ Supplier Quote Tier", List.of());
        RfqSupplierQuoteDetailEntity detail = tier.getQuoteDetail();
        RfqSupplierQuoteEntity supplierQuote = detail.getSupplierQuote();
        if (supplierQuote == null) {
            return new Calculation(null, "ไม่พบ Supplier Quote", List.of());
        }
        List<SourcePackage> sources = supplierQuote.getPackages().stream()
                .sorted(Comparator
                        .comparing((RfqSupplierQuotePackageEntity item) -> Optional.ofNullable(item.getSortOrder()).orElse(Integer.MAX_VALUE))
                        .thenComparing(item -> Optional.ofNullable(item.getId()).orElse(Long.MAX_VALUE)))
                .map(item -> new SourcePackage(item.getId(), item.getPackageName(), item.getPackageDimension(), item.getPackageWeight(), item.getPackageCapacity(), item.getSortOrder()))
                .toList();
        if (sources.isEmpty()) {
            return new Calculation(null, "ไม่พบข้อมูลใน rfq_supplier_quote_package", List.of());
        }
        List<PurchaseOrderPackageSnapshotDto> snapshots = new ArrayList<>();
        sources.forEach(source -> snapshots.add(buildSnapshot(source, true, quantity)));
        BigDecimal total = sumPackageTotalDto(snapshots);
        return new Calculation(total, total == null ? "ข้อมูลขนาดกล่องหรือจำนวนชิ้นต่อกล่องไม่ครบ" : null, snapshots);
    }

    private BigDecimal sumPackageTotal(List<PurchaseOrderDetailPackageEntity> packages) {
        if (packages.isEmpty() || packages.stream().anyMatch(item -> item.getTotalCbm() == null)) return null;
        return packages.stream()
                .map(PurchaseOrderDetailPackageEntity::getTotalCbm)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPackageTotalDto(List<PurchaseOrderPackageSnapshotDto> packages) {
        if (packages.isEmpty() || packages.stream().anyMatch(item -> item.getTotalCbm() == null)) return null;
        return packages.stream()
                .map(PurchaseOrderPackageSnapshotDto::getTotalCbm)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private void clearCalculatedFields(PurchaseOrderDetailPackageEntity item) {
        item.setCartonCount(null);
        item.setCbmPerCarton(null);
        item.setTotalCbm(null);
    }

    private PurchaseOrderPackageSnapshotDto buildSnapshot(SourcePackage source, boolean selected, BigDecimal quantity) {
        PurchaseOrderPackageSnapshotDto dto = new PurchaseOrderPackageSnapshotDto();
        dto.setSourcePackageId(source.id());
        dto.setPackageName(source.name());
        dto.setPackageDimension(source.dimension());
        dto.setPackageWeight(source.weight());
        dto.setPackageCapacity(source.capacity());
        dto.setSortOrder(source.sortOrder());
        dto.setSelectedForCalculation(selected);
        BigDecimal[] dimensions = parseDimensions(source.dimension());
        BigDecimal capacity = parseCapacityValue(source.capacity());
        if (dimensions != null) {
            dto.setWidthCm(dimensions[0]);
            dto.setLengthCm(dimensions[1]);
            dto.setHeightCm(dimensions[2]);
        }
        dto.setCapacityQty(capacity);
        calculateSnapshot(dto, quantity);
        return dto;
    }

    private void calculateSnapshot(PurchaseOrderPackageSnapshotDto dto, BigDecimal quantity) {
        if (dto.getWidthCm() == null || dto.getLengthCm() == null || dto.getHeightCm() == null || dto.getCapacityQty() == null || quantity == null || quantity.signum() <= 0) return;
        long cartons = quantity.divide(dto.getCapacityQty(), 0, RoundingMode.CEILING).longValue();
        BigDecimal perCarton = dto.getWidthCm().multiply(dto.getLengthCm()).multiply(dto.getHeightCm())
                .divide(CUBIC_CENTIMETERS_PER_CBM, 6, RoundingMode.HALF_UP);
        dto.setCartonCount(cartons);
        dto.setCbmPerCarton(perCarton);
        dto.setTotalCbm(perCarton.multiply(BigDecimal.valueOf(cartons)).setScale(6, RoundingMode.HALF_UP));
    }

    private void calculateSnapshot(PurchaseOrderDetailPackageEntity entity, BigDecimal quantity) {
        PurchaseOrderPackageSnapshotDto dto = toDto(entity); calculateSnapshot(dto, quantity);
        entity.setCartonCount(dto.getCartonCount()); entity.setCbmPerCarton(dto.getCbmPerCarton()); entity.setTotalCbm(dto.getTotalCbm());
    }

    private BigDecimal[] parseDimensions(String value) {
        if (StringUtils.isBlank(value)) return null;
        String[] parts = value.trim().split("[xX×]");
        if (parts.length != 3) return null;
        BigDecimal[] result = {
                parseDimensionValue(parts[0]),
                parseDimensionValue(parts[1]),
                parseDimensionValue(parts[2])
        };
        return Arrays.stream(result).anyMatch(Objects::isNull) ? null : result;
    }

    private BigDecimal parseDimensionValue(String value) {
        String normalizedValue = StringUtils.trimToEmpty(value)
                .replaceFirst("(?i)\\s*cm\\.?\\s*$", "")
                .trim();
        return parsePositive(normalizedValue);
    }

    private BigDecimal parseCapacityValue(String value) {
        String normalizedValue = StringUtils.trimToEmpty(value)
                .replaceFirst("(?i)\\s*pcs\\.?\\s*$", "")
                .trim();
        return parsePositive(normalizedValue);
    }

    private BigDecimal parsePositive(String value) {
        try { BigDecimal result = new BigDecimal(StringUtils.trimToEmpty(value)); return result.signum() > 0 ? result : null; }
        catch (NumberFormatException exception) { return null; }
    }

    private PurchaseOrderDetailPackageEntity toEntity(PurchaseOrderPackageSnapshotDto dto) {
        PurchaseOrderDetailPackageEntity entity = new PurchaseOrderDetailPackageEntity();
        entity.setSourcePackageId(dto.getSourcePackageId()); entity.setPackageName(dto.getPackageName()); entity.setPackageDimension(dto.getPackageDimension());
        entity.setPackageWeight(dto.getPackageWeight()); entity.setPackageCapacity(dto.getPackageCapacity()); entity.setWidthCm(dto.getWidthCm());
        entity.setLengthCm(dto.getLengthCm()); entity.setHeightCm(dto.getHeightCm()); entity.setCapacityQty(dto.getCapacityQty());
        entity.setCartonCount(dto.getCartonCount()); entity.setCbmPerCarton(dto.getCbmPerCarton()); entity.setTotalCbm(dto.getTotalCbm());
        entity.setSelectedForCalculation(dto.getSelectedForCalculation()); entity.setSortOrder(dto.getSortOrder()); return entity;
    }

    private PurchaseOrderDetailPackageEntity copy(PurchaseOrderDetailPackageEntity source) { return toEntity(toDto(source)); }
    public PurchaseOrderPackageSnapshotDto toDto(PurchaseOrderDetailPackageEntity entity) {
        PurchaseOrderPackageSnapshotDto dto = new PurchaseOrderPackageSnapshotDto();
        dto.setId(entity.getId()); dto.setSourcePackageId(entity.getSourcePackageId()); dto.setPackageName(entity.getPackageName()); dto.setPackageDimension(entity.getPackageDimension());
        dto.setPackageWeight(entity.getPackageWeight()); dto.setPackageCapacity(entity.getPackageCapacity()); dto.setWidthCm(entity.getWidthCm()); dto.setLengthCm(entity.getLengthCm());
        dto.setHeightCm(entity.getHeightCm()); dto.setCapacityQty(entity.getCapacityQty()); dto.setCartonCount(entity.getCartonCount()); dto.setCbmPerCarton(entity.getCbmPerCarton());
        dto.setTotalCbm(entity.getTotalCbm()); dto.setSelectedForCalculation(entity.getSelectedForCalculation()); dto.setSortOrder(entity.getSortOrder()); return dto;
    }

    private record SourcePackage(Long id, String name, String dimension, String weight, String capacity, Integer sortOrder) {}
    private record RequestedItemContext(PurchaseOrderCbmPreviewRequest.Item request, SalesOrderDetailEntity source) {}
    private record Calculation(BigDecimal totalCbm, String reason, List<PurchaseOrderPackageSnapshotDto> packages) {}
}
