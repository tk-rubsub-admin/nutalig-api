package com.nutalig.controller.user;

import com.nutalig.controller.auth.response.InviteLineRegistrationResponse;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.LineAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/users")
public class AdminUserController {

    private final LineAuthService lineAuthService;

    @PostMapping("/{id}/invite-line-registration")
    public GeneralResponse<InviteLineRegistrationResponse> inviteLineRegistration(
            @PathVariable("id") String userId
    ) throws InvalidRequestException, DataNotFoundException {
        log.info("=== Start invite LINE registration for {} ===", userId);
        InviteLineRegistrationResponse response = lineAuthService.inviteLineRegistration(userId);
        log.info("=== End invite LINE registration for {} ===", userId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{id}/reset-line-binding")
    public GeneralResponse<Void> resetLineBinding(@PathVariable("id") String userId) throws DataNotFoundException {
        log.info("=== Start reset LINE binding for {} ===", userId);
        lineAuthService.resetLineBinding(userId);
        log.info("=== End reset LINE binding for {} ===", userId);
        return new GeneralResponse<>(SUCCESS, null);
    }
}
