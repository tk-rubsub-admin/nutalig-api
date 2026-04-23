package com.nutalig.service;

import com.nutalig.constant.SystemConstant;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.systemconfig.request.CreateSystemConfigRequest;
import com.nutalig.controller.systemconfig.request.SearchSystemConfigRequest;
import com.nutalig.controller.systemconfig.request.UpdateSystemConfigRequest;
import com.nutalig.controller.systemconfig.response.GetAllSystemConfigResponse;
import com.nutalig.dto.SystemConfigDto;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.SystemConfigMapper;
import com.nutalig.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nutalig.repository.specification.SystemConfigSpecification.codeEqual;
import static com.nutalig.repository.specification.SystemConfigSpecification.groupCodeEqual;
import static com.nutalig.repository.specification.SystemConfigSpecification.keywordContain;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigMapper systemConfigMapper;

    public List<SystemConfigDto> getSystemConfigByGroupCode(SystemConstant groupCode) {
        log.info("Get system config by group code : {}", groupCode);

        List<SystemConfigEntity> systemConfigEntities = systemConfigRepository.findByIdGroupCodeOrderBySortAsc(groupCode);

        List<SystemConfigDto> systemConfigDtoList = systemConfigMapper.toDtoList(systemConfigEntities);

        log.info("Get system config by group code : {}, size : {}", groupCode, systemConfigEntities.size());
        return systemConfigDtoList;
    }

    public GetAllSystemConfigResponse getAllSystemConfig(SearchSystemConfigRequest searchRequest, PageableRequest pageableRequest) {
        log.info("Get all system config with criteria : {}, page : {}, size : {}",
                searchRequest, pageableRequest.getPage(), pageableRequest.getSize());

        Pageable pageable = buildPageable(pageableRequest);
        Page<SystemConfigDto> systemConfigDtoPage = systemConfigRepository.findAll(buildSearchCriteria(searchRequest), pageable)
                .map(systemConfigMapper::toDto);

        GetAllSystemConfigResponse response = new GetAllSystemConfigResponse();
        response.setSystemConfigList(systemConfigDtoPage.getContent());
        response.setPagination(Pagination.build(systemConfigDtoPage));

        log.info("Get all system config success, total records : {}", systemConfigDtoPage.getTotalElements());
        return response;
    }

    public SystemConfigDto createSystemConfig(CreateSystemConfigRequest request) throws InvalidRequestException {
        log.info("Create system config request : {}", request);

        boolean existed = systemConfigRepository.findByIdGroupCodeAndIdCode(request.getGroupCode(), request.getCode()).isPresent();
        if (existed) {
            throw new InvalidRequestException(
                    "System config groupCode " + request.getGroupCode() + " and code " + request.getCode() + " already exists."
            );
        }

        SystemConfigDto dto = new SystemConfigDto();
        dto.setGroupCode(request.getGroupCode());
        dto.setCode(request.getCode());
        dto.setNameTh(request.getNameTh());
        dto.setNameEn(request.getNameEn());
        dto.setSort(request.getSort());

        SystemConfigEntity entity = systemConfigMapper.toEntity(dto);
        entity = systemConfigRepository.save(entity);

        log.info("Create system config success groupCode : {}, code : {}", request.getGroupCode(), request.getCode());
        return systemConfigMapper.toDto(entity);
    }

    public SystemConfigDto updateSystemConfig(SystemConstant groupCode, String code, UpdateSystemConfigRequest request)
            throws DataNotFoundException {
        log.info("Update system config groupCode : {}, code : {}, request : {}", groupCode, code, request);

        SystemConfigEntity entity = systemConfigRepository.findByIdGroupCodeAndIdCode(groupCode, code)
                .orElseThrow(() -> new DataNotFoundException(
                        "System config groupCode " + groupCode + " and code " + code + " not found."
                ));

        if (request.getNameTh() != null) {
            entity.setNameTh(request.getNameTh());
        }

        if (request.getNameEn() != null) {
            entity.setNameEn(request.getNameEn());
        }

        if (request.getSort() != null) {
            entity.setSort(request.getSort());
        }

        entity = systemConfigRepository.save(entity);

        log.info("Update system config success groupCode : {}, code : {}", groupCode, code);
        return systemConfigMapper.toDto(entity);
    }

    public SystemConfigEntity getConfigEntity(SystemConstant groupCode, String code) {
        return systemConfigRepository.findByIdGroupCodeAndIdCode(groupCode, code)
                .orElse(null);
    }

    public SystemConfigEntity getConfigEntityByValue(SystemConstant groupCode, String value) {
        return systemConfigRepository.findByIdGroupCodeAndNameTh(groupCode, value)
                .orElse(null);
    }

    public List<SystemConfigEntity> toEntityList(List<SystemConfigDto> dtoList) {
        return dtoList.stream()
                .map(systemConfigMapper::toEntity)
                .toList();
    }

    public String getConfig(List<SystemConfigDto> config, String code) {
        return config.stream()
                .filter(c -> code.equalsIgnoreCase(c.getCode()))
                .findFirst()
                .map(SystemConfigDto::getNameTh)
                .orElse("");
    }

    private Pageable buildPageable(PageableRequest pageableRequest) {
        if (pageableRequest.getSortBy() != null && pageableRequest.getSortDirection() != null) {
            return pageableRequest.build();
        }

        Sort sort = Sort.by(Sort.Order.asc("id.groupCode"))
                .and(Sort.by(Sort.Order.asc("sort")))
                .and(Sort.by(Sort.Order.asc("id.code")));

        return PageRequest.of(pageableRequest.getPage() - 1, pageableRequest.getSize(), sort);
    }

    private Specification<SystemConfigEntity> buildSearchCriteria(SearchSystemConfigRequest request) {
        if (request == null) {
            return Specification.where(null);
        }

        return Specification.where(groupCodeEqual(request.getGroupCode()))
                .and(codeEqual(request.getCode()))
                .and(keywordContain(request.getKeyword()));
    }

}
