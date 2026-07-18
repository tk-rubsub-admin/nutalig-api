package com.nutalig.service;

import com.nutalig.controller.freelancesale.request.CreateFreelanceSaleRequest;
import com.nutalig.dto.FreelanceSaleDto;
import com.nutalig.entity.FreelanceSaleEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.FreelanceSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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
        return freelanceSaleRepository.findAll().stream()
                .sorted(
                        Comparator.comparing(FreelanceSaleEntity::getName, Comparator.nullsLast(String::compareToIgnoreCase))
                                .thenComparing(FreelanceSaleEntity::getId, Comparator.nullsLast(String::compareToIgnoreCase))
                )
                .map(this::toDto)
                .toList();
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
}
