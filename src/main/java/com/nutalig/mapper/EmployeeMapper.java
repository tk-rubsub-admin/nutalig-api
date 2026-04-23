package com.nutalig.mapper;

import com.nutalig.controller.employee.request.CreateEmployeeRequest;
import com.nutalig.dto.EmployeeDto;
import com.nutalig.entity.EmployeeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {SystemConfigMapper.class})
public interface EmployeeMapper {

    EmployeeDto toDto(EmployeeEntity entity);

    @Mapping(target = "employeeId", source = "employeeId")
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "position", ignore = true)
    EmployeeEntity toEntity(CreateEmployeeRequest request);
}
