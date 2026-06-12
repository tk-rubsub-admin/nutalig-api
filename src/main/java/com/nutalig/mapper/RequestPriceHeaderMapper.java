package com.nutalig.mapper;

import com.nutalig.controller.rfq.request.CreateRequestPriceHeaderRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceHeaderRequest;
import com.nutalig.dto.RequestPriceHeaderDto;
import com.nutalig.dto.RequestPriceAdditionalCostDto;
import com.nutalig.dto.RequestPriceDetailDto;
import com.nutalig.dto.RequestPricePicturesDto;
import com.nutalig.dto.RequestPriceTierDto;
import com.nutalig.entity.*;
import com.nutalig.dto.SalesAccountDto;
import com.nutalig.entity.RfqTierEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CustomerMapper.class, SystemConfigMapper.class, ProductFamilyMapper.class, ProductSubtype1Mapper.class,
                ProductSubtype2Mapper.class, ProductMaterialMapper.class, SupplierMapper.class})
public interface RequestPriceHeaderMapper {

    @Mapping(target = "productFamily", source = "productFamilyEntity")
    @Mapping(target = "productSubtype1", source = "productUsage")
    @Mapping(target = "productSubType2", source = "systemMechanic")
    RequestPriceHeaderDto toDto(RfqHeaderEntity entity);

    List<RequestPriceHeaderDto> toDtoList(List<RfqHeaderEntity> entities);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requestedDate", ignore = true)
    @Mapping(target = "sales", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "pictures", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "additionalCosts", ignore = true)
    @Mapping(target = "productUsage", ignore = true)
    @Mapping(target = "systemMechanic", ignore = true)
    @Mapping(target = "materialCode", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "saleOrderId", ignore = true)
    @Mapping(target = "confirmedDetailId", ignore = true)
    @Mapping(target = "confirmedTierId", ignore = true)
    @Mapping(target = "confirmedShippingMethod", ignore = true)
    @Mapping(target = "confirmedPrice", ignore = true)
    @Mapping(target = "confirmedDate", ignore = true)
    RfqHeaderEntity toEntity(CreateRequestPriceHeaderRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sales", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "pictures", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "additionalCosts", ignore = true)
    @Mapping(target = "productUsage", ignore = true)
    @Mapping(target = "systemMechanic", ignore = true)
    @Mapping(target = "materialCode", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "saleOrderId", ignore = true)
    @Mapping(target = "confirmedDetailId", ignore = true)
    @Mapping(target = "confirmedTierId", ignore = true)
    @Mapping(target = "confirmedShippingMethod", ignore = true)
    @Mapping(target = "confirmedPrice", ignore = true)
    @Mapping(target = "confirmedDate", ignore = true)
    void updateEntityFromRequest(UpdateRequestPriceHeaderRequest request, @MappingTarget RfqHeaderEntity entity);

    RequestPricePicturesDto toPictureDto(RfqPicturesEntity entity);

    List<RequestPricePicturesDto> toPictureDtoList(List<RfqPicturesEntity> entities);

    RequestPriceDetailDto toDetailDto(RfqDetailEntity entity);

    List<RequestPriceDetailDto> toDetailDtoList(List<RfqDetailEntity> entities);

    RequestPriceTierDto toTierDto(RfqTierEntity entity);

    List<RequestPriceTierDto> toTierDtoList(List<RfqTierEntity> entities);

    RequestPriceAdditionalCostDto toAdditionalCostDto(RfqAdditionalCostEntity entity);

    List<RequestPriceAdditionalCostDto> toAdditionalCostDtoList(List<RfqAdditionalCostEntity> entities);

    SalesAccountDto toSalesDto(SalesEntity entity);
}
