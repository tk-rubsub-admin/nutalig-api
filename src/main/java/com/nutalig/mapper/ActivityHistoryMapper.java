package com.nutalig.mapper;

import com.nutalig.dto.ActivityHistoryDto;
import com.nutalig.entity.ActivityHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ActivityHistoryMapper {

    ActivityHistoryDto toDto(ActivityHistoryEntity entity);

    List<ActivityHistoryDto> toDtoList(List<ActivityHistoryEntity> entities);
}
