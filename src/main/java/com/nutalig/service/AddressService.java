package com.nutalig.service;

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
import org.springframework.stereotype.Service;

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

    public List<ProvinceDto> getAllProvince() {
        log.info("Get Province in Thailand");

        List<ProvinceEntity> provinceEntities = provinceRepository.findAll();

        log.info("Get province size : {}", provinceEntities.size());

        return provinceEntities
                .stream()
                .map(addressMapper::toProvinceDto)
                .sorted(Comparator.comparing(ProvinceDto::getNameTh))
                .toList();
    }

    public List<DistrictDto> getDistrictByProvince(String provinceId) {
        log.info("Get district by province id : {}", provinceId);

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
}
