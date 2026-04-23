package com.nutalig.controller.employee.response;

import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.EmployeeDto;
import lombok.Data;

import java.util.List;

@Data
public class SearchEmployeeResponse {

    private List<EmployeeDto> employees;
    private Pagination pagination;
}
