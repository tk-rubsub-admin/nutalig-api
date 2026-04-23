package com.nutalig.service;

import com.nutalig.constant.Effect;
import com.nutalig.dto.UserDto;
import com.nutalig.entity.UserPermissionOverrideEntity;
import com.nutalig.repository.RolePermissionRepository;
import com.nutalig.repository.UserPermissionOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;

    public Set<String> getEffectivePermission(UserDto user) {
        log.info("Get Effective Permission for user : {}, role : {}", user.getId(), user.getRole().getRoleCode());
        String roleCode = user.getRole().getRoleCode();;

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
}
