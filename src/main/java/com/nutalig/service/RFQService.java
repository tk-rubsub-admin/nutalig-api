package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.PromptTemplateEngine;
import com.nutalig.config.TemplateProperties;
import com.nutalig.constant.*;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.rfq.request.*;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.nutalig.entity.id.ProductMaterialId;
import com.nutalig.entity.id.RfqStatusTimelineId;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.RequestPriceHeaderMapper;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.nutalig.constant.BusinessConstant.MessageTemplateCode.RFQ_NOT_FOUND_TH;
import static com.nutalig.constant.BusinessConstant.MessageTemplateCode.RFQ_TRACKING_STATUS_TH;
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
    private final RfqStatusTimelineRepository rfqStatusTimelineRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ProductFamilyRepository productFamilyEntityRepository;
    private final ProductSubtype1Repository productSubtype1Repository;
    private final ProductSubtype2Repository productSubtype2Repository;
    private final ProductMaterialRepository productMaterialRepository;
    private final QuotationRepository quotationRepository;
    private final SupplierRepository supplierRepository;
    private final SystemConfigService systemConfigService;
    private final FileStorageService fileStorageService;
    private final ActivityHistoryService activityHistoryService;
    private final UserProfileService userProfileService;
    private final SlaConfigService slaConfigService;
    private final SupplierService supplierService;
    private final LineMessageService lineMessageService;
    private final ObjectMapper objectMapper;
    private final RequestPriceHeaderMapper requestPriceHeaderMapper;
    private final PromptTemplateEngine promptTemplateEngine;
    private final TemplateProperties templateProperties;

    @Transactional(readOnly = true)
    public com.nutalig.controller.response.Pageable<RfqHeaderDto> getAllRFQ(SearchRFQRequest searchRequest, PageableRequest pageableRequest) {
        if (pageableRequest.getSortBy() == null || pageableRequest.getSortDirection() == null) {
            pageableRequest.setSortBy("requestedDate");
            pageableRequest.setSortDirection(Sort.Direction.DESC);
        }

        Page<RfqHeaderDto> page = requestPriceHeaderRepository.findAll(buildSearchCriteria(searchRequest), pageableRequest.build())
                .map(requestPriceHeaderMapper::toDto);

        com.nutalig.controller.response.Pageable<RfqHeaderDto> response =
                new com.nutalig.controller.response.Pageable<>();
        response.setRecords(page.getContent());
        response.setPagination(Pagination.build(page));
        return response;
    }

    @Transactional
    public RfqHeaderDto getRFQById(String id, String userId) throws DataNotFoundException {
        log.info("Get RFQ by id : {}", id);
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

    @Transactional
    public void getRFQAndSendMessageToLine(String userId, String message) throws Exception {
        log.info("Get RFQ detail line message for id {}", message);

        Map<String, String> variables = new LinkedHashMap<>();
        try {
            RfqHeaderDto rfqHeader = mapToDto(getEntityById(message));

            variables.put("rfqId", StringUtils.defaultString(rfqHeader.getId(), "-"));
            variables.put("status", displayRfqStatus(rfqHeader.getStatus()));
            variables.put("updatedDate", formatRfqUpdatedDate(rfqHeader.getUpdatedDate()));

            String msg = promptTemplateEngine.render(templateProperties.getTexts().get(RFQ_TRACKING_STATUS_TH), variables);

            log.info("Send message {} to user {}", msg, userId);

            lineMessageService.sendTextMessage(userId, msg);
        } catch (DataNotFoundException ex) {
            variables.put("rfqId", message);

            String notFoundMsg = promptTemplateEngine.render(templateProperties.getTexts().get(RFQ_NOT_FOUND_TH), variables);
            lineMessageService.sendTextMessage(userId, notFoundMsg);
        }
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
    public RfqHeaderDto requestInformation(RequestRfqInformationRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        if (request == null || StringUtils.isBlank(request.getRfqId())) {
            throw new InvalidRequestException("rfqId is required.");
        }
        if (StringUtils.isBlank(request.getRequestInformation())) {
            throw new InvalidRequestException("requestInformation is required.");
        }

        RfqHeaderEntity entity = getEntityById(request.getRfqId().trim());
        Map<String, Object> beforeDetail = buildActivityDetail(entity);

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String updatedBy = userProfileService.getNameFromId(userId);
        entity.setRequestInformation(appendRequestInformation(
                entity.getRequestInformation(),
                StringUtils.trimToNull(request.getRequestInformation()),
                updatedBy,
                now
        ));
        entity.setStatus(RfqStatus.REQUESTED_INFO);
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedDate(now);
        entity = requestPriceHeaderRepository.save(entity);

        Map<String, Object> afterDetail = buildActivityDetail(entity);
        Map<String, Object> changedBeforeDetail = new LinkedHashMap<>();
        Map<String, Object> changedAfterDetail = new LinkedHashMap<>();
        extractChangedDetails(beforeDetail, afterDetail, changedBeforeDetail, changedAfterDetail);
        changedBeforeDetail.remove("requestInformation");
        changedAfterDetail.remove("requestInformation");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", changedBeforeDetail);
        detail.put("after", changedAfterDetail);
        detail.put("requestInformation", parseRequestInformation(entity.getRequestInformation()));
        detail.put("requestInformationText", StringUtils.trimToNull(request.getRequestInformation()));

        String summary = "ขอข้อมูลเพิ่มเติมของคำขอราคาเลขที่ " + entity.getId();
        if (StringUtils.isNotBlank(request.getRequestInformation())) {
            summary += " : " + StringUtils.abbreviate(request.getRequestInformation().trim(), 120);
        }

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.REQUEST_INFORMATION,
                ActivitySource.API,
                summary,
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto closeRfq(CloseRfqRequest request, String userId) throws DataNotFoundException, InvalidRequestException {
        if (request == null || StringUtils.isBlank(request.getRfqId())) {
            throw new InvalidRequestException("rfqId is required.");
        }

        RfqHeaderEntity entity = getEntityById(request.getRfqId().trim());
        java.util.Map<String, Object> beforeDetail = buildActivityDetail(entity);

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String updatedBy = userProfileService.getNameFromId(userId);

        entity.setStatus(RfqStatus.CLOSED);
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedDate(now);
        entity.setRemark(request.getRemark());
        entity = requestPriceHeaderRepository.save(entity);

        java.util.Map<String, Object> afterDetail = buildActivityDetail(entity);
        java.util.Map<String, Object> changedBeforeDetail = new LinkedHashMap<>();
        java.util.Map<String, Object> changedAfterDetail = new LinkedHashMap<>();
        extractChangedDetails(beforeDetail, afterDetail, changedBeforeDetail, changedAfterDetail);

        java.util.Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", changedBeforeDetail);
        detail.put("after", changedAfterDetail);

        String summary = "ปิดงานคำขอราคาเลขที่ " + entity.getId();

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.STATUS_CHANGE,
                ActivitySource.API,
                summary,
                detail
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto createRFQ(CreateRequestPriceHeaderRequest request, String userId) throws Exception {
        RfqHeaderEntity entity = requestPriceHeaderMapper.toEntity(request);
        entity.setRequestedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        entity.setStatus(RfqStatus.NEW);
        entity.setShippingMethod(normalizeRfqShippingMethod(request.getShippingMethod()));
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
        detail.put("shippingMethod", entity.getShippingMethod());
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

        saveRfqStatusTimeline(entity, entity.getStatus(), entity.getRequestedDate());

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto addRFQDetail(
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

        if (entity.getStatus() == RfqStatus.SUPPLIER_QUOTED) {
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
                "คำขอราคาเลขที่ " + entity.getId() + " ได้ราคาแล้ว",
                detail
        );

        if (entity.getStatus() == RfqStatus.QUOTED) {
            saveRfqStatusTimeline(entity, RfqStatus.QUOTED, now);
        }

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto addRFQAdditionalCosts(
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
    public RfqHeaderDto updateCustomer(
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
    public RfqHeaderDto linkSalesOrder(
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

        saveRfqStatusTimeline(entity, RfqStatus.COMPLETED, entity.getConfirmedDate());

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto updateRFQDetail(
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
        detailEntity.setRecommend(updatedDetail.getRecommend());
        detailEntity.setPackageDimension(updatedDetail.getPackageDimension());
        detailEntity.setPackageWeight(updatedDetail.getPackageWeight());
        detailEntity.setPackageCapacity(updatedDetail.getPackageCapacity());
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
    public RfqHeaderDto updateRFQAdditionalCost(
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
    public RfqHeaderDto deleteRFQDetail(
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
    public RfqHeaderDto deleteRFQAdditionalCost(
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
    public void updateRFQStatus(String id, RfqStatus status, String userId) throws Exception {
        RfqHeaderEntity entity = getEntityById(id);
        String actor = userProfileService.getNameFromId(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        log.info("Update RFQ {} with new status {} by {}", id, status, actor);
        entity.setStatus(status);
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(now);

        requestPriceHeaderRepository.save(entity);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "ลูกค้าปฏิเสธคำขอราคาเลขที่ " + entity.getId(),
                null
        );

        if (RfqStatus.REJECTED.equals(status) && StringUtils.isNotEmpty(entity.getQuotationNo())) {
            log.info("Update Quotation {} with status {}", entity.getQuotationNo(), status);

            QuotationEntity quotationEntity = quotationRepository.findById(entity.getQuotationNo())
                    .orElse(null);

            if (quotationEntity != null) {
                quotationEntity.setStatus(QuotationStatus.REJECTED);
                quotationEntity.setUpdatedDate(now);
                quotationEntity.setUpdatedBy(actor);

                quotationRepository.save(quotationEntity);

                activityHistoryService.record(
                        ActivityEntityType.QUOTATION,
                        quotationEntity.getQuotationNo(),
                        userId,
                        ActivityActorType.USER,
                        ActivityAction.CREATE,
                        ActivitySource.API,
                        "ลูกค้าปฏิเสธใบเสนอราคาเลขที่ " + quotationEntity.getQuotationNo(),
                        null
                );
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto updateRFQ(String id, UpdateRequestPriceHeaderRequest request, String userId) throws Exception {
        RfqHeaderEntity entity = getEntityById(id);
        java.util.Map<String, Object> beforeDetail = buildActivityDetail(entity);

        if (!hasRFQUpdateChanges(entity, request) && !RfqStatus.REQUESTED_INFO.equals(entity.getStatus())) {
            return mapToDto(entity);
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        List<String> editFields = new ArrayList<>();

        if (RfqStatus.REQUESTED_INFO.equals(entity.getStatus())) {
            log.info("Update status from {} to {}", RfqStatus.REQUESTED_INFO, RfqStatus.IN_PROGRESS);
            entity.setStatus(RfqStatus.IN_PROGRESS);

            SlaConfigDto sla = slaConfigService.getSlaConfigById(SLA);
            entity.setSlaDate(slaConfigService.calculateSlaDate(sla, now));
            editFields.add("สถานะ");
        }

        String requestOrderTypeCode = normalizeRequestValue(request.getOrderTypeCode());
        if (StringUtils.isNotEmpty(requestOrderTypeCode) && !StringUtils.equals(
                requestOrderTypeCode,
                entity.getOrderType() != null && entity.getOrderType().getId() != null
                        ? entity.getOrderType().getId().getCode()
                        : null
        )) {
            entity.setOrderType(resolveOrderType(requestOrderTypeCode));
            editFields.add("ประเภทงาน");
        }
        String requestProductFamily = normalizeRequestValue(request.getProductFamily());
        if (StringUtils.isNotEmpty(requestProductFamily)
                && !StringUtils.equals(requestProductFamily, entity.getProductFamily())) {
            editFields.add("หมวดหมู่หลัก (Product Family)");
        }
        String requestProductUsage = normalizeRequestValue(request.getProductUsage());
        if (StringUtils.isNotEmpty(requestProductUsage)
                && !StringUtils.equals(
                requestProductUsage,
                entity.getProductUsage() != null ? entity.getProductUsage().getCode() : null
        )) {
            editFields.add("Product Subtype 1");
        }
        String requestSystemMechanic = normalizeRequestValue(request.getSystemMechanic());
        if (StringUtils.isNotEmpty(requestSystemMechanic)
                && !StringUtils.equals(
                requestSystemMechanic,
                entity.getSystemMechanic() != null ? entity.getSystemMechanic().getCode() : null
        )) {
            editFields.add("Product Subtype 2");
        }
        String requestMaterial = normalizeRequestValue(request.getMaterial());
        if (StringUtils.isNotEmpty(requestMaterial)
                && !StringUtils.equals(requestMaterial, entity.getMaterialCode())) {
            editFields.add("วัสดุ");
        }
        String requestCapacity = normalizeRequestValue(request.getCapacity());
        if (StringUtils.isNotEmpty(requestCapacity) && !StringUtils.equals(requestCapacity, entity.getCapacity())) {
            entity.setCapacity(request.getCapacity());
            editFields.add("ความจุ");
        }
        String requestDescription = normalizeRequestValue(request.getDescription());
        if (StringUtils.isNotEmpty(requestDescription) && !StringUtils.equals(requestDescription, entity.getDescription())) {
            entity.setDescription(request.getDescription());
            editFields.add("รายละเอียด");
        }

        applyProductHierarchyIfChanged(
                entity,
                requestProductFamily,
                requestProductUsage,
                requestSystemMechanic,
                requestMaterial
        );

        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(now);

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

    private boolean hasRFQUpdateChanges(RfqHeaderEntity entity, UpdateRequestPriceHeaderRequest request) {
        if (entity == null || request == null) {
            return false;
        }

        String requestOrderTypeCode = normalizeRequestValue(request.getOrderTypeCode());
        String requestProductFamily = normalizeRequestValue(request.getProductFamily());
        String requestProductUsage = normalizeRequestValue(request.getProductUsage());
        String requestSystemMechanic = normalizeRequestValue(request.getSystemMechanic());
        String requestMaterial = normalizeRequestValue(request.getMaterial());
        String requestCapacity = normalizeRequestValue(request.getCapacity());
        String requestDescription = normalizeRequestValue(request.getDescription());

        return !StringUtils.equals(
                requestOrderTypeCode,
                entity.getOrderType() != null && entity.getOrderType().getId() != null
                        ? entity.getOrderType().getId().getCode()
                        : null
        ) || !StringUtils.equals(requestProductFamily, entity.getProductFamily())
                || !StringUtils.equals(
                requestProductUsage,
                entity.getProductUsage() != null ? entity.getProductUsage().getCode() : null
        ) || !StringUtils.equals(
                requestSystemMechanic,
                entity.getSystemMechanic() != null ? entity.getSystemMechanic().getCode() : null
        ) || !StringUtils.equals(requestMaterial, entity.getMaterialCode())
                || !StringUtils.equals(requestCapacity, entity.getCapacity())
                || !StringUtils.equals(requestDescription, entity.getDescription());
    }

    private String normalizeRequestValue(String value) {
        return StringUtils.trimToNull(value);
    }

    private void applyProductHierarchyIfChanged(
            RfqHeaderEntity entity,
            String productFamily,
            String productUsage,
            String systemMechanic,
            String material
    ) throws DataNotFoundException, InvalidRequestException {
        if (entity == null) {
            return;
        }

        String currentProductFamily = entity.getProductFamily();
        String currentProductUsage = entity.getProductUsage() != null ? entity.getProductUsage().getCode() : null;
        String currentSystemMechanic =
                entity.getSystemMechanic() != null ? entity.getSystemMechanic().getCode() : null;
        String currentMaterial = entity.getMaterialCode();

        if (StringUtils.equals(productFamily, currentProductFamily)
                && StringUtils.equals(productUsage, currentProductUsage)
                && StringUtils.equals(systemMechanic, currentSystemMechanic)
                && StringUtils.equals(material, currentMaterial)) {
            return;
        }

        applyProductHierarchy(
                entity,
                StringUtils.isNotEmpty(productFamily) ? productFamily : currentProductFamily,
                StringUtils.isNotEmpty(productUsage) ? productUsage : currentProductUsage,
                StringUtils.isNotEmpty(systemMechanic) ? systemMechanic : currentSystemMechanic,
                StringUtils.isNotEmpty(material) ? material : currentMaterial
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto deletePicture(String rfqId, Long pictureId, String userId) throws DataNotFoundException {
        return deleteStoredAttachment(rfqId, pictureId, userId, "ลบรูปภาพของคำขอราคาเลขที่ ");
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto deleteAttachment(String rfqId, Long attachmentId, String userId) throws DataNotFoundException {
        return deleteStoredAttachment(rfqId, attachmentId, userId, "ลบไฟล์แนบของคำขอราคาเลขที่ ");
    }

    @Transactional(rollbackFor = Exception.class)
    public void finalRfqFromLine(String userId, String jsonStr) throws Exception {
        log.info("Final RFQ from line by {}", userId);
        lineMessageService.sendTextMessage(userId, "ระบบกำลังประมวลผล");

        FinalRfqFromLineDto finalRfqFromLineDto = objectMapper.readValue(jsonStr, FinalRfqFromLineDto.class);

        log.info("Final RFQ {} from line", finalRfqFromLineDto.getRfqId());
    }

    private RfqHeaderDto deleteStoredAttachment(String rfqId, Long attachmentId, String userId, String activityMessagePrefix) throws DataNotFoundException {
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
    public RfqHeaderDto replacePicture(String rfqId, Long pictureId, MultipartFile pictureFile, String userId) throws Exception {
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
    public RfqHeaderDto addPictures(String rfqId, List<MultipartFile> pictures, String userId) throws Exception {
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
    public RfqHeaderDto addAttachments(String rfqId, List<MultipartFile> attachments, String userId) throws Exception {
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
    public RfqHeaderDto reorderPictures(String rfqId, ReorderRFQPicturesRequest request, String userId)
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

    public RfqHeaderDto mapToDto(RfqHeaderEntity entity) throws DataNotFoundException {
        return requestPriceHeaderMapper.toDto(entity);
//        dto.setServiceLevelAgreement(slaConfigService.getSlaConfigById(SLA));
//        dto.getServiceLevelAgreement().setDayLeft(slaConfigService.calculateDayLeft(dto.getServiceLevelAgreement(), dto.getRequestedDate().toLocalDate()));
//        return dto;
    }

    private java.util.Map<String, Object> buildActivityDetail(RfqHeaderEntity entity) {
        java.util.Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("requestedDate", entity.getRequestedDate());
        detail.put("status", entity.getStatus());
        detail.put("requestInformation", entity.getRequestInformation());
        detail.put("contactName", entity.getContactName());
        detail.put("contactPhone", entity.getContactPhone());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("orderTypeCode", entity.getOrderType() != null ? entity.getOrderType().getId().getCode() : null);
        detail.put("shippingMethod", entity.getShippingMethod());
        detail.put("productFamily", entity.getProductFamily());
        detail.put("productUsage", entity.getProductUsage() != null ? entity.getProductUsage().getCode() : null);
        detail.put("systemMechanic", entity.getSystemMechanic() != null ? entity.getSystemMechanic().getCode() : null);
        detail.put("material", entity.getMaterialCode());
        detail.put("capacity", entity.getCapacity());
        detail.put("description", entity.getDescription());
        detail.put("pictureCount", entity.getPictures() != null ? entity.getPictures().size() : 0);
        return detail;
    }

    private String appendRequestInformation(
            String existingRequestInformation,
            String requestInformation,
            String requestedBy,
            ZonedDateTime requestedDate
    ) throws InvalidRequestException {
        List<RequestInformationEntry> entries = parseRequestInformation(existingRequestInformation);
        entries.add(new RequestInformationEntry(requestInformation, requestedBy, requestedDate));

        try {
            return objectMapper.writeValueAsString(entries);
        } catch (Exception exception) {
            throw new InvalidRequestException("Cannot serialize request information.");
        }
    }

    private List<RequestInformationEntry> parseRequestInformation(String requestInformationJson) {
        if (StringUtils.isBlank(requestInformationJson)) {
            return new ArrayList<>();
        }

        try {
            RequestInformationEntry[] entries =
                    objectMapper.readValue(requestInformationJson, RequestInformationEntry[].class);
            return new ArrayList<>(Arrays.asList(entries));
        } catch (Exception exception) {
            log.warn("Cannot parse request information json", exception);
            return new ArrayList<>();
        }
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
        detailEntity.setRecommend(StringUtils.trimToNull(request.getRecommend()));
        detailEntity.setPackageDimension(StringUtils.trimToNull(request.getPackageDimension()));
        detailEntity.setPackageWeight(StringUtils.trimToNull(request.getPackageWeight()));
        detailEntity.setPackageCapacity(StringUtils.trimToNull(request.getPackageCapacity()));
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
            tierEntity.setCurrency(tierRequest.getCurrency());
            tierEntity.setExchangeRate(tierRequest.getExchangeRate());
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

    private SupplierEntity getSupplierEntity(String supplierId) throws DataNotFoundException {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new DataNotFoundException("Supplier " + supplierId + " not found."));
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

    private String normalizeRfqShippingMethod(String shippingMethod) throws InvalidRequestException {
        String normalized = StringUtils.defaultIfBlank(shippingMethod, "ALL").trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "LAND", "SEA").contains(normalized)) {
            throw new InvalidRequestException("shippingMethod must be ALL, LAND, or SEA");
        }
        return normalized;
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
                .and(procurementIdEqual(request.getProcurementId()))
                .and(orderTypeCodeEqual(
                        request.getOrderTypeCode() != null && !request.getOrderTypeCode().isBlank()
                                ? request.getOrderTypeCode()
                                : request.getOrderType()
                ))
                .and(productFamilyEqual(request.getProductFamily()))
                .and(requestedDateBetween(request.getRequestedDateStart(), request.getRequestedDateEnd()))
                .and(keywordContain(request.getKeyword()));
    }

    private String displayRfqStatus(RfqStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case NEW -> "ใหม่";
            case IN_PROGRESS -> "กำลังดำเนินการ";
            case SUPPLIER_QUOTED -> "ซัพพลายเออร์ตอบแล้ว";
            case REQUESTED_INFO -> "ขอข้อมูลเพิ่มเติม";
            case QUOTED -> "เสนอราคาแล้ว";
            case CANCELED -> "ยกเลิก";
            case CLOSED -> "ปิดงาน";
            case COMPLETED -> "เสร็จสิ้น";
            case REJECTED -> "ปฏิเสธ";
        };
    }

    private String formatRfqUpdatedDate(ZonedDateTime updatedDate) {
        if (updatedDate == null) {
            return "-";
        }
        return updatedDate
                .withZoneSameInstant(DateUtil.getTimeZone())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
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

    private record RequestInformationEntry(
            String requestInformation,
            String requestedBy,
            ZonedDateTime requestedDate
    ) {
    }
}
