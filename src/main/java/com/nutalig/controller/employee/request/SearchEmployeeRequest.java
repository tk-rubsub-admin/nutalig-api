package com.nutalig.controller.employee.request;

import com.nutalig.constant.EmployeeStatus;
import lombok.Data;

@Data
public class SearchEmployeeRequest {

    private String employeeIdEqual;
    private String nameContain;
    private String positionEqual;
    private EmployeeStatus statusEqual;
    private String keyword;
}
