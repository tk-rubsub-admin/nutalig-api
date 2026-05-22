package com.nutalig.mapper;

import com.nutalig.dto.ProductSubtype1Dto;
import com.nutalig.entity.ProductSubtype1Entity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductSubtype1Mapper {

    ProductSubtype1Dto toDto(ProductSubtype1Entity entity);

    ProductSubtype1Entity toEntity(ProductSubtype1Dto dto);

}
