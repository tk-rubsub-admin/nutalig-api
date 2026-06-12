package com.nutalig.service;

import com.nutalig.controller.user.request.UpdateSearchFieldVisibilityRequest;
import com.nutalig.dto.RoleSearchFieldVisibilityDto;
import com.nutalig.dto.SearchFieldDto;
import com.nutalig.dto.UserDto;
import com.nutalig.entity.RoleSearchFieldVisibilityEntity;
import com.nutalig.entity.SearchFieldEntity;
import com.nutalig.entity.UserRoleEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.RoleSearchFieldVisibilityRepository;
import com.nutalig.repository.SearchFieldRepository;
import com.nutalig.repository.SearchScreenRepository;
import com.nutalig.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchFieldVisibilityService {

    private final SearchScreenRepository searchScreenRepository;
    private final SearchFieldRepository searchFieldRepository;
    private final RoleSearchFieldVisibilityRepository roleSearchFieldVisibilityRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionService permissionService;

    public List<SearchFieldDto> getVisibleSearchFields(UserDto user, String screenCode) throws InvalidRequestException {
        validateScreenCode(screenCode);

        String roleCode = permissionService.getRoleCode(user);
        if (roleCode == null || roleCode.isBlank()) {
            return List.of();
        }

        Map<String, SearchFieldEntity> activeFieldsByCode = searchFieldRepository
                .findByScreenCodeAndActiveTrueOrderBySortOrderAscFieldCodeAsc(screenCode)
                .stream()
                .collect(Collectors.toMap(SearchFieldEntity::getFieldCode, field -> field));

        return roleSearchFieldVisibilityRepository.findByRoleCodeAndScreenCodeAndVisibleTrue(roleCode, screenCode).stream()
                .map(RoleSearchFieldVisibilityEntity::getFieldCode)
                .map(activeFieldsByCode::get)
                .filter(field -> field != null)
                .sorted(Comparator.comparing(SearchFieldEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SearchFieldEntity::getFieldCode))
                .map(field -> toSearchFieldDto(field, true))
                .toList();
    }

    public List<RoleSearchFieldVisibilityDto> getRoleSearchFieldVisibility(String roleCode, String screenCode)
            throws InvalidRequestException {
        validateScreenCode(screenCode);

        List<UserRoleEntity> roles = resolveRoles(roleCode);
        List<SearchFieldEntity> fields = searchFieldRepository.findByScreenCodeAndActiveTrueOrderBySortOrderAscFieldCodeAsc(screenCode);

        Map<String, Map<String, Boolean>> visibilityByRole = roleSearchFieldVisibilityRepository
                .findByRoleCodeInAndScreenCodeIn(
                        roles.stream().map(UserRoleEntity::getRoleCode).toList(),
                        List.of(screenCode)
                )
                .stream()
                .collect(Collectors.groupingBy(
                        RoleSearchFieldVisibilityEntity::getRoleCode,
                        Collectors.toMap(
                                RoleSearchFieldVisibilityEntity::getFieldCode,
                                entity -> Boolean.TRUE.equals(entity.getVisible()),
                                (left, right) -> right
                        )
                ));

        return roles.stream()
                .sorted(Comparator.comparing(UserRoleEntity::getRoleCode))
                .map(role -> toRoleVisibilityDto(role, screenCode, fields, visibilityByRole.getOrDefault(role.getRoleCode(), Map.of())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleSearchFieldVisibilityDto updateRoleSearchFieldVisibility(
            UpdateSearchFieldVisibilityRequest request,
            UserDto updatedBy
    ) throws InvalidRequestException {
        validateUpdateRequest(request);

        roleSearchFieldVisibilityRepository.deleteByRoleCodeAndScreenCode(request.getRoleCode(), request.getScreenCode());
        roleSearchFieldVisibilityRepository.saveAll(toVisibilityEntities(request, updatedBy));

        return getRoleSearchFieldVisibility(request.getRoleCode(), request.getScreenCode()).stream()
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Search field visibility update failed"));
    }

    private void validateScreenCode(String screenCode) throws InvalidRequestException {
        if (screenCode == null || screenCode.isBlank()) {
            throw new InvalidRequestException("screenCode is required");
        }
        if (!searchScreenRepository.existsById(screenCode)) {
            throw new InvalidRequestException("Unknown screenCode: " + screenCode);
        }
    }

    private List<UserRoleEntity> resolveRoles(String roleCode) throws InvalidRequestException {
        if (roleCode == null || roleCode.isBlank()) {
            return userRoleRepository.findAll();
        }
        UserRoleEntity role = userRoleRepository.findById(roleCode)
                .orElseThrow(() -> new InvalidRequestException("Unknown roleCode: " + roleCode));
        return List.of(role);
    }

    private void validateUpdateRequest(UpdateSearchFieldVisibilityRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("request is required");
        }
        validateScreenCode(request.getScreenCode());
        if (request.getRoleCode() == null || request.getRoleCode().isBlank()) {
            throw new InvalidRequestException("roleCode is required");
        }
        if (!userRoleRepository.existsById(request.getRoleCode())) {
            throw new InvalidRequestException("Unknown roleCode: " + request.getRoleCode());
        }
        if (request.getFields() == null) {
            throw new InvalidRequestException("fields is required");
        }
        if (request.getFields().keySet().stream().anyMatch(fieldCode -> fieldCode == null || fieldCode.isBlank())) {
            throw new InvalidRequestException("fieldCode is required");
        }

        Set<String> validFieldCodes = searchFieldRepository
                .findByScreenCodeAndActiveTrueOrderBySortOrderAscFieldCodeAsc(request.getScreenCode())
                .stream()
                .map(SearchFieldEntity::getFieldCode)
                .collect(Collectors.toSet());

        Set<String> requestedFieldCodes = new HashSet<>(request.getFields().keySet());
        requestedFieldCodes.removeAll(validFieldCodes);
        if (!requestedFieldCodes.isEmpty()) {
            throw new InvalidRequestException("Unknown field codes: " + requestedFieldCodes);
        }
    }

    private List<RoleSearchFieldVisibilityEntity> toVisibilityEntities(
            UpdateSearchFieldVisibilityRequest request,
            UserDto updatedBy
    ) {
        List<RoleSearchFieldVisibilityEntity> entities = new ArrayList<>();
        request.getFields().forEach((fieldCode, visible) -> {
            if (Boolean.TRUE.equals(visible)) {
                RoleSearchFieldVisibilityEntity entity = new RoleSearchFieldVisibilityEntity();
                entity.setRoleCode(request.getRoleCode());
                entity.setScreenCode(request.getScreenCode());
                entity.setFieldCode(fieldCode);
                entity.setVisible(true);
                entity.setUpdatedBy(updatedBy == null ? null : updatedBy.getId());
                entities.add(entity);
            }
        });
        return entities;
    }

    private RoleSearchFieldVisibilityDto toRoleVisibilityDto(
            UserRoleEntity role,
            String screenCode,
            List<SearchFieldEntity> fields,
            Map<String, Boolean> visibilityByFieldCode
    ) {
        RoleSearchFieldVisibilityDto dto = new RoleSearchFieldVisibilityDto();
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleNameTh(role.getRoleNameTh());
        dto.setRoleNameEn(role.getRoleNameEn());
        dto.setScreenCode(screenCode);
        dto.setFields(fields.stream()
                .map(field -> toSearchFieldDto(field, Boolean.TRUE.equals(visibilityByFieldCode.get(field.getFieldCode()))))
                .toList());
        return dto;
    }

    private SearchFieldDto toSearchFieldDto(SearchFieldEntity field, boolean visible) {
        SearchFieldDto dto = new SearchFieldDto();
        dto.setScreenCode(field.getScreenCode());
        dto.setFieldCode(field.getFieldCode());
        dto.setLabelTh(field.getLabelTh());
        dto.setLabelEn(field.getLabelEn());
        dto.setInputType(field.getInputType());
        dto.setSortOrder(field.getSortOrder());
        dto.setVisible(visible);
        return dto;
    }
}
