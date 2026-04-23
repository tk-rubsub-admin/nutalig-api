package com.nutalig.controller.sla;

import com.nutalig.controller.sla.request.CreateSlaConfigRequest;
import com.nutalig.controller.sla.request.UpdateSlaConfigRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.SlaConfigDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.SlaConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sla-configs")
public class SlaConfigController {

    private final SlaConfigService slaConfigService;

    @GetMapping
    public GeneralResponse<List<SlaConfigDto>> getAllSlaConfigs() {
        log.info("=== Start get all sla configs ===");

        List<SlaConfigDto> response = slaConfigService.getAllSlaConfigs();

        log.info("=== End get all sla configs size {} ===", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}")
    public GeneralResponse<SlaConfigDto> getSlaConfigById(@PathVariable("id") String id)
            throws DataNotFoundException {
        log.info("=== Start get sla config {} ===", id);

        SlaConfigDto response = slaConfigService.getSlaConfigById(id);

        log.info("=== End get sla config {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping
    public GeneralResponse<SlaConfigDto> createSlaConfig(@RequestBody CreateSlaConfigRequest request, @RequestHeader("userId") String userId)
            throws InvalidRequestException {
        log.info("=== Start create sla config {} ===", request.getSlaCode());

        SlaConfigDto response = slaConfigService.createSlaConfig(request, userId);

        log.info("=== End create sla config {} ===", response.getSlaCode());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}")
    public GeneralResponse<SlaConfigDto> updateSlaConfig(
            @PathVariable("id") String id,
            @RequestBody UpdateSlaConfigRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException {
        log.info("=== Start update sla config {} ===", id);

        SlaConfigDto response = slaConfigService.updateSlaConfig(id, request, userId);

        log.info("=== End update sla config {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }
}
