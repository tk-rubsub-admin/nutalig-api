package com.nutalig.mapper;

import com.nutalig.dto.SlaConfigDto;
import com.nutalig.entity.SlaConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SlaConfigMapper {

    SlaConfigDto toDto(SlaConfigEntity entity);

    List<SlaConfigDto> toDtoList(List<SlaConfigEntity> entities);
}
