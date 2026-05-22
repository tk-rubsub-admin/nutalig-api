package com.nutalig.mapper;

import com.nutalig.controller.supplier.request.CreateSupplierRequest;
import com.nutalig.dto.SupplierDto;
import com.nutalig.entity.SupplierContactEntity;
import com.nutalig.entity.SupplierEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SupplierMapper {

    SupplierDto toDto(SupplierEntity entity);

    com.nutalig.dto.SupplierContactDto toDto(SupplierContactEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    @Mapping(target = "capabilities", ignore = true)
    SupplierEntity toEntity(CreateSupplierRequest request);
}
