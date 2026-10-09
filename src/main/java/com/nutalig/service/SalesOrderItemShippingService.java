package com.nutalig.service;

import com.nutalig.entity.RfqTierSplitEntity;
import com.nutalig.entity.SalesOrderDetailEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.RequestPriceTierSplitRepository;
import com.nutalig.utils.ShippingMethodUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesOrderItemShippingService {
    private final RequestPriceTierSplitRepository tierSplitRepository;

    public Map<Long, String> resolveShippingMethods(Collection<SalesOrderDetailEntity> items) {
        Set<Long> splitIds = items.stream().map(SalesOrderDetailEntity::getRfqTierSplitId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, RfqTierSplitEntity> splits = splitIds.isEmpty() ? Map.of()
                : tierSplitRepository.findAllById(splitIds).stream()
                .collect(Collectors.toMap(RfqTierSplitEntity::getId, split -> split));
        Map<Long, String> methods = new HashMap<>();
        for (SalesOrderDetailEntity item : items) {
            String method = item.getShippingMethod();
            if (item.getRfqTierSplitId() != null) {
                RfqTierSplitEntity split = splits.get(item.getRfqTierSplitId());
                if (split == null || split.getRequestPriceDetail() == null
                        || !Objects.equals(split.getRequestPriceDetail().getId(), item.getRfqDetailId())
                        || (split.getSupplier() != null && (item.getSupplier() == null
                        || !Objects.equals(split.getSupplier().getId(), item.getSupplier().getId())))
                        || ShippingMethodUtil.getShippingMethodCategory(split.getShippingMethod())
                        != ShippingMethodUtil.getShippingMethodCategory(item.getShippingMethod())) {
                    continue;
                }
                method = split.getShippingMethod();
            }
            if (ShippingMethodUtil.getShippingMethodCategory(method) != null) {
                methods.put(item.getId(), StringUtils.upperCase(StringUtils.trimToEmpty(method), Locale.ROOT));
            }
        }
        return methods;
    }

    public Selection selectItems(Collection<SalesOrderDetailEntity> items, List<Long> requestedIds,
                                 String supplierId, String shippingMethod) throws InvalidRequestException {
        if (requestedIds.isEmpty() || requestedIds.stream().anyMatch(Objects::isNull)
                || new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new InvalidRequestException("salesOrderDetailIds must contain unique sales order item IDs");
        }
        Map<Long, SalesOrderDetailEntity> byId = items.stream()
                .collect(Collectors.toMap(SalesOrderDetailEntity::getId, item -> item));
        List<SalesOrderDetailEntity> selected = new ArrayList<>();
        for (Long id : requestedIds) {
            SalesOrderDetailEntity item = byId.get(id);
            if (item == null || item.getSupplier() == null
                    || !StringUtils.equals(item.getSupplier().getId(), supplierId)) {
                throw new InvalidRequestException("Sales order item " + id + " does not belong to the selected supplier");
            }
            selected.add(item);
        }
        if (ShippingMethodUtil.getShippingMethodCategory(shippingMethod) == null) {
            throw new InvalidRequestException("Invalid purchase order shipping method");
        }
        boolean supplierHasSplit = items.stream().anyMatch(item -> item.getRfqTierSplitId() != null
                && item.getSupplier() != null && StringUtils.equals(item.getSupplier().getId(), supplierId));
        Map<Long, String> methods;
        if (supplierHasSplit) {
            methods = resolveShippingMethods(selected);
            for (SalesOrderDetailEntity item : selected) {
                if (!StringUtils.equals(methods.get(item.getId()), shippingMethod)) {
                    throw new InvalidRequestException("Sales order item " + item.getId()
                            + " does not match the selected shipping method or its tier split cannot be resolved");
                }
            }
        } else {
            // A supplier without split items uses the transport selected on the PO form.
            methods = selected.stream().collect(Collectors.toMap(SalesOrderDetailEntity::getId,
                    item -> shippingMethod));
        }
        selected.sort(Comparator.comparing(item -> Optional.ofNullable(item.getLineNo()).orElse(0)));
        return new Selection(selected, methods);
    }

    public record Selection(List<SalesOrderDetailEntity> items, Map<Long, String> shippingMethods) {}
}
