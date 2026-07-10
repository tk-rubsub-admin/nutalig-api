package com.nutalig.controller.approval;

import com.nutalig.constant.ActivityEntityType;
import com.nutalig.controller.approval.request.ApprovalRejectTokenSubmitRequest;
import com.nutalig.controller.approval.response.ApprovalRejectTokenResolveResponse;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.ApprovalRequestDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/v1/approvals/entity/{entityType}/{referenceId}")
    public GeneralResponse<ApprovalRequestDto> getLatestApprovalByEntity(
            @PathVariable("entityType") ActivityEntityType entityType,
            @PathVariable("referenceId") String referenceId
    ) throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, approvalService.getLatestApprovalByEntity(entityType, referenceId));
    }

    @GetMapping("/v1/approvals/reject-form")
    public GeneralResponse<ApprovalRejectTokenResolveResponse> resolveRejectToken(
            @RequestParam("token") String token
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, approvalService.resolveRejectToken(token));
    }

    @PostMapping("/v1/approvals/reject-form")
    public GeneralResponse<ApprovalRequestDto> rejectByToken(
            @RequestBody ApprovalRejectTokenSubmitRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, approvalService.rejectByToken(request.getToken(), request.getReason()));
    }
}
