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

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AppSessionService appSessionService;

    private static String DEFAULT_PASSWORD = "password1234";

    @Transactional
    public void onUserLogin(String userId) {
        log.info("On user login : {}", userId);
    }

    public void onUserLogout(String token) {
        log.info("On user logout by token");
        appSessionService.revokeSessionByToken(token);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByLineUserId(String lineUserId) throws DataNotFoundException {
        log.info("Get user by line user id {}", lineUserId);

        UserEntity userEntity = userRepository.findByLineUserId(lineUserId)
                .orElseThrow(() -> new DataNotFoundException("User with line user id " + lineUserId + " not found."));

        log.info("Get user with id {}", userEntity.getId());
        return userMapper.toDto(userEntity);
    }

    public String getNameFromId(String userId) {
        Optional<UserEntity> userEntityOptional = userRepository.findById(userId);

        if (userEntityOptional.isPresent()) {
            return userEntityOptional.get().getDisplayName();
        } else {
            return userId;
        }
    }

    @Transactional(readOnly = true)
    public String getRoleCodeFromId(String userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getUserRoleEntity)
                .map(role -> role.getRoleCode())
                .orElse(null);
    }

}
