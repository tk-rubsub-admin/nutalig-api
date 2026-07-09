package com.nutalig.mapper;

import com.nutalig.controller.supplier.request.CreateSupplierRequest;
import com.nutalig.dto.SupplierDto;
import com.nutalig.dto.SupplierShippingDestinationDto;
import com.nutalig.dto.SupplierShippingDto;
import com.nutalig.entity.SupplierContactEntity;
import com.nutalig.entity.SupplierEntity;
import com.nutalig.entity.SupplierShippingDestinationEntity;
import com.nutalig.entity.SupplierShippingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SupplierMapper {

    SupplierDto toDto(SupplierEntity entity);

    com.nutalig.dto.SupplierContactDto toDto(SupplierContactEntity entity);

    SupplierShippingDto toDto(SupplierShippingEntity entity);

    SupplierShippingDestinationDto toDto(SupplierShippingDestinationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    @Mapping(target = "capabilities", ignore = true)
    SupplierEntity toEntity(CreateSupplierRequest request);
}
