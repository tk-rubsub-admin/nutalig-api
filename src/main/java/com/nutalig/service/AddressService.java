package com.nutalig.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.dto.CountryDto;
import com.nutalig.dto.DistrictDto;
import com.nutalig.dto.ProvinceDto;
import com.nutalig.dto.SubDistrictDto;
import com.nutalig.entity.DistrictEntity;
import com.nutalig.entity.ProvinceEntity;
import com.nutalig.entity.SubDistrictEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.mapper.AddressMapper;
import com.nutalig.repository.DistrictRepository;
import com.nutalig.repository.ProvinceRepository;
import com.nutalig.repository.SubDistrictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final SubDistrictRepository subDistrictRepository;
    private final AddressMapper addressMapper;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${geothai.enabled:false}")
    private boolean geothaiEnabled;

    @Value("${geothai.base-url:https://geoth.thiti.dev/api}")
    private String geothaiBaseUrl;

    public List<ProvinceDto> getAllProvince() {
        if (geothaiEnabled) {
            List<ProvinceDto> provinces = getGeoThaiResponse("/provinces/all",
                    new ParameterizedTypeReference<List<ProvinceDto>>() {
                    }, "provinces");

            log.info("Get province size : {}", provinces.size());
            return provinces.stream()
                    .sorted(Comparator.comparing(ProvinceDto::getNameTh))
                    .toList();
        }

        log.info("Get Province in Thailand");

        List<ProvinceEntity> provinceEntities = provinceRepository.findAll();

        log.info("Get province size : {}", provinceEntities.size());

        return provinceEntities
                .stream()
                .map(addressMapper::toProvinceDto)
                .sorted(Comparator.comparing(ProvinceDto::getNameTh))
                .toList();
    }

    public List<CountryDto> getAllCountry() {
        log.info("Get countries from country.json");

        ClassPathResource resource = new ClassPathResource("country.json");

        try {
            List<CountryDto> countryDtos = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<CountryDto>>() {
                    }
            );

            log.info("Get country size : {}", countryDtos.size());
            return countryDtos;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load countries from country.json", e);
        }
    }

    public List<DistrictDto> getDistrictByProvince(String provinceId) {
        log.info("Get district by province id : {}", provinceId);

        if (geothaiEnabled) {
            String normalizedProvinceId = StringUtils.trimToEmpty(provinceId);
            List<GeoThaiProvinceWithDistricts> provinces;
            if (normalizedProvinceId.isEmpty()) {
                provinces = getGeoThaiResponse("/provinces-with-districts/all",
                        new ParameterizedTypeReference<List<GeoThaiProvinceWithDistricts>>() {
                        }, "districts");
            } else {
                GeoThaiProvinceWithDistricts province = getGeoThaiResponse(
                        "/provinces-with-districts/{provinceId}",
                        new ParameterizedTypeReference<GeoThaiProvinceWithDistricts>() {
                        }, "districts", normalizedProvinceId);
                provinces = List.of(province);
            }

            List<DistrictDto> districts = provinces.stream()
                    .flatMap(province -> {
                        if (province.districts() == null) {
                            throw new IllegalStateException("Failed to load districts from GeoThai: missing districts");
                        }
                        return province.districts().stream().map(district -> {
                            district.setProvinceId(province.id());
                            return district;
                        });
                    })
                    .toList();
            log.info("Get district by province id : {} ,size : {}", provinceId, districts.size());
            return districts;
        }

        List<DistrictEntity> districtEntities = new ArrayList<>();
        if (StringUtils.isEmpty(provinceId)) {
            districtEntities = districtRepository.findAll();
        } else {
            districtEntities = districtRepository.findByProvinceId(provinceId);
        }
        log.info("Get district by province id : {} ,size : {}", provinceId, districtEntities.size());

        return districtEntities
                .stream()
                .map(addressMapper::toDistrictDto)
                .toList();
    }

    public List<SubDistrictDto> getSubDistrictByDistrict(String districtId) {
        log.info("Get SubDistrict by District id : {}", districtId);

        if (geothaiEnabled) {
            String normalizedDistrictId = StringUtils.trimToEmpty(districtId);
            List<GeoThaiDistrictWithSubDistricts> districts;
            if (normalizedDistrictId.isEmpty()) {
                districts = getGeoThaiResponse("/districts-with-subdistricts/all",
                        new ParameterizedTypeReference<List<GeoThaiDistrictWithSubDistricts>>() {
                        }, "subdistricts");
            } else {
                GeoThaiDistrictWithSubDistricts district = getGeoThaiResponse(
                        "/districts-with-subdistricts/{districtId}",
                        new ParameterizedTypeReference<GeoThaiDistrictWithSubDistricts>() {
                        }, "subdistricts", normalizedDistrictId);
                districts = List.of(district);
            }

            List<SubDistrictDto> subdistricts = districts.stream()
                    .flatMap(district -> {
                        if (district.subdistricts() == null) {
                            throw new IllegalStateException("Failed to load subdistricts from GeoThai: missing subdistricts");
                        }
                        return district.subdistricts().stream().map(subdistrict -> {
                            SubDistrictDto dto = new SubDistrictDto();
                            dto.setId(subdistrict.id());
                            dto.setDistrictId(district.id());
                            dto.setNameTh(subdistrict.nameTh());
                            dto.setNameEn(subdistrict.nameEn());
                            dto.setZipCode(subdistrict.zipCode());
                            return dto;
                        });
                    })
                    .toList();
            log.info("Get SubDistrict by District id : {} ,size : {}", districtId, subdistricts.size());
            return subdistricts;
        }

        List<SubDistrictEntity> SubDistrictEntities = new ArrayList<>();
        if (StringUtils.isEmpty(districtId)) {
            SubDistrictEntities = subDistrictRepository.findAll();
        } else {
            SubDistrictEntities = subDistrictRepository.findByDistrictId(districtId);
        }
        log.info("Get SubDistrict by District id : {} ,size : {}", districtId, SubDistrictEntities.size());

        return SubDistrictEntities
                .stream()
                .map(addressMapper::toSubDistrictDto)
                .toList();
    }

    public ProvinceEntity getProvinceEntity(String provinceId) throws DataNotFoundException {
        return provinceRepository.findById(provinceId)
                .orElseThrow(() -> new DataNotFoundException("Province id " + provinceId + " not found"));
    }

    public DistrictEntity getDistrictEntity(String districtId) throws DataNotFoundException {
        return districtRepository.findById(districtId)
                .orElseThrow(() -> new DataNotFoundException("District id " + districtId + " not found"));
    }

    public SubDistrictEntity getSubDistrictEntity(String SubDistrictId) throws DataNotFoundException {
        return subDistrictRepository.findById(SubDistrictId)
                .orElseThrow(() -> new DataNotFoundException("SubDistrict id " + SubDistrictId + " not found"));
    }

    public ProvinceEntity getProvinceEntityByName(String name) throws DataNotFoundException {
        return provinceRepository.findByNameTh(name)
                .orElseThrow(() -> new DataNotFoundException("Province " + name + " not found"));
    }

    public DistrictEntity getDistrictEntityByName(String name) throws DataNotFoundException {
        return districtRepository.findByNameTh(name)
                .orElseThrow(() -> new DataNotFoundException("District " + name + " not found"));
    }

    public SubDistrictEntity getSubDistrictEntityByName(String name) throws DataNotFoundException {
        return subDistrictRepository.findByNameTh(name)
                .orElseThrow(() -> new DataNotFoundException("SubDistrict " + name + " not found"));
    }

    public DistrictDto toDistrictDtoFromEntity(DistrictEntity entity) {
        return addressMapper.toDistrictDto(entity);
    }

    public ProvinceDto toProvinceDtoFromEntity(ProvinceEntity entity) {
        return addressMapper.toProvinceDto(entity);
    }

    public SubDistrictDto toSubDistrictDtoFromEntity(SubDistrictEntity entity) { return addressMapper.toSubDistrictDto(entity); }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeoThaiDistrictWithSubDistricts(String id, List<GeoThaiSubDistrict> subdistricts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeoThaiSubDistrict(String id, String nameTh, String nameEn, String zipCode) {
    }


    private <T> T getGeoThaiResponse(String path, ParameterizedTypeReference<T> responseType,
                                     String dataName, Object... uriVariables) {
        String url = StringUtils.stripEnd(geothaiBaseUrl, "/") + path;
        log.info("Get {} from GeoThai: {}", dataName, url);

        T response = restClient.get()
                .uri(url, uriVariables)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new IllegalStateException("Failed to load " + dataName + " from GeoThai: HTTP "
                            + clientResponse.getStatusCode().value());
                })
                .body(responseType);

        if (response == null) {
            throw new IllegalStateException("Failed to load " + dataName + " from GeoThai: empty response");
        }
        return response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeoThaiProvinceWithDistricts(String id, List<DistrictDto> districts) {
    }
}
