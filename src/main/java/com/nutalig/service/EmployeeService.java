package com.nutalig.service;

import com.nutalig.constant.EmployeeStatus;
import com.nutalig.constant.SystemConstant;
import com.nutalig.controller.employee.request.CreateEmployeeRequest;
import com.nutalig.controller.employee.request.SearchEmployeeRequest;
import com.nutalig.controller.employee.request.UpdateEmployeeRequest;
import com.nutalig.controller.employee.response.SearchEmployeeResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.EmployeeDto;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.EmployeeMapper;
import com.nutalig.mapper.UserMapper;
import com.nutalig.repository.EmployeeProcurementMappingRepository;
import com.nutalig.repository.EmployeeRepository;
import com.nutalig.repository.SystemConfigRepository;
import com.nutalig.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.nutalig.repository.specification.EmployeeSpecification.employeeIdEqual;
import static com.nutalig.repository.specification.EmployeeSpecification.keywordContain;
import static com.nutalig.repository.specification.EmployeeSpecification.nameContain;
import static com.nutalig.repository.specification.EmployeeSpecification.positionEqual;
import static com.nutalig.repository.specification.EmployeeSpecification.statusEqual;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProcurementMappingRepository employeeProcurementMappingRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final EmployeeMapper employeeMapper;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Transactional
    public EmployeeDto createEmployee(CreateEmployeeRequest request) throws InvalidRequestException {
        log.info("Create employee request {}", request);

        validateCreateRequest(request);

        boolean existed = employeeRepository.findById(request.getEmployeeId().trim()).isPresent();
        if (existed) {
            throw new InvalidRequestException("Employee id " + request.getEmployeeId() + " already exists.");
        }

        EmployeeEntity entity = employeeMapper.toEntity(request);
        entity.setEmployeeId(request.getEmployeeId().trim());
        if (entity.getStatus() == null) {
            entity.setStatus(EmployeeStatus.ACTIVE);
        }
        entity.setTeam(getOptionalSystemConfig(SystemConstant.TEAM, request.getTeam(), "Team"));
        entity.setPosition(getOptionalSystemConfig(SystemConstant.POSITION, request.getPosition(), "Position"));

        entity = employeeRepository.save(entity);

        log.info("Create employee success id {}", entity.getEmployeeId());
        return employeeMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public SearchEmployeeResponse searchEmployee(SearchEmployeeRequest searchRequest, PageableRequest pageableRequest) {
        log.info("Search employee by criteria {} page {} size {}", searchRequest, pageableRequest.getPage(), pageableRequest.getSize());

        pageableRequest.setSortBy("employeeId");
        pageableRequest.setSortDirection(Sort.Direction.ASC);
        org.springframework.data.domain.Pageable pageable = pageableRequest.build();

        Page<EmployeeEntity> employeePage = employeeRepository.findAll(buildSearchCriteria(searchRequest), pageable);
        List<EmployeeDto> employees = enrichEmployees(employeePage.getContent().stream()
                .map(employeeMapper::toDto)
                .toList());

        SearchEmployeeResponse response = new SearchEmployeeResponse();
        response.setEmployees(employees);
        response.setPagination(Pagination.build(employeePage));
        return response;
    }

    @Transactional(readOnly = true)
    public com.nutalig.controller.response.Pageable<EmployeeDto> searchEmployees(Integer page, Integer size, String keyword) {
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedSize = size == null || size < 1 ? 10 : size;

        SearchEmployeeRequest searchRequest = new SearchEmployeeRequest();
        searchRequest.setKeyword(StringUtils.trimToNull(keyword));

        PageableRequest pageableRequest = new PageableRequest();
        pageableRequest.setPage(normalizedPage);
        pageableRequest.setSize(normalizedSize);

        SearchEmployeeResponse searchResponse = searchEmployee(searchRequest, pageableRequest);

        com.nutalig.controller.response.Pageable<EmployeeDto> response = new com.nutalig.controller.response.Pageable<>();
        response.setRecords(searchResponse.getEmployees());
        response.setPagination(searchResponse.getPagination());
        return response;
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(String employeeId) throws DataNotFoundException {
        EmployeeEntity entity = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new DataNotFoundException("Employee id " + employeeId + " not found."));

        EmployeeDto dto = employeeMapper.toDto(entity);
        userRepository.findByEmployeeEntity_EmployeeId(employeeId)
                .ifPresentOrElse(
                        user -> {
                            dto.setHasUser(Boolean.TRUE);
                            dto.setIsLineConnected(StringUtils.isNotBlank(user.getLineUserId()));
                            dto.setUserId(user.getId());
                            dto.setUserDto(userMapper.toDto(user));
                        },
                        () -> {
                            dto.setHasUser(Boolean.FALSE);
                            dto.setIsLineConnected(Boolean.FALSE);
                            dto.setUserId(null);
                            dto.setUserDto(null);
                        }
                );
        return dto;
    }

    @Transactional
    public EmployeeDto updateEmployee(String employeeId, UpdateEmployeeRequest request)
            throws DataNotFoundException, InvalidRequestException {
        log.info("Update employee id {} request {}", employeeId, request);

        EmployeeEntity entity = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new DataNotFoundException("Employee id " + employeeId + " not found."));

        if (request.getFirstNameTh() != null) {
            entity.setFirstNameTh(StringUtils.trimToNull(request.getFirstNameTh()));
        }
        if (request.getLastNameTh() != null) {
            entity.setLastNameTh(StringUtils.trimToNull(request.getLastNameTh()));
        }
        if (request.getNickName() != null) {
            entity.setNickName(StringUtils.trimToNull(request.getNickName()));
        }
        if (request.getPosition() != null) {
            entity.setPosition(getOptionalSystemConfig(SystemConstant.POSITION, request.getPosition(), "Position"));
        }
        if (request.getTeam() != null) {
            entity.setTeam(getOptionalSystemConfig(SystemConstant.TEAM, request.getTeam(), "Team"));
        }
        if (request.getPhoneNumber() != null) {
            entity.setPhoneNumber(StringUtils.trimToNull(request.getPhoneNumber()));
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getAdditional() != null) {
            entity.setAdditional(StringUtils.trimToNull(request.getAdditional()));
        }

        entity = employeeRepository.save(entity);

        log.info("Update employee success id {}", employeeId);
        return employeeMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getProcurementEmployeesBySalesEmployeeId(String salesEmployeeId) {
        log.info("Get procurement employees by sales employee id {}", salesEmployeeId);

        if (StringUtils.isBlank(salesEmployeeId)) {
            return List.of();
        }

        return employeeProcurementMappingRepository.findBySalesEmployee_EmployeeId(salesEmployeeId.trim()).stream()
                .filter(mapping -> mapping.getProcurementEmployee() != null)
                .collect(java.util.stream.Collectors.toMap(
                        mapping -> mapping.getProcurementEmployee().getEmployeeId(),
                        mapping -> mapping,
                        (left, right) -> Boolean.TRUE.equals(left.getIsDefault()) ? left : right,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(mapping ->
                        StringUtils.defaultString(mapping.getProcurementEmployee().getEmployeeId())))
                .map(mapping -> {
                    EmployeeDto dto = employeeMapper.toDto(mapping.getProcurementEmployee());
                    dto.setIsDefault(mapping.getIsDefault());
                    return dto;
                })
                .toList()
                .stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        this::enrichEmployees
                ));
    }

    private void validateCreateRequest(CreateEmployeeRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }
        if (StringUtils.isBlank(request.getEmployeeId())) {
            throw new InvalidRequestException("Employee id is required.");
        }
    }

    private Specification<EmployeeEntity> buildSearchCriteria(SearchEmployeeRequest request) {
        if (request == null) {
            return Specification.where(null);
        }

        return Specification.where(employeeIdEqual(request.getEmployeeIdEqual()))
                .and(nameContain(request.getNameContain()))
                .and(positionEqual(request.getPositionEqual()))
                .and(statusEqual(request.getStatusEqual()))
                .and(keywordContain(request.getKeyword()));
    }

    private List<EmployeeDto> enrichEmployees(List<EmployeeDto> employees) {
        if (employees == null || employees.isEmpty()) {
            return employees;
        }

        List<String> employeeIds = employees.stream()
                .map(EmployeeDto::getEmployeeId)
                .filter(StringUtils::isNotBlank)
                .toList();

        if (employeeIds.isEmpty()) {
            employees.forEach(this::clearUserFlags);
            return employees;
        }

        Map<String, UserEntity> userByEmployeeId = userRepository.findByEmployeeEntity_EmployeeIdIn(employeeIds).stream()
                .filter(user -> user.getEmployeeEntity() != null
                        && StringUtils.isNotBlank(user.getEmployeeEntity().getEmployeeId()))
                .collect(java.util.stream.Collectors.toMap(
                        user -> user.getEmployeeEntity().getEmployeeId(),
                        user -> user,
                        (left, right) -> left
                ));

        employees.forEach(employee -> applyUserFlags(employee, userByEmployeeId.get(employee.getEmployeeId())));
        return employees;
    }

    private void applyUserFlags(EmployeeDto employee, UserEntity user) {
        if (employee == null) {
            return;
        }
        if (user == null) {
            clearUserFlags(employee);
            return;
        }

        employee.setHasUser(Boolean.TRUE);
        employee.setIsLineConnected(StringUtils.isNotBlank(user.getLineUserId()));
        employee.setUserId(user.getId());
        employee.setUserDto(userMapper.toDto(user));
    }

    private void clearUserFlags(EmployeeDto employee) {
        if (employee == null) {
            return;
        }

        employee.setHasUser(Boolean.FALSE);
        employee.setIsLineConnected(Boolean.FALSE);
        employee.setUserId(null);
        employee.setUserDto(null);
    }

    private SystemConfigEntity getOptionalSystemConfig(SystemConstant groupCode, String code, String fieldName)
            throws InvalidRequestException {
        String trimmedCode = StringUtils.trimToNull(code);
        if (trimmedCode == null) {
            return null;
        }

        return systemConfigRepository.findByIdGroupCodeAndIdCode(groupCode, trimmedCode)
                .orElseThrow(() -> new InvalidRequestException(fieldName + " " + trimmedCode + " not found."));
    }
}
