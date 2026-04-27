package com.nutalig.dto;

import com.nutalig.constant.EmployeeStatus;
import com.nutalig.constant.Status;
import lombok.Data;

import java.time.ZonedDateTime;

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
    private String userId;
    private UserDto userDto;

}
