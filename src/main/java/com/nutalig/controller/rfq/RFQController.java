package com.nutalig.controller.rfq;

import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.rfq.request.*;
import com.nutalig.dto.RequestPriceHeaderDto;
import com.nutalig.dto.RfqSupplierInquiryDto;
import com.nutalig.dto.RfqSupplierQuoteDto;
import com.nutalig.dto.SupplierDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.RFQService;
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

    @PostMapping("/search")
    public GeneralResponse<com.nutalig.controller.response.Pageable<RequestPriceHeaderDto>> searchRFQ(
            @RequestBody(required = false) SearchRFQRequest searchRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search rfq page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        com.nutalig.controller.response.Pageable<RequestPriceHeaderDto> response =
                rfqService.getAllRFQ(searchRequest, pageableRequest);

        log.info("=== End search rfq page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
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

    @GetMapping("/{id}/suggest-suppliers")
    public GeneralResponse<java.util.List<SupplierDto>> suggestSuppliers(@PathVariable("id") String id)
            throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start suggest suppliers for rfq {} ===", id);

        java.util.List<SupplierDto> response = rfqService.suggestSuppliers(id);

        log.info("=== End suggest suppliers for rfq {} size {} ===", id, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{id}/inquiries/generate")
    public GeneralResponse<RfqSupplierInquiryDto> generateInquiry(
            @PathVariable("id") String id,
            @RequestBody GenerateRfqSupplierInquiryRequest request,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start generate inquiry rfq {} by {} ===", id, userId);

        RfqSupplierInquiryDto response = rfqService.generateInquiry(id, request, userId);

        log.info("=== End generate inquiry {} for rfq {} ===", response.getId(), id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}/inquiries")
    public GeneralResponse<java.util.List<RfqSupplierInquiryDto>> getInquiries(@PathVariable("id") String id)
            throws DataNotFoundException {
        log.info("=== Start get inquiries rfq {} ===", id);

        java.util.List<RfqSupplierInquiryDto> response = rfqService.getInquiries(id);

        log.info("=== End get inquiries rfq {} size {} ===", id, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}/inquiries/{inquiryId}")
    public GeneralResponse<RfqSupplierInquiryDto> getInquiry(
            @PathVariable("id") String id,
            @PathVariable("inquiryId") String inquiryId
    ) throws DataNotFoundException {
        log.info("=== Start get inquiry {} rfq {} ===", inquiryId, id);

        RfqSupplierInquiryDto response = rfqService.getInquiry(id, inquiryId);

        log.info("=== End get inquiry {} rfq {} ===", inquiryId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/inquiries/{inquiryId}")
    public GeneralResponse<RfqSupplierInquiryDto> updateInquiry(
            @PathVariable("id") String id,
            @PathVariable("inquiryId") String inquiryId,
            @RequestBody UpdateRfqSupplierInquiryRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start update inquiry {} rfq {} by {} ===", inquiryId, id, userId);

        RfqSupplierInquiryDto response = rfqService.updateInquiry(id, inquiryId, request, userId);

        log.info("=== End update inquiry {} rfq {} ===", inquiryId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{id}/inquiries/{inquiryId}/finalize")
    public GeneralResponse<RfqSupplierInquiryDto> finalizeInquiry(
            @PathVariable("id") String id,
            @PathVariable("inquiryId") String inquiryId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start finalize inquiry {} rfq {} by {} ===", inquiryId, id, userId);

        RfqSupplierInquiryDto response = rfqService.finalizeInquiry(id, inquiryId, userId);

        log.info("=== End finalize inquiry {} rfq {} ===", inquiryId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}/supplier-quotes")
    public GeneralResponse<java.util.List<RfqSupplierQuoteDto>> getSupplierQuotes(@PathVariable("id") String id)
            throws DataNotFoundException {
        log.info("=== Start get supplier quotes rfq {} ===", id);

        java.util.List<RfqSupplierQuoteDto> response = rfqService.getSupplierQuotes(id);

        log.info("=== End get supplier quotes rfq {} size {} ===", id, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}/supplier-quotes/{quoteId}")
    public GeneralResponse<RfqSupplierQuoteDto> getSupplierQuote(
            @PathVariable("id") String id,
            @PathVariable("quoteId") String quoteId
    ) throws DataNotFoundException {
        log.info("=== Start get supplier quote {} rfq {} ===", quoteId, id);

        RfqSupplierQuoteDto response = rfqService.getSupplierQuote(id, quoteId);

        log.info("=== End get supplier quote {} rfq {} ===", quoteId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{id}/supplier-quotes")
    public GeneralResponse<RfqSupplierQuoteDto> createSupplierQuote(
            @PathVariable("id") String id,
            @RequestBody UpsertRfqSupplierQuoteRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start create supplier quote rfq {} by {} ===", id, userId);

        RfqSupplierQuoteDto response = rfqService.createSupplierQuote(id, request, userId);

        log.info("=== End create supplier quote {} rfq {} ===", response.getId(), id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/supplier-quotes/{quoteId}")
    public GeneralResponse<RfqSupplierQuoteDto> updateSupplierQuote(
            @PathVariable("id") String id,
            @PathVariable("quoteId") String quoteId,
            @RequestBody UpsertRfqSupplierQuoteRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start update supplier quote {} rfq {} by {} ===", quoteId, id, userId);

        RfqSupplierQuoteDto response = rfqService.updateSupplierQuote(id, quoteId, request, userId);

        log.info("=== End update supplier quote {} rfq {} ===", quoteId, id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/{id}/supplier-quotes/{quoteId}")
    public GeneralResponse<Void> deleteSupplierQuote(
            @PathVariable("id") String id,
            @PathVariable("quoteId") String quoteId
    ) throws DataNotFoundException {
        log.info("=== Start delete supplier quote {} rfq {} ===", quoteId, id);

        rfqService.deleteSupplierQuote(id, quoteId);

        log.info("=== End delete supplier quote {} rfq {} ===", quoteId, id);
        return new GeneralResponse<>(SUCCESS);
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

    @PostMapping("/{id}/customers")
    public GeneralResponse<RequestPriceHeaderDto> updateCustomer(
            @PathVariable("id") String id,
            @RequestBody UpdateRfqCustomerRequest request,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start update rfq customer {} by {} ===", id, userId);

        RequestPriceHeaderDto response = rfqService.updateCustomer(id, request, userId);

        log.info("=== End update rfq customer {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/{id}/sales-order")
    public GeneralResponse<RequestPriceHeaderDto> linkSalesOrder(
            @PathVariable("id") String id,
            @RequestBody LinkRfqSalesOrderRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start link rfq {} to sale order {} by {} ===", id, request == null ? null : request.getSaleOrderId(), userId);

        RequestPriceHeaderDto response = rfqService.linkSalesOrder(id, request, userId);

        log.info("=== End link rfq {} to sale order {} ===", id, response.getSaleOrderId());
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

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public GeneralResponse<RequestPriceHeaderDto> deleteAttachment(
            @PathVariable("id") String id,
            @PathVariable("attachmentId") Long attachmentId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException {
        log.info("=== Start delete rfq attachment {} from {} ===", attachmentId, id);

        RequestPriceHeaderDto response = rfqService.deleteAttachment(id, attachmentId, userId);

        log.info("=== End delete rfq attachment {} from {} ===", attachmentId, id);
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

    @PostMapping("/{id}/attachments")
    public GeneralResponse<RequestPriceHeaderDto> addAttachments(
            @PathVariable("id") String id,
            @RequestPart("attachments") java.util.List<MultipartFile> attachments,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start add rfq attachments {} by {} ===", id, userId);

        RequestPriceHeaderDto response = rfqService.addAttachments(id, attachments, userId);

        log.info("=== End add rfq attachments {} ===", id);
        return new GeneralResponse<>(SUCCESS, response);
    }

}
