package com.nutalig.mapper;

import com.nutalig.controller.customer.request.CreateCustomerRequest;
import com.nutalig.dto.CustomerAddressDto;
import com.nutalig.dto.CustomerContactDto;
import com.nutalig.dto.CustomerDto;
import com.nutalig.entity.CustomerAddressEntity;
import com.nutalig.entity.CustomerContactEntity;
import com.nutalig.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {SystemConfigMapper.class})
public interface CustomerMapper {

    CustomerDto toDto(CustomerEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerType", ignore = true)
    @Mapping(target = "customerCreditTerm", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    CustomerEntity toEntity(CreateCustomerRequest request);

    @Mapping(target = "fullAddress", expression = "java(buildFullAddress(entity))")
    CustomerAddressDto toAddressDto(CustomerAddressEntity entity);

    CustomerContactDto toContactDto(CustomerContactEntity entity);

    default String buildFullAddress(CustomerAddressEntity address) {
        if (address == null) {
            return null;
        }

        boolean isBangkok = "กรุงเทพมหานคร".equals(address.getProvince());

        String subdistrictPrefix = isBangkok ? "แขวง" : "ตำบล";
        String districtPrefix = isBangkok ? "เขต" : "อำเภอ";

        StringBuilder sb = new StringBuilder();

        append(sb, address.getAddressLine1());
        append(sb, address.getAddressLine2());

        if (address.getSubdistrict() != null) {
            append(sb, subdistrictPrefix + address.getSubdistrict());
        }

        if (address.getDistrict() != null) {
            append(sb, districtPrefix + address.getDistrict());
        }

        if (address.getProvince() != null) {
            if (isBangkok) {
                append(sb, address.getProvince());
            } else {
                append(sb, "จังหวัด" + address.getProvince());
            }
        }

        append(sb, address.getPostcode());

        return sb.toString().trim();
    }

    default void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(value.trim());
        }
    }
}
