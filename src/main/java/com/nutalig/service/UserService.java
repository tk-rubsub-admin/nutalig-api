package com.nutalig.service;

import com.nutalig.constant.Status;
import com.nutalig.controller.user.request.CreateUserRequest;
import com.nutalig.dto.UserDto;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.entity.UserRoleEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.EmployeeRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmployeeRepository employeeRepository;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public UserDto createUser(CreateUserRequest request) throws InvalidRequestException, DataNotFoundException {
        validateCreateUserRequest(request);

        String employeeId = request.getEmployeeId().trim();
        String roleCode = request.getRoleCode().trim();

        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new DataNotFoundException("Employee id " + employeeId + " not found."));

        UserRoleEntity role = userRoleRepository.findById(roleCode)
                .orElseThrow(() -> new DataNotFoundException("Role code " + roleCode + " not found."));

        if (userRepository.findByEmployeeEntity_EmployeeId(employeeId).isPresent()) {
            throw new InvalidRequestException("Employee id " + employeeId + " already has user.");
        }

        UserEntity user = new UserEntity();
        user.setUserRoleEntity(role);
        user.setStatus(Status.PENDING_ACTIVATE);
        user.setCreatedBy(SYSTEM_ACTOR);
        user.setUpdatedBy(SYSTEM_ACTOR);
        user.setEmployeeEntity(employee);
        user.setIsVerified(Boolean.FALSE);

        user = userRepository.save(user);
        log.info("Create user success id {} for employee {}", user.getId(), employeeId);
        return userDetailsService.getUserById(user.getId());
    }

    private void validateCreateUserRequest(CreateUserRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }
        if (StringUtils.isBlank(request.getRoleCode())) {
            throw new InvalidRequestException("roleCode is required.");
        }
        if (StringUtils.isBlank(request.getEmployeeId())) {
            throw new InvalidRequestException("employeeId is required.");
        }
    }

    private String buildDisplayName(EmployeeEntity employee) {
        String firstName = StringUtils.trimToEmpty(employee.getFirstNameTh());
        String lastName = StringUtils.trimToEmpty(employee.getLastNameTh());
        String displayName = (firstName + " " + lastName).trim();

        if (StringUtils.isNotBlank(displayName)) {
            return displayName;
        }

        return employee.getEmployeeId();
    }
}
