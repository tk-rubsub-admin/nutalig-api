package com.nutalig.mapper;

import com.nutalig.dto.SystemConfigDto;
import com.nutalig.entity.SystemConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SystemConfigMapper {

    @Mapping(source = "id.groupCode", target = "groupCode")
    @Mapping(source = "id.code", target = "code")
    SystemConfigDto toDto(SystemConfigEntity entity);

    @Mapping(source = "groupCode", target = "id.groupCode")
    @Mapping(source = "code", target = "id.code")
    SystemConfigEntity toEntity(SystemConfigDto dto);

    List<SystemConfigDto> toDtoList(List<SystemConfigEntity> entityList);

    @Named("mapSystemConfig")
    default SystemConfigDto mapSystemConfig(SystemConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        SystemConfigDto dto = new SystemConfigDto();
        dto.setCode(entity.getId().getCode());
        dto.setGroupCode(entity.getId().getGroupCode());
        dto.setNameTh(entity.getNameTh());
        dto.setNameEn(entity.getNameEn());
        return dto;
    }

}
