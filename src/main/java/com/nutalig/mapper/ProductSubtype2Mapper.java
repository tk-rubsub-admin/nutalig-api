package com.nutalig.mapper;

import com.nutalig.dto.ProductSubtype2Dto;
import com.nutalig.entity.ProductSubtype2Entity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductSubtype2Mapper {

    ProductSubtype2Dto toDto(ProductSubtype2Entity entity);

    ProductSubtype2Entity toEntity(ProductSubtype2Dto dto);

}
