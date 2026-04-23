package com.nutalig.service;

import com.nutalig.constant.SystemConstant;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.sales.request.CreateSalesRequest;
import com.nutalig.controller.sales.request.SearchSalesRequest;
import com.nutalig.controller.sales.request.UpdateSalesRequest;
import com.nutalig.dto.SalesAccountDto;
import com.nutalig.entity.SalesEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.SalesRepository;
import com.nutalig.repository.SystemConfigRepository;
import com.nutalig.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.nutalig.repository.specification.SalesSpecification.keywordContain;
import static com.nutalig.repository.specification.SalesSpecification.salesIdEqual;
import static com.nutalig.repository.specification.SalesSpecification.typeEqual;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigMapper systemConfigMapper;

    @Transactional
    public SalesAccountDto createSales(CreateSalesRequest request) throws InvalidRequestException {
        log.info("Create sales request {}", request);

        validateCreateRequest(request);

        boolean existed = salesRepository.findById(request.getSalesId().trim()).isPresent();
        if (existed) {
            throw new InvalidRequestException("Sales id " + request.getSalesId() + " already exists.");
        }

        SalesEntity entity = new SalesEntity();
        entity.setSalesId(request.getSalesId().trim());
        entity.setType(getRequiredSystemConfig(SystemConstant.POSITION, request.getType(), "Sales type"));
        entity.setName(StringUtils.trimToNull(request.getName()));
        entity.setNickname(StringUtils.trimToNull(request.getNickname()));
        entity.setMobileNo(StringUtils.trimToNull(request.getMobileNo()));
        entity.setBankAccountNo(StringUtils.trimToNull(request.getBankAccountNo()));
        entity.setBankName(StringUtils.trimToNull(request.getBankName()));
        entity.setBankAccountName(StringUtils.trimToNull(request.getBankAccountName()));
        entity.setTeam(getOptionalSystemConfig(SystemConstant.TEAM, request.getTeam(), "Sales team"));

        entity = salesRepository.save(entity);

        log.info("Create sales success id {}", entity.getSalesId());
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public Pageable<SalesAccountDto> searchSales(SearchSalesRequest searchRequest, PageableRequest pageableRequest)  {
        log.info("Search sales by criteria {} page {} size {}", searchRequest, pageableRequest.getPage(), pageableRequest.getSize());

        org.springframework.data.domain.Pageable pageable = PageRequest.of(
                pageableRequest.getPage() - 1,
                pageableRequest.getSize(),
                Sort.by(Sort.Order.desc("team.id.code"))
                        .and(Sort.by(Sort.Order.asc("salesId")))
        );

        Page<SalesEntity> salesPage = salesRepository.findAll(buildSearchCriteria(searchRequest), pageable);
        List<SalesAccountDto> dtoList = salesPage.map(this::toDto).getContent();

        Pageable<SalesAccountDto> response = new Pageable<>();
        response.setRecords(dtoList);
        response.setPagination(Pagination.build(salesPage));

        return response;
    }

    @Transactional
    public SalesAccountDto updateSales(String salesId, UpdateSalesRequest request)
            throws DataNotFoundException, InvalidRequestException {
        log.info("Update sales id {} request {}", salesId, request);

        SalesEntity entity = salesRepository.findById(salesId)
                .orElseThrow(() -> new DataNotFoundException("Sales id " + salesId + " not found."));

        if (request.getType() != null) {
            entity.setType(getRequiredSystemConfig(SystemConstant.POSITION, request.getType(), "Sales type"));
        }
        if (request.getName() != null) {
            entity.setName(StringUtils.trimToNull(request.getName()));
        }
        if (request.getNickname() != null) {
            entity.setNickname(StringUtils.trimToNull(request.getNickname()));
        }
        if (request.getMobileNo() != null) {
            entity.setMobileNo(StringUtils.trimToNull(request.getMobileNo()));
        }
        if (request.getBankAccountNo() != null) {
            entity.setBankAccountNo(StringUtils.trimToNull(request.getBankAccountNo()));
        }
        if (request.getBankName() != null) {
            entity.setBankName(StringUtils.trimToNull(request.getBankName()));
        }
        if (request.getBankAccountName() != null) {
            entity.setBankAccountName(StringUtils.trimToNull(request.getBankAccountName()));
        }
        if (request.getTeam() != null) {
            entity.setTeam(getOptionalSystemConfig(SystemConstant.TEAM, request.getTeam(), "Sales team"));
        }

        entity = salesRepository.save(entity);

        log.info("Update sales success id {}", salesId);
        return toDto(entity);
    }

    private void validateCreateRequest(CreateSalesRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }
        if (StringUtils.isBlank(request.getSalesId())) {
            throw new InvalidRequestException("Sales id is required.");
        }
        if (request.getType() == null) {
            throw new InvalidRequestException("Sales type is required.");
        }
    }

    private Specification<SalesEntity> buildSearchCriteria(SearchSalesRequest request) {
        if (request == null) {
            return Specification.<SalesEntity>where(null);
        }

        return Specification.<SalesEntity>where(null)
                .and(salesIdEqual(request.getSalesIdEqual()))
                .and(typeEqual(request.getTypeEqual()))
                .and(keywordContain(request.getKeyword()));
    }

    private SalesAccountDto toDto(SalesEntity entity) {
        SalesAccountDto dto = new SalesAccountDto();
        dto.setSalesId(entity.getSalesId());
        dto.setType(systemConfigMapper.mapSystemConfig(entity.getType()));
        dto.setName(entity.getName());
        dto.setNickname(entity.getNickname());
        dto.setMobileNo(entity.getMobileNo());
        dto.setBankAccountNo(entity.getBankAccountNo());
        dto.setBankName(entity.getBankName());
        dto.setBankAccountName(entity.getBankAccountName());
        dto.setTeam(systemConfigMapper.mapSystemConfig(entity.getTeam()));
        return dto;
    }

    private SystemConfigEntity getRequiredSystemConfig(SystemConstant groupCode, String code, String fieldName)
            throws InvalidRequestException {
        if (StringUtils.isBlank(code)) {
            throw new InvalidRequestException(fieldName + " is required.");
        }
        return getOptionalSystemConfig(groupCode, code, fieldName);
    }

    private SystemConfigEntity getOptionalSystemConfig(SystemConstant groupCode, String code, String fieldName)
            throws InvalidRequestException {
        String trimmedCode = StringUtils.trimToNull(code);
        if (trimmedCode == null) {
            return null;
        }

        return systemConfigRepository.findByIdGroupCodeAndIdCode(groupCode, trimmedCode)
                .orElseThrow(() -> new InvalidRequestException(fieldName + " " + trimmedCode + " not found."));
    }
}
