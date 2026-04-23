package com.nutalig.mapper;

import com.nutalig.dto.DistrictDto;
import com.nutalig.dto.ProvinceDto;
import com.nutalig.dto.SubDistrictDto;
import com.nutalig.entity.DistrictEntity;
import com.nutalig.entity.ProvinceEntity;
import com.nutalig.entity.SubDistrictEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AddressMapper {

    @Mapping(source = "province.id", target = "provinceId")
    DistrictDto toDistrictDto(DistrictEntity entity);

    ProvinceDto toProvinceDto(ProvinceEntity entity);

    @Mapping(source = "district.id", target = "districtId")
    SubDistrictDto toSubDistrictDto(SubDistrictEntity entity);

}
