package com.nutalig.service;

import com.nutalig.constant.Effect;
import com.nutalig.dto.PermissionDto;
import com.nutalig.dto.RolePermissionDto;
import com.nutalig.dto.UserDto;
import com.nutalig.entity.PermissionEntity;
import com.nutalig.entity.RolePermissionEntity;
import com.nutalig.entity.UserPermissionOverrideEntity;
import com.nutalig.entity.UserRoleEntity;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.PermissionRepository;
import com.nutalig.repository.RolePermissionRepository;
import com.nutalig.repository.UserPermissionOverrideRepository;
import com.nutalig.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;

    public Set<String> getEffectivePermission(UserDto user) {
        log.info("Get Effective Permission for user : {}, role : {}", user.getId(), user.getRole().getRoleCode());
        String roleCode = user.getRole().getRoleCode();

        Set<String> perms = new HashSet<>(rolePermissionRepository.findPermissionCodesByRoleCode(roleCode));

        List<UserPermissionOverrideEntity> uop = userPermissionOverrideRepository.findByUserId(user.getId());
        Set<String> userAllow = uop.stream()
                .filter(x -> x.getEffect() == Effect.ALLOW)
                .map(UserPermissionOverrideEntity::getPermissionCode)
                .collect(Collectors.toSet());

        Set<String> userDeny = uop.stream()
                .filter(x -> x.getEffect() == Effect.DENY)
                .map(UserPermissionOverrideEntity::getPermissionCode)
                .collect(Collectors.toSet());

        perms.addAll(userAllow);
        perms.removeAll(userDeny);

        return perms;
    }

    public List<GrantedAuthority> toAuthorities(Set<String> perms) {
        return perms.stream()
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p))
                .toList();
    }

    public List<GrantedAuthority> toAuthorities(Set<String> perms, String roleCode) {
        Set<String> authorities = new HashSet<>(perms);
        if (roleCode != null && !roleCode.isBlank()) {
            authorities.add(roleCode);
            authorities.add("ROLE_" + roleCode);
        }
        return toAuthorities(authorities);
    }

    public List<RolePermissionDto> getAllRolePermissions() {
        Map<String, PermissionDto> permissionsByCode = permissionRepository.findAll().stream()
                .map(this::toPermissionDto)
                .collect(Collectors.toMap(PermissionDto::getCode, permission -> permission));

        Map<String, List<RolePermissionEntity>> rolePermissionsByRoleCode = rolePermissionRepository.findAll().stream()
                .collect(Collectors.groupingBy(RolePermissionEntity::getRoleCode));

        return userRoleRepository.findAll().stream()
                .sorted(Comparator.comparing(UserRoleEntity::getRoleCode))
                .map(role -> toRolePermissionDto(
                        role,
                        rolePermissionsByRoleCode.getOrDefault(role.getRoleCode(), List.of()),
                        permissionsByCode
                ))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public List<RolePermissionDto> updateRolePermissions(Map<String, Map<String, Boolean>> rolePermissionMap)
            throws InvalidRequestException {
        if (rolePermissionMap == null || rolePermissionMap.isEmpty()) {
            throw new InvalidRequestException("rolePermissions is required");
        }

        validateRolePermissionMap(rolePermissionMap);

        rolePermissionRepository.deleteByRoleCodeIn(rolePermissionMap.keySet());
        rolePermissionRepository.saveAll(toRolePermissionEntities(rolePermissionMap));

        return getAllRolePermissions();
    }

    private void validateRolePermissionMap(Map<String, Map<String, Boolean>> rolePermissionMap)
            throws InvalidRequestException {
        Set<String> roleCodes = rolePermissionMap.keySet();
        if (roleCodes.stream().anyMatch(roleCode -> roleCode == null || roleCode.isBlank())) {
            throw new InvalidRequestException("roleCode is required");
        }

        Set<String> existingRoleCodes = userRoleRepository.findAllById(roleCodes).stream()
                .map(UserRoleEntity::getRoleCode)
                .collect(Collectors.toSet());

        if (!existingRoleCodes.containsAll(roleCodes)) {
            Set<String> unknownRoleCodes = new HashSet<>(roleCodes);
            unknownRoleCodes.removeAll(existingRoleCodes);
            throw new InvalidRequestException("Unknown role codes: " + unknownRoleCodes);
        }

        Set<String> permissionCodes = new HashSet<>();
        for (Map.Entry<String, Map<String, Boolean>> entry : rolePermissionMap.entrySet()) {
            if (entry.getValue() == null) {
                throw new InvalidRequestException("permissions are required for role " + entry.getKey());
            }
            if (entry.getValue().keySet().stream().anyMatch(permissionCode -> permissionCode == null || permissionCode.isBlank())) {
                throw new InvalidRequestException("permissionCode is required for role " + entry.getKey());
            }
            permissionCodes.addAll(entry.getValue().keySet());
        }

        Set<String> existingPermissionCodes = permissionRepository.findAllById(permissionCodes).stream()
                .map(PermissionEntity::getCode)
                .collect(Collectors.toSet());

        if (!existingPermissionCodes.containsAll(permissionCodes)) {
            Set<String> unknownPermissionCodes = new HashSet<>(permissionCodes);
            unknownPermissionCodes.removeAll(existingPermissionCodes);
            throw new InvalidRequestException("Unknown permission codes: " + unknownPermissionCodes);
        }
    }

    private List<RolePermissionEntity> toRolePermissionEntities(Map<String, Map<String, Boolean>> rolePermissionMap) {
        List<RolePermissionEntity> entities = new ArrayList<>();

        rolePermissionMap.forEach((roleCode, permissions) -> permissions.forEach((permissionCode, allowed) -> {
            if (Boolean.TRUE.equals(allowed)) {
                RolePermissionEntity entity = new RolePermissionEntity();
                entity.setRoleCode(roleCode);
                entity.setPermissionCode(permissionCode);
                entities.add(entity);
            }
        }));

        return entities;
    }

    private RolePermissionDto toRolePermissionDto(
            UserRoleEntity role,
            List<RolePermissionEntity> rolePermissions,
            Map<String, PermissionDto> permissionsByCode
    ) {
        RolePermissionDto dto = new RolePermissionDto();
        dto.setRoleCode(role.getRoleCode());
        dto.setRoleNameTh(role.getRoleNameTh());
        dto.setRoleNameEn(role.getRoleNameEn());
        dto.setPermissions(rolePermissions.stream()
                .map(RolePermissionEntity::getPermissionCode)
                .map(permissionCode -> permissionsByCode.getOrDefault(permissionCode, toPermissionDto(permissionCode)))
                .sorted(Comparator.comparing(PermissionDto::getGroup, Comparator.nullsLast(String::compareTo))
                        .thenComparing(PermissionDto::getCode))
                .toList());
        return dto;
    }

    private PermissionDto toPermissionDto(PermissionEntity permission) {
        PermissionDto dto = new PermissionDto();
        dto.setCode(permission.getCode());
        dto.setNameTh(permission.getNameTh());
        dto.setNameEn(permission.getNameEn());
        dto.setGroup(permission.getGroup());
        return dto;
    }

    private PermissionDto toPermissionDto(String permissionCode) {
        PermissionDto dto = new PermissionDto();
        dto.setCode(permissionCode);
        return dto;
    }
}
