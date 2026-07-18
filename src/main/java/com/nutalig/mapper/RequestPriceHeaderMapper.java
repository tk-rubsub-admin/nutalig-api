package com.nutalig.mapper;

import com.nutalig.controller.rfq.request.CreateRequestPriceHeaderRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceHeaderRequest;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CustomerMapper.class, SystemConfigMapper.class, ProductFamilyMapper.class, ProductSubtype1Mapper.class,
                ProductSubtype2Mapper.class, ProductMaterialMapper.class, SupplierMapper.class})
public interface RequestPriceHeaderMapper {

    ObjectMapper MOQ_OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "productFamily", source = "productFamilyEntity")
    @Mapping(target = "productSubtype1", source = "productUsage")
    @Mapping(target = "productSubType2", source = "systemMechanic")
    @Mapping(target = "rfqStatusTimeline", source = "statusTimelines")
    @Mapping(target = "requestedMoqs", source = "requestedMoq")
    @Mapping(target = "referenceRfq", source = "referenceRfq", qualifiedByName = "toReferenceDto")
    RfqHeaderDto toDto(RfqHeaderEntity entity);

    List<RfqHeaderDto> toDtoList(List<RfqHeaderEntity> entities);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requestedDate", ignore = true)
    @Mapping(target = "sales", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "referenceRfq", ignore = true)
    @Mapping(target = "rfqType", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "pictures", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "additionalCosts", ignore = true)
    @Mapping(target = "productUsage", ignore = true)
    @Mapping(target = "systemMechanic", ignore = true)
    @Mapping(target = "materialCode", ignore = true)
    @Mapping(target = "requestedMoq", source = "requestedMoqs")
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "saleOrderId", ignore = true)
    @Mapping(target = "requestInformation", ignore = true)
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
    @Mapping(target = "referenceRfq", ignore = true)
    @Mapping(target = "rfqType", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "pictures", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "additionalCosts", ignore = true)
    @Mapping(target = "productUsage", ignore = true)
    @Mapping(target = "systemMechanic", ignore = true)
    @Mapping(target = "materialCode", ignore = true)
    @Mapping(target = "requestedMoq", source = "requestedMoqs")
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "saleOrderId", ignore = true)
    @Mapping(target = "requestInformation", ignore = true)
    @Mapping(target = "confirmedDetailId", ignore = true)
    @Mapping(target = "confirmedTierId", ignore = true)
    @Mapping(target = "confirmedShippingMethod", ignore = true)
    @Mapping(target = "confirmedPrice", ignore = true)
    @Mapping(target = "confirmedDate", ignore = true)
    void updateEntityFromRequest(UpdateRequestPriceHeaderRequest request, @MappingTarget RfqHeaderEntity entity);

    RfqPicturesDto toPictureDto(RfqPicturesEntity entity);

    List<RfqPicturesDto> toPictureDtoList(List<RfqPicturesEntity> entities);

    RfqDetailDto toDetailDto(RfqDetailEntity entity);

    List<RfqDetailDto> toDetailDtoList(List<RfqDetailEntity> entities);

    RfqTierDto toTierDto(RfqTierEntity entity);

    List<RfqTierDto> toTierDtoList(List<RfqTierEntity> entities);

    RfqAdditionalCostDto toAdditionalCostDto(RfqAdditionalCostEntity entity);

    List<RfqAdditionalCostDto> toAdditionalCostDtoList(List<RfqAdditionalCostEntity> entities);

    @Mapping(target = "rfqId", source = "id.rfqId")
    @Mapping(target = "status", source = "id.status")
    @Mapping(target = "statusDatetime", source = "statusDatetime")
    RfqStatusTimelineDto toRfqStatusTimelineDto(RfqStatusTimelineEntity entity);

    List<RfqStatusTimelineDto> toRfqStatusTimelineDtoList(List<RfqStatusTimelineEntity> entities);

    SalesAccountDto toSalesDto(SalesEntity entity);

    @Named("toReferenceDto")
    @Mapping(target = "productFamily", source = "productFamilyEntity")
    @Mapping(target = "productSubtype1", source = "productUsage")
    @Mapping(target = "productSubType2", source = "systemMechanic")
    RfqReferenceDto toReferenceDto(RfqHeaderEntity entity);

    default String mapRequestedMoqs(List<BigDecimal> requestedMoqs) {
        if (requestedMoqs == null || requestedMoqs.isEmpty()) {
            return null;
        }

        List<BigDecimal> filtered = requestedMoqs.stream()
                .filter(value -> value != null)
                .toList();

        if (filtered.isEmpty()) {
            return null;
        }

        try {
            return MOQ_OBJECT_MAPPER.writeValueAsString(filtered);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot serialize requested MOQ.", exception);
        }
    }

    default List<BigDecimal> mapRequestedMoq(String requestedMoq) {
        if (requestedMoq == null || requestedMoq.isBlank()) {
            return new ArrayList<>();
        }

        try {
            return MOQ_OBJECT_MAPPER.readValue(requestedMoq, new TypeReference<List<BigDecimal>>() {
            });
        } catch (Exception exception) {
            return new ArrayList<>();
        }
    }
}
