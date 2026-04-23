package com.nutalig.controller.employee.request;

import com.nutalig.constant.EmployeeStatus;
import lombok.Data;

@Data
public class UpdateEmployeeRequest {

    private String firstNameTh;
    private String lastNameTh;
    private String nickName;
    private String position;
    private String phoneNumber;
    private EmployeeStatus status;
    private String additional;
    private String team;
}
