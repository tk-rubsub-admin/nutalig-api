package com.nutalig.controller.user;

import com.nutalig.controller.user.request.CreateUserRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.UserDto;
import com.nutalig.dto.UserRoleDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserRoleRepository;
import com.nutalig.service.UserProfileService;
import com.nutalig.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final UserService userService;
    private final UserRoleRepository userRoleRepository;

    @PostMapping("/v1/users")
    public GeneralResponse<UserDto> createUser(@RequestBody CreateUserRequest request)
            throws InvalidRequestException, DataNotFoundException {
        log.info("===== Start create user for employee {} role {} =====", request.getEmployeeId(), request.getRoleCode());

        UserDto response = userService.createUser(request);

        log.info("===== End create user {} =====", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/user/profile")
    public GeneralResponse<UserDto> getUserProfile(Authentication authentication) throws DataNotFoundException {
        log.info("===== Start get user profile from token =====");
        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();
        log.info("===== End get user profile from token =====");
        return new GeneralResponse<>(SUCCESS, userDto);
    }

    @GetMapping("/v1/user/roles")
    public GeneralResponse<List<UserRoleDto>> getUserRoles() {
        log.info("===== Start get user roles =====");

        List<UserRoleDto> response = userRoleRepository.findAll().stream()
                .map(role -> {
                    UserRoleDto dto = new UserRoleDto();
                    dto.setRoleCode(role.getRoleCode());
                    dto.setRoleNameTh(role.getRoleNameTh());
                    dto.setRoleNameEn(role.getRoleNameEn());
                    return dto;
                })
                .toList();

        log.info("===== End get user roles size {} =====", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

}
