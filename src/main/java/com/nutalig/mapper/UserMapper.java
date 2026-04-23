package com.nutalig.mapper;

import com.nutalig.dto.UserDto;
import com.nutalig.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "authorities", source = "userRoleEntity.roleCode")
    @Mapping(target = "role", source = "userRoleEntity")
    @Mapping(target = "employeeId", source = "employeeEntity.employeeId")
    UserDto toDto(UserEntity entity);

//    @Mapping(target = "username", source = "email")
//    UserEntity toEntity(CreateNewUserRequest request);

    default List<GrantedAuthority> mapAuthorities(String role) {
        return Arrays.stream(role.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

}
