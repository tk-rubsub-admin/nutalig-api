package com.nutalig.controller.user;

import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.UserDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

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

}
