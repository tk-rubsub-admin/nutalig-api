package com.nutalig.service;

import com.nutalig.constant.ActivityAction;
import com.nutalig.constant.ActivityActorType;
import com.nutalig.constant.ActivityEntityType;
import com.nutalig.constant.ActivitySource;
import com.nutalig.constant.RFQStatus;
import com.nutalig.constant.SystemConstant;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.rfq.request.CreateRequestPriceAdditionalCostRequest;
import com.nutalig.controller.rfq.request.CreateRequestPriceDetailRequest;
import com.nutalig.controller.rfq.request.CreateRequestPriceHeaderRequest;
import com.nutalig.controller.rfq.request.ReorderRFQPicturesRequest;
import com.nutalig.controller.rfq.request.SearchRFQRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceAdditionalCostRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceDetailRequest;
import com.nutalig.controller.rfq.request.UpdateRequestPriceHeaderRequest;
import com.nutalig.dto.RequestPriceHeaderDto;
import com.nutalig.dto.SlaConfigDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.RequestPriceHeaderMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.customerIdEqual;
import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.idEqual;
import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.keywordContain;
import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.orderTypeCodeEqual;
import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.salesIdEqual;
import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.statusEqual;

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
    private final SystemConfigService systemConfigService;
    private final FileStorageService fileStorageService;
    private final ActivityHistoryService activityHistoryService;
    private final UserProfileService userProfileService;
    private final SlaConfigService slaConfigService;

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
        RequestPriceHeaderEntity entity = getEntityById(id);

        if (shouldMoveToInProgressOnView(entity, userId)) {
            entity.setStatus(RFQStatus.IN_PROGRESS);
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

    @Transactional(rollbackFor = Exception.class)
    public RequestPriceHeaderDto createRFQ(CreateRequestPriceHeaderRequest request, String userId) throws Exception {
        RequestPriceHeaderEntity entity = requestPriceHeaderMapper.toEntity(request);
        entity.setRequestedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        entity.setStatus(RFQStatus.NEW);
        entity.setCreatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));

        applyRelations(entity, request.getSalesId(), request.getCustomerId(), request.getOrderTypeCode(), request.getProcurementId());
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
        RequestPriceHeaderEntity entity = getEntityById(rfqId);
        if (requests == null || requests.isEmpty()) {
            throw new InvalidRequestException("details are required");
        }

        String updatedBy = userProfileService.getNameFromId(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        List<Map<String, Object>> addedDetails = new ArrayList<>();
        for (CreateRequestPriceDetailRequest request : requests) {
            RequestPriceDetailEntity detailEntity = buildRequestPriceDetailEntity(request, updatedBy);
            entity.addDetail(detailEntity);

            Map<String, Object> addedDetail = new LinkedHashMap<>();
            addedDetail.put("optionName", detailEntity.getOptionName());
            addedDetail.put("tierCount", detailEntity.getTiers().size());
            addedDetails.add(addedDetail);
        }

        if (entity.getStatus() == RFQStatus.IN_PROGRESS) {
            entity.setStatus(RFQStatus.QUOTED);
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
        RequestPriceHeaderEntity entity = getEntityById(rfqId);
        if (requests == null || requests.isEmpty()) {
            throw new InvalidRequestException("additionalCosts are required");
        }

        String updatedBy = userProfileService.getNameFromId(userId);
        List<Map<String, Object>> addedAdditionalCosts = new ArrayList<>();
        for (CreateRequestPriceAdditionalCostRequest request : requests) {
            RequestPriceAdditionalCostEntity additionalCostEntity = buildRequestPriceAdditionalCostEntity(request);
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
    public RequestPriceHeaderDto updateRFQDetail(
            String rfqId,
            Long detailId,
            UpdateRequestPriceDetailRequest request,
            String userId
    ) throws Exception {
        RequestPriceHeaderEntity entity = getEntityById(rfqId);
        RequestPriceDetailEntity detailEntity = getDetailFromHeader(entity, detailId);

        RequestPriceDetailEntity updatedDetail = buildRequestPriceDetailEntity(
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
        RequestPriceHeaderEntity entity = getEntityById(rfqId);
        RequestPriceAdditionalCostEntity additionalCostEntity = getAdditionalCostFromHeader(entity, additionalCostId);
        RequestPriceAdditionalCostEntity updatedAdditionalCost = buildRequestPriceAdditionalCostEntity(request);

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
        RequestPriceHeaderEntity entity = getEntityById(rfqId);
        RequestPriceDetailEntity detailEntity = getDetailFromHeader(entity, detailId);
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
        RequestPriceHeaderEntity entity = getEntityById(rfqId);
        RequestPriceAdditionalCostEntity additionalCostEntity = getAdditionalCostFromHeader(entity, additionalCostId);
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
        RequestPriceHeaderEntity entity = getEntityById(id);
        java.util.Map<String, Object> beforeDetail = buildActivityDetail(entity);

        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        List<String> editFields = new ArrayList<>();

        if (StringUtils.isNotEmpty(request.getOrderTypeCode())) {
            entity.setOrderType(resolveOrderType(request.getOrderTypeCode()));
            editFields.add("ประเภทงาน");
        }
        if (StringUtils.isNotEmpty(request.getProductFamily())) {
            ProductFamilyEntity productFamilyEntity = productFamilyEntityRepository
                    .getReferenceById(request.getProductFamily());
            entity.setProductFamilyEntity(productFamilyEntity);
            editFields.add("หมวดหมู่หลัก (Product Family)");
        }
        if (StringUtils.isNotEmpty(request.getSystemMechanic())) {
            entity.setSystemMechanic(request.getSystemMechanic());
            editFields.add("System mechanic");
        }
        if (StringUtils.isNotEmpty(request.getMaterial())) {
            entity.setMaterial(request.getMaterial());
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
        RequestPriceHeaderEntity entity = getEntityById(rfqId);
        RequestPricePicturesEntity picture = getPictureFromHeader(entity, attachmentId);

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

        RequestPriceHeaderEntity entity = getEntityById(rfqId);
        RequestPricePicturesEntity picture = getPictureFromHeader(entity, pictureId);
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

        RequestPriceHeaderEntity entity = getEntityById(rfqId);
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

        RequestPriceHeaderEntity entity = getEntityById(rfqId);
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
        RequestPriceHeaderEntity entity = getEntityById(rfqId);

        List<RequestPricePicturesEntity> currentPictures = new ArrayList<>(entity.getPictures());
        List<Long> requestedIds = request.getPictureIds();
        if (requestedIds == null || requestedIds.isEmpty()) {
            throw new InvalidRequestException("pictureIds is required");
        }

        List<Long> currentIds = currentPictures.stream()
                .map(RequestPricePicturesEntity::getId)
                .sorted()
                .toList();
        List<Long> normalizedRequestedIds = requestedIds.stream().sorted().toList();

        if (!currentIds.equals(normalizedRequestedIds)) {
            throw new InvalidRequestException("pictureIds must contain all existing picture ids exactly once");
        }

        for (int i = 0; i < request.getPictureIds().size(); i++) {
            Long pictureId = request.getPictureIds().get(i);
            RequestPricePicturesEntity picture = getPictureFromHeader(entity, pictureId);
            picture.setSort(i + 1);
            picture.setUpdatedBy(userProfileService.getNameFromId(userId));
            picture.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        }

        requestPricePicturesRepository.saveAll(entity.getPictures());
        return mapToDto(entity);
    }

    public RequestPriceHeaderDto mapToDto(RequestPriceHeaderEntity entity) throws DataNotFoundException {
        return requestPriceHeaderMapper.toDto(entity);
//        dto.setServiceLevelAgreement(slaConfigService.getSlaConfigById(SLA));
//        dto.getServiceLevelAgreement().setDayLeft(slaConfigService.calculateDayLeft(dto.getServiceLevelAgreement(), dto.getRequestedDate().toLocalDate()));
//        return dto;
    }

    private java.util.Map<String, Object> buildActivityDetail(RequestPriceHeaderEntity entity) {
        java.util.Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("requestedDate", entity.getRequestedDate());
        detail.put("status", entity.getStatus());
        detail.put("contactName", entity.getContactName());
        detail.put("contactPhone", entity.getContactPhone());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("orderTypeCode", entity.getOrderType() != null ? entity.getOrderType().getId().getCode() : null);
        detail.put("productFamily", entity.getProductFamily());
        detail.put("productUsage", entity.getProductUsage());
        detail.put("systemMechanic", entity.getSystemMechanic());
        detail.put("material", entity.getMaterial());
        detail.put("capacity", entity.getCapacity());
        detail.put("description", entity.getDescription());
        detail.put("pictureCount", entity.getPictures() != null ? entity.getPictures().size() : 0);
        return detail;
    }

    private boolean shouldMoveToInProgressOnView(RequestPriceHeaderEntity entity, String userId) {
        if (entity == null || userId == null || entity.getStatus() != RFQStatus.NEW) {
            return false;
        }

        String roleCode = userProfileService.getRoleCodeFromId(userId);
        return PROCUREMENT_ROLE_CODE.equalsIgnoreCase(roleCode) || SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(roleCode);
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

    private RequestPriceDetailEntity buildRequestPriceDetailEntity(
            CreateRequestPriceDetailRequest request,
            String updatedBy
    ) throws InvalidRequestException {
        if (StringUtils.isBlank(request.getSpec())) {
            throw new InvalidRequestException("spec is required");
        }
        if (request.getTiers() == null || request.getTiers().isEmpty()) {
            throw new InvalidRequestException("tiers are required");
        }

        RequestPriceDetailEntity detailEntity = new RequestPriceDetailEntity();
        detailEntity.setOptionName(StringUtils.trimToNull(request.getOptionName()));
        detailEntity.setSpec(request.getSpec().trim());
        detailEntity.setSortOrder(request.getSortOrder());
        detailEntity.setRemark(StringUtils.trimToNull(request.getRemark()));
        detailEntity.setUpdatedBy(updatedBy);

        int nextSortOrder = 1;
        for (CreateRequestPriceDetailRequest.CreateRequestPriceTierRequest tierRequest : request.getTiers()) {
            if (tierRequest.getQuantity() == null) {
                throw new InvalidRequestException("tier.quantity is required");
            }
            if (tierRequest.getProductPrice() == null) {
                throw new InvalidRequestException("tier.productPrice is required");
            }

            RequestPriceTierEntity tierEntity = new RequestPriceTierEntity();
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

    private RequestPriceAdditionalCostEntity buildRequestPriceAdditionalCostEntity(
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

        RequestPriceAdditionalCostEntity additionalCostEntity = new RequestPriceAdditionalCostEntity();
        additionalCostEntity.setDescription(request.getDescription().trim());
        additionalCostEntity.setUnit(StringUtils.trimToNull(request.getUnit()));
        additionalCostEntity.setValue(request.getValue());
        additionalCostEntity.setSortOrder(request.getSortOrder());
        return additionalCostEntity;
    }

    private RequestPriceHeaderEntity getEntityById(String id) throws DataNotFoundException {
        return requestPriceHeaderRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("RFQ " + id + " not found."));
    }

    private void applyRelations(RequestPriceHeaderEntity entity, String salesId, String customerId, String orderTypeCode, String procurementId)
            throws DataNotFoundException {
        entity.setSales(resolveSales(salesId));
        entity.setCustomer(resolveCustomer(customerId));
        entity.setOrderType(resolveOrderType(orderTypeCode));
        entity.setProcurement(resolveProcurement(procurementId));
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

    private void attachPictures(RequestPriceHeaderEntity entity, List<MultipartFile> pictures, String fileType, String userId) throws Exception {
        if (pictures == null || pictures.isEmpty()) {
            return;
        }

        int nextSort = entity.getPictures().stream()
                .map(RequestPricePicturesEntity::getSort)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        for (MultipartFile picture : pictures) {
            if (picture == null || picture.isEmpty()) {
                continue;
            }

            UploadFileResponse uploadedFile = fileStorageService.uploadFile(picture);

            RequestPricePicturesEntity pictureEntity = new RequestPricePicturesEntity();
            pictureEntity.setPictureUrl(uploadedFile.getUrl());
            pictureEntity.setFileName(uploadedFile.getFileName());
            pictureEntity.setFileType(fileType);
            pictureEntity.setSort(nextSort++);
            pictureEntity.setUpdatedBy(userProfileService.getNameFromId(userId));
            pictureEntity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));

            entity.addPicture(pictureEntity);
        }
    }

    private RequestPricePicturesEntity getPictureFromHeader(RequestPriceHeaderEntity entity, Long pictureId) throws DataNotFoundException {
        return entity.getPictures().stream()
                .filter(picture -> Objects.equals(picture.getId(), pictureId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Picture " + pictureId + " not found in RFQ " + entity.getId()));
    }

    private RequestPriceDetailEntity getDetailFromHeader(RequestPriceHeaderEntity entity, Long detailId) throws DataNotFoundException {
        return entity.getDetails().stream()
                .filter(detail -> Objects.equals(detail.getId(), detailId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Detail " + detailId + " not found in RFQ " + entity.getId()));
    }

    private RequestPriceAdditionalCostEntity getAdditionalCostFromHeader(RequestPriceHeaderEntity entity, Long additionalCostId)
            throws DataNotFoundException {
        return entity.getAdditionalCosts().stream()
                .filter(additionalCost -> Objects.equals(additionalCost.getId(), additionalCostId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(
                        "Additional cost " + additionalCostId + " not found in RFQ " + entity.getId()
                ));
    }

    private void normalizePictureSort(RequestPriceHeaderEntity entity) {
        List<RequestPricePicturesEntity> sortedPictures = entity.getPictures().stream()
                .sorted(Comparator.comparing(RequestPricePicturesEntity::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RequestPricePicturesEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        for (int i = 0; i < sortedPictures.size(); i++) {
            sortedPictures.get(i).setSort(i + 1);
        }
    }

    private Specification<RequestPriceHeaderEntity> buildSearchCriteria(SearchRFQRequest request) {
        if (request == null) {
            return Specification.where(null);
        }

        return Specification.where(idEqual(request.getId()))
                .and(statusEqual(request.getStatus()))
                .and(customerIdEqual(request.getCustomerId()))
                .and(salesIdEqual(request.getSalesId()))
                .and(orderTypeCodeEqual(request.getOrderTypeCode()))
                .and(keywordContain(request.getKeyword()));
    }
}
