package com.nutalig.service;

import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderDeliveryRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.PurchaseOrderDeliverySearchDto;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.PurchaseOrderDeliveryMapper;
import com.nutalig.repository.PurchaseOrderDeliveryRepository;
import com.nutalig.repository.specification.PurchaseOrderDeliverySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseOrderDeliverySearchService {
    private final PurchaseOrderDeliveryRepository repository;
    private static final Set<String> SORT_FIELDS = Set.of("id", "deliveryNo", "shippedAt", "expectedWarehouseDate", "createdDate");

    @Transactional(readOnly = true)
    public Pageable<PurchaseOrderDeliverySearchDto> search(SearchPurchaseOrderDeliveryRequest request, PageableRequest pagination)
            throws InvalidRequestException {
        if (request == null) request = new SearchPurchaseOrderDeliveryRequest();
        if (pagination == null) pagination = new PageableRequest();
        if (request.getShippedDateStart() != null && request.getShippedDateEnd() != null
                && request.getShippedDateEnd().isBefore(request.getShippedDateStart()))
            throw new InvalidRequestException("shippedDateEnd must not be before shippedDateStart.");
        if (request.getExpectedWarehouseDateStart() != null && request.getExpectedWarehouseDateEnd() != null
                && request.getExpectedWarehouseDateEnd().isBefore(request.getExpectedWarehouseDateStart()))
            throw new InvalidRequestException("expectedWarehouseDateEnd must not be before expectedWarehouseDateStart.");
        int page = pagination.getPage() == null ? 1 : pagination.getPage();
        int size = pagination.getSize() == null ? 10 : pagination.getSize();
        if (page < 1 || size < 1 || size > 100)
            throw new InvalidRequestException("page must be at least 1 and size must be between 1 and 100.");
        String sortBy = pagination.getSortBy() == null ? "shippedAt" : pagination.getSortBy();
        if (!SORT_FIELDS.contains(sortBy)) throw new InvalidRequestException("Unsupported delivery sort field.");
        Sort.Direction direction = pagination.getSortDirection() == null ? Sort.Direction.DESC : pagination.getSortDirection();
        Sort sort = Sort.by(direction, sortBy);
        if (!"id".equals(sortBy)) sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        var results = repository.findAll(PurchaseOrderDeliverySpecification.search(request), PageRequest.of(page - 1, size, sort))
                .map(PurchaseOrderDeliveryMapper::toSearchDto);
        Pageable<PurchaseOrderDeliverySearchDto> response = new Pageable<>();
        response.setRecords(results.getContent());
        response.setPagination(Pagination.build(results));
        return response;
    }
}
