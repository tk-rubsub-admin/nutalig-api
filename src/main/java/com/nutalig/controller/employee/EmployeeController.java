package com.nutalig.controller.employee;

import com.nutalig.controller.employee.request.CreateEmployeeRequest;
import com.nutalig.controller.employee.request.SearchEmployeeRequest;
import com.nutalig.controller.employee.request.UpdateEmployeeRequest;
import com.nutalig.controller.employee.response.SearchEmployeeResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.response.Pageable;
import com.nutalig.dto.EmployeeDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/search")
    public GeneralResponse<SearchEmployeeResponse> searchEmployees(
            @RequestBody(required = false) SearchEmployeeRequest request,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search employees page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        SearchEmployeeResponse response = employeeService.searchEmployee(request, pageableRequest);

        log.info("=== End search employees page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
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

    @GetMapping("/check-existing")
    public GeneralResponse<?> checkExistingEmployeeId(@RequestParam("employeeId") String employeeId) {
        log.info("=== Start check existing employee id {} ===", employeeId);

        boolean exists = employeeService.checkExistingEmployeeId(employeeId);
        record CheckExistingEmployeeResponse(boolean exists) {
        }

        log.info("=== End check existing employee id {} ===", employeeId);
        return new GeneralResponse<>(SUCCESS, new CheckExistingEmployeeResponse(exists));
    }

    @PatchMapping("/{employeeId}")
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
