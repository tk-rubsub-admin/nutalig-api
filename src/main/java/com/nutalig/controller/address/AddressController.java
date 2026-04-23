package com.nutalig.controller.address;

import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.DistrictDto;
import com.nutalig.dto.ProvinceDto;
import com.nutalig.dto.SubDistrictDto;
import com.nutalig.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;


@Slf4j
@RestController
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/v1/provinces")
    public GeneralResponse<List<ProvinceDto>> getProvince() {
        log.info("=== Start get province ===");

        List<ProvinceDto> provinceDtos = addressService.getAllProvince();

        log.info("=== End get province ===");
        return new GeneralResponse<>(SUCCESS, provinceDtos);
    }

    @GetMapping("/v1/provinces/{provinceId}/districts")
    public GeneralResponse<List<DistrictDto>> getDistrictByProvince(@PathVariable("provinceId") String provinceId) {
        log.info("=== Start get amphure by province ===");

        List<DistrictDto> DistrictDtos = addressService.getDistrictByProvince(provinceId.trim());

        log.info("=== End get amphure by province ===");
        return new GeneralResponse<>(SUCCESS, DistrictDtos);
    }

    @GetMapping("/v1/provinces/{provinceId}/districts/{amphureId}/subdistricts")
    public GeneralResponse<List<SubDistrictDto>> getTumbonByProvinceAndAmphure(
            @PathVariable("provinceId") String provinceId,
            @PathVariable("amphureId") String amphureId) {
        log.info("=== Start get tumbon by province and amphure ===");

        List<SubDistrictDto> SubDistrictDtos = addressService.getSubDistrictByDistrict(amphureId.trim());

        log.info("=== End get tumbon by province and amphure ===");
        return new GeneralResponse<>(SUCCESS, SubDistrictDtos);
    }
}
