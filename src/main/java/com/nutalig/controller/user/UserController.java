package com.nutalig.controller.user;

import com.nutalig.constant.UserTodoStatus;
import com.nutalig.constant.UserTodoType;
import com.nutalig.controller.request.DateTimeRangeModelRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.user.request.CreateMyCalendarEventRequest;
import com.nutalig.controller.user.request.CreateUserRequest;
import com.nutalig.controller.user.request.UpdateMyCalendarEventRequest;
import com.nutalig.controller.user.request.CreateUserTodoRequest;
import com.nutalig.dto.CalendarEventDto;
import com.nutalig.dto.RolePermissionDto;
import com.nutalig.dto.UserDto;
import com.nutalig.dto.UserRoleDto;
import com.nutalig.dto.UserTodoDto;
import com.nutalig.service.CalendarEventService;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.UserRoleRepository;
import com.nutalig.service.PermissionService;
import com.nutalig.service.UserProfileService;
import com.nutalig.service.UserService;
import com.nutalig.service.UserTodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final PermissionService permissionService;
    private final UserTodoService userTodoService;
    private final CalendarEventService calendarEventService;

    @PostMapping("/v1/users")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
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

    @GetMapping("/v1/me/permissions")
    public GeneralResponse<List<String>> getMyPermissions(Authentication authentication) throws DataNotFoundException {
        log.info("===== Start get my permissions =====");
        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();
        log.info("===== End get my permissions size {} =====", userDto.getPermissions().size());
        return new GeneralResponse<>(SUCCESS, userDto.getPermissions());
    }

    @GetMapping("/v1/user/roles")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
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

    @GetMapping("/v1/admin/role-permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public GeneralResponse<List<RolePermissionDto>> getAllRolePermissions() {
        log.info("===== Start get all role permissions =====");

        List<RolePermissionDto> response = permissionService.getAllRolePermissions();

        log.info("===== End get all role permissions size {} =====", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/v1/admin/role-permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public GeneralResponse<List<RolePermissionDto>> updateRolePermissions(
            @RequestBody Map<String, Map<String, Boolean>> request
    ) throws InvalidRequestException {
        log.info("===== Start update role permissions =====");

        List<RolePermissionDto> response = permissionService.updateRolePermissions(request);

        log.info("===== End update role permissions size {} =====", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/me/to-dos")
    public GeneralResponse<List<UserTodoDto>> getMyTodos(
            Authentication authentication,
            @RequestParam(required = false) List<UserTodoStatus> statuses,
            @RequestParam(required = false) UserTodoType todoType
    ) throws DataNotFoundException {
        log.info("=== Start get user todos statuses {} todoType {} ===", statuses, todoType);

        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();

        List<UserTodoDto> response = userTodoService.getTodosByUser(userDto.getId(), statuses, todoType);

        log.info("=== End get user todos user {} size {} ===", userDto.getId(), response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/v1/me/to-dos")
    public GeneralResponse<UserTodoDto> createMyTodo(
            Authentication authentication,
            @RequestBody CreateUserTodoRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start create user todo title {} ===", request != null ? request.getTitle() : null);

        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();

        UserTodoDto response = userTodoService.createTodoForUser(userDto.getId(), request);

        log.info("=== End create user todo {} by user {} ===", response.getId(), userDto.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/v1/me/to-dos/{id}/done")
    public GeneralResponse<UserTodoDto> markTodoAsDone(
            Authentication authentication,
            @PathVariable("id") Long todoId
    ) throws DataNotFoundException {
        log.info("=== Start mark user todo {} as done ===", todoId);

        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();

        UserTodoDto response = userTodoService.markTodoAsDone(userDto.getId(), todoId);

        log.info("=== End mark user todo {} as done by user {} ===", todoId, userDto.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/v1/me/calendar-events")
    public GeneralResponse<CalendarEventDto> createMyCalendarEvent(
            Authentication authentication,
            @RequestBody CreateMyCalendarEventRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start create my calendar event title {} ===", request != null ? request.getTitle() : null);

        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();

        CalendarEventDto response = calendarEventService.createMyCalendarEvent(userDto.getId(), request);

        log.info("=== End create my calendar event {} by user {} ===", response.getId(), userDto.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/me/calendar-event")
    public GeneralResponse<List<CalendarEventDto>> getMyCalendarEvents(
            Authentication authentication,
            @ModelAttribute DateTimeRangeModelRequest request
    ) throws DataNotFoundException {
        log.info("=== Start get my private calendar events start {} end {} ===", request.getStart(), request.getEnd());

        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();

        List<CalendarEventDto> response = calendarEventService.getMyPrivateCalendarEvents(userDto.getId(), request);

        log.info("=== End get my private calendar events user {} size {} ===", userDto.getId(), response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/v1/me/calendar-event/{id}")
    public GeneralResponse<CalendarEventDto> updateMyCalendarEvent(
            Authentication authentication,
            @PathVariable("id") Long eventId,
            @RequestBody UpdateMyCalendarEventRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start update my calendar event {} ===", eventId);

        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();

        CalendarEventDto response = calendarEventService.updateMyPrivateCalendarEvent(
                userDto.getId(),
                eventId,
                request
        );

        log.info("=== End update my calendar event {} by user {} ===", eventId, userDto.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/v1/me/calendar-event/{id}")
    public GeneralResponse<Boolean> deleteMyCalendarEvent(
            Authentication authentication,
            @PathVariable("id") Long eventId
    ) throws DataNotFoundException {
        log.info("=== Start delete my calendar event {} ===", eventId);

        if (authentication == null) {
            throw new DataNotFoundException("User not found");
        }
        UserDto userDto = (UserDto) authentication.getPrincipal();

        calendarEventService.deleteMyPrivateCalendarEvent(userDto.getId(), eventId);

        log.info("=== End delete my calendar event {} by user {} ===", eventId, userDto.getId());
        return new GeneralResponse<>(SUCCESS, true);
    }

//    @PostMapping("/api/users/notification-token")
//    public ResponseEntity<?> saveToken(@RequestBody NotificationTokenRequest request) {
//
//        notificationTokenService.save(
//                request.getUserId(),
//                request.getToken(),
//                request.getPlatform()
//
//        );
//
//        return ResponseEntity.ok().build();
//
//    }

}
