package com.nutalig.controller.rfq;

import com.nutalig.constant.RfqStatus;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.rfq.request.AddRfqNoteRequest;
import com.nutalig.controller.rfq.request.CloseRfqRequest;
import com.nutalig.controller.rfq.request.RejectUrgentRfqRequest;
import com.nutalig.controller.rfq.request.RequestRfqInformationRequest;
import com.nutalig.dto.RfqHeaderDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.RFQService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/rfqs")
public class RFQActionController {

    private final RFQService rfqService;

    @PatchMapping("/request-information")
    public GeneralResponse<RfqHeaderDto> requestInformation(
            @RequestBody RequestRfqInformationRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start request information rfq {} by {} ===", request == null ? null : request.getRfqId(), userId);

        RfqHeaderDto response = rfqService.requestInformation(request, userId);

        log.info("=== End request information rfq {} ===", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/note")
    public GeneralResponse<RfqHeaderDto> addNote(
            @PathVariable("id") String id,
            @RequestBody AddRfqNoteRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start add note rfq {} by {} ===", id, userId);

        RfqHeaderDto response = rfqService.addNote(id, request, userId);

        log.info("=== End add note rfq {} ===", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/accept")
    public GeneralResponse<RfqHeaderDto> acceptRfq(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start accept rfq {} by {} ===", id, userId);

        RfqHeaderDto response = rfqService.acceptRFQ(id, userId);

        log.info("=== End accept rfq {} ===", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/close")
    public GeneralResponse<RfqHeaderDto> closeRfq(
            @RequestBody CloseRfqRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start close rfq {} by {} ===", request == null ? null : request.getRfqId(), userId);

        RfqHeaderDto response = rfqService.closeRfq(request, userId);

        log.info("=== End rclose rfq {} ===", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/reject")
    public GeneralResponse cancelRfq(@PathVariable("id") String id, @RequestHeader("userId") String userId) throws Exception {
        log.info("=== Start cancel rfq {} ===", id);

        rfqService.updateRFQStatus(id, RfqStatus.REJECTED, userId);

        log.info("=== End cancel rfq {} ===", id);

        return new GeneralResponse<>(SUCCESS);
    }

    @PatchMapping("/{id}/urgent/approve")
    public GeneralResponse<RfqHeaderDto> approveUrgentRfq(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start approve urgent rfq {} by {} ===", id, userId);

        RfqHeaderDto response = rfqService.approveUrgentRequest(id, userId);

        log.info("=== End approve urgent rfq {} ===", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/urgent/reject")
    public GeneralResponse<RfqHeaderDto> rejectUrgentRfq(
            @PathVariable("id") String id,
            @RequestBody RejectUrgentRfqRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start reject urgent rfq {} by {} ===", id, userId);

        RfqHeaderDto response = rfqService.rejectUrgentRequest(id, request, userId);

        log.info("=== End reject urgent rfq {} ===", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}/customer-quoted")
    public GeneralResponse<String> getCustomerQuoted(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start get customer quoted for rfq {} by {} ===", id, userId);

        String response = rfqService.getCustomerQuoted(id, userId);

        log.info("=== End get customer quoted for rfq {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }
}
