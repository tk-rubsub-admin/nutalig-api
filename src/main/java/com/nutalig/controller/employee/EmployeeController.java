package com.nutalig.controller.employee;

import com.nutalig.controller.employee.request.CreateEmployeeRequest;
import com.nutalig.controller.employee.request.UpdateEmployeeRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.dto.EmployeeDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public GeneralResponse<EmployeeDto> createEmployee(@RequestBody CreateEmployeeRequest request)
            throws InvalidRequestException {
        log.info("=== Start create employee {} ===", request.getEmployeeId());

        EmployeeDto response = employeeService.createEmployee(request);

        log.info("=== End create employee {} ===", response.getEmployeeId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping
    public GeneralResponse<Pageable<EmployeeDto>> searchEmployees(
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        log.info("=== Start search employees page {} size {} keyword {} ===", page, size, keyword);

        Pageable<EmployeeDto> response = employeeService.searchEmployees(page, size, keyword);

        log.info("=== End search employees page {} size {} ===", page, size);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}")
    public GeneralResponse<EmployeeDto> getEmployeeById(
            @PathVariable("id") String employeeId
    ) throws DataNotFoundException {
        log.info("=== Start get employee {} ===", employeeId);

        EmployeeDto response = employeeService.getEmployeeById(employeeId);

        log.info("=== End get employee {} ===", employeeId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PutMapping("/{employeeId}")
    public GeneralResponse<EmployeeDto> updateEmployee(
            @PathVariable("employeeId") String employeeId,
            @RequestBody UpdateEmployeeRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start update employee {} ===", employeeId);

        EmployeeDto response = employeeService.updateEmployee(employeeId, request);

        log.info("=== End update employee {} ===", employeeId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{salesEmployeeId}/procurement-employees")
    public GeneralResponse<List<EmployeeDto>> getProcurementEmployeesBySalesEmployeeId(
            @PathVariable("salesEmployeeId") String salesEmployeeId
    ) {
        log.info("=== Start get procurement employees by sales employee {} ===", salesEmployeeId);

        List<EmployeeDto> response = employeeService.getProcurementEmployeesBySalesEmployeeId(salesEmployeeId);

        log.info("=== End get procurement employees by sales employee {} size {} ===", salesEmployeeId, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }
}
