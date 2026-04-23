package com.nutalig.controller.systemconfig;

import com.nutalig.constant.SystemConstant;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.systemconfig.request.CreateSystemConfigRequest;
import com.nutalig.controller.systemconfig.request.SearchSystemConfigRequest;
import com.nutalig.controller.systemconfig.request.UpdateSystemConfigRequest;
import com.nutalig.controller.systemconfig.response.GetAllSystemConfigResponse;
import com.nutalig.dto.SystemConfigDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/v1/system-configs")
    public GeneralResponse<GetAllSystemConfigResponse> getAllSystemConfig(
            SearchSystemConfigRequest searchRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start get all system config with criteria {} page {} size {} ===",
                searchRequest, pageableRequest.getPage(), pageableRequest.getSize());

        GetAllSystemConfigResponse response = systemConfigService.getAllSystemConfig(searchRequest, pageableRequest);

        log.info("=== End get all system config with criteria {} page {} size {} ===",
                searchRequest, pageableRequest.getPage(), pageableRequest.getSize());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/system-constants")
    public GeneralResponse<List<SystemConstant>> getAllSystemConstant() {
        log.info("=== Start get all system constants ===");

        List<SystemConstant> systemConstants = Arrays.stream(SystemConstant.values()).toList();

        log.info("=== End get all system constants, size : {} ===", systemConstants.size());
        return new GeneralResponse<>(SUCCESS, systemConstants);
    }

    @GetMapping("/v1/system-configs/{groupCode}")
    public GeneralResponse<List<SystemConfigDto>> getSystemConfig(@PathVariable("groupCode") SystemConstant groupCode) {
        log.info("=== Start get system config of code : {} ===", groupCode);

        List<SystemConfigDto> systemConfigDtoList = systemConfigService.getSystemConfigByGroupCode(groupCode);

        log.info("=== End get system config of code : {} ===", groupCode);
        return new GeneralResponse<>(SUCCESS, systemConfigDtoList);
    }

    @PostMapping("/v1/system-configs")
    public GeneralResponse<SystemConfigDto> createSystemConfig(@RequestBody CreateSystemConfigRequest request)
            throws InvalidRequestException {
        log.info("=== Start create system config groupCode : {}, code : {} ===", request.getGroupCode(), request.getCode());

        SystemConfigDto response = systemConfigService.createSystemConfig(request);

        log.info("=== End create system config groupCode : {}, code : {} ===", request.getGroupCode(), request.getCode());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PutMapping("/v1/system-configs/{groupCode}/{code}")
    public GeneralResponse<SystemConfigDto> updateSystemConfig(
            @PathVariable("groupCode") SystemConstant groupCode,
            @PathVariable("code") String code,
            @RequestBody UpdateSystemConfigRequest request
    ) throws DataNotFoundException {
        log.info("=== Start update system config groupCode : {}, code : {} ===", groupCode, code);

        SystemConfigDto response = systemConfigService.updateSystemConfig(groupCode, code, request);

        log.info("=== End update system config groupCode : {}, code : {} ===", groupCode, code);
        return new GeneralResponse<>(SUCCESS, response);
    }
}
