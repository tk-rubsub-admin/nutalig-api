package com.nutalig.mapper;

import com.nutalig.dto.ProductMaterialDto;
import com.nutalig.entity.ProductMaterialEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMaterialMapper {

    ProductMaterialDto toDto(ProductMaterialEntity entity);

    ProductMaterialEntity toEntity(ProductMaterialDto dto);

}
