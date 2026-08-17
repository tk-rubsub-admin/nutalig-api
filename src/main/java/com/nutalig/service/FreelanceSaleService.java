package com.nutalig.service;

import com.nutalig.controller.freelancesale.request.CreateFreelanceSaleRequest;
import com.nutalig.controller.freelancesale.request.SearchFreelanceSaleRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.FreelanceSaleDto;
import com.nutalig.entity.FreelanceSaleEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.FreelanceSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelanceSaleService {

    private final FreelanceSaleRepository freelanceSaleRepository;

    @Transactional
    public FreelanceSaleDto createFreelanceSale(CreateFreelanceSaleRequest request) throws InvalidRequestException {
        log.info("Create freelance sale request {}", request);

        validateCreateRequest(request);

        FreelanceSaleEntity entity = new FreelanceSaleEntity();
        entity.setName(request.getName().trim());
        entity.setContactNumber(StringUtils.trimToNull(request.getContactNumber()));
        entity.setSaleCoverage(StringUtils.trimToNull(request.getSaleCoverage()));
        entity.setAdditional(StringUtils.trimToNull(request.getAdditional()));

        entity = freelanceSaleRepository.save(entity);

        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<FreelanceSaleDto> getFreelanceSales() {
        return freelanceSaleRepository.findAll(defaultSort()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Pageable<FreelanceSaleDto> searchFreelanceSales(SearchFreelanceSaleRequest request) {
        String keyword = request == null ? null : StringUtils.trimToNull(request.getKeyword());
        Specification<FreelanceSaleEntity> specification = buildSearchSpecification(keyword);
        Page<FreelanceSaleEntity> page = freelanceSaleRepository.findAll(specification, buildPageRequest(request));

        Pageable<FreelanceSaleDto> response = new Pageable<>();
        response.setPagination(Pagination.build(page));
        response.setRecords(page.stream()
                .map(this::toDto)
                .toList());
        return response;
    }

    private void validateCreateRequest(CreateFreelanceSaleRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }
        if (StringUtils.isBlank(request.getName())) {
            throw new InvalidRequestException("Name is required.");
        }
    }

    private FreelanceSaleDto toDto(FreelanceSaleEntity entity) {
        FreelanceSaleDto dto = new FreelanceSaleDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setContactNumber(entity.getContactNumber());
        dto.setSaleCoverage(entity.getSaleCoverage());
        dto.setAdditional(entity.getAdditional());
        return dto;
    }

    private Specification<FreelanceSaleEntity> buildSearchSpecification(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return Specification.where(null);
        }

        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("id")), "%" + normalizedKeyword + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + normalizedKeyword + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("contactNumber")), "%" + normalizedKeyword + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("saleCoverage")), "%" + normalizedKeyword + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("additional")), "%" + normalizedKeyword + "%")
        );
    }

    private Sort defaultSort() {
        return Sort.by(
                Sort.Order.asc("name").ignoreCase(),
                Sort.Order.asc("id").ignoreCase()
        );
    }

    private PageRequest buildPageRequest(SearchFreelanceSaleRequest request) {
        int page = Optional.ofNullable(request)
                .map(SearchFreelanceSaleRequest::getPage)
                .filter(value -> value != null && value > 0)
                .orElse(1);
        int size = Optional.ofNullable(request)
                .map(SearchFreelanceSaleRequest::getSize)
                .filter(value -> value != null && value > 0)
                .orElse(10);

        String sortBy = Optional.ofNullable(request)
                .map(SearchFreelanceSaleRequest::getSortBy)
                .map(StringUtils::trimToNull)
                .orElse(null);
        String sortDirection = Optional.ofNullable(request)
                .map(SearchFreelanceSaleRequest::getSortDirection)
                .map(StringUtils::trimToNull)
                .orElse(null);

        if (StringUtils.isNotBlank(sortBy) && StringUtils.isNotBlank(sortDirection)) {
            Sort.Direction direction = Sort.Direction.fromString(sortDirection);
            return PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
        }

        return PageRequest.of(page - 1, size, defaultSort());
    }
}
