package com.nutalig.controller.rfq;

import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.rfq.request.CreateRequestPriceAdditionalCostRequest;
import com.nutalig.controller.rfq.request.CreateRequestPriceDetailRequest;
import com.nutalig.controller.rfq.request.ReorderRFQPicturesRequest;
import com.nutalig.controller.rfq.request.SearchRFQRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceAdditionalCostRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceDetailRequest;
import com.nutalig.dto.RequestPriceHeaderDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.RFQService;
import com.nutalig.controller.rfq.request.CreateRequestPriceHeaderRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceHeaderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/rfqs")
public class RFQController {

    private final RFQService rfqService;

    @GetMapping
    public GeneralResponse<com.nutalig.controller.response.Pageable<RequestPriceHeaderDto>> getAllRFQ(
            SearchRFQRequest searchRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start get all rfq page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        com.nutalig.controller.response.Pageable<RequestPriceHeaderDto> response = rfqService.getAllRFQ(searchRequest, pageableRequest);

        log.info("=== End get all rfq page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}")
    public GeneralResponse<RequestPriceHeaderDto> getRFQById(
            @PathVariable("id") String id,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException {
        log.info("=== Start get rfq {} ===", id);

        RequestPriceHeaderDto response = rfqService.getRFQById(id, userId);

        log.info("=== End get rfq {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping
    public GeneralResponse<RequestPriceHeaderDto> createRFQ(
            @ModelAttribute CreateRequestPriceHeaderRequest request,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start create rfq by {} ===", userId);

        RequestPriceHeaderDto response = rfqService.createRFQ(request, userId);

        log.info("=== End create rfq {} ===", response.getId());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{id}/details")
    public GeneralResponse<RequestPriceHeaderDto> addRFQDetail(
            @PathVariable("id") String id,
            @RequestBody java.util.List<CreateRequestPriceDetailRequest> requests,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start add rfq detail {} by {} ===", id, userId);

        RequestPriceHeaderDto response = rfqService.addRFQDetail(id, requests, userId);

        log.info("=== End add rfq detail {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{id}/additional-costs")
    public GeneralResponse<RequestPriceHeaderDto> addRFQAdditionalCosts(
            @PathVariable("id") String id,
            @RequestBody java.util.List<CreateRequestPriceAdditionalCostRequest> requests,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start add rfq additional costs {} by {} ===", id, userId);

        RequestPriceHeaderDto response = rfqService.addRFQAdditionalCosts(id, requests, userId);

        log.info("=== End add rfq additional costs {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/details/{detailId}")
    public GeneralResponse<RequestPriceHeaderDto> updateRFQDetail(
            @PathVariable("id") String id,
            @PathVariable("detailId") Long detailId,
            @RequestBody UpdateRequestPriceDetailRequest request,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start update rfq detail {} from {} by {} ===", detailId, id, userId);

        RequestPriceHeaderDto response = rfqService.updateRFQDetail(id, detailId, request, userId);

        log.info("=== End update rfq detail {} from {} ===", detailId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/additional-costs/{additionalCostId}")
    public GeneralResponse<RequestPriceHeaderDto> updateRFQAdditionalCost(
            @PathVariable("id") String id,
            @PathVariable("additionalCostId") Long additionalCostId,
            @RequestBody UpdateRequestPriceAdditionalCostRequest request,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start update rfq additional cost {} from {} by {} ===", additionalCostId, id, userId);

        RequestPriceHeaderDto response = rfqService.updateRFQAdditionalCost(id, additionalCostId, request, userId);

        log.info("=== End update rfq additional cost {} from {} ===", additionalCostId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/{id}/details/{detailId}")
    public GeneralResponse<RequestPriceHeaderDto> deleteRFQDetail(
            @PathVariable("id") String id,
            @PathVariable("detailId") Long detailId,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start delete rfq detail {} from {} by {} ===", detailId, id, userId);

        RequestPriceHeaderDto response = rfqService.deleteRFQDetail(id, detailId, userId);

        log.info("=== End delete rfq detail {} from {} ===", detailId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/{id}/additional-costs/{additionalCostId}")
    public GeneralResponse<RequestPriceHeaderDto> deleteRFQAdditionalCost(
            @PathVariable("id") String id,
            @PathVariable("additionalCostId") Long additionalCostId,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start delete rfq additional cost {} from {} by {} ===", additionalCostId, id, userId);

        RequestPriceHeaderDto response = rfqService.deleteRFQAdditionalCost(id, additionalCostId, userId);

        log.info("=== End delete rfq additional cost {} from {} ===", additionalCostId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}")
    public GeneralResponse<RequestPriceHeaderDto> updateRFQ(
            @PathVariable("id") String id,
            @RequestBody UpdateRequestPriceHeaderRequest request,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start update rfq {} by {} ===", id, userId);

        RequestPriceHeaderDto response = rfqService.updateRFQ(id, request, userId);

        log.info("=== End update rfq {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/{id}/pictures/{pictureId}")
    public GeneralResponse<RequestPriceHeaderDto> deletePicture(
            @PathVariable("id") String id,
            @PathVariable("pictureId") Long pictureId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException {
        log.info("=== Start delete rfq picture {} from {} ===", pictureId, id);

        RequestPriceHeaderDto response = rfqService.deletePicture(id, pictureId, userId);

        log.info("=== End delete rfq picture {} from {} ===", pictureId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{id}/pictures")
    public GeneralResponse<RequestPriceHeaderDto> addPictures(
            @PathVariable("id") String id,
            @RequestPart("pictures") java.util.List<MultipartFile> pictures,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start add rfq pictures {} by {} ===", id, userId);

        RequestPriceHeaderDto response = rfqService.addPictures(id, pictures, userId);

        log.info("=== End add rfq pictures {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

}
