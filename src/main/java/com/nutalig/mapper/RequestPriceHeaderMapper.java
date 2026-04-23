package com.nutalig.mapper;

import com.nutalig.controller.rfq.request.CreateRequestPriceHeaderRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceHeaderRequest;
import com.nutalig.dto.RequestPriceHeaderDto;
import com.nutalig.dto.RequestPriceAdditionalCostDto;
import com.nutalig.dto.RequestPriceDetailDto;
import com.nutalig.dto.RequestPricePicturesDto;
import com.nutalig.dto.RequestPriceTierDto;
import com.nutalig.entity.RequestPriceAdditionalCostEntity;
import com.nutalig.entity.RequestPriceDetailEntity;
import com.nutalig.dto.SalesAccountDto;
import com.nutalig.entity.RequestPriceHeaderEntity;
import com.nutalig.entity.RequestPricePicturesEntity;
import com.nutalig.entity.RequestPriceTierEntity;
import com.nutalig.entity.SalesEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {CustomerMapper.class, SystemConfigMapper.class, ProductFamilyMapper.class})
public interface RequestPriceHeaderMapper {

    @Mapping(target = "productFamily", source = "productFamilyEntity")
    RequestPriceHeaderDto toDto(RequestPriceHeaderEntity entity);

    List<RequestPriceHeaderDto> toDtoList(List<RequestPriceHeaderEntity> entities);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requestedDate", ignore = true)
    @Mapping(target = "sales", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "pictures", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "additionalCosts", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    RequestPriceHeaderEntity toEntity(CreateRequestPriceHeaderRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sales", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "pictures", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "additionalCosts", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    void updateEntityFromRequest(UpdateRequestPriceHeaderRequest request, @MappingTarget RequestPriceHeaderEntity entity);

    RequestPricePicturesDto toPictureDto(RequestPricePicturesEntity entity);

    List<RequestPricePicturesDto> toPictureDtoList(List<RequestPricePicturesEntity> entities);

    RequestPriceDetailDto toDetailDto(RequestPriceDetailEntity entity);

    List<RequestPriceDetailDto> toDetailDtoList(List<RequestPriceDetailEntity> entities);

    RequestPriceTierDto toTierDto(RequestPriceTierEntity entity);

    List<RequestPriceTierDto> toTierDtoList(List<RequestPriceTierEntity> entities);

    RequestPriceAdditionalCostDto toAdditionalCostDto(RequestPriceAdditionalCostEntity entity);

    List<RequestPriceAdditionalCostDto> toAdditionalCostDtoList(List<RequestPriceAdditionalCostEntity> entities);

    SalesAccountDto toSalesDto(SalesEntity entity);
}
