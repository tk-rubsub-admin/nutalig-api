package com.nutalig.dto;

import com.nutalig.constant.EmployeeStatus;
import lombok.Data;

@Data
public class EmployeeDto {

    private String employeeId;
    private String firstNameTh;
    private String lastNameTh;
    private String nickName;
    private SystemConfigDto position;
    private String phoneNumber;
    private EmployeeStatus status;
    private String additional;
    private SystemConfigDto team;
    private Boolean isDefault;
    private Boolean hasUser;
    private Boolean isLineConnected;
    private String userId;
    private UserDto userDto;

}
