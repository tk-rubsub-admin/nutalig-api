package com.nutalig.controller.user;

import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.user.request.UpdateSearchFieldVisibilityRequest;
import com.nutalig.dto.RoleSearchFieldVisibilityDto;
import com.nutalig.dto.SearchFieldDto;
import com.nutalig.dto.UserDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.SearchFieldVisibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchFieldVisibilityController {

    private final SearchFieldVisibilityService searchFieldVisibilityService;

    @GetMapping("/v1/me/search-fields")
    public GeneralResponse<List<SearchFieldDto>> getMySearchFields(
            @RequestParam(name = "screenCode") String screenCode,
            Authentication authentication
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("===== Start get my search fields screenCode {} =====", screenCode);
        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }

        UserDto user = (UserDto) authentication.getPrincipal();
        List<SearchFieldDto> response = searchFieldVisibilityService.getVisibleSearchFields(user, screenCode);

        log.info("===== End get my search fields size {} =====", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/admin/search-field-visibility")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public GeneralResponse<List<RoleSearchFieldVisibilityDto>> getSearchFieldVisibility(
            @RequestParam(name = "screenCode") String screenCode,
            @RequestParam(name = "roleCode", required = false) String roleCode
    ) throws InvalidRequestException {
        log.info("===== Start get search field visibility screenCode {} roleCode {} =====", screenCode, roleCode);

        List<RoleSearchFieldVisibilityDto> response = searchFieldVisibilityService
                .getRoleSearchFieldVisibility(roleCode, screenCode);

        log.info("===== End get search field visibility size {} =====", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/v1/admin/search-field-visibility")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public GeneralResponse<RoleSearchFieldVisibilityDto> updateSearchFieldVisibility(
            @RequestBody UpdateSearchFieldVisibilityRequest request,
            Authentication authentication
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("===== Start update search field visibility =====");
        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }

        UserDto user = (UserDto) authentication.getPrincipal();
        RoleSearchFieldVisibilityDto response = searchFieldVisibilityService
                .updateRoleSearchFieldVisibility(request, user);

        log.info("===== End update search field visibility =====");
        return new GeneralResponse<>(SUCCESS, response);
    }
}
