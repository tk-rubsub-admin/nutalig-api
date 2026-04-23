package com.nutalig.mapper;

import com.nutalig.dto.ProductFamilyDto;
import com.nutalig.entity.ProductFamilyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {SystemConfigMapper.class})
public interface ProductFamilyMapper {

    ProductFamilyDto toDto(ProductFamilyEntity entity);

    ProductFamilyEntity toEntity(ProductFamilyDto dto);

}
