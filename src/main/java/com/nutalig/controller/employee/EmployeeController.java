package com.nutalig.controller.employee;

import com.nutalig.controller.employee.request.CreateEmployeeRequest;
import com.nutalig.controller.employee.request.SearchEmployeeRequest;
import com.nutalig.controller.employee.request.UpdateEmployeeRequest;
import com.nutalig.controller.employee.response.SearchEmployeeResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.EmployeeDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public GeneralResponse<SearchEmployeeResponse> searchEmployee(
            SearchEmployeeRequest searchRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search employee page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        SearchEmployeeResponse response = employeeService.searchEmployee(searchRequest, pageableRequest);

        log.info("=== End search employee page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
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
