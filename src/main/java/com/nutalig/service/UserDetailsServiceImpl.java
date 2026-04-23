package com.nutalig.service;

import com.nutalig.dto.UserDto;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.mapper.UserMapper;
import com.nutalig.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl {

    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) throws DataNotFoundException {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User with id : " + userId + " not found"));

        return enrichUser(userEntity);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByLineUserId(String lineUserId) throws DataNotFoundException {
        UserEntity userEntity = userRepository.findByLineUserId(lineUserId)
                .orElseThrow(() -> new DataNotFoundException("User with line user id : " + lineUserId + " not found"));

        return enrichUser(userEntity);
    }

    private UserDto enrichUser(UserEntity userEntity) {
        UserDto userDto = userMapper.toDto(userEntity);

        Set<String> perms = permissionService.getEffectivePermission(userDto);
        userDto.setAuthorities(permissionService.toAuthorities(perms));
        userDto.setPermissions(perms.stream().toList());
        return userDto;
    }

}
