package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.PromptTemplateEngine;
import com.nutalig.constant.*;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.rfq.request.*;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.nutalig.entity.id.ProductMaterialId;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.RequestPriceHeaderMapper;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RFQService {
    private final static String SLA = "SLA-RFQ-PRICE";
    private final static String PROCUREMENT_ROLE_CODE = "PROCUREMENT";
    private final static String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
    private final static String PICTURE_FILE_TYPE = "PICTURE";
    private final static String OTHER_FILE_TYPE = "OTHER";
    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final RequestPricePicturesRepository requestPricePicturesRepository;
    private final RequestPriceDetailRepository requestPriceDetailRepository;
    private final RequestPriceTierRepository requestPriceTierRepository;
    private final RequestPriceHeaderMapper requestPriceHeaderMapper;
    private final SalesRepository salesRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ProductFamilyRepository productFamilyEntityRepository;
    private final ProductSubtype1Repository productSubtype1Repository;
    private final ProductSubtype2Repository productSubtype2Repository;
    private final ProductMaterialRepository productMaterialRepository;
    private final RfqSupplierInquiryRepository rfqSupplierInquiryRepository;
    private final RfqSupplierQuoteRepository rfqSupplierQuoteRepository;
    private final SupplierRepository supplierRepository;
    private final SystemConfigService systemConfigService;
    private final FileStorageService fileStorageService;
    private final ActivityHistoryService activityHistoryService;
    private final UserProfileService userProfileService;
    private final SlaConfigService slaConfigService;
    private final SupplierService supplierService;
    private final OpenAiService openAiService;
    private final PromptService promptService;
    private final PromptTemplateEngine promptTemplateEngine;
    private final ObjectMapper objectMapper;
    private final SupplierMapper supplierMapper;
    private static final String RFQ_SUPPLIER_INQUIRY_TEMPLATE_CODE = "RFQ_SUPPLIER_INQUIRY_TH";

    @Transactional(readOnly = true)
    public com.nutalig.controller.response.Pageable<RequestPriceHeaderDto> getAllRFQ(SearchRFQRequest searchRequest, PageableRequest pageableRequest) {
        if (pageableRequest.getSortBy() == null || pageableRequest.getSortDirection() == null) {
            pageableRequest.setSortBy("requestedDate");
            pageableRequest.setSortDirection(Sort.Direction.DESC);
        }

        Page<RequestPriceHeaderDto> page = requestPriceHeaderRepository.findAll(buildSearchCriteria(searchRequest), pageableRequest.build())
                .map(requestPriceHeaderMapper::toDto);

        com.nutalig.controller.response.Pageable<RequestPriceHeaderDto> response =
                new com.nutalig.controller.response.Pageable<>();
        response.setRecords(page.getContent());
        response.setPagination(Pagination.build(page));
        return response;
    }

    @Transactional
    public RequestPriceHeaderDto getRFQById(String id, String userId) throws DataNotFoundException {
        RfqHeaderEntity entity = getEntityById(id);

        if (shouldMoveToInProgressOnView(entity, userId)) {
            entity.setStatus(RfqStatus.IN_PROGRESS);
            entity.setUpdatedBy(userProfileService.getNameFromId(userId));
            entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
            entity = requestPriceHeaderRepository.save(entity);

            activityHistoryService.record(
                    ActivityEntityType.RFQ,
                    entity.getId(),
                    userId,
                    ActivityActorType.USER,
                    ActivityAction.VIEW,
                    ActivitySource.API,
                    "จัดซื้อดูคำขอราคาเลขที่ " + entity.getId(),
                    null
            );
        }
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> suggestSuppliers(String id) throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity entity = getEntityById(id);

        String productFamilyCode = StringUtils.trimToNull(entity.getProductFamily());
        if (productFamilyCode == null) {
            throw new InvalidRequestException("RFQ " + id + " does not have product family.");
        }

        String productMaterialCode = StringUtils.trimToNull(entity.getMaterialCode());
        return supplierService.suggestSuppliers(productFamilyCode, productMaterialCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqSupplierInquiryDto generateInquiry(
            String rfqId,
            GenerateRfqSupplierInquiryRequest request,
            String userId
    )
            throws Exception {
        RfqHeaderEntity rfq = getEntityById(rfqId);

        if (request == null || StringUtils.isBlank(request.getSupplierId())) {
            throw new InvalidRequestException("Supplier id is required.");
        }
        SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new DataNotFoundException("Supplier " + request.getSupplierId() + " not found."));

        Optional<RfqSupplierInquiryEntity> existingInquiry =
                rfqSupplierInquiryRepository.findFirstByRequestPriceHeader_IdAndSupplier_IdOrderByVersionNoDesc(
                        rfqId,
                        supplier.getId()
                );
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

        String actorName = userProfileService.getNameFromId(userId);

        RfqSupplierInquiryEntity inquiry = new RfqSupplierInquiryEntity();
        inquiry.setRequestPriceHeader(rfq);
        inquiry.setSupplier(supplier);
        inquiry.setVersionNo(nextVersionNo);
        inquiry.setStatus(status);
        inquiry.setThaiMessage(thaiMessage);
        inquiry.setChineseMessage(StringUtils.trimToNull(chineseMessage));
        inquiry.setSourceSnapshot(buildInquirySourceSnapshot(rfq));
        inquiry.setCreatedBy(actorName);
        inquiry.setUpdatedBy(actorName);

        inquiry = rfqSupplierInquiryRepository.save(inquiry);
        return toInquiryDto(inquiry);
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
        if (rfqSupplierQuoteRepository.findByRequestPriceHeader_IdAndSupplier_Id(rfqId, supplier.getId()).isPresent()) {
            throw new InvalidRequestException("Supplier quote already exists for supplier " + supplier.getId() + ".");
        }

        RfqSupplierQuoteEntity quote = new RfqSupplierQuoteEntity();
        quote.setRequestPriceHeader(rfq);
        quote.setSupplier(supplier);
        quote.setStatus(request.getStatus() == null ? RfqSupplierQuoteStatus.RESPONDED : request.getStatus());
        quote.setCreatedBy(userProfileService.getNameFromId(userId));
        applySupplierQuoteRequest(rfq, quote, request, userId);

        return toSupplierQuoteDto(rfqSupplierQuoteRepository.save(quote));
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
        if (StringUtils.isNotBlank(request.getSupplierId())
                && !Objects.equals(request.getSupplierId(), quote.getSupplier().getId())) {
            SupplierEntity supplier = getSupplierEntity(request.getSupplierId());
            Optional<RfqSupplierQuoteEntity> existingQuote =
                    rfqSupplierQuoteRepository.findByRequestPriceHeader_IdAndSupplier_Id(rfqId, supplier.getId());
            if (existingQuote.isPresent() && !Objects.equals(existingQuote.get().getId(), quote.getId())) {
                throw new InvalidRequestException("Supplier quote already exists for supplier " + supplier.getId() + ".");
            }
            quote.setSupplier(supplier);
        }

        quote.getDetails().clear();
        quote.getAdditionalCosts().clear();
        applySupplierQuoteRequest(rfq, quote, request, userId);
        return toSupplierQuoteDto(rfqSupplierQuoteRepository.save(quote));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplierQuote(String rfqId, String quoteId) throws DataNotFoundException {
        RfqSupplierQuoteEntity quote = getSupplierQuoteEntity(rfqId, quoteId);
        rfqSupplierQuoteRepository.delete(quote);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto createRFQ(CreateRequestPriceHeaderRequest request, String userId) throws Exception {
        RfqHeaderEntity entity = requestPriceHeaderMapper.toEntity(request);
        entity.setRequestedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        entity.setStatus(RfqStatus.NEW);
        entity.setCreatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));

        applyRelations(entity, request.getSalesId(), request.getCustomerId(), request.getOrderTypeCode(), request.getProcurementId());
        applyProductHierarchy(
                entity,
                request.getProductFamily(),
                request.getProductUsage(),
                request.getSystemMechanic(),
                request.getMaterial()
        );
        attachPictures(entity, request.getPictures(), PICTURE_FILE_TYPE, userId);
        attachPictures(entity, request.getAttachments(), OTHER_FILE_TYPE, userId);

        if (entity.getCustomer() != null) {
            entity.setContactName(entity.getCustomer().getContacts().getFirst().getContactName());
            entity.setContactPhone(entity.getCustomer().getContacts().getFirst().getContactNumber());
        } else {
            entity.setContactName(request.getContactName());
            entity.setContactPhone(request.getContactPhone());
        }

        SlaConfigDto sla = slaConfigService.getSlaConfigById(SLA);
        entity.setSlaDate(slaConfigService.calculateSlaDate(sla, entity.getRequestedDate()));

        entity = requestPriceHeaderRepository.save(entity);

        java.util.Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("status", entity.getStatus());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("pictureCount", entity.getPictures() != null ? entity.getPictures().size() : 0);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้างคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto addRFQDetail(
            String rfqId,
            List<CreateRequestPriceDetailRequest> requests,
            String userId
    ) throws Exception {
        RfqHeaderEntity entity = getEntityById(rfqId);
        if (requests == null || requests.isEmpty()) {
            throw new InvalidRequestException("details are required");
        }

        String updatedBy = userProfileService.getNameFromId(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        List<Map<String, Object>> addedDetails = new ArrayList<>();
        for (CreateRequestPriceDetailRequest request : requests) {
            RfqDetailEntity detailEntity = buildRequestPriceDetailEntity(request, updatedBy);
            entity.addDetail(detailEntity);

            Map<String, Object> addedDetail = new LinkedHashMap<>();
            addedDetail.put("optionName", detailEntity.getOptionName());
            addedDetail.put("tierCount", detailEntity.getTiers().size());
            addedDetails.add(addedDetail);
        }

        if (entity.getStatus() == RfqStatus.IN_PROGRESS) {
            entity.setStatus(RfqStatus.QUOTED);
            entity.setQuotedDate(now);
        }

        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedDate(now);
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("count", addedDetails.size());
        detail.put("details", addedDetails);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "เพิ่มรายละเอียดราคาคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto addRFQAdditionalCosts(
            String rfqId,
            List<CreateRequestPriceAdditionalCostRequest> requests,
            String userId
    ) throws Exception {
        RfqHeaderEntity entity = getEntityById(rfqId);
        if (requests == null || requests.isEmpty()) {
            throw new InvalidRequestException("additionalCosts are required");
        }

        String updatedBy = userProfileService.getNameFromId(userId);
        List<Map<String, Object>> addedAdditionalCosts = new ArrayList<>();
        for (CreateRequestPriceAdditionalCostRequest request : requests) {
            RfqAdditionalCostEntity additionalCostEntity = buildRequestPriceAdditionalCostEntity(request);
            entity.addAdditionalCost(additionalCostEntity);

            Map<String, Object> addedAdditionalCost = new LinkedHashMap<>();
            addedAdditionalCost.put("description", additionalCostEntity.getDescription());
            addedAdditionalCost.put("value", additionalCostEntity.getValue());
            addedAdditionalCost.put("sortOrder", additionalCostEntity.getSortOrder());
            addedAdditionalCosts.add(addedAdditionalCost);
        }

        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("count", addedAdditionalCosts.size());
        detail.put("additionalCosts", addedAdditionalCosts);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "เพิ่มค่าใช้จ่ายเพิ่มเติมของคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto updateCustomer(
        String rfqId,
        UpdateRfqCustomerRequest request,
        String userId
    ) throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity entity = getEntityById(rfqId);
        if (request == null || request.getCustomerId().isEmpty()) {
            throw new InvalidRequestException("CustomerId is required");
        }
        CustomerEntity customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new DataNotFoundException("Customer " + request.getCustomerId() + " not found."));

        String updatedBy = userProfileService.getNameFromId(userId);

        entity.setCustomer(customer);
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto linkSalesOrder(
            String rfqId,
            LinkRfqSalesOrderRequest request,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity entity = getEntityById(rfqId);
        if (request == null || StringUtils.isBlank(request.getSaleOrderId())) {
            throw new InvalidRequestException("saleOrderId is required");
        }
        if (request.getDetailId() == null) {
            throw new InvalidRequestException("detailId is required");
        }
        if (request.getTierId() == null) {
            throw new InvalidRequestException("tierId is required");
        }
        if (StringUtils.isNotBlank(entity.getSaleOrderId())
                && !entity.getSaleOrderId().equals(request.getSaleOrderId().trim())) {
            throw new InvalidRequestException("RFQ " + rfqId + " already linked to sale order " + entity.getSaleOrderId());
        }

        RfqDetailEntity detail = getDetailFromHeader(entity, request.getDetailId());
        RfqTierEntity tier = detail.getTiers().stream()
                .filter(item -> Objects.equals(item.getId(), request.getTierId()))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Tier " + request.getTierId() + " not found."));

        String shippingMethod = StringUtils.trimToNull(request.getShippingMethod());
        if (!"LAND".equalsIgnoreCase(shippingMethod) && !"SEA".equalsIgnoreCase(shippingMethod)) {
            throw new InvalidRequestException("shippingMethod must be LAND or SEA");
        }

        BigDecimal confirmedPrice = request.getPrice();
        if (confirmedPrice == null) {
            confirmedPrice = "SEA".equalsIgnoreCase(shippingMethod) ? tier.getSeaTotalPrice() : tier.getLandTotalPrice();
        }

        entity.setSaleOrderId(request.getSaleOrderId().trim());
        entity.setConfirmedDetailId(detail.getId());
        entity.setConfirmedTierId(tier.getId());
        entity.setConfirmedShippingMethod(shippingMethod.toUpperCase(Locale.ROOT));
        entity.setConfirmedPrice(confirmedPrice);
        entity.setConfirmedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        entity.setStatus(RfqStatus.COMPLETED);
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> activityDetail = new LinkedHashMap<>();
        activityDetail.put("saleOrderId", entity.getSaleOrderId());
        activityDetail.put("detailId", entity.getConfirmedDetailId());
        activityDetail.put("tierId", entity.getConfirmedTierId());
        activityDetail.put("shippingMethod", entity.getConfirmedShippingMethod());
        activityDetail.put("price", entity.getConfirmedPrice());

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "สร้าง Sales Order จากคำขอราคาเลขที่ " + entity.getId(),
                activityDetail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto updateRFQDetail(
            String rfqId,
            Long detailId,
            UpdateRequestPriceDetailRequest request,
            String userId
    ) throws Exception {
        RfqHeaderEntity entity = getEntityById(rfqId);
        RfqDetailEntity detailEntity = getDetailFromHeader(entity, detailId);

        RfqDetailEntity updatedDetail = buildRequestPriceDetailEntity(
                request,
                userProfileService.getNameFromId(userId)
        );

        detailEntity.setOptionName(updatedDetail.getOptionName());
        detailEntity.setSpec(updatedDetail.getSpec());
        detailEntity.setSortOrder(updatedDetail.getSortOrder());
        detailEntity.setRemark(updatedDetail.getRemark());
        detailEntity.setUpdatedBy(updatedDetail.getUpdatedBy());
        detailEntity.getTiers().clear();
        updatedDetail
                .getTiers()
                .forEach(detailEntity::addTier);

        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("detailId", detailEntity.getId());
        detail.put("optionName", detailEntity.getOptionName());
        detail.put("tierCount", detailEntity.getTiers().size());

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "แก้ไขรายละเอียดคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto updateRFQAdditionalCost(
            String rfqId,
            Long additionalCostId,
            UpdateRequestPriceAdditionalCostRequest request,
            String userId
    ) throws Exception {
        RfqHeaderEntity entity = getEntityById(rfqId);
        RfqAdditionalCostEntity additionalCostEntity = getAdditionalCostFromHeader(entity, additionalCostId);
        RfqAdditionalCostEntity updatedAdditionalCost = buildRequestPriceAdditionalCostEntity(request);

        additionalCostEntity.setDescription(updatedAdditionalCost.getDescription());
        additionalCostEntity.setUnit(updatedAdditionalCost.getUnit());
        additionalCostEntity.setValue(updatedAdditionalCost.getValue());
        additionalCostEntity.setSortOrder(updatedAdditionalCost.getSortOrder());

        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("additionalCostId", additionalCostEntity.getId());
        detail.put("description", additionalCostEntity.getDescription());
        detail.put("value", additionalCostEntity.getValue());

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "แก้ไขค่าใช้จ่ายเพิ่มเติมของคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto deleteRFQDetail(
            String rfqId,
            Long detailId,
            String userId
    ) throws Exception {
        RfqHeaderEntity entity = getEntityById(rfqId);
        RfqDetailEntity detailEntity = getDetailFromHeader(entity, detailId);
        String optionName = detailEntity.getOptionName();

        entity.removeDetail(detailEntity);
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("detailId", detailId);
        detail.put("optionName", optionName);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.DELETE,
                ActivitySource.API,
                "ลบรายละเอียดคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto deleteRFQAdditionalCost(
            String rfqId,
            Long additionalCostId,
            String userId
    ) throws Exception {
        RfqHeaderEntity entity = getEntityById(rfqId);
        RfqAdditionalCostEntity additionalCostEntity = getAdditionalCostFromHeader(entity, additionalCostId);
        String description = additionalCostEntity.getDescription();

        entity.removeAdditionalCost(additionalCostEntity);
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("additionalCostId", additionalCostId);
        detail.put("description", description);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.DELETE,
                ActivitySource.API,
                "ลบค่าใช้จ่ายเพิ่มเติมของคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto updateRFQ(String id, UpdateRequestPriceHeaderRequest request, String userId) throws Exception {
        RfqHeaderEntity entity = getEntityById(id);
        java.util.Map<String, Object> beforeDetail = buildActivityDetail(entity);

        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        List<String> editFields = new ArrayList<>();

        if (StringUtils.isNotEmpty(request.getOrderTypeCode())) {
            entity.setOrderType(resolveOrderType(request.getOrderTypeCode()));
            editFields.add("ประเภทงาน");
        }
        if (StringUtils.isNotEmpty(request.getProductFamily())) {
            editFields.add("หมวดหมู่หลัก (Product Family)");
        }
        if (StringUtils.isNotEmpty(request.getProductUsage())) {
            editFields.add("Product Subtype 1");
        }
        if (StringUtils.isNotEmpty(request.getSystemMechanic())) {
            editFields.add("Product Subtype 2");
        }
        if (StringUtils.isNotEmpty(request.getMaterial())) {
            editFields.add("วัสดุ");
        }
        if (StringUtils.isNotEmpty(request.getCapacity())) {
            entity.setCapacity(request.getCapacity());
            editFields.add("ความจุ");
        }
        if (StringUtils.isNotEmpty(request.getDescription())) {
            entity.setDescription(request.getDescription());
            editFields.add("รายละเอียด");
        }

        applyProductHierarchy(
                entity,
                StringUtils.isNotEmpty(request.getProductFamily()) ? request.getProductFamily() : entity.getProductFamily(),
                StringUtils.isNotEmpty(request.getProductUsage())
                        ? request.getProductUsage()
                        : entity.getProductUsage() == null ? null : entity.getProductUsage().getCode(),
                StringUtils.isNotEmpty(request.getSystemMechanic())
                        ? request.getSystemMechanic()
                        : entity.getSystemMechanic() == null ? null : entity.getSystemMechanic().getCode(),
                StringUtils.isNotEmpty(request.getMaterial())
                        ? request.getMaterial()
                        : entity.getMaterialCode()
        );

        entity = requestPriceHeaderRepository.save(entity);
        java.util.Map<String, Object> afterDetail = buildActivityDetail(entity);
        java.util.Map<String, Object> changedBeforeDetail = new LinkedHashMap<>();
        java.util.Map<String, Object> changedAfterDetail = new LinkedHashMap<>();
        extractChangedDetails(beforeDetail, afterDetail, changedBeforeDetail, changedAfterDetail);

        java.util.Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", changedBeforeDetail);
        detail.put("after", changedAfterDetail);

        String summary = "แก้ไขคำขอราคาเลขที่ " + entity.getId();
        if (!editFields.isEmpty()) {
            summary += " : แก้ไขฟิลด์ " + String.join(", ", editFields);
        }

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                summary,
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto deletePicture(String rfqId, Long pictureId, String userId) throws DataNotFoundException {
        return deleteStoredAttachment(rfqId, pictureId, userId, "ลบรูปภาพของคำขอราคาเลขที่ ");
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto deleteAttachment(String rfqId, Long attachmentId, String userId) throws DataNotFoundException {
        return deleteStoredAttachment(rfqId, attachmentId, userId, "ลบไฟล์แนบของคำขอราคาเลขที่ ");
    }

    private RequestPriceHeaderDto deleteStoredAttachment(String rfqId, Long attachmentId, String userId, String activityMessagePrefix) throws DataNotFoundException {
        RfqHeaderEntity entity = getEntityById(rfqId);
        RfqPicturesEntity picture = getPictureFromHeader(entity, attachmentId);

        entity.removePicture(picture);
        normalizePictureSort(entity);

        requestPriceHeaderRepository.save(entity);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.DELETE_PICTURE,
                ActivitySource.WEB,
                activityMessagePrefix + entity.getId(),
                null
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto replacePicture(String rfqId, Long pictureId, MultipartFile pictureFile, String userId) throws Exception {
        if (pictureFile == null || pictureFile.isEmpty()) {
            throw new InvalidRequestException("Picture file is required");
        }

        RfqHeaderEntity entity = getEntityById(rfqId);
        RfqPicturesEntity picture = getPictureFromHeader(entity, pictureId);
        UploadFileResponse uploadedFile = fileStorageService.uploadFile(pictureFile);

        picture.setPictureUrl(uploadedFile.getUrl());
        picture.setFileName(uploadedFile.getFileName());
        if (StringUtils.isBlank(picture.getFileType())) {
            picture.setFileType(PICTURE_FILE_TYPE);
        }
        picture.setUpdatedBy(userProfileService.getNameFromId(userId));
        picture.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));

        requestPricePicturesRepository.save(picture);
        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto addPictures(String rfqId, List<MultipartFile> pictures, String userId) throws Exception {
        if (pictures == null || pictures.isEmpty()) {
            throw new InvalidRequestException("Pictures are required");
        }

        RfqHeaderEntity entity = getEntityById(rfqId);
        attachPictures(entity, pictures, PICTURE_FILE_TYPE, userId);
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));

        entity = requestPriceHeaderRepository.save(entity);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPLOAD_PICTURE,
                ActivitySource.WEB,
                "เพิ่มรูปภาพของคำขอราคาเลขที่ " + entity.getId(),
                null
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto addAttachments(String rfqId, List<MultipartFile> attachments, String userId) throws Exception {
        if (attachments == null || attachments.isEmpty()) {
            throw new InvalidRequestException("Attachments are required");
        }

        RfqHeaderEntity entity = getEntityById(rfqId);
        attachPictures(entity, attachments, OTHER_FILE_TYPE, userId);
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));

        entity = requestPriceHeaderRepository.save(entity);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPLOAD_PICTURE,
                ActivitySource.WEB,
                "เพิ่มไฟล์แนบของคำขอราคาเลขที่ " + entity.getId(),
                null
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto reorderPictures(String rfqId, ReorderRFQPicturesRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity entity = getEntityById(rfqId);

        List<RfqPicturesEntity> currentPictures = new ArrayList<>(entity.getPictures());
        List<Long> requestedIds = request.getPictureIds();
        if (requestedIds == null || requestedIds.isEmpty()) {
            throw new InvalidRequestException("pictureIds is required");
        }

        List<Long> currentIds = currentPictures.stream()
                .map(RfqPicturesEntity::getId)
                .sorted()
                .toList();
        List<Long> normalizedRequestedIds = requestedIds.stream().sorted().toList();

        if (!currentIds.equals(normalizedRequestedIds)) {
            throw new InvalidRequestException("pictureIds must contain all existing picture ids exactly once");
        }

        for (int i = 0; i < request.getPictureIds().size(); i++) {
            Long pictureId = request.getPictureIds().get(i);
            RfqPicturesEntity picture = getPictureFromHeader(entity, pictureId);
            picture.setSort(i + 1);
            picture.setUpdatedBy(userProfileService.getNameFromId(userId));
            picture.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        }

        requestPricePicturesRepository.saveAll(entity.getPictures());
        return mapToDto(entity);
    }

    public RequestPriceHeaderDto mapToDto(RfqHeaderEntity entity) throws DataNotFoundException {
        return requestPriceHeaderMapper.toDto(entity);
//        dto.setServiceLevelAgreement(slaConfigService.getSlaConfigById(SLA));
//        dto.getServiceLevelAgreement().setDayLeft(slaConfigService.calculateDayLeft(dto.getServiceLevelAgreement(), dto.getRequestedDate().toLocalDate()));
//        return dto;
    }

    private RfqSupplierInquiryDto toInquiryDto(RfqSupplierInquiryEntity entity) throws DataNotFoundException {
        RfqSupplierInquiryDto dto = new RfqSupplierInquiryDto();
        dto.setId(entity.getId());
        dto.setRfqId(entity.getRequestPriceHeader().getId());
        dto.setSupplierId(entity.getSupplier().getId());
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

    private RfqSupplierQuoteDto toSupplierQuoteDto(RfqSupplierQuoteEntity entity) {
        RfqSupplierQuoteDto dto = new RfqSupplierQuoteDto();
        dto.setId(entity.getId());
        dto.setRfqId(entity.getRequestPriceHeader().getId());
        dto.setSupplier(supplierMapper.toDto(entity.getSupplier()));
        dto.setInquiryId(entity.getInquiry() == null ? null : entity.getInquiry().getId());
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
        dto.setTiers(entity.getTiers().stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteTierEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteTierEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toSupplierQuoteTierDto)
                .toList());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private RfqSupplierQuoteTierDto toSupplierQuoteTierDto(RfqSupplierQuoteTierEntity entity) {
        RfqSupplierQuoteTierDto dto = new RfqSupplierQuoteTierDto();
        dto.setId(entity.getId());
        dto.setQuantity(entity.getQuantity());
        dto.setProductPrice(entity.getProductPrice());
        dto.setLandFreightCost(entity.getLandFreightCost());
        dto.setSeaFreightCost(entity.getSeaFreightCost());
        dto.setLandTotalPrice(entity.getLandTotalPrice());
        dto.setSeaTotalPrice(entity.getSeaTotalPrice());
        dto.setSortOrder(entity.getSortOrder());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
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

    private String buildThaiInquiryMessage(RfqHeaderEntity rfq) throws DataNotFoundException {
        String template = promptService.getActivePrompt(RFQ_SUPPLIER_INQUIRY_TEMPLATE_CODE).getUserPromptTemplate();
        Map<String, String> variables = new LinkedHashMap<>();
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

    private java.util.Map<String, Object> buildActivityDetail(RfqHeaderEntity entity) {
        java.util.Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("requestedDate", entity.getRequestedDate());
        detail.put("status", entity.getStatus());
        detail.put("contactName", entity.getContactName());
        detail.put("contactPhone", entity.getContactPhone());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("orderTypeCode", entity.getOrderType() != null ? entity.getOrderType().getId().getCode() : null);
        detail.put("productFamily", entity.getProductFamily());
        detail.put("productUsage", entity.getProductUsage() != null ? entity.getProductUsage().getCode() : null);
        detail.put("systemMechanic", entity.getSystemMechanic() != null ? entity.getSystemMechanic().getCode() : null);
        detail.put("material", entity.getMaterialCode());
        detail.put("capacity", entity.getCapacity());
        detail.put("description", entity.getDescription());
        detail.put("pictureCount", entity.getPictures() != null ? entity.getPictures().size() : 0);
        return detail;
    }

    private boolean shouldMoveToInProgressOnView(RfqHeaderEntity entity, String userId) {
        if (entity == null || userId == null || entity.getStatus() != RfqStatus.NEW) {
            return false;
        }

        String roleCode = userProfileService.getRoleCodeFromId(userId);
        return PROCUREMENT_ROLE_CODE.equalsIgnoreCase(roleCode)
                || SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(roleCode);
    }

    private void extractChangedDetails(
            Map<String, Object> beforeDetail,
            Map<String, Object> afterDetail,
            Map<String, Object> changedBeforeDetail,
            Map<String, Object> changedAfterDetail
    ) {
        for (Map.Entry<String, Object> entry : beforeDetail.entrySet()) {
            String key = entry.getKey();
            Object beforeValue = entry.getValue();
            Object afterValue = afterDetail.get(key);

            if (!Objects.equals(beforeValue, afterValue)) {
                changedBeforeDetail.put(key, beforeValue);
                changedAfterDetail.put(key, afterValue);
            }
        }
    }

    private RfqDetailEntity buildRequestPriceDetailEntity(
            CreateRequestPriceDetailRequest request,
            String updatedBy
    ) throws InvalidRequestException, DataNotFoundException {
        if (StringUtils.isBlank(request.getSpec())) {
            throw new InvalidRequestException("spec is required");
        }
        if (request.getTiers() == null || request.getTiers().isEmpty()) {
            throw new InvalidRequestException("tiers are required");
        }

        RfqDetailEntity detailEntity = new RfqDetailEntity();
        detailEntity.setOptionName(StringUtils.trimToNull(request.getOptionName()));
        detailEntity.setSpec(request.getSpec().trim());
        detailEntity.setSortOrder(request.getSortOrder());
        detailEntity.setRemark(StringUtils.trimToNull(request.getRemark()));
        SupplierEntity supplier = null;
        if (StringUtils.isNotBlank(request.getSupplierId())) {
            supplier = getSupplierEntity(request.getSupplierId().trim());
            detailEntity.setSupplier(supplier);
        }
        detailEntity.setUpdatedBy(updatedBy);

        int nextSortOrder = 1;
        for (CreateRequestPriceDetailRequest.CreateRequestPriceTierRequest tierRequest : request.getTiers()) {
            if (tierRequest.getQuantity() == null) {
                throw new InvalidRequestException("tier.quantity is required");
            }
            if (tierRequest.getProductPrice() == null) {
                throw new InvalidRequestException("tier.productPrice is required");
            }

            RfqTierEntity tierEntity = new RfqTierEntity();
            tierEntity.setSupplier(supplier);
            tierEntity.setQuantity(tierRequest.getQuantity());
            tierEntity.setProductPrice(tierRequest.getProductPrice());
            tierEntity.setLandFreightCost(tierRequest.getLandFreightCost());
            tierEntity.setSeaFreightCost(tierRequest.getSeaFreightCost());
            tierEntity.setLandTotalPrice(tierRequest.getLandTotalPrice());
            tierEntity.setSeaTotalPrice(tierRequest.getSeaTotalPrice());
            tierEntity.setSortOrder(
                    tierRequest.getSortOrder() != null ? tierRequest.getSortOrder() : nextSortOrder++
            );
            detailEntity.addTier(tierEntity);
        }

        return detailEntity;
    }

    private RfqAdditionalCostEntity buildRequestPriceAdditionalCostEntity(
            CreateRequestPriceAdditionalCostRequest request
    ) throws InvalidRequestException, DataNotFoundException {
        if (request == null) {
            throw new InvalidRequestException("additionalCost is required");
        }
        if (StringUtils.isBlank(request.getDescription())) {
            throw new InvalidRequestException("description is required");
        }
        if (request.getValue() == null) {
            throw new InvalidRequestException("value is required");
        }

        RfqAdditionalCostEntity additionalCostEntity = new RfqAdditionalCostEntity();
        if (StringUtils.isNotBlank(request.getSupplierId())) {
            additionalCostEntity.setSupplier(getSupplierEntity(request.getSupplierId().trim()));
        }
        additionalCostEntity.setDescription(request.getDescription().trim());
        additionalCostEntity.setUnit(StringUtils.trimToNull(request.getUnit()));
        additionalCostEntity.setValue(request.getValue());
        additionalCostEntity.setSortOrder(request.getSortOrder());
        return additionalCostEntity;
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

        if (StringUtils.isNotBlank(request.getInquiryId())) {
            RfqSupplierInquiryEntity inquiry = getInquiryEntity(rfq.getId(), request.getInquiryId());
            if (!Objects.equals(inquiry.getSupplier().getId(), quote.getSupplier().getId())) {
                throw new InvalidRequestException("Inquiry supplier does not match quote supplier.");
            }
            quote.setInquiry(inquiry);
        } else {
            quote.setInquiry(null);
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
        validateNonNegative(request.getLandFreightCost(), "tier.landFreightCost");
        validateNonNegative(request.getSeaFreightCost(), "tier.seaFreightCost");

        BigDecimal landFreightCost = defaultZero(request.getLandFreightCost());
        BigDecimal seaFreightCost = defaultZero(request.getSeaFreightCost());

        RfqSupplierQuoteTierEntity entity = new RfqSupplierQuoteTierEntity();
        entity.setQuantity(request.getQuantity());
        entity.setProductPrice(request.getProductPrice());
        entity.setLandFreightCost(landFreightCost);
        entity.setSeaFreightCost(seaFreightCost);
        entity.setLandTotalPrice(request.getLandTotalPrice() == null
                ? request.getProductPrice().add(landFreightCost)
                : request.getLandTotalPrice());
        entity.setSeaTotalPrice(request.getSeaTotalPrice() == null
                ? request.getProductPrice().add(seaFreightCost)
                : request.getSeaTotalPrice());
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

    private void applyRelations(RfqHeaderEntity entity, String salesId, String customerId, String orderTypeCode, String procurementId)
            throws DataNotFoundException {
        entity.setSales(resolveSales(salesId));
        entity.setCustomer(resolveCustomer(customerId));
        entity.setOrderType(resolveOrderType(orderTypeCode));
        entity.setProcurement(resolveProcurement(procurementId));
    }

    private void applyProductHierarchy(
            RfqHeaderEntity entity,
            String productFamilyCodeInput,
            String productSubtype1CodeInput,
            String productSubtype2CodeInput,
            String productMaterialCodeInput
    ) throws DataNotFoundException, InvalidRequestException {
        String productFamilyCode = StringUtils.trimToNull(productFamilyCodeInput);
        String productSubtype1Code = StringUtils.trimToNull(productSubtype1CodeInput);
        String productSubtype2Code = StringUtils.trimToNull(productSubtype2CodeInput);
        String productMaterialCode = StringUtils.trimToNull(productMaterialCodeInput);

        ProductSubtype1Entity productSubtype1 = resolveProductSubtype1(productSubtype1Code);
        ProductSubtype2Entity productSubtype2 = resolveProductSubtype2(productSubtype2Code);

        if (productSubtype2 != null) {
            if (productSubtype1 != null && !StringUtils.equals(productSubtype2.getProductSubtype1Code(), productSubtype1.getCode())) {
                throw new InvalidRequestException("Product subtype2 does not belong to product subtype1.");
            }
            if (productSubtype1 == null) {
                productSubtype1 = productSubtype2.getProductSubtype1Entity();
            }
        }

        if (productSubtype1 != null) {
            if (productFamilyCode != null && !StringUtils.equals(productSubtype1.getProductFamilyCode(), productFamilyCode)) {
                throw new InvalidRequestException("Product subtype1 does not belong to product family.");
            }
            if (productFamilyCode == null) {
                productFamilyCode = productSubtype1.getProductFamilyCode();
            }
        }

        ProductMaterialEntity productMaterial = resolveProductMaterial(productFamilyCode, productMaterialCode);
        ProductFamilyEntity productFamily = resolveProductFamily(productFamilyCode);

        entity.setProductFamily(productFamilyCode);
        entity.setProductFamilyEntity(productFamily);
        entity.setProductUsage(productSubtype1);
        entity.setSystemMechanic(productSubtype2);
        entity.setMaterialCode(productMaterialCode);
        entity.setMaterial(productMaterial);
    }

    private EmployeeEntity resolveSales(String salesId) throws DataNotFoundException {
        if (StringUtils.isBlank(salesId)) {
            return null;
        }

        return employeeRepository.findById(salesId.trim())
                .orElseThrow(() -> new DataNotFoundException("Sales " + salesId + " not found."));
    }

    private EmployeeEntity resolveProcurement(String procurementId) throws DataNotFoundException {
        if (StringUtils.isBlank(procurementId)) {
            return null;
        }

        return employeeRepository.findById(procurementId.trim())
                .orElseThrow(() -> new DataNotFoundException("Procurement " + procurementId + " not found."));
    }

    private ProductFamilyEntity resolveProductFamily(String productFamilyCode) throws DataNotFoundException {
        if (StringUtils.isBlank(productFamilyCode)) {
            return null;
        }

        return productFamilyEntityRepository.findById(productFamilyCode.trim())
                .orElseThrow(() -> new DataNotFoundException("Product family code " + productFamilyCode + " not found."));
    }

    private ProductSubtype1Entity resolveProductSubtype1(String productSubtype1Code) throws DataNotFoundException {
        if (StringUtils.isBlank(productSubtype1Code)) {
            return null;
        }

        return productSubtype1Repository.findById(productSubtype1Code.trim())
                .orElseThrow(() -> new DataNotFoundException("Product subtype1 code " + productSubtype1Code + " not found."));
    }

    private ProductSubtype2Entity resolveProductSubtype2(String productSubtype2Code) throws DataNotFoundException {
        if (StringUtils.isBlank(productSubtype2Code)) {
            return null;
        }

        return productSubtype2Repository.findById(productSubtype2Code.trim())
                .orElseThrow(() -> new DataNotFoundException("Product subtype2 code " + productSubtype2Code + " not found."));
    }

    private ProductMaterialEntity resolveProductMaterial(String productFamilyCode, String productMaterialCode)
            throws DataNotFoundException, InvalidRequestException {
        if (StringUtils.isBlank(productMaterialCode)) {
            return null;
        }
        if (StringUtils.isBlank(productFamilyCode)) {
            throw new InvalidRequestException("Product family is required when material is provided.");
        }

        ProductMaterialId productMaterialId = new ProductMaterialId();
        productMaterialId.setProductFamilyCode(productFamilyCode.trim());
        productMaterialId.setCode(productMaterialCode.trim());

        return productMaterialRepository.findById(productMaterialId)
                .orElseThrow(() -> new DataNotFoundException(
                        "Product material code " + productMaterialCode + " not found in family " + productFamilyCode + "."
                ));
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
            return safeValue(rfq.getProductFamilyEntity().getNameEn() != null ? rfq.getProductFamilyEntity().getNameEn() : rfq.getProductFamilyEntity().getNameTh())
                    + " (" + safeValue(rfq.getProductFamilyEntity().getCode()) + ")";
        }
        return safeValue(rfq.getProductFamily());
    }

    private String displayProductSubtype1(RfqHeaderEntity rfq) {
        if (rfq.getProductUsage() != null) {
            return safeValue(rfq.getProductUsage().getNameEn() != null ? rfq.getProductUsage().getNameEn() : rfq.getProductUsage().getNameTh())
                    + " (" + safeValue(rfq.getProductUsage().getCode()) + ")";
        }
        return "-";
    }

    private String displayProductSubtype2(RfqHeaderEntity rfq) {
        if (rfq.getSystemMechanic() != null) {
            return safeValue(rfq.getSystemMechanic().getNameEn() != null ? rfq.getSystemMechanic().getNameEn() : rfq.getSystemMechanic().getNameTh())
                    + " (" + safeValue(rfq.getSystemMechanic().getCode()) + ")";
        }
        return "-";
    }

    private String displayProductMaterial(RfqHeaderEntity rfq) {
        if (rfq.getMaterial() != null) {
            return safeValue(rfq.getMaterial().getNameEn() != null ? rfq.getMaterial().getNameEn() : rfq.getMaterial().getNameTh())
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

    private String safeValue(String value) {
        String trimmedValue = StringUtils.trimToNull(value);
        return trimmedValue == null ? "-" : trimmedValue;
    }

    private CustomerEntity resolveCustomer(String customerId) throws DataNotFoundException {
        if (StringUtils.isBlank(customerId)) {
            return null;
        }

        return customerRepository.findById(customerId.trim())
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));
    }

    private SystemConfigEntity resolveOrderType(String orderTypeCode) throws DataNotFoundException {
        if (StringUtils.isBlank(orderTypeCode)) {
            return null;
        }

        SystemConfigEntity orderType = systemConfigService.getConfigEntity(SystemConstant.ORDER_TYPE, orderTypeCode.trim());
        if (orderType == null) {
            throw new DataNotFoundException("Order type " + orderTypeCode + " not found.");
        }
        return orderType;
    }

    private SystemConfigEntity resolveCostType(String costTypeCode) throws DataNotFoundException {
        SystemConfigEntity costType = systemConfigService.getConfigEntity(SystemConstant.COST_TYPE, costTypeCode.trim());
        if (costType == null) {
            throw new DataNotFoundException("Cost type " + costTypeCode + " not found.");
        }
        return costType;
    }

    private void attachPictures(RfqHeaderEntity entity, List<MultipartFile> pictures, String fileType, String userId) throws Exception {
        if (pictures == null || pictures.isEmpty()) {
            return;
        }

        int nextSort = entity.getPictures().stream()
                .map(RfqPicturesEntity::getSort)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        for (MultipartFile picture : pictures) {
            if (picture == null || picture.isEmpty()) {
                continue;
            }

            UploadFileResponse uploadedFile = fileStorageService.uploadFile(picture);

            RfqPicturesEntity pictureEntity = new RfqPicturesEntity();
            pictureEntity.setPictureUrl(uploadedFile.getUrl());
            pictureEntity.setFileName(uploadedFile.getFileName());
            pictureEntity.setFileType(fileType);
            pictureEntity.setSort(nextSort++);
            pictureEntity.setUpdatedBy(userProfileService.getNameFromId(userId));
            pictureEntity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));

            entity.addPicture(pictureEntity);
        }
    }

    private RfqPicturesEntity getPictureFromHeader(RfqHeaderEntity entity, Long pictureId) throws DataNotFoundException {
        return entity.getPictures().stream()
                .filter(picture -> Objects.equals(picture.getId(), pictureId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Picture " + pictureId + " not found in RFQ " + entity.getId()));
    }

    private RfqDetailEntity getDetailFromHeader(RfqHeaderEntity entity, Long detailId) throws DataNotFoundException {
        return entity.getDetails().stream()
                .filter(detail -> Objects.equals(detail.getId(), detailId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Detail " + detailId + " not found in RFQ " + entity.getId()));
    }

    private RfqAdditionalCostEntity getAdditionalCostFromHeader(RfqHeaderEntity entity, Long additionalCostId)
            throws DataNotFoundException {
        return entity.getAdditionalCosts().stream()
                .filter(additionalCost -> Objects.equals(additionalCost.getId(), additionalCostId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(
                        "Additional cost " + additionalCostId + " not found in RFQ " + entity.getId()
                ));
    }

    private void normalizePictureSort(RfqHeaderEntity entity) {
        List<RfqPicturesEntity> sortedPictures = entity.getPictures().stream()
                .sorted(Comparator.comparing(RfqPicturesEntity::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqPicturesEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        for (int i = 0; i < sortedPictures.size(); i++) {
            sortedPictures.get(i).setSort(i + 1);
        }
    }

    private Specification<RfqHeaderEntity> buildSearchCriteria(SearchRFQRequest request) {
        if (request == null) {
            return Specification.where(null);
        }

        List<RfqStatus> statuses = request.getStatuses();
        if (statuses == null || statuses.isEmpty()) {
            statuses = request.getStatus() == null ? null : List.of(request.getStatus());
        }

        return Specification.where(idEqual(request.getId()))
                .and(statusIn(statuses))
                .and(customerIdEqual(request.getCustomerId()))
                .and(salesIdEqual(request.getSalesId()))
                .and(orderTypeCodeEqual(request.getOrderTypeCode()))
                .and(keywordContain(request.getKeyword()));
    }
}
