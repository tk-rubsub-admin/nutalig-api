package com.nutalig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nutalig.config.PromptTemplateEngine;
import com.nutalig.constant.*;
import com.nutalig.controller.rfq.request.UpdateRfqSupplierInquiryRequest;
import com.nutalig.controller.rfq.request.UpsertRfqSupplierQuoteRequest;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.nutalig.entity.id.RfqStatusTimelineId;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RFQSupplierService {
    private static final String RFQ_SUPPLIER_INQUIRY_TEMPLATE_CODE = "RFQ_SUPPLIER_INQUIRY_TH";
    private static final String RFQ_FINAL_QUOTE_INQUIRY_TH = "RFQ_FINAL_QUOTE_INQUIRY_TH";
    private static final String RFQ_SUPPLIER_QUOTE_EXTRACTION_TEMPLATE_CODE = "RFQ_SUPPLIER_QUOTE_EXTRACTION";
    private static final String FINAL_RFQ_EXTRACTION = "FINAL_RFQ_EXTRACTION";

    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final RfqSupplierInquiryRepository rfqSupplierInquiryRepository;
    private final RfqSupplierQuoteRepository rfqSupplierQuoteRepository;
    private final RfqStatusTimelineRepository rfqStatusTimelineRepository;
    private final SupplierRepository supplierRepository;
    private final ActivityHistoryService activityHistoryService;
    private final UserProfileService userProfileService;
    private final AiExecutionService aiExecutionService;
    private final OpenAiService openAiService;
    private final PromptService promptService;
    private final PromptTemplateEngine promptTemplateEngine;
    private final ObjectMapper objectMapper;
    private final SupplierMapper supplierMapper;

    @Transactional(rollbackFor = Exception.class)
    public RfqSupplierInquiryDto generateInquiry(String rfqId, String userId) throws Exception {
        RfqHeaderEntity rfq = getEntityById(rfqId);

        Optional<RfqSupplierInquiryEntity> existingInquiry =
                rfqSupplierInquiryRepository.findFirstByRequestPriceHeader_IdOrderByVersionNoDesc(rfqId);

        if (existingInquiry.isPresent()) {
            return toInquiryDto(existingInquiry.get());
        }

        Integer maxVersionNo = rfqSupplierInquiryRepository.findMaxVersionNoByRfqId(rfqId);
        int nextVersionNo = (maxVersionNo == null ? 0 : maxVersionNo) + 1;

        String thaiMessage = buildThaiInquiryMessage(rfq);
        String chineseMessage = null;
        RfqSupplierInquiryStatus status = RfqSupplierInquiryStatus.DRAFT;
        try {
            chineseMessage = translateToChinese(thaiMessage);
            if (StringUtils.isNotBlank(chineseMessage)) {
                status = RfqSupplierInquiryStatus.TRANSLATED;
            }
        } catch (Exception exception) {
            log.warn("Translate inquiry to Chinese failed for rfq {}", rfqId, exception);
        }

        RfqSupplierInquiryEntity inquiry = new RfqSupplierInquiryEntity();
        inquiry.setRequestPriceHeader(rfq);
        inquiry.setVersionNo(nextVersionNo);
        inquiry.setStatus(status);
        inquiry.setThaiMessage(thaiMessage);
        inquiry.setChineseMessage(StringUtils.trimToNull(chineseMessage));
        inquiry.setSourceSnapshot(buildInquirySourceSnapshot(rfq));
        inquiry.setCreatedBy(userId);
        inquiry.setUpdatedBy(userId);

        inquiry = rfqSupplierInquiryRepository.save(inquiry);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                rfq.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.GENERATE_INQUIRY_MESSAGE,
                ActivitySource.API,
                "จัดซื้อทำการ generate ข้อความถามราคากับซัพพลายเออร์ของคำขอราคาเลขที่ " + rfq.getId(),
                null
        );


        return toInquiryDto(inquiry);
    }

    @Transactional(rollbackFor = Exception.class)
    public String generateFinalInquiry(String rfqId, String userId) throws Exception {
        RfqHeaderEntity rfq = getEntityById(rfqId);
        RfqSupplierQuoteEntity supplierQuote = rfqSupplierQuoteRepository
                .findAllByRequestPriceHeader_IdOrderByUpdatedDateDesc(rfqId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Supplier quote is required before generate final inquiry."
                ));

        return buildThaiFinalQuoteInquiryMessage(rfq, supplierQuote);
    }

    @Transactional(readOnly = true)
    public List<RfqSupplierInquiryDto> getInquiries(String rfqId) throws DataNotFoundException {
        getEntityById(rfqId);
        List<RfqSupplierInquiryDto> inquiries = new ArrayList<>();
        for (RfqSupplierInquiryEntity inquiry : rfqSupplierInquiryRepository
                .findAllByRequestPriceHeader_IdOrderByVersionNoDesc(rfqId)) {
            inquiries.add(toInquiryDto(inquiry));
        }
        return inquiries;
    }

    @Transactional(readOnly = true)
    public RfqSupplierInquiryDto getInquiry(String rfqId, String inquiryId) throws DataNotFoundException {
        return toInquiryDto(getInquiryEntity(rfqId, inquiryId));
    }

    @Transactional
    public RfqSupplierInquiryDto updateInquiry(
            String rfqId,
            String inquiryId,
            UpdateRfqSupplierInquiryRequest request,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        RfqSupplierInquiryEntity inquiry = getInquiryEntity(rfqId, inquiryId);
        Map<String, Object> beforeDetail = buildInquiryActivityDetail(inquiry);

        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }
        if (request.getThaiMessage() != null) {
            inquiry.setThaiMessage(trimRequiredMessage(request.getThaiMessage(), "Thai message"));
        }
        if (request.getChineseMessage() != null) {
            inquiry.setChineseMessage(StringUtils.trimToNull(request.getChineseMessage()));
        }

        inquiry.setStatus(determineInquiryStatus(inquiry.getChineseMessage(), inquiry.getStatus()));
        inquiry.setUpdatedBy(userProfileService.getNameFromId(userId));
        inquiry = rfqSupplierInquiryRepository.save(inquiry);

        Map<String, Object> afterDetail = buildInquiryActivityDetail(inquiry);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", beforeDetail);
        detail.put("after", afterDetail);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                rfqId,
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE_INQUIRY_MESSAGE,
                ActivitySource.API,
                "จัดซื้ออัพเดตข้อความสอบถามราคาของคำขอราคาเลขที่ " + rfqId + " เวอร์ชัน " + inquiry.getVersionNo(),
                detail
        );

        return toInquiryDto(inquiry);
    }

    @Transactional
    public RfqSupplierInquiryDto finalizeInquiry(String rfqId, String inquiryId, String userId)
            throws DataNotFoundException, InvalidRequestException {
        RfqSupplierInquiryEntity inquiry = getInquiryEntity(rfqId, inquiryId);
        if (StringUtils.isBlank(inquiry.getThaiMessage())) {
            throw new InvalidRequestException("Thai message is required before finalize.");
        }
        if (StringUtils.isBlank(inquiry.getChineseMessage())) {
            throw new InvalidRequestException("Chinese message is required before finalize.");
        }

        inquiry.setStatus(RfqSupplierInquiryStatus.FINAL);
        inquiry.setUpdatedBy(userProfileService.getNameFromId(userId));
        inquiry = rfqSupplierInquiryRepository.save(inquiry);
        return toInquiryDto(inquiry);
    }

    @Transactional(readOnly = true)
    public List<RfqSupplierQuoteDto> getSupplierQuotes(String rfqId) throws DataNotFoundException {
        getEntityById(rfqId);
        return rfqSupplierQuoteRepository.findAllByRequestPriceHeader_IdOrderByUpdatedDateDesc(rfqId)
                .stream()
                .map(this::toSupplierQuoteDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RfqSupplierQuoteDto getSupplierQuote(String rfqId, String quoteId) throws DataNotFoundException {
        return toSupplierQuoteDto(getSupplierQuoteEntity(rfqId, quoteId));
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqSupplierQuoteDto createSupplierQuote(
            String rfqId,
            UpsertRfqSupplierQuoteRequest request,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity rfq = getEntityById(rfqId);
        if (request == null || StringUtils.isBlank(request.getSupplierId())) {
            throw new InvalidRequestException("Supplier id is required.");
        }
        SupplierEntity supplier = getSupplierEntity(request.getSupplierId());
        String actor = userProfileService.getNameFromId(userId);
        Integer maxRevisionNo = rfqSupplierQuoteRepository
                .findMaxRevisionNoByRequestPriceHeader_IdAndSupplier_Id(rfqId, supplier.getId());
        int nextRevisionNo = (maxRevisionNo == null ? 0 : maxRevisionNo) + 1;

        RfqSupplierQuoteEntity quote = new RfqSupplierQuoteEntity();
        quote.setRequestPriceHeader(rfq);
        quote.setSupplier(supplier);
        quote.setRevisionNo(nextRevisionNo);
        quote.setStatus(request.getStatus() == null ? RfqSupplierQuoteStatus.RESPONDED : request.getStatus());
        quote.setCreatedBy(actor);
        applySupplierQuoteRequest(rfq, quote, request, userId);

        quote = rfqSupplierQuoteRepository.save(quote);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("request", request);

        rfq.setStatus(RfqStatus.SUPPLIER_QUOTED);
        rfq.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        rfq.setUpdatedBy(actor);

        rfq = requestPriceHeaderRepository.save(rfq);
        saveRfqStatusTimeline(rfq, RfqStatus.SUPPLIER_QUOTED, rfq.getUpdatedDate());

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                quote.getRequestPriceHeader().getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "จัดซื้อบันทึกราคาจากซัพพลายเออร์ของคำขอราคาเลขที่ " + quote.getRequestPriceHeader().getId(),
                detail
        );

        return toSupplierQuoteDto(quote);
    }

    @Transactional
    public UpsertRfqSupplierQuoteRequest extractSupplierQuoteRequest(
            String rfqId,
            com.nutalig.controller.rfq.request.ExtractRfqSupplierQuoteRequest request
    ) throws Exception {
        return extractSupplierQuoteRequestByTemplate(rfqId, request, RFQ_SUPPLIER_QUOTE_EXTRACTION_TEMPLATE_CODE);
    }

    @Transactional(rollbackFor = Exception.class)
    public UpsertRfqSupplierQuoteRequest finalExtractSupplierQuoteRequest(
            String rfqId,
            com.nutalig.controller.rfq.request.ExtractRfqSupplierQuoteRequest request
    ) throws Exception {
        return extractSupplierQuoteRequestByTemplate(rfqId, request, FINAL_RFQ_EXTRACTION);
    }

    private UpsertRfqSupplierQuoteRequest extractSupplierQuoteRequestByTemplate(
            String rfqId,
            com.nutalig.controller.rfq.request.ExtractRfqSupplierQuoteRequest request,
            String templateCode
    ) throws Exception {
        if (request == null || StringUtils.isBlank(request.getSupplierId())) {
            throw new InvalidRequestException("Supplier id is required.");
        }
        if (StringUtils.isBlank(request.getSupplierMessage())) {
            throw new InvalidRequestException("Supplier message is required.");
        }

        RfqHeaderEntity rfq = getEntityById(rfqId);
        getSupplierEntity(request.getSupplierId());

        Map<String, String> variables = new HashMap<>();
        variables.put("supplierId", request.getSupplierId().trim());
        variables.put("inquiryId_or_null", StringUtils.defaultIfBlank(StringUtils.trimToNull(request.getInquiryId()), "null"));
        variables.put("defaultCurrency", String.valueOf(
                request.getDefaultCurrency() == null ? com.nutalig.constant.Currency.CNY : request.getDefaultCurrency()
        ));
        variables.put("rfqId", rfq.getId());
        variables.put("rfqContext_json_or_empty_object", buildRfqContextJson(rfq));
        variables.put("rfqDetailHints_json_or_empty_array", buildRfqDetailHintsJson(rfq));
        variables.put("supplierMessage", request.getSupplierMessage().trim());

        String rawResponse = aiExecutionService.execute(templateCode, variables);
        String cleanedJson = com.nutalig.utils.ObjectUtil.extractJsonObject(rawResponse);
        String sanitizedJson = sanitizeExtractedSupplierQuoteJson(
                cleanedJson,
                request.getDefaultCurrency() == null ? com.nutalig.constant.Currency.CNY : request.getDefaultCurrency()
        );
        UpsertRfqSupplierQuoteRequest extractedRequest =
                objectMapper.readValue(sanitizedJson, UpsertRfqSupplierQuoteRequest.class);

        return normalizeExtractedSupplierQuoteRequest(extractedRequest, request);
    }

    private String sanitizeExtractedSupplierQuoteJson(
            String cleanedJson,
            com.nutalig.constant.Currency defaultCurrency
    ) throws Exception {
        JsonNode rootNode = objectMapper.readTree(cleanedJson);
        if (!(rootNode instanceof ObjectNode rootObject)) {
            return cleanedJson;
        }

        JsonNode detailsNode = rootObject.get("details");
        if (detailsNode instanceof ArrayNode detailsArray) {
            for (JsonNode detailNode : detailsArray) {
                if (!(detailNode instanceof ObjectNode detailObject)) {
                    continue;
                }

                JsonNode tiersNode = detailObject.get("tiers");
                if (!(tiersNode instanceof ArrayNode tiersArray)) {
                    continue;
                }

                for (JsonNode tierNode : tiersArray) {
                    if (!(tierNode instanceof ObjectNode tierObject)) {
                        continue;
                    }

                    String rawCurrency = getTextValue(tierObject, "currency");
                    String rawProductPriceCurrency = getTextValue(tierObject, "productPriceCurrency");
                    String rawShippingCostCurrency = getTextValue(tierObject, "shippingCostCurrency");

                    com.nutalig.constant.Currency fallbackCurrency =
                            parseSupportedCurrency(rawCurrency, defaultCurrency);
                    com.nutalig.constant.Currency productPriceCurrency =
                            parseSupportedCurrency(rawProductPriceCurrency, fallbackCurrency);
                    com.nutalig.constant.Currency shippingCostCurrency =
                            parseSupportedCurrency(rawShippingCostCurrency, fallbackCurrency);

                    tierObject.put("currency", productPriceCurrency.name());
                    tierObject.put("productPriceCurrency", productPriceCurrency.name());
                    tierObject.put("shippingCostCurrency", shippingCostCurrency.name());
                }
            }
        }

        return objectMapper.writeValueAsString(rootObject);
    }

    private String getTextValue(ObjectNode objectNode, String fieldName) {
        JsonNode valueNode = objectNode.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        return StringUtils.trimToNull(valueNode.asText());
    }

    private com.nutalig.constant.Currency parseSupportedCurrency(
            String rawCurrency,
            com.nutalig.constant.Currency fallbackCurrency
    ) {
        if (StringUtils.isBlank(rawCurrency)) {
            return fallbackCurrency;
        }

        try {
            return com.nutalig.constant.Currency.valueOf(rawCurrency.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallbackCurrency;
        }
    }

    private void saveRfqStatusTimeline(RfqHeaderEntity rfq, RfqStatus status, ZonedDateTime statusDatetime) {
        if (rfq == null || status == null || statusDatetime == null) {
            return;
        }

        if (rfqStatusTimelineRepository.findByIdRfqIdAndIdStatus(rfq.getId(), status).isPresent()) {
            return;
        }

        RfqStatusTimelineEntity timeline = new RfqStatusTimelineEntity();
        RfqStatusTimelineId timelineId = new RfqStatusTimelineId();
        timelineId.setRfqId(rfq.getId());
        timelineId.setStatus(status);
        timeline.setId(timelineId);
        timeline.setRfqHeader(rfq);
        timeline.setStatusDatetime(statusDatetime);
        rfqStatusTimelineRepository.save(timeline);
    }

    private String buildRfqDetailHintsJson(RfqHeaderEntity rfq) {
        try {
            List<Map<String, Object>> detailHints = Optional.ofNullable(rfq.getDetails())
                    .orElse(List.of())
                    .stream()
                    .sorted(Comparator.comparing(RfqDetailEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(RfqDetailEntity::getId, Comparator.nullsLast(Long::compareTo)))
                    .map(detail -> {
                        Map<String, Object> hint = new LinkedHashMap<>();
                        hint.put("id", detail.getId());
                        hint.put("optionName", StringUtils.trimToNull(detail.getOptionName()));
                        hint.put("spec", StringUtils.trimToNull(detail.getSpec()));
                        hint.put("remark", StringUtils.trimToNull(detail.getRemark()));
                        return hint;
                    })
                    .toList();
            return objectMapper.writeValueAsString(detailHints);
        } catch (Exception exception) {
            log.warn("Serialize rfq detail hints failed for rfq {}", rfq.getId(), exception);
            return "[]";
        }
    }

    private String buildRfqContextJson(RfqHeaderEntity rfq) {
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("rfqId", rfq.getId());
            context.put("productFamily", displayProductFamily(rfq));
            context.put("productSubtype1", displayProductSubtype1(rfq));
            context.put("productSubtype2", displayProductSubtype2(rfq));
            context.put("material", displayProductMaterial(rfq));
            context.put("capacity", safeValue(rfq.getCapacity()));
            context.put("description", safeValue(rfq.getDescription()));
            context.put("shippingType", displayShippingType(rfq));
            context.put("detailHints", objectMapper.readValue(buildRfqDetailHintsJson(rfq), Object.class));
            return objectMapper.writeValueAsString(context);
        } catch (Exception exception) {
            log.warn("Serialize rfq context failed for rfq {}", rfq.getId(), exception);
            return "{}";
        }
    }

    private UpsertRfqSupplierQuoteRequest normalizeExtractedSupplierQuoteRequest(
            UpsertRfqSupplierQuoteRequest extractedRequest,
            com.nutalig.controller.rfq.request.ExtractRfqSupplierQuoteRequest contextRequest
    ) {
        UpsertRfqSupplierQuoteRequest normalized = extractedRequest == null
                ? new UpsertRfqSupplierQuoteRequest()
                : extractedRequest;

        normalized.setSupplierId(StringUtils.trimToNull(contextRequest.getSupplierId()));
        normalized.setInquiryId(StringUtils.trimToNull(contextRequest.getInquiryId()));
        normalized.setStatus(RfqSupplierQuoteStatus.RESPONDED);
        normalized.setRemark(StringUtils.trimToNull(normalized.getRemark()));
        normalized.setRecommend(StringUtils.trimToNull(normalized.getRecommend()));

        List<UpsertRfqSupplierQuoteRequest.DetailRequest> normalizedDetails = new ArrayList<>();
        List<UpsertRfqSupplierQuoteRequest.DetailRequest> detailRequests =
                Optional.ofNullable(normalized.getDetails()).orElse(List.of());
        for (int detailIndex = 0; detailIndex < detailRequests.size(); detailIndex++) {
            UpsertRfqSupplierQuoteRequest.DetailRequest detail = detailRequests.get(detailIndex);
            if (detail == null) {
                continue;
            }

            detail.setOptionName(StringUtils.trimToNull(detail.getOptionName()));
            detail.setSpec(StringUtils.trimToNull(detail.getSpec()));
            detail.setRemark(StringUtils.trimToNull(detail.getRemark()));
            detail.setPackageName(StringUtils.trimToNull(detail.getPackageName()));
            detail.setPackageDimension(StringUtils.trimToNull(detail.getPackageDimension()));
            detail.setPackageWeight(StringUtils.trimToNull(detail.getPackageWeight()));
            detail.setPackageCapacity(StringUtils.trimToNull(detail.getPackageCapacity()));
            detail.setSortOrder(detailIndex + 1);

            List<UpsertRfqSupplierQuoteRequest.PackageRequest> normalizedPackages = new ArrayList<>();
            List<UpsertRfqSupplierQuoteRequest.PackageRequest> packageRequests =
                    Optional.ofNullable(detail.getPackages()).orElse(List.of());
            for (int packageIndex = 0; packageIndex < packageRequests.size(); packageIndex++) {
                UpsertRfqSupplierQuoteRequest.PackageRequest packageRequest = packageRequests.get(packageIndex);
                if (packageRequest == null) {
                    continue;
                }

                packageRequest.setPackageName(StringUtils.trimToNull(packageRequest.getPackageName()));
                packageRequest.setPackageDimension(StringUtils.trimToNull(packageRequest.getPackageDimension()));
                packageRequest.setPackageWeight(StringUtils.trimToNull(packageRequest.getPackageWeight()));
                packageRequest.setPackageCapacity(StringUtils.trimToNull(packageRequest.getPackageCapacity()));
                packageRequest.setSortOrder(packageIndex + 1);
                normalizedPackages.add(packageRequest);
            }
            detail.setPackages(normalizedPackages);

            if ((detail.getPackageName() == null || detail.getPackageDimension() == null || detail.getPackageWeight() == null || detail.getPackageCapacity() == null)
                    && !normalizedPackages.isEmpty()) {
                UpsertRfqSupplierQuoteRequest.PackageRequest firstPackage = normalizedPackages.get(0);
                if (detail.getPackageName() == null) {
                    detail.setPackageName(firstPackage.getPackageName());
                }
                if (detail.getPackageDimension() == null) {
                    detail.setPackageDimension(firstPackage.getPackageDimension());
                }
                if (detail.getPackageWeight() == null) {
                    detail.setPackageWeight(firstPackage.getPackageWeight());
                }
                if (detail.getPackageCapacity() == null) {
                    detail.setPackageCapacity(firstPackage.getPackageCapacity());
                }
            }

            List<UpsertRfqSupplierQuoteRequest.TierRequest> normalizedTiers = new ArrayList<>();
            List<UpsertRfqSupplierQuoteRequest.TierRequest> tierRequests =
                    Optional.ofNullable(detail.getTiers()).orElse(List.of());
            for (int tierIndex = 0; tierIndex < tierRequests.size(); tierIndex++) {
                UpsertRfqSupplierQuoteRequest.TierRequest tierRequest = tierRequests.get(tierIndex);
                if (tierRequest == null) {
                    continue;
                }

                tierRequest.setShippingCost(tierRequest.getShippingCost() == null ? BigDecimal.ZERO : tierRequest.getShippingCost());
                com.nutalig.constant.Currency defaultCurrency = contextRequest.getDefaultCurrency() == null
                        ? com.nutalig.constant.Currency.CNY
                        : contextRequest.getDefaultCurrency();
                com.nutalig.constant.Currency fallbackCurrency = tierRequest.getCurrency() == null
                        ? defaultCurrency
                        : tierRequest.getCurrency();
                tierRequest.setProductPriceCurrency(tierRequest.getProductPriceCurrency() == null
                        ? fallbackCurrency
                        : tierRequest.getProductPriceCurrency());
                tierRequest.setShippingCostCurrency(tierRequest.getShippingCostCurrency() == null
                        ? fallbackCurrency
                        : tierRequest.getShippingCostCurrency());
                tierRequest.setCurrency(tierRequest.getProductPriceCurrency());
                tierRequest.setSortOrder(tierIndex + 1);
                normalizedTiers.add(tierRequest);
            }
            detail.setTiers(normalizedTiers);
            normalizedDetails.add(detail);
        }
        normalized.setDetails(normalizedDetails);

        List<UpsertRfqSupplierQuoteRequest.AdditionalCostRequest> normalizedAdditionalCosts = new ArrayList<>();
        List<UpsertRfqSupplierQuoteRequest.AdditionalCostRequest> additionalCostRequests =
                Optional.ofNullable(normalized.getAdditionalCosts()).orElse(List.of());
        for (int additionalCostIndex = 0; additionalCostIndex < additionalCostRequests.size(); additionalCostIndex++) {
            UpsertRfqSupplierQuoteRequest.AdditionalCostRequest additionalCost = additionalCostRequests.get(additionalCostIndex);
            if (additionalCost == null || StringUtils.isBlank(additionalCost.getDescription())) {
                continue;
            }

            additionalCost.setDescription(additionalCost.getDescription().trim());
            additionalCost.setUnit(StringUtils.trimToNull(additionalCost.getUnit()));
            additionalCost.setValue(StringUtils.trimToNull(additionalCost.getValue()));
            additionalCost.setSortOrder(normalizedAdditionalCosts.size() + 1);
            normalizedAdditionalCosts.add(additionalCost);
        }
        normalized.setAdditionalCosts(normalizedAdditionalCosts);

        return normalized;
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqSupplierQuoteDto updateSupplierQuote(
            String rfqId,
            String quoteId,
            UpsertRfqSupplierQuoteRequest request,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity rfq = getEntityById(rfqId);
        RfqSupplierQuoteEntity quote = getSupplierQuoteEntity(rfqId, quoteId);
        if (request == null) {
            throw new InvalidRequestException("Supplier quote is required.");
        }
        Map<String, Object> beforeDetail = buildSupplierQuoteActivityDetail(quote);

        if (StringUtils.isNotBlank(request.getSupplierId())
                && !Objects.equals(request.getSupplierId(), quote.getSupplier().getId())) {
            SupplierEntity supplier = getSupplierEntity(request.getSupplierId());
            quote.setSupplier(supplier);
            Integer maxRevisionNo = rfqSupplierQuoteRepository
                    .findMaxRevisionNoByRequestPriceHeader_IdAndSupplier_Id(rfqId, supplier.getId());
            int nextRevisionNo = (maxRevisionNo == null ? 0 : maxRevisionNo) + 1;
            quote.setRevisionNo(nextRevisionNo);
        }

        quote.getDetails().clear();
        quote.getAdditionalCosts().clear();
        applySupplierQuoteRequest(rfq, quote, request, userId);

        quote = rfqSupplierQuoteRepository.save(quote);

        Map<String, Object> afterDetail = buildSupplierQuoteActivityDetail(quote);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", beforeDetail);
        detail.put("after", afterDetail);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                quote.getRequestPriceHeader().getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "จัดซื้ออัพเดตราคาจากซัพพลายเออร์ของคำขอราคาเลขที่ " + quote.getRequestPriceHeader().getId(),
                detail
        );

        return toSupplierQuoteDto(quote);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierQuote(String rfqId, String quoteId) throws DataNotFoundException {
        RfqSupplierQuoteEntity quote = getSupplierQuoteEntity(rfqId, quoteId);
        rfqSupplierQuoteRepository.delete(quote);
    }

    private RfqSupplierInquiryDto toInquiryDto(RfqSupplierInquiryEntity entity) {
        RfqSupplierInquiryDto dto = new RfqSupplierInquiryDto();
        dto.setId(entity.getId());
        dto.setRfqId(entity.getRequestPriceHeader().getId());
        dto.setVersionNo(entity.getVersionNo());
        dto.setStatus(entity.getStatus());
        dto.setThaiMessage(entity.getThaiMessage());
        dto.setChineseMessage(entity.getChineseMessage());
        dto.setSourceSnapshot(entity.getSourceSnapshot());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private Map<String, Object> buildInquiryActivityDetail(RfqSupplierInquiryEntity inquiry) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", inquiry.getId());
        detail.put("rfqId", inquiry.getRequestPriceHeader() != null ? inquiry.getRequestPriceHeader().getId() : null);
        detail.put("versionNo", inquiry.getVersionNo());
        detail.put("status", inquiry.getStatus());
        detail.put("thaiMessage", inquiry.getThaiMessage());
        detail.put("chineseMessage", inquiry.getChineseMessage());
        detail.put("sourceSnapshot", inquiry.getSourceSnapshot());
        detail.put("updatedBy", inquiry.getUpdatedBy());
        detail.put("createdBy", inquiry.getCreatedBy());
        detail.put("createdDate", inquiry.getCreatedDate());
        detail.put("updatedDate", inquiry.getUpdatedDate());
        return detail;
    }

    private RfqSupplierQuoteDto toSupplierQuoteDto(RfqSupplierQuoteEntity entity) {
        RfqSupplierQuoteDto dto = new RfqSupplierQuoteDto();
        dto.setId(entity.getId());
        dto.setRfqId(entity.getRequestPriceHeader().getId());
        dto.setSupplier(supplierMapper.toDto(entity.getSupplier()));
        dto.setInquiryId(entity.getInquiry() == null ? null : entity.getInquiry().getId());
        dto.setRevisionNo(entity.getRevisionNo());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());
        dto.setDetails(entity.getDetails().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteDetailEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteDetailEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toSupplierQuoteDetailDto)
                .toList());
        dto.setAdditionalCosts(entity.getAdditionalCosts().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteAdditionalCostEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteAdditionalCostEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toSupplierQuoteAdditionalCostDto)
                .toList());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private RfqSupplierQuoteDetailDto toSupplierQuoteDetailDto(RfqSupplierQuoteDetailEntity entity) {
        RfqSupplierQuoteDetailDto dto = new RfqSupplierQuoteDetailDto();
        dto.setId(entity.getId());
        dto.setRfqDetailId(entity.getRequestPriceDetail() == null ? null : entity.getRequestPriceDetail().getId());
        dto.setOptionName(entity.getOptionName());
        dto.setSpec(entity.getSpec());
        dto.setSortOrder(entity.getSortOrder());
        dto.setRemark(entity.getRemark());
        dto.setPackageName(resolvePrimaryPackageName(entity));
        dto.setPackageDimension(entity.getPackageDimension());
        dto.setPackageWeight(entity.getPackageWeight());
        dto.setPackageCapacity(entity.getPackageCapacity());
        dto.setPackages(resolveSupplierQuotePackages(entity).stream()
                .map(this::toSupplierQuoteDetailPackageDto)
                .toList());
        dto.setTiers(entity.getTiers().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteTierEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteTierEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toSupplierQuoteTierDto)
                .toList());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private String resolvePrimaryPackageName(RfqSupplierQuoteDetailEntity entity) {
        return resolveSupplierQuotePackages(entity).stream()
                .map(RfqSupplierQuoteDetailPackageEntity::getPackageName)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(StringUtils::trimToNull)
                .orElse(null);
    }

    private RfqSupplierQuoteTierDto toSupplierQuoteTierDto(RfqSupplierQuoteTierEntity entity) {
        RfqSupplierQuoteTierDto dto = new RfqSupplierQuoteTierDto();
        dto.setId(entity.getId());
        dto.setQuantity(entity.getQuantity());
        dto.setProductPrice(entity.getProductPrice());
        dto.setShippingCost(entity.getShippingCost());
        dto.setProductPriceCurrency(entity.getProductPriceCurrency() == null
                ? entity.getCurrency()
                : entity.getProductPriceCurrency());
        dto.setShippingCostCurrency(entity.getShippingCostCurrency() == null
                ? entity.getCurrency()
                : entity.getShippingCostCurrency());
        dto.setCurrency(dto.getProductPriceCurrency());
        dto.setSortOrder(entity.getSortOrder());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private RfqSupplierQuoteDetailPackageDto toSupplierQuoteDetailPackageDto(
            RfqSupplierQuoteDetailPackageEntity entity
    ) {
        RfqSupplierQuoteDetailPackageDto dto = new RfqSupplierQuoteDetailPackageDto();
        dto.setId(entity.getId());
        dto.setPackageName(entity.getPackageName());
        dto.setPackageDimension(entity.getPackageDimension());
        dto.setPackageWeight(entity.getPackageWeight());
        dto.setPackageCapacity(entity.getPackageCapacity());
        dto.setSortOrder(entity.getSortOrder());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private List<RfqSupplierQuoteDetailPackageEntity> resolveSupplierQuotePackages(
            RfqSupplierQuoteDetailEntity entity
    ) {
        if (!entity.getPackages().isEmpty()) {
            return entity.getPackages().stream()
                    .sorted(Comparator.comparing(RfqSupplierQuoteDetailPackageEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(RfqSupplierQuoteDetailPackageEntity::getId, Comparator.nullsLast(Long::compareTo)))
                    .toList();
        }

        if (StringUtils.isAllBlank(
                entity.getPackageDimension(),
                entity.getPackageWeight(),
                entity.getPackageCapacity()
        )) {
            return List.of();
        }

        return List.of(buildSupplierQuoteDetailPackageEntity(
                null,
                entity.getPackageDimension(),
                entity.getPackageWeight(),
                entity.getPackageCapacity(),
                1
        ));
    }

    private RfqSupplierQuoteAdditionalCostDto toSupplierQuoteAdditionalCostDto(
            RfqSupplierQuoteAdditionalCostEntity entity
    ) {
        RfqSupplierQuoteAdditionalCostDto dto = new RfqSupplierQuoteAdditionalCostDto();
        dto.setId(entity.getId());
        dto.setDescription(entity.getDescription());
        dto.setUnit(entity.getUnit());
        dto.setValue(entity.getValue());
        dto.setSortOrder(entity.getSortOrder());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private Map<String, Object> buildSupplierQuoteActivityDetail(RfqSupplierQuoteEntity entity) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", entity.getId());
        detail.put("rfqId", entity.getRequestPriceHeader() == null ? null : entity.getRequestPriceHeader().getId());
        detail.put("supplierId", entity.getSupplier() == null ? null : entity.getSupplier().getId());
        detail.put("revisionNo", entity.getRevisionNo());
        detail.put("status", entity.getStatus());
        detail.put("remark", entity.getRemark());
        detail.put("details", entity.getDetails().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteDetailEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteDetailEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::buildSupplierQuoteDetailActivityDetail)
                .toList());
        detail.put("additionalCosts", entity.getAdditionalCosts().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteAdditionalCostEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteAdditionalCostEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::buildSupplierQuoteAdditionalCostActivityDetail)
                .toList());
        return detail;
    }

    private Map<String, Object> buildSupplierQuoteDetailActivityDetail(RfqSupplierQuoteDetailEntity entity) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", entity.getId());
        detail.put("rfqDetailId", entity.getRequestPriceDetail() == null ? null : entity.getRequestPriceDetail().getId());
        detail.put("optionName", entity.getOptionName());
        detail.put("spec", entity.getSpec());
        detail.put("sortOrder", entity.getSortOrder());
        detail.put("remark", entity.getRemark());
        detail.put("packageName", resolvePrimaryPackageName(entity));
        detail.put("packageDimension", entity.getPackageDimension());
        detail.put("packageWeight", entity.getPackageWeight());
        detail.put("packageCapacity", entity.getPackageCapacity());
        detail.put("packages", resolveSupplierQuotePackages(entity).stream()
                .map(this::buildSupplierQuoteDetailPackageActivityDetail)
                .toList());
        detail.put("tiers", entity.getTiers().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteTierEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteTierEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::buildSupplierQuoteTierActivityDetail)
                .toList());
        return detail;
    }

    private Map<String, Object> buildSupplierQuoteTierActivityDetail(RfqSupplierQuoteTierEntity entity) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", entity.getId());
        detail.put("quantity", entity.getQuantity());
        detail.put("productPrice", entity.getProductPrice());
        detail.put("shippingCost", entity.getShippingCost());
        detail.put("productPriceCurrency", entity.getProductPriceCurrency() == null
                ? entity.getCurrency()
                : entity.getProductPriceCurrency());
        detail.put("shippingCostCurrency", entity.getShippingCostCurrency() == null
                ? entity.getCurrency()
                : entity.getShippingCostCurrency());
        detail.put("currency", entity.getProductPriceCurrency() == null
                ? entity.getCurrency()
                : entity.getProductPriceCurrency());
        detail.put("sortOrder", entity.getSortOrder());
        return detail;
    }

    private Map<String, Object> buildSupplierQuoteDetailPackageActivityDetail(
            RfqSupplierQuoteDetailPackageEntity entity
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", entity.getId());
        detail.put("packageName", entity.getPackageName());
        detail.put("packageDimension", entity.getPackageDimension());
        detail.put("packageWeight", entity.getPackageWeight());
        detail.put("packageCapacity", entity.getPackageCapacity());
        detail.put("sortOrder", entity.getSortOrder());
        return detail;
    }

    private Map<String, Object> buildSupplierQuoteAdditionalCostActivityDetail(
            RfqSupplierQuoteAdditionalCostEntity entity
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", entity.getId());
        detail.put("description", entity.getDescription());
        detail.put("unit", entity.getUnit());
        detail.put("value", entity.getValue());
        detail.put("sortOrder", entity.getSortOrder());
        return detail;
    }

    private String buildThaiInquiryMessage(RfqHeaderEntity rfq) throws DataNotFoundException {
        String template = promptService.getActivePrompt(RFQ_SUPPLIER_INQUIRY_TEMPLATE_CODE).getUserPromptTemplate();
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("customerName", safeValue(rfq.getContactName()));
        variables.put("salesId", safeValue(rfq.getSales().getNickName()));
        variables.put("procurementId", safeValue(rfq.getProcurement().getNickName()));
        variables.put("rfqId", safeValue(rfq.getId()));
        variables.put("productFamily", displayProductFamily(rfq));
        variables.put("productSubtype1", displayProductSubtype1(rfq));
        variables.put("productSubtype2", displayProductSubtype2(rfq));
        variables.put("material", displayProductMaterial(rfq));
        variables.put("capacity", safeValue(rfq.getCapacity()));
        variables.put("description", safeValue(rfq.getDescription()));
        variables.put("detailSection", buildInquiryDetailSection(rfq));
        variables.put("additionalCostSection", buildInquiryAdditionalCostSection(rfq));
        return promptTemplateEngine.render(template, variables).trim();
    }

    private String buildThaiFinalQuoteInquiryMessage(
            RfqHeaderEntity rfq,
            RfqSupplierQuoteEntity supplierQuote
    ) throws DataNotFoundException {
        String template = promptService.getActivePrompt(RFQ_FINAL_QUOTE_INQUIRY_TH).getUserPromptTemplate();
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("rfqId", safeValue(rfq.getId()));
        variables.put("supplier", supplierQuote.getSupplier() == null ? "-" : safeValue(supplierQuote.getSupplier().getSupplierName()));
        variables.put("sales", rfq.getSales().getNickName());
        variables.put("procurement", rfq.getProcurement().getNickName());
        variables.put("customer", rfq.getContactName());
        variables.put("productFamily", displayProductFamily(rfq));
        variables.put("productSubtype1", displayProductSubtype1(rfq));
        variables.put("productSubtype2", displayProductSubtype2(rfq));
        variables.put("material", displayProductMaterial(rfq));
        variables.put("capacity", safeValue(rfq.getCapacity()));
        variables.put("shippingType", displayShippingType(rfq));
        variables.put("description", safeValue(rfq.getDescription()));
        variables.put("detailSection", buildFinalQuoteInquiryDetailSection(supplierQuote));
        variables.put("additionalCostSection", buildFinalQuoteInquiryAdditionalCostSection(supplierQuote));
        return promptTemplateEngine.render(template, variables).trim();
    }

    private String displayShippingType(RfqHeaderEntity rfq) {
        switch (rfq.getShippingMethod()) {
            case "ALL" : return "ขนส่งทางรถ/ขนส่งทางเรือ";
            case "SHIP" : return "ขนส่งทางเรือ";
            case "LAND" : return "ขนส่งทางรถ";
            default: return "";
        }
    }

    private String translateToChinese(String thaiMessage) {
        String prompt = """
                Translate the following Thai procurement inquiry into Simplified Chinese for a supplier WeChat group.
                Keep the structure, bullet points, quantities, units, and business meaning accurate.
                Return only the Chinese translation.

                %s
                """.formatted(thaiMessage);
        return StringUtils.trimToNull(openAiService.chat(prompt));
    }

    private String buildInquirySourceSnapshot(RfqHeaderEntity rfq) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("rfqId", rfq.getId());
        snapshot.put("productFamily", rfq.getProductFamily());
        snapshot.put("productSubtype1", rfq.getProductUsage() == null ? null : rfq.getProductUsage().getCode());
        snapshot.put("productSubtype2", rfq.getSystemMechanic() == null ? null : rfq.getSystemMechanic().getCode());
        snapshot.put("material", rfq.getMaterialCode());
        snapshot.put("capacity", rfq.getCapacity());
        snapshot.put("description", rfq.getDescription());
        snapshot.put("detailCount", rfq.getDetails() == null ? 0 : rfq.getDetails().size());

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            log.warn("Serialize inquiry source snapshot failed for rfq {}", rfq.getId(), exception);
            return null;
        }
    }

    private RfqHeaderEntity getEntityById(String id) throws DataNotFoundException {
        return requestPriceHeaderRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("RFQ " + id + " not found."));
    }

    private RfqSupplierInquiryEntity getInquiryEntity(String rfqId, String inquiryId) throws DataNotFoundException {
        return rfqSupplierInquiryRepository.findByIdAndRequestPriceHeader_Id(inquiryId, rfqId)
                .orElseThrow(() -> new DataNotFoundException("Inquiry " + inquiryId + " not found in RFQ " + rfqId + "."));
    }

    private RfqSupplierQuoteEntity getSupplierQuoteEntity(String rfqId, String quoteId) throws DataNotFoundException {
        return rfqSupplierQuoteRepository.findByIdAndRequestPriceHeader_Id(quoteId, rfqId)
                .orElseThrow(() -> new DataNotFoundException("Supplier quote " + quoteId + " not found in RFQ " + rfqId + "."));
    }

    private SupplierEntity getSupplierEntity(String supplierId) throws DataNotFoundException {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new DataNotFoundException("Supplier " + supplierId + " not found."));
    }

    private void applySupplierQuoteRequest(
            RfqHeaderEntity rfq,
            RfqSupplierQuoteEntity quote,
            UpsertRfqSupplierQuoteRequest request,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new InvalidRequestException("Supplier quote details are required.");
        }

        quote.setStatus(request.getStatus() == null ? quote.getStatus() : request.getStatus());
        if (quote.getStatus() == null) {
            quote.setStatus(RfqSupplierQuoteStatus.RESPONDED);
        }
        quote.setRemark(StringUtils.trimToNull(request.getRemark()));
        quote.setUpdatedBy(userProfileService.getNameFromId(userId));

        int detailSortOrder = 1;
        for (UpsertRfqSupplierQuoteRequest.DetailRequest detailRequest : request.getDetails()) {
            quote.addDetail(buildSupplierQuoteDetailEntity(
                    rfq,
                    detailRequest,
                    detailRequest.getSortOrder() == null ? detailSortOrder : detailRequest.getSortOrder()
            ));
            detailSortOrder++;
        }

        int additionalCostSortOrder = 1;
        for (UpsertRfqSupplierQuoteRequest.AdditionalCostRequest additionalCostRequest :
                Optional.ofNullable(request.getAdditionalCosts()).orElse(List.of())) {
            quote.addAdditionalCost(buildSupplierQuoteAdditionalCostEntity(
                    additionalCostRequest,
                    additionalCostRequest.getSortOrder() == null ? additionalCostSortOrder : additionalCostRequest.getSortOrder()
            ));
            additionalCostSortOrder++;
        }
    }

    private RfqSupplierQuoteDetailEntity buildSupplierQuoteDetailEntity(
            RfqHeaderEntity rfq,
            UpsertRfqSupplierQuoteRequest.DetailRequest request,
            Integer sortOrder
    ) throws DataNotFoundException, InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Supplier quote detail is required.");
        }
        if (StringUtils.isBlank(request.getSpec())) {
            throw new InvalidRequestException("Supplier quote detail spec is required.");
        }
        if (request.getTiers() == null || request.getTiers().isEmpty()) {
            throw new InvalidRequestException("Supplier quote detail tiers are required.");
        }

        RfqSupplierQuoteDetailEntity entity = new RfqSupplierQuoteDetailEntity();
        if (request.getRfqDetailId() != null) {
            entity.setRequestPriceDetail(getDetailFromHeader(rfq, request.getRfqDetailId()));
        }
        entity.setOptionName(StringUtils.trimToNull(request.getOptionName()));
        entity.setSpec(request.getSpec().trim());
        entity.setRemark(StringUtils.trimToNull(request.getRemark()));
        entity.setSortOrder(sortOrder);

        List<UpsertRfqSupplierQuoteRequest.PackageRequest> packageRequests =
                Optional.ofNullable(request.getPackages()).orElse(List.of());
        String packageDimension = normalizePackageDimension(request.getPackageDimension());
        String packageWeight = normalizePackageWeight(request.getPackageWeight());
        String packageCapacity = normalizePackageCapacity(request.getPackageCapacity());
        if (!packageRequests.isEmpty()) {
            UpsertRfqSupplierQuoteRequest.PackageRequest firstPackageRequest = packageRequests.stream()
                    .sorted(Comparator.comparing(UpsertRfqSupplierQuoteRequest.PackageRequest::getSortOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .findFirst()
                    .orElse(null);
            if (StringUtils.isBlank(packageDimension) && firstPackageRequest != null) {
                packageDimension = normalizePackageDimension(firstPackageRequest.getPackageDimension());
            }
            if (StringUtils.isBlank(packageWeight) && firstPackageRequest != null) {
                packageWeight = normalizePackageWeight(firstPackageRequest.getPackageWeight());
            }
            if (StringUtils.isBlank(packageCapacity) && firstPackageRequest != null) {
                packageCapacity = normalizePackageCapacity(firstPackageRequest.getPackageCapacity());
            }
            int packageSortOrder = 1;
            for (UpsertRfqSupplierQuoteRequest.PackageRequest packageRequest : packageRequests) {
                entity.addPackage(buildSupplierQuoteDetailPackageEntity(
                        packageRequest,
                        packageRequest.getSortOrder() == null ? packageSortOrder : packageRequest.getSortOrder()
                ));
                packageSortOrder++;
            }
        } else if (StringUtils.isNotBlank(request.getPackageDimension())
                || StringUtils.isNotBlank(request.getPackageWeight())
                || StringUtils.isNotBlank(request.getPackageCapacity())) {
            entity.addPackage(buildSupplierQuoteDetailPackageEntity(
                    null,
                    request.getPackageDimension(),
                    request.getPackageWeight(),
                    request.getPackageCapacity(),
                    1
            ));
        }
        entity.setPackageDimension(packageDimension);
        entity.setPackageWeight(packageWeight);
        entity.setPackageCapacity(packageCapacity);

        int tierSortOrder = 1;
        for (UpsertRfqSupplierQuoteRequest.TierRequest tierRequest : request.getTiers()) {
            entity.addTier(buildSupplierQuoteTierEntity(
                    tierRequest,
                    tierRequest.getSortOrder() == null ? tierSortOrder : tierRequest.getSortOrder()
            ));
            tierSortOrder++;
        }
        return entity;
    }

    private RfqSupplierQuoteTierEntity buildSupplierQuoteTierEntity(
            UpsertRfqSupplierQuoteRequest.TierRequest request,
            Integer sortOrder
    ) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Supplier quote tier is required.");
        }
        validatePositive(request.getQuantity(), "tier.quantity");
        validatePositive(request.getProductPrice(), "tier.productPrice");
        validateNonNegative(request.getShippingCost(), "tier.shippingCost");

        RfqSupplierQuoteTierEntity entity = new RfqSupplierQuoteTierEntity();
        entity.setQuantity(request.getQuantity());
        entity.setProductPrice(request.getProductPrice());
        entity.setShippingCost(defaultZero(request.getShippingCost()));
        entity.setProductPriceCurrency(request.getProductPriceCurrency() == null
                ? request.getCurrency()
                : request.getProductPriceCurrency());
        entity.setShippingCostCurrency(request.getShippingCostCurrency() == null
                ? request.getCurrency()
                : request.getShippingCostCurrency());
        entity.setCurrency(entity.getProductPriceCurrency());
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private RfqSupplierQuoteAdditionalCostEntity buildSupplierQuoteAdditionalCostEntity(
            UpsertRfqSupplierQuoteRequest.AdditionalCostRequest request,
            Integer sortOrder
    ) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Supplier quote additional cost is required.");
        }
        if (StringUtils.isBlank(request.getDescription())) {
            throw new InvalidRequestException("Supplier quote additional cost description is required.");
        }

        RfqSupplierQuoteAdditionalCostEntity entity = new RfqSupplierQuoteAdditionalCostEntity();
        entity.setDescription(request.getDescription().trim());
        entity.setUnit(StringUtils.trimToNull(request.getUnit()));
        entity.setValue(StringUtils.trimToNull(request.getValue()));
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private RfqSupplierQuoteDetailPackageEntity buildSupplierQuoteDetailPackageEntity(
            UpsertRfqSupplierQuoteRequest.PackageRequest request,
            Integer sortOrder
    ) {
        return buildSupplierQuoteDetailPackageEntity(
                request == null ? null : request.getPackageName(),
                request == null ? null : request.getPackageDimension(),
                request == null ? null : request.getPackageWeight(),
                request == null ? null : request.getPackageCapacity(),
                sortOrder
        );
    }

    private RfqSupplierQuoteDetailPackageEntity buildSupplierQuoteDetailPackageEntity(
            String packageName,
            String packageDimension,
            String packageWeight,
            String packageCapacity,
            Integer sortOrder
    ) {
        RfqSupplierQuoteDetailPackageEntity entity = new RfqSupplierQuoteDetailPackageEntity();
        entity.setPackageName(StringUtils.trimToNull(packageName));
        entity.setPackageDimension(normalizePackageDimension(packageDimension));
        entity.setPackageWeight(normalizePackageWeight(packageWeight));
        entity.setPackageCapacity(normalizePackageCapacity(packageCapacity));
        entity.setSortOrder(sortOrder);
        return entity;
    }

    private String normalizePackageDimension(String value) {
        return appendPackageUnit(value, "cm.");
    }

    private String normalizePackageWeight(String value) {
        return appendPackageUnit(value, "kg.");
    }

    private String normalizePackageCapacity(String value) {
        return appendPackageUnit(value, "pcs.");
    }

    private String appendPackageUnit(String value, String unit) {
        String normalizedValue = StringUtils.trimToNull(value);
        if (normalizedValue == null) {
            return null;
        }

        String valueWithoutDot = normalizedValue.replaceAll("\\.$", "");
        String normalizedUnit = unit.toLowerCase(Locale.ROOT);
        if (valueWithoutDot.toLowerCase(Locale.ROOT).endsWith(normalizedUnit)) {
            return valueWithoutDot + ".";
        }

        return normalizedValue + " " + normalizedUnit + ".";
    }

    private void validatePositive(BigDecimal value, String fieldName) throws InvalidRequestException {
        if (value == null || value.signum() <= 0) {
            throw new InvalidRequestException(fieldName + " must be greater than 0.");
        }
    }

    private void validateNonNegative(BigDecimal value, String fieldName) throws InvalidRequestException {
        if (value != null && value.signum() < 0) {
            throw new InvalidRequestException(fieldName + " must be 0 or greater.");
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private RfqSupplierInquiryStatus determineInquiryStatus(String chineseMessage, RfqSupplierInquiryStatus currentStatus) {
        if (currentStatus == RfqSupplierInquiryStatus.FINAL) {
            return RfqSupplierInquiryStatus.FINAL;
        }
        return StringUtils.isNotBlank(chineseMessage) ? RfqSupplierInquiryStatus.TRANSLATED : RfqSupplierInquiryStatus.DRAFT;
    }

    private String trimRequiredMessage(String message, String fieldName) throws InvalidRequestException {
        String trimmedMessage = StringUtils.trimToNull(message);
        if (trimmedMessage == null) {
            throw new InvalidRequestException(fieldName + " is required.");
        }
        return trimmedMessage;
    }

    private String displayProductFamily(RfqHeaderEntity rfq) {
        if (rfq.getProductFamilyEntity() != null) {
            return safeValue(rfq.getProductFamilyEntity().getNameTh() != null ? rfq.getProductFamilyEntity().getNameTh() : rfq.getProductFamilyEntity().getNameTh())
                    + " (" + safeValue(rfq.getProductFamilyEntity().getCode()) + ")";
        }
        return safeValue(rfq.getProductFamily());
    }

    private String displayProductSubtype1(RfqHeaderEntity rfq) {
        if (rfq.getProductUsage() != null) {
            return safeValue(rfq.getProductUsage().getNameTh() != null ? rfq.getProductUsage().getNameTh() : rfq.getProductUsage().getNameTh())
                    + " (" + safeValue(rfq.getProductUsage().getCode()) + ")";
        }
        return "-";
    }

    private String displayProductSubtype2(RfqHeaderEntity rfq) {
        if (rfq.getSystemMechanic() != null) {
            return safeValue(rfq.getSystemMechanic().getNameTh() != null ? rfq.getSystemMechanic().getNameTh() : rfq.getSystemMechanic().getNameTh())
                    + " (" + safeValue(rfq.getSystemMechanic().getCode()) + ")";
        }
        return "-";
    }

    private String displayProductMaterial(RfqHeaderEntity rfq) {
        if (rfq.getMaterial() != null) {
            return safeValue(rfq.getMaterial().getNameTh() != null ? rfq.getMaterial().getNameTh() : rfq.getMaterial().getNameTh())
                    + " (" + safeValue(rfq.getMaterial().getCode()) + ")";
        }
        return safeValue(rfq.getMaterialCode());
    }

    private String buildInquiryDetailSection(RfqHeaderEntity rfq) {
        if (rfq.getDetails().isEmpty()) {
            return "-";
        }

        List<String> lines = new ArrayList<>();
        int index = 1;
        for (RfqDetailEntity detail : rfq.getDetails().stream()
                .sorted(Comparator.comparing(RfqDetailEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqDetailEntity::getId))
                .toList()) {
            lines.add(index + ". " + safeValue(detail.getOptionName()));
            lines.add("Spec: " + safeValue(detail.getSpec()));
            if (StringUtils.isNotBlank(detail.getRemark())) {
                lines.add("Remark: " + detail.getRemark().trim());
            }
            if (!detail.getTiers().isEmpty()) {
                lines.add("Quantity tiers:");
                for (RfqTierEntity tier : detail.getTiers().stream()
                        .sorted(Comparator.comparing(RfqTierEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(RfqTierEntity::getId))
                        .toList()) {
                    lines.add("- " + tier.getQuantity().toPlainString() + " units");
                }
            }
            index++;
        }
        return String.join("\n", lines);
    }

    private String buildInquiryAdditionalCostSection(RfqHeaderEntity rfq) {
        if (rfq.getAdditionalCosts().isEmpty()) {
            return "-";
        }

        List<String> lines = new ArrayList<>();
        for (RfqAdditionalCostEntity additionalCost : rfq.getAdditionalCosts().stream()
                .sorted(Comparator.comparing(RfqAdditionalCostEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqAdditionalCostEntity::getId))
                .toList()) {
            lines.add("- " + safeValue(additionalCost.getDescription())
                    + (StringUtils.isNotBlank(additionalCost.getValue()) ? ": " + additionalCost.getValue().trim() : ""));
        }
        return String.join("\n", lines);
    }

    private String buildFinalQuoteInquiryDetailSection(RfqSupplierQuoteEntity supplierQuote) {
        if (supplierQuote.getDetails() == null || supplierQuote.getDetails().isEmpty()) {
            return "-";
        }

        List<String> lines = new ArrayList<>();
        int index = 1;
        for (RfqSupplierQuoteDetailEntity detail : supplierQuote.getDetails().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteDetailEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteDetailEntity::getId))
                .toList()) {
            lines.add("Option ที่ " + index + ". " + safeValue(detail.getOptionName()));
            lines.add("Spec: " + safeValue(detail.getSpec()));
            if (StringUtils.isNotBlank(detail.getRemark())) {
                lines.add("Remark: " + detail.getRemark().trim());
            }
            if (detail.getPackages() != null && !detail.getPackages().isEmpty()) {
                lines.add("Packing lists:");
                for (RfqSupplierQuoteDetailPackageEntity packageEntity : detail.getPackages()) {
                    StringBuilder packages = new StringBuilder(packageEntity.getPackageName());
                    packages.append(" ");
                    packages.append(StringUtils.isNotEmpty(packageEntity.getPackageDimension()) ? packageEntity.getPackageDimension() : "");
                    packages.append(" ");
                    packages.append(StringUtils.isNotEmpty(packageEntity.getPackageWeight()) ? packageEntity.getPackageWeight() : "");
                    packages.append(" ");
                    packages.append(StringUtils.isNotEmpty(packageEntity.getPackageCapacity()) ? packageEntity.getPackageCapacity() : "");
                    lines.add(packages.toString());
                }
            }
            if (detail.getTiers() != null && !detail.getTiers().isEmpty()) {
                lines.add("Quantity tiers:");
                for (RfqSupplierQuoteTierEntity tier : detail.getTiers().stream()
                        .sorted(Comparator.comparing(RfqSupplierQuoteTierEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(RfqSupplierQuoteTierEntity::getId))
                        .toList()) {
                    StringBuilder tierLine = new StringBuilder("- MOQ ")
                            .append(tier.getQuantity() == null ? "-" : tier.getQuantity().stripTrailingZeros().toPlainString());
                    if (tier.getProductPrice() != null) {
                        tierLine.append(", ราคาสินค้า: ")
                                .append(tier.getProductPrice().stripTrailingZeros().toPlainString());
                        if (tier.getProductPriceCurrency() != null) {
                            tierLine.append(" ").append(tier.getProductPriceCurrency().name());
                        }
                    }
                    if (tier.getShippingCost() != null) {
                        tierLine.append(", ค่าขนส่ง: ")
                                .append(tier.getShippingCost().stripTrailingZeros().toPlainString());
                        if (tier.getShippingCostCurrency() != null) {
                            tierLine.append(" ").append(tier.getShippingCostCurrency().name());
                        }
                    }
                    lines.add(tierLine.toString());
                }
            }
            index++;
        }
        return String.join("\n", lines);
    }

    private String buildFinalQuoteInquiryAdditionalCostSection(RfqSupplierQuoteEntity supplierQuote) {
        if (supplierQuote.getAdditionalCosts() == null || supplierQuote.getAdditionalCosts().isEmpty()) {
            return "-";
        }

        List<String> lines = new ArrayList<>();
        for (RfqSupplierQuoteAdditionalCostEntity additionalCost : supplierQuote.getAdditionalCosts().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteAdditionalCostEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteAdditionalCostEntity::getId))
                .toList()) {
            String line = "- " + safeValue(additionalCost.getDescription());
            if (StringUtils.isNotBlank(additionalCost.getValue())) {
                line += ": " + additionalCost.getValue().trim();
            }
            if (StringUtils.isNotBlank(additionalCost.getUnit())) {
                line += " " + additionalCost.getUnit().trim();
            }
            lines.add(line.trim());
        }
        return String.join("\n", lines);
    }

    private String safeValue(String value) {
        String trimmedValue = StringUtils.trimToNull(value);
        return trimmedValue == null ? "-" : trimmedValue;
    }

    private RfqDetailEntity getDetailFromHeader(RfqHeaderEntity entity, Long detailId) throws DataNotFoundException {
        return entity.getDetails().stream()
                .filter(detail -> Objects.equals(detail.getId(), detailId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Detail " + detailId + " not found in RFQ " + entity.getId()));
    }
}
