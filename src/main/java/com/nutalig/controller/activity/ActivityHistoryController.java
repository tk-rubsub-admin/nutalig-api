package com.nutalig.controller.activity;

import com.nutalig.constant.ActivityEntityType;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.ActivityHistoryDto;
import com.nutalig.service.ActivityHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/activity-history")
public class ActivityHistoryController {

    private final ActivityHistoryService activityHistoryService;

    @GetMapping
    public GeneralResponse<List<ActivityHistoryDto>> getActivityHistory(
            @RequestParam("entityType") ActivityEntityType entityType,
            @RequestParam("referenceId") String referenceId
    ) {
        log.info("=== Start get activity history entityType : {}, referenceId : {} ===", entityType, referenceId);

        List<ActivityHistoryDto> response = activityHistoryService.getHistory(entityType, referenceId);

        log.info("=== End get activity history entityType : {}, referenceId : {}, size : {} ===",
                entityType, referenceId, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }
}
