package com.nutalig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.AppProperties;
import com.nutalig.config.LineConfiguration;
import com.nutalig.config.PromptTemplateEngine;
import com.nutalig.config.TemplateProperties;
import com.nutalig.constant.*;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.rfq.request.*;
import com.nutalig.controller.rfq.response.UploadRfqErrorResponse;
import com.nutalig.controller.rfq.response.UploadRfqResponse;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.nutalig.entity.id.ProductMaterialId;
import com.nutalig.entity.id.RfqStatusTimelineId;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.RequestPriceHeaderMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import com.nutalig.utils.DocumentStatusResolver;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.nutalig.constant.BusinessConstant.MessageTemplateCode.RFQ_NOT_FOUND_TH;
import static com.nutalig.constant.BusinessConstant.MessageTemplateCode.RFQ_TRACKING_STATUS_TH;
import static com.nutalig.repository.specification.RequestPriceHeaderSpecification.*;
import static com.nutalig.utils.RfqUtil.*;
import static com.nutalig.utils.ObjectUtil.safeValue;

@Slf4j
@Service
@RequiredArgsConstructor
public class RFQService {
    private final static String SLA = "SLA-RFQ-PRICE";
    private final static String PROCUREMENT_ROLE_CODE = "PROCUREMENT";
    private final static String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
    private final static String PICTURE_FILE_TYPE = "PICTURE";
    private final static String OTHER_FILE_TYPE = "OTHER";
    private final static String RFQ_CUSTOMER_QUOTED_TH = "RFQ_CUSTOMER_QUOTED_TH";
    private static final int MONEY_SCALE = 4;
    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final RequestPricePicturesRepository requestPricePicturesRepository;
    private final RfqStatusTimelineRepository rfqStatusTimelineRepository;
    private final EmployeeProcurementMappingRepository employeeProcurementMappingRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ProductFamilyRepository productFamilyEntityRepository;
    private final ProductSubtype1Repository productSubtype1Repository;
    private final ProductSubtype2Repository productSubtype2Repository;
    private final ProductMaterialRepository productMaterialRepository;
    private final QuotationRepository quotationRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final RfqSupplierQuoteRepository rfqSupplierQuoteRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigService systemConfigService;
    private final FileStorageService fileStorageService;
    private final ActivityHistoryService activityHistoryService;
    private final UserProfileService userProfileService;
    private final SlaConfigService slaConfigService;
    private final SupplierService supplierService;
    private final UserRepository userRepository;
    private final UserTodoService userTodoService;
    private final LineMessageService lineMessageService;
    private final ApprovalService approvalService;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final RequestPriceHeaderMapper requestPriceHeaderMapper;
    private final PromptTemplateEngine promptTemplateEngine;
    private final TemplateProperties templateProperties;
    private final LineConfiguration lineConfiguration;

    @Transactional(readOnly = true)
    public com.nutalig.controller.response.Pageable<RfqHeaderDto> getAllRFQ(SearchRFQRequest searchRequest, PageableRequest pageableRequest) {
        if (Boolean.TRUE.equals(searchRequest != null ? searchRequest.getPrioritizeApprovedUrgent() : null)) {
            pageableRequest.setSortBy(null);
            pageableRequest.setSortDirection(null);
        } else if (pageableRequest.getSortBy() == null || pageableRequest.getSortDirection() == null) {
            pageableRequest.setSortBy("requestedDate");
            pageableRequest.setSortDirection(Sort.Direction.DESC);
        }

        Page<RfqHeaderDto> page = requestPriceHeaderRepository.findAll(buildSearchCriteria(searchRequest), pageableRequest.build())
                .map(requestPriceHeaderMapper::toDto);

        com.nutalig.controller.response.Pageable<RfqHeaderDto> response =
                new com.nutalig.controller.response.Pageable<>();
        List<RfqHeaderDto> records = new ArrayList<>(page.getContent());
        if (Boolean.TRUE.equals(searchRequest != null ? searchRequest.getIsCreatedPurchaseOrder() : null)) {
            enrichAndFilterCreatedPurchaseOrders(records);
        }
        response.setRecords(records);
        response.setPagination(Pagination.build(page));
        return response;
    }

    @Transactional(readOnly = true)
    public byte[] exportRFQ(SearchRFQRequest searchRequest) {
        List<RfqHeaderDto> records = requestPriceHeaderRepository.findAll(
                        buildSearchCriteria(searchRequest),
                        Sort.by(Sort.Direction.DESC, "requestedDate")
                ).stream()
                .map(requestPriceHeaderMapper::toDto)
                .toList();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("RFQ");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "เลข RFQ",
                    "วันที่ขอราคา",
                    "สถานะ",
                    "รหัสลูกค้า",
                    "ชื่อลูกค้า",
                    "รหัสเซลล์",
                    "ชื่อเซลล์",
                    "รหัสจัดซื้อ",
                    "ชื่อจัดซื้อ",
                    "ประเภท RFQ",
                    "ประเภทงาน",
                    "ผู้ติดต่อ",
                    "เบอร์ติดต่อ",
                    "ช่องทางติดต่อ",
                    "Product Family",
                    "Product Subtype 1",
                    "Product Material",
                    "Capacity",
                    "Description",
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (RfqHeaderDto record : records) {
                Row row = sheet.createRow(rowIndex++);
                int columnIndex = 0;

                row.createCell(columnIndex++).setCellValue(StringUtils.defaultString(record.getId()));
                row.createCell(columnIndex++).setCellValue(formatExportDateTime(record.getRequestedDate()));
                row.createCell(columnIndex++).setCellValue(displayRfqStatus(record.getStatus()));
                row.createCell(columnIndex++).setCellValue(StringUtils.defaultString(record.getCustomer() != null ? record.getCustomer().getId() : ""));
                row.createCell(columnIndex++).setCellValue(resolveCustomerName(record));
                row.createCell(columnIndex++).setCellValue(resolveEmployeeId(record.getSales()));
                row.createCell(columnIndex++).setCellValue(resolveEmployeeName(record.getSales()));
                row.createCell(columnIndex++).setCellValue(resolveEmployeeId(record.getProcurement()));
                row.createCell(columnIndex++).setCellValue(resolveEmployeeName(record.getProcurement()));
                row.createCell(columnIndex++).setCellValue(resolveSystemConfigName(record.getRfqType()));
                row.createCell(columnIndex++).setCellValue(resolveSystemConfigName(record.getOrderType()));
                row.createCell(columnIndex++).setCellValue(StringUtils.defaultString(record.getContactName()));
                row.createCell(columnIndex++).setCellValue(StringUtils.defaultString(record.getContactPhone()));
                row.createCell(columnIndex++).setCellValue(StringUtils.defaultString(record.getContactChannel()));
                row.createCell(columnIndex++).setCellValue(resolveProductFamilyName(record));
                row.createCell(columnIndex++).setCellValue(resolveProductSubtype1Name(record));
                row.createCell(columnIndex++).setCellValue(resolveProductMaterialName(record));
                row.createCell(columnIndex++).setCellValue(StringUtils.defaultString(record.getCapacity()));
                row.createCell(columnIndex++).setCellValue(StringUtils.defaultString(record.getDescription()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Cannot export RFQ data", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<RfqHeaderDto> getPendingAcceptanceRfqsInWindow(
            ZonedDateTime requestedDateStart,
            ZonedDateTime requestedDateEnd
    ) {
        if (requestedDateStart == null || requestedDateEnd == null) {
            throw new IllegalArgumentException("requestedDateStart and requestedDateEnd are required.");
        }
        if (requestedDateEnd.isBefore(requestedDateStart)) {
            throw new IllegalArgumentException("requestedDateEnd must be greater than or equal to requestedDateStart.");
        }

        List<RfqHeaderDto> records = requestPriceHeaderRepository.findAll(
                        Specification.where(statusEqual(RfqStatus.NEW))
                                .and((root, query, cb) -> cb.isFalse(root.get("isAccept")))
                                .and(requestedDateTimeBetween(requestedDateStart, requestedDateEnd)),
                        Sort.by(Sort.Direction.ASC, "requestedDate")
                ).stream()
                .map(requestPriceHeaderMapper::toDto)
                .toList();

        log.info(
                "Found {} pending acceptance RFQs in window {} to {}",
                records.size(),
                requestedDateStart,
                requestedDateEnd
        );

        return records;
    }

    @Transactional(readOnly = true)
    public List<RfqHeaderDto> getPendingAcceptanceRfqsByConfig(AppProperties.RfqPendingAcceptance config) {
        if (config == null) {
            throw new IllegalArgumentException("rfq pending acceptance config is required.");
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        ZonedDateTime requestedDateStart = now.toLocalDate()
                .minusDays(config.getStartOffsetDays())
                .atTime(config.getStartTime())
                .atZone(DateUtil.getTimeZone());
        ZonedDateTime requestedDateEnd = now.toLocalDate()
                .minusDays(config.getEndOffsetDays())
                .atTime(config.getEndTime())
                .atZone(DateUtil.getTimeZone());

        return getPendingAcceptanceRfqsInWindow(requestedDateStart, requestedDateEnd);
    }

    @Transactional(readOnly = true)
    public void sendPendingAcceptanceSummaryNotifications(
            List<RfqHeaderDto> rfqs,
            ZonedDateTime requestedDateStart,
            ZonedDateTime requestedDateEnd
    ) {
        if (CollectionUtils.isEmpty(rfqs)) {
            log.info("Skip pending acceptance summary notifications because no RFQs were found.");
            return;
        }

        Map<String, List<RfqHeaderDto>> rfqsByProcurementId = rfqs.stream()
                .filter(Objects::nonNull)
                .filter(rfq -> rfq.getProcurement() != null && StringUtils.isNotBlank(rfq.getProcurement().getEmployeeId()))
                .collect(java.util.stream.Collectors.groupingBy(rfq -> rfq.getProcurement().getEmployeeId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));

        if (rfqsByProcurementId.isEmpty()) {
            log.warn("Skip pending acceptance summary notifications because no procurement owners were found.");
            return;
        }

        List<UserEntity> procurementUsers = userRepository.findByEmployeeEntity_EmployeeIdIn(new ArrayList<>(rfqsByProcurementId.keySet()));
        Map<String, UserEntity> userByEmployeeId = procurementUsers.stream()
                .filter(user -> user.getEmployeeEntity() != null && StringUtils.isNotBlank(user.getEmployeeEntity().getEmployeeId()))
                .collect(java.util.stream.Collectors.toMap(
                        user -> user.getEmployeeEntity().getEmployeeId(),
                        user -> user,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        rfqsByProcurementId.forEach((employeeId, procurementRfqs) -> {
            UserEntity userEntity = userByEmployeeId.get(employeeId);
            if (userEntity != null && StringUtils.isNotBlank(userEntity.getLineUserId()) && Status.ACTIVE.equals(userEntity.getStatus())) {
                try {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("altText", "มี RFQ รอรับงาน จำนวน " + procurementRfqs.size() + " คำขอ");
                    placeholders.put("title", "RFQ รอรับงาน");
                    placeholders.put(
                            "detail",
                            String.format(
                                    "คุณมี RFQ รอรับงาน จำนวน %d คำขอ กรุณาทำการตรวจสอบและกดรับงาน",
                                    procurementRfqs.size()
                            )
                    );
                    placeholders.put(
                            "detailUrl",
                            buildRfqManagementUrl(employeeId, requestedDateStart, requestedDateEnd)
                    );

                    JsonNode message = renderNotificationTemplate(placeholders);
                    lineMessageService.sendFlexMessage(userEntity.getLineUserId(), message);
                } catch (Exception exception) {
                    log.warn(
                            "Cannot send pending acceptance summary notification to procurement user {}",
                            userEntity.getId(),
                            exception
                    );
                }
            } else {
                log.warn("No LINE-bound procurement user found for employeeId {}", employeeId);
            }
        });
    }

    @Transactional
    public RfqHeaderDto getRFQById(String id, String userId) throws DataNotFoundException {
        log.info("Get RFQ by id : {}", id);
        RfqHeaderEntity entity = getEntityById(id);

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

        sendRequestInformationNotificationToSales(
                entity,
                StringUtils.trimToEmpty(request.getRequestInformation()),
                updatedBy
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto addNote(String rfqId, AddRfqNoteRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        if (StringUtils.isBlank(rfqId)) {
            throw new InvalidRequestException("rfqId is required.");
        }
        if (request == null || StringUtils.isBlank(request.getNote())) {
            throw new InvalidRequestException("note is required.");
        }

        RfqHeaderEntity entity = getEntityById(rfqId.trim());
        Map<String, Object> beforeDetail = buildActivityDetail(entity);

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String updatedBy = userProfileService.getNameFromId(userId);
        entity.setNote(appendRfqNote(
                entity.getNote(),
                StringUtils.trimToNull(request.getNote()),
                updatedBy,
                now
        ));
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedDate(now);
        entity = requestPriceHeaderRepository.save(entity);

        Map<String, Object> afterDetail = buildActivityDetail(entity);
        Map<String, Object> changedBeforeDetail = new LinkedHashMap<>();
        Map<String, Object> changedAfterDetail = new LinkedHashMap<>();
        extractChangedDetails(beforeDetail, afterDetail, changedBeforeDetail, changedAfterDetail);
        changedBeforeDetail.remove("note");
        changedAfterDetail.remove("note");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", changedBeforeDetail);
        detail.put("after", changedAfterDetail);
        detail.put("note", parseRfqNotes(entity.getNote()));
        detail.put("noteText", StringUtils.trimToNull(request.getNote()));

        String summary = "เพิ่มโน้ตของคำขอราคาเลขที่ " + entity.getId();
        if (StringUtils.isNotBlank(request.getNote())) {
            summary += " : " + StringUtils.abbreviate(request.getNote().trim(), 120);
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
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String actor = userProfileService.getNameFromId(userId);

        entity.setRequestedDate(now);
        entity.setStatus(RfqStatus.NEW);
        entity.setIsAccept(Boolean.FALSE);
        entity.setShippingMethod(normalizeRfqShippingMethod(request.getShippingMethod()));
        entity.setCreatedBy(actor);
        entity.setUpdatedBy(actor);
        entity.setUrgentRequest(Boolean.TRUE.equals(request.getUrgentRequest()));

        if (Boolean.TRUE.equals(entity.getUrgentRequest())) {
            if (StringUtils.isBlank(request.getUrgentRequestReason())) {
                throw new InvalidRequestException("urgentRequestReason is required.");
            }

            entity.setUrgentRequestReason(request.getUrgentRequestReason().trim());
            entity.setUrgentRequestStatus(UrgentRequestStatus.PENDING_APPROVAL);
            entity.setUrgentRequestedBy(actor);
            entity.setUrgentRequestedDate(now);
        } else {
            entity.setUrgentRequest(Boolean.FALSE);
        }

        applyRelations(
                entity,
                request.getSalesId(),
                request.getCustomerId(),
                request.getRfqTypeCode(),
                request.getOrderTypeCode(),
                request.getProcurementId(),
                request.getReferenceRfqId()
        );
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
            if (!CollectionUtils.isEmpty(entity.getCustomer().getContacts()) && entity.getCustomer().getContacts().getFirst() != null) {
                entity.setContactName(entity.getCustomer().getCustomerName());
                entity.setContactPhone(entity.getCustomer().getContacts().getFirst().getContactNumber());
            } else {
                entity.setContactName("ลูกค้า");
                entity.setContactPhone("");
            }
        } else {
            entity.setContactName(request.getContactName());
            entity.setContactPhone(request.getContactPhone());
        }

        entity = requestPriceHeaderRepository.save(entity);
        int copiedSupplierQuoteCount = copyReferenceSupplierQuotesIfNeeded(entity, request, userId, actor);

        java.util.Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("status", entity.getStatus());
        detail.put("urgentRequest", entity.getUrgentRequest());
        detail.put("urgentRequestStatus", entity.getUrgentRequestStatus());
        detail.put("urgentRequestReason", entity.getUrgentRequestReason());
        detail.put("requestSample", entity.getRequestSample());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("referenceRfqId", entity.getReferenceRfqId());
        detail.put("shippingMethod", entity.getShippingMethod());
        detail.put("pictureCount", entity.getPictures() != null ? entity.getPictures().size() : 0);
        detail.put("copiedSupplierQuoteCount", copiedSupplierQuoteCount);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                Boolean.TRUE.equals(entity.getUrgentRequest())
                        ? "สร้างคำขอราคาเร่งด่วนเลขที่ " + entity.getId()
                        : "สร้างคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        if (Boolean.TRUE.equals(entity.getUrgentRequest())) {
            approvalService.createUrgentRfqApprovalRequest(entity, userId);
        } else {
            sendAwaitingAcceptNotifications(entity);
            Optional<UserEntity> userEntityOptional = userRepository.findByEmployeeEntity_EmployeeId(entity.getProcurement().getEmployeeId());

            if (userEntityOptional.isPresent()) {
                userTodoService.buildUserTodoEntity(
                        userEntityOptional.get(),
                        UserTodoType.PRICE_INQUIRY,
                        entity.getId() + " รอรับงาน",
                        null,
                        UserTodoStatus.TODO,
                        UserTodoPriority.HIGH,
                        ActivityEntityType.PRICE_INQUIRY.name(),
                        entity.getId(),
                        null,
                        now.plusDays(1),
                        1,
                        userId
                );
            }
        }

        saveRfqStatusTimeline(entity, entity.getStatus(), entity.getRequestedDate());

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadRfqResponse uploadRfqs(MultipartFile file, String userId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("RFQ file is required.");
        }

        log.info("Upload rfqs from file {} by {}", file.getOriginalFilename(), userId);

        UploadRfqResponse response = new UploadRfqResponse();
        List<UploadRfqErrorResponse> errors = new ArrayList<>();
        int totalRows = 0;
        int createdCount = 0;
        int failedCount = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new InvalidRequestException("RFQ excel sheet not found.");
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new InvalidRequestException("RFQ excel header row not found.");
            }

            Map<String, Integer> headerIndexMap = buildRfqUploadHeaderIndexMap(headerRow, evaluator);
            validateRfqUploadHeaders(headerIndexMap);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRfqUploadRow(row, evaluator, headerIndexMap)) {
                    continue;
                }

                totalRows++;

                try {
                    RfqHeaderEntity entity = buildRfqHeaderEntityFromUploadRow(row, headerIndexMap, evaluator, userId);
                    entity = requestPriceHeaderRepository.save(entity);

                    activityHistoryService.record(
                            ActivityEntityType.RFQ,
                            entity.getId(),
                            userId,
                            ActivityActorType.USER,
                            ActivityAction.CREATE,
                            ActivitySource.API,
                            "อัปโหลดคำขอราคา " + entity.getId(),
                            Map.of(
                                    "source", "UPLOAD_RFQ_EXCEL",
                                    "rowNumber", rowIndex + 1,
                                    "rfq", buildActivityDetail(entity)
                            )
                    );
                    createdCount++;
                } catch (Exception ex) {
                    failedCount++;
                    errors.add(new UploadRfqErrorResponse(
                            rowIndex + 1,
                            StringUtils.defaultString(readRfqUploadCell(row, headerIndexMap, evaluator, "salesid"), "-"),
                            StringUtils.defaultString(readRfqUploadCell(row, headerIndexMap, evaluator, "description"), "-"),
                            ex.getMessage()
                    ));
                    log.warn("Skip rfq row {} because {}", rowIndex + 1, ex.getMessage());
                }
            }
        }

        response.setTotalRows(totalRows);
        response.setCreatedCount(createdCount);
        response.setFailedCount(failedCount);
        response.setErrors(errors);
        log.info("Upload rfqs completed created={} failed={}", createdCount, failedCount);
        return response;
    }

    @Transactional
    public void createUrgentRfqApprovalRequest(
            String rfqId,
            com.nutalig.controller.rfq.request.RequestUrgentRfqApproveRequest request,
            String userId
    ) throws Exception {
        log.info("Create urgent rfq approval request for rfq {} by {}", rfqId, userId);

        RfqHeaderEntity entity = getEntityById(rfqId);
        String actor = userProfileService.getNameFromId(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        String urgentRequestMessage = request == null ? null : request.getUrgentRequestMessage();
        if (StringUtils.isBlank(urgentRequestMessage)) {
            throw new InvalidRequestException("urgentRequestMessage is required.");
        }

        entity.setUrgentRequest(Boolean.TRUE);
        entity.setUrgentRequestReason(urgentRequestMessage.trim());
        entity.setUrgentRequestStatus(UrgentRequestStatus.PENDING_APPROVAL);
        entity.setUrgentRequestedBy(actor);
        entity.setUrgentRequestedDate(now);
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(now);
        requestPriceHeaderRepository.save(entity);

        approvalService.createUrgentRfqApprovalRequest(entity, userId);
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
            addedDetail.put("tierSplitCount", detailEntity.getTierSplits().size());
            addedDetails.add(addedDetail);
        }

        boolean isFinalQuoted = entity.getStatus() == RfqStatus.SUPPLIER_QUOTED || entity.getStatus() == RfqStatus.SPECIAL_PRICE_REVIEW;
        if (isFinalQuoted) {
            entity.setStatus(RfqStatus.QUOTED);
            entity.setQuotedDate(now);
        }

        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedDate(now);
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("count", addedDetails.size());
        detail.put("details", addedDetails);
        detail.put("request", requests);

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

        if (isFinalQuoted) {
            sendFinalQuotedNotificationToSales(entity, updatedBy);
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
        if (StringUtils.isNotBlank(entity.getSaleOrderId())
                && !entity.getSaleOrderId().equals(request.getSaleOrderId().trim())) {
            throw new InvalidRequestException("RFQ " + rfqId + " already linked to sale order " + entity.getSaleOrderId());
        }
        List<ResolvedLinkSelection> selections = resolveLinkSelections(entity, request);
        ResolvedLinkSelection primarySelection = selections.get(0);

        entity.setSaleOrderId(request.getSaleOrderId().trim());
        entity.setConfirmedDetailId(primarySelection.getDetail().getId());
        entity.setConfirmedTierId(primarySelection.getTier().getId());
        entity.setConfirmedShippingMethod(primarySelection.getShippingMethod());
        entity.setConfirmedPrice(scaleMoney(primarySelection.getConfirmedPrice()));
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
        activityDetail.put("selectedCount", selections.size());
        activityDetail.put("selections", selections.stream().map(selection -> {
            Map<String, Object> selectionDetail = new LinkedHashMap<>();
            selectionDetail.put("detailId", selection.getDetail().getId());
            selectionDetail.put("tierId", selection.getTier().getId());
            selectionDetail.put("shippingMethod", selection.getShippingMethod());
            selectionDetail.put("price", selection.getConfirmedPrice());
            return selectionDetail;
        }).toList());

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

    private List<ResolvedLinkSelection> resolveLinkSelections(
            RfqHeaderEntity entity,
            LinkRfqSalesOrderRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        List<LinkRfqSalesOrderRequest.Selection> requestedSelections =
                request.getSelections() == null ? new ArrayList<>() : new ArrayList<>(request.getSelections());

        if (requestedSelections.isEmpty()
                && request.getDetailId() != null
                && request.getTierId() != null
                && StringUtils.isNotBlank(request.getShippingMethod())) {
            LinkRfqSalesOrderRequest.Selection fallbackSelection = new LinkRfqSalesOrderRequest.Selection();
            fallbackSelection.setDetailId(request.getDetailId());
            fallbackSelection.setTierId(request.getTierId());
            fallbackSelection.setShippingMethod(request.getShippingMethod());
            fallbackSelection.setPrice(request.getPrice());
            requestedSelections.add(fallbackSelection);
        }

        if (requestedSelections.isEmpty()) {
            throw new InvalidRequestException("at least one selection is required");
        }

        List<ResolvedLinkSelection> resolvedSelections = new ArrayList<>();

        for (LinkRfqSalesOrderRequest.Selection selection : requestedSelections) {
            if (selection.getDetailId() == null) {
                throw new InvalidRequestException("detailId is required");
            }
            if (selection.getTierId() == null) {
                throw new InvalidRequestException("tierId is required");
            }

            RfqDetailEntity detail = getDetailFromHeader(entity, selection.getDetailId());
            RfqTierEntity tier = detail.getTiers().stream()
                    .filter(item -> Objects.equals(item.getId(), selection.getTierId()))
                    .findFirst()
                    .orElseThrow(() -> new DataNotFoundException("Tier " + selection.getTierId() + " not found."));

            String shippingMethod = StringUtils.trimToNull(selection.getShippingMethod());
            if (!"LAND".equalsIgnoreCase(shippingMethod) && !"SEA".equalsIgnoreCase(shippingMethod)) {
                throw new InvalidRequestException("shippingMethod must be LAND or SEA");
            }

            BigDecimal confirmedPrice = selection.getPrice();
            if (confirmedPrice == null) {
                confirmedPrice = "SEA".equalsIgnoreCase(shippingMethod) ? tier.getSeaTotalPrice() : tier.getLandTotalPrice();
            }

            resolvedSelections.add(new ResolvedLinkSelection(
                    detail,
                    tier,
                    shippingMethod.toUpperCase(Locale.ROOT),
                    confirmedPrice
            ));
        }

        return resolvedSelections;
    }

    @Data
    private static class ResolvedLinkSelection {
        private final RfqDetailEntity detail;
        private final RfqTierEntity tier;
        private final String shippingMethod;
        private final BigDecimal confirmedPrice;
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
        detailEntity.setCommission(scaleMoney(updatedDetail.getCommission()));
        detailEntity.setPackageDimension(updatedDetail.getPackageDimension());
        detailEntity.setPackageWeight(updatedDetail.getPackageWeight());
        detailEntity.setPackageCapacity(updatedDetail.getPackageCapacity());
        detailEntity.setUpdatedBy(updatedDetail.getUpdatedBy());
        if (request.getTiers() != null) {
            detailEntity.getTiers().clear();
            updatedDetail
                    .getTiers()
                    .forEach(detailEntity::addTier);
        }
        if (request.getTierSplits() != null) {
            detailEntity.getTierSplits().clear();
            updatedDetail
                    .getTierSplits()
                    .forEach(detailEntity::addTierSplit);
        }

        entity.setUpdatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("detailId", detailEntity.getId());
        detail.put("optionName", detailEntity.getOptionName());
        detail.put("tierCount", detailEntity.getTiers().size());
        detail.put("tierSplitCount", detailEntity.getTierSplits().size());

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
    public RfqHeaderDto updateRFQDetailTier(
            String rfqId,
            Long detailId,
            Long tierId,
            UpdateRequestPriceTierRequest request,
            String userId
    ) throws Exception {
        if (request == null) {
            throw new InvalidRequestException("request is required");
        }

        RfqHeaderEntity entity = getEntityById(rfqId);
        RfqDetailEntity detailEntity = getDetailFromHeader(entity, detailId);
        RfqTierEntity tierEntity = getTierFromDetail(detailEntity, tierId);

        if (request.getQuantity() == null) {
            throw new InvalidRequestException("quantity is required");
        }
        if (request.getProductPrice() == null) {
            throw new InvalidRequestException("productPrice is required");
        }

        boolean quantityExists = detailEntity.getTiers().stream()
                .filter(item -> !Objects.equals(item.getId(), tierEntity.getId()))
                .anyMatch(item -> item.getQuantity() != null && item.getQuantity().compareTo(request.getQuantity()) == 0);
        if (quantityExists) {
            throw new InvalidRequestException(
                    "quantity " + request.getQuantity() + " already exists in detail " + detailId
            );
        }

        SupplierEntity supplier = tierEntity.getSupplier();
        if (StringUtils.isNotBlank(request.getSupplierId())) {
            supplier = getSupplierEntity(request.getSupplierId().trim());
        } else if (detailEntity.getSupplier() != null) {
            supplier = detailEntity.getSupplier();
        }

        String actor = userProfileService.getNameFromId(userId);
        tierEntity.setSupplier(supplier);
        tierEntity.setQuantity(request.getQuantity());
        tierEntity.setProductPrice(scaleMoney(request.getProductPrice()));
        tierEntity.setCommission(scaleMoney(request.getCommission()));
        tierEntity.setCurrency(request.getCurrency());
        tierEntity.setLandFreightCost(scaleMoney(request.getLandFreightCost()));
        tierEntity.setSeaFreightCost(scaleMoney(request.getSeaFreightCost()));
        boolean isShareFcl = Boolean.TRUE.equals(request.getIsShareFCL());
        tierEntity.setIsShareFCL(isShareFcl);
        tierEntity.setIsFcl(Boolean.TRUE.equals(request.getIsFcl()) || isShareFcl);
        tierEntity.setLandTotalPrice(scaleMoney(request.getLandTotalPrice()));
        tierEntity.setSeaTotalPrice(scaleMoney(request.getSeaTotalPrice()));
        tierEntity.setSupplierQuoteTierId(request.getSupplierQuoteTierId());
        tierEntity.setSortOrder(request.getSortOrder());

        detailEntity.setUpdatedBy(actor);
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("detailId", detailId);
        detail.put("tierId", tierId);
        detail.put("quantity", tierEntity.getQuantity());
        detail.put("productPrice", tierEntity.getProductPrice());

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "แก้ไข tier ของรายละเอียดคำขอราคาเลขที่ " + entity.getId(),
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

        List<QuotationEntity> quotationEntities = quotationRepository.findAllByRfqIdOrderByCreatedDateDesc(entity.getId());
        if (RfqStatus.REJECTED.equals(status) && !quotationEntities.isEmpty()) {
            log.info("Update Quotation(s) for RFQ {} with status {}", entity.getId(), status);

            for (QuotationEntity quotationEntity : quotationEntities) {
                quotationEntity.setStatus(QuotationStatus.REJECTED);
                quotationEntity.setUpdatedDate(now);
                quotationEntity.setUpdatedBy(actor);

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
            quotationRepository.saveAll(quotationEntities);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto acceptRFQ(String id, String userId) throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity entity = getEntityById(id);
        String actor = userProfileService.getNameFromId(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        if (!RfqStatus.NEW.equals(entity.getStatus())) {
            throw new InvalidRequestException("Only RFQ with NEW status can be accepted.");
        }

        SlaConfigDto sla = slaConfigService.getSlaConfigById(SLA);
        entity.setSlaDate(slaConfigService.calculateSlaDate(sla, entity.getRequestedDate()));
        entity.setStatus(RfqStatus.IN_PROGRESS);
        String currentProcurementId = entity.getProcurement() != null ? entity.getProcurement().getEmployeeId() : null;
        if (!StringUtils.equals(currentProcurementId, userId)) {
            UserEntity procurement = userRepository.findById(userId).get();
            entity.setProcurement(procurement.getEmployeeEntity());
        }
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(now);
        entity.setIsAccept(Boolean.TRUE);
        entity = requestPriceHeaderRepository.save(entity);

        saveRfqStatusTimeline(entity, RfqStatus.IN_PROGRESS, now);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("beforeStatus", RfqStatus.NEW);
        detail.put("afterStatus", RfqStatus.IN_PROGRESS);
        if (!StringUtils.equals(currentProcurementId, userId)) {
            detail.put("beforeProcurement", currentProcurementId);
            detail.put("afterProcurement", userId);
        }

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.STATUS_CHANGE,
                ActivitySource.API,
                "รับงานคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        sendAcceptedByProcurementNotificationToSales(entity, actor);

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto approveUrgentRequest(String id, String userId) throws DataNotFoundException, InvalidRequestException {
        validateSuperAdmin(userId);
        approvalService.approveLatestApprovalByEntity(ActivityEntityType.RFQ, id, userId);
        return mapToDto(getEntityById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto requestSpecialPrice(String id, RequestSpecialPriceRequest request, String userId) throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity entity = getEntityById(id);
        String actor = userProfileService.getNameFromId(userId);
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        if (request == null || request.getTargetPrice() == null) {
            throw new InvalidRequestException("targetPrice is required.");
        }

        if (!RfqStatus.QUOTED.equals(entity.getStatus())) {
            throw new InvalidRequestException("Only RFQ with QUOTED status can request special price review.");
        }

        entity.setTargetPrice(scaleMoney(request.getTargetPrice()));
        entity.setStatus(RfqStatus.SPECIAL_PRICE_REVIEW);
        entity.setUpdatedBy(actor);
        entity.setUpdatedDate(now);
        entity = requestPriceHeaderRepository.save(entity);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("targetPrice", request.getTargetPrice());

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "ขอทบทวนราคาพิเศษสำหรับคำขอราคาเลขที่ " + entity.getId(),
                detail
        );

        sendRequestSpecialPriceReviewNotificationToProcurement(entity, actor);

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto rejectUrgentRequest(String id, RejectUrgentRfqRequest request, String userId) throws DataNotFoundException, InvalidRequestException {
        validateSuperAdmin(userId);

        if (request == null || StringUtils.isBlank(request.getReason())) {
            throw new InvalidRequestException("reason is required.");
        }

        approvalService.rejectLatestApprovalByEntity(ActivityEntityType.RFQ, id, request.getReason().trim(), userId);
        return mapToDto(getEntityById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public RfqHeaderDto updateRFQ(String id, UpdateRequestPriceHeaderRequest request, String userId) throws Exception {
        RfqHeaderEntity entity = getEntityById(id);
        java.util.Map<String, Object> beforeDetail = buildActivityDetail(entity);
        String updatedBy = userProfileService.getNameFromId(userId);

        if (!hasRFQUpdateChanges(entity, request) && !RfqStatus.REQUESTED_INFO.equals(entity.getStatus())) {
            return mapToDto(entity);
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        List<String> editFields = new ArrayList<>();
        boolean shouldNotifyProcurementSalesUpdatedInfo = false;

        String requestContactName = normalizeRequestValue(request.getContactName());
        if (!StringUtils.equals(requestContactName, normalizeRequestValue(entity.getContactName()))) {
            entity.setContactName(requestContactName);
            editFields.add("ชื่อผู้ติดต่อ");
        }

        String requestContactPhone = normalizeRequestValue(request.getContactPhone());
        if (!StringUtils.equals(requestContactPhone, normalizeRequestValue(entity.getContactPhone()))) {
            entity.setContactPhone(requestContactPhone);
            editFields.add("เบอร์โทรผู้ติดต่อ");
        }

        String requestContactChannel = normalizeRequestValue(request.getContactChannel());
        if (!StringUtils.equals(requestContactChannel, normalizeRequestValue(entity.getContactChannel()))) {
            entity.setContactChannel(requestContactChannel);
            editFields.add("ช่องทางติดต่อ");
        }

        String requestSalesId = normalizeRequestValue(request.getSalesId());
        if (!StringUtils.equals(
                requestSalesId,
                entity.getSales() != null ? normalizeRequestValue(entity.getSales().getEmployeeId()) : null
        )) {
            entity.setSales(resolveSales(requestSalesId));
            editFields.add("เซลล์");
        }

        String requestProcurementId = normalizeRequestValue(request.getProcurementId());
        if (!StringUtils.equals(
                requestProcurementId,
                entity.getProcurement() != null ? normalizeRequestValue(entity.getProcurement().getEmployeeId()) : null
        )) {
            entity.setProcurement(resolveProcurement(requestProcurementId));
            editFields.add("จัดซื้อที่ดูแล");
        }

        if (RfqStatus.REQUESTED_INFO.equals(entity.getStatus()) && Boolean.TRUE.equals(entity.getIsAccept())) {
            log.info("Update status from {} to {}", RfqStatus.REQUESTED_INFO, RfqStatus.IN_PROGRESS);
            entity.setStatus(RfqStatus.IN_PROGRESS);
            shouldNotifyProcurementSalesUpdatedInfo = true;

            SlaConfigDto sla = slaConfigService.getSlaConfigById(SLA);
            entity.setSlaDate(slaConfigService.calculateSlaDate(sla, now));
            editFields.add("สถานะ");
        } else if (RfqStatus.REQUESTED_INFO.equals(entity.getStatus()) && Boolean.FALSE.equals(entity.getIsAccept())) {
            log.info("Update status from {} to {}", RfqStatus.REQUESTED_INFO, RfqStatus.NEW);
            entity.setStatus(RfqStatus.NEW);
            editFields.add("สถานะ");
        }

        String requestRfqTypeCode = normalizeRequestValue(request.getRfqTypeCode());
        if (!StringUtils.equals(
                requestRfqTypeCode,
                entity.getRfqType() != null && entity.getRfqType().getId() != null
                        ? entity.getRfqType().getId().getCode()
                        : null
        )) {
            entity.setRfqType(resolveRfqType(requestRfqTypeCode));
            editFields.add("ประเภท RFQ");
        }

        String requestOrderTypeCode = normalizeRequestValue(request.getOrderTypeCode());
        String requestReferenceRfqId = normalizeRequestValue(request.getReferenceRfqId());
        if (StringUtils.isNotEmpty(requestOrderTypeCode) && !StringUtils.equals(
                requestOrderTypeCode,
                entity.getOrderType() != null && entity.getOrderType().getId() != null
                        ? entity.getOrderType().getId().getCode()
                        : null
        )) {
            entity.setOrderType(resolveOrderType(requestOrderTypeCode));
            editFields.add("ประเภทงาน");
        }
        if (!StringUtils.equals(requestReferenceRfqId, entity.getReferenceRfqId())) {
            applyReferenceRfq(entity, requestReferenceRfqId);
            editFields.add("RFQ ตัวหลัก");
        }
        String requestShippingMethod = normalizeRequestValue(request.getShippingMethod());
        String normalizedCurrentShippingMethod = StringUtils.defaultIfBlank(entity.getShippingMethod(), "ALL");
        String normalizedRequestShippingMethod = StringUtils.defaultIfBlank(requestShippingMethod, "ALL");
        if (!StringUtils.equals(normalizedRequestShippingMethod, normalizedCurrentShippingMethod)) {
            entity.setShippingMethod(normalizeRfqShippingMethod(normalizedRequestShippingMethod));
            editFields.add("การขนส่ง");
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
        BigDecimal requestTargetPrice = scaleMoney(request.getTargetPrice());
        if (!Objects.equals(requestTargetPrice, entity.getTargetPrice())) {
            entity.setTargetPrice(requestTargetPrice);
            editFields.add("Target Price");
        }
        List<BigDecimal> requestRequestedMoqs = request.getRequestedMoqs() == null
                ? new ArrayList<>()
                : request.getRequestedMoqs().stream().filter(Objects::nonNull).toList();
        List<BigDecimal> currentRequestedMoqs = parseRequestedMoq(entity.getRequestedMoq());
        if (!Objects.equals(requestRequestedMoqs, currentRequestedMoqs)) {
            entity.setRequestedMoq(requestPriceHeaderMapper.mapRequestedMoqs(requestRequestedMoqs));
            editFields.add("MOQ ที่ต้องการ");
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

        entity.setUpdatedBy(updatedBy);
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

        if (shouldNotifyProcurementSalesUpdatedInfo) {
            sendSalesUpdatedInformationNotificationToProcurement(entity, updatedBy);
        }

        return mapToDto(entity);
    }


    @Transactional(rollbackFor = Exception.class)
    public String getCustomerQuoted(String rfqId, String userId) throws DataNotFoundException, InvalidRequestException {
        RfqHeaderEntity rfq = getEntityById(rfqId);

        if (!List.of(RfqStatus.QUOTED, RfqStatus.COMPLETED).contains(rfq.getStatus())) {
            throw new InvalidRequestException("Cannot get customer quoted for rfq isn't QUOTED.");
        }
        return buildThaiCustomerQuoteMessage(rfq);
    }

    public String buildThaiCustomerQuoteMessage(RfqHeaderEntity rfq) throws DataNotFoundException {
        String template = promptService.getActivePrompt(RFQ_CUSTOMER_QUOTED_TH).getUserPromptTemplate();
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("requestDate", buildCustomerQuotedRequestedDate(rfq));
        variables.put("rfqId", safeValue(rfq.getId()));
        variables.put("sales", rfq.getSales().getNickName());
        variables.put("procurement", rfq.getProcurement().getNickName());
        variables.put("customer", buildFinalQuoteInquiryCustomerLabel(rfq));
        variables.put("productFamily", displayProductFamily(rfq));
        variables.put("productSubtype1", displayProductSubtype1(rfq));
        variables.put("productSubtype2", displayProductSubtype2(rfq));
        variables.put("material", displayProductMaterial(rfq));
        variables.put("capacity", safeValue(rfq.getCapacity()));
        variables.put("spec", rfq.getDetails().getFirst().getSpec());
        variables.put("tiersSection", buildCustomerQuotedTiers(rfq));
        if (!rfq.getDetails().isEmpty() && rfq.getDetails().getFirst().getRemark() != null && !rfq.getDetails().getFirst().getRemark().isEmpty()) {
            variables.put("remarkSection", rfq.getDetails().getFirst().getRemark().replaceAll("หมายเหตุ", ""));
        } else {
            variables.put("remarkSection", "");
        }
        variables.put("recommend", rfq.getDetails().getFirst().getRecommend());
        return promptTemplateEngine.render(template, variables).trim();
    }

    private String buildFinalQuoteInquiryCustomerLabel(RfqHeaderEntity rfq) {
        if (rfq == null) {
            return "";
        }

        if (rfq.getCustomer() == null) {
            return safeValue(rfq.getContactName());
        }

        String customerName = StringUtils.firstNonBlank(
                rfq.getCustomer().getCustomerName(),
                rfq.getCustomer().getCompanyName(),
                "-"
        );
        return String.format("%s (%s)", customerName, safeValue(rfq.getCustomer().getId()));
    }


    private boolean hasRFQUpdateChanges(RfqHeaderEntity entity, UpdateRequestPriceHeaderRequest request) {
        if (entity == null || request == null) {
            return false;
        }

        String requestContactName = normalizeRequestValue(request.getContactName());
        String requestContactPhone = normalizeRequestValue(request.getContactPhone());
        String requestContactChannel = normalizeRequestValue(request.getContactChannel());
        String requestSalesId = normalizeRequestValue(request.getSalesId());
        String requestProcurementId = normalizeRequestValue(request.getProcurementId());
        String requestRfqTypeCode = normalizeRequestValue(request.getRfqTypeCode());
        String requestOrderTypeCode = normalizeRequestValue(request.getOrderTypeCode());
        String requestReferenceRfqId = normalizeRequestValue(request.getReferenceRfqId());
        String requestShippingMethod = normalizeRequestValue(request.getShippingMethod());
        String requestProductFamily = normalizeRequestValue(request.getProductFamily());
        String requestProductUsage = normalizeRequestValue(request.getProductUsage());
        String requestSystemMechanic = normalizeRequestValue(request.getSystemMechanic());
        String requestMaterial = normalizeRequestValue(request.getMaterial());
        String requestCapacity = normalizeRequestValue(request.getCapacity());
        List<BigDecimal> requestRequestedMoqs = request.getRequestedMoqs() == null
                ? new ArrayList<>()
                : request.getRequestedMoqs().stream().filter(Objects::nonNull).toList();
        String requestDescription = normalizeRequestValue(request.getDescription());

        return !StringUtils.equals(requestContactName, normalizeRequestValue(entity.getContactName()))
                || !StringUtils.equals(requestContactPhone, normalizeRequestValue(entity.getContactPhone()))
                || !StringUtils.equals(requestContactChannel, normalizeRequestValue(entity.getContactChannel()))
                || !StringUtils.equals(
                requestSalesId,
                entity.getSales() != null ? normalizeRequestValue(entity.getSales().getEmployeeId()) : null
        )
                || !StringUtils.equals(
                requestProcurementId,
                entity.getProcurement() != null ? normalizeRequestValue(entity.getProcurement().getEmployeeId()) : null
        )
                || !StringUtils.equals(
                requestRfqTypeCode,
                entity.getRfqType() != null && entity.getRfqType().getId() != null
                        ? entity.getRfqType().getId().getCode()
                        : null
        )
                || !StringUtils.equals(
                requestOrderTypeCode,
                entity.getOrderType() != null && entity.getOrderType().getId() != null
                        ? entity.getOrderType().getId().getCode()
                        : null
        ) || !StringUtils.equals(requestReferenceRfqId, entity.getReferenceRfqId())
                || !StringUtils.equals(
                StringUtils.defaultIfBlank(requestShippingMethod, "ALL"),
                StringUtils.defaultIfBlank(entity.getShippingMethod(), "ALL")
        )
                || !StringUtils.equals(requestProductFamily, entity.getProductFamily())
                || !StringUtils.equals(
                requestProductUsage,
                entity.getProductUsage() != null ? entity.getProductUsage().getCode() : null
        ) || !StringUtils.equals(
                requestSystemMechanic,
                entity.getSystemMechanic() != null ? entity.getSystemMechanic().getCode() : null
        ) || !StringUtils.equals(requestMaterial, entity.getMaterialCode())
                || !StringUtils.equals(requestCapacity, entity.getCapacity())
                || !Objects.equals(request.getTargetPrice(), entity.getTargetPrice())
                || !Objects.equals(requestRequestedMoqs, parseRequestedMoq(entity.getRequestedMoq()))
                || !StringUtils.equals(requestDescription, entity.getDescription());
    }

    private String normalizeRequestValue(String value) {
        return StringUtils.trimToNull(value);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value == null ? null : value.setScale(MONEY_SCALE, java.math.RoundingMode.HALF_UP);
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

    private void applyReferenceRfq(RfqHeaderEntity entity, String referenceRfqId) throws DataNotFoundException {
        String normalizedReferenceRfqId = normalizeRequestValue(referenceRfqId);
        entity.setReferenceRfqId(normalizedReferenceRfqId);
        entity.setReferenceRfq(resolveReferenceRfq(normalizedReferenceRfqId));
    }

    private int copyReferenceSupplierQuotesIfNeeded(
            RfqHeaderEntity targetRfq,
            CreateRequestPriceHeaderRequest request,
            String userId,
            String actor
    ) throws DataNotFoundException {
        if (targetRfq == null || request == null) {
            return 0;
        }

        if (!StringUtils.equalsAny(
                normalizeRequestValue(request.getRfqTypeCode()),
                "REPEAT_PRICE",
                "REORDER"
        )) {
            return 0;
        }

        String referenceRfqId = normalizeRequestValue(request.getReferenceRfqId());
        if (StringUtils.isBlank(referenceRfqId)) {
            return 0;
        }

        List<RfqSupplierQuoteEntity> sourceQuotes = rfqSupplierQuoteRepository
                .findAllByRequestPriceHeader_IdOrderByUpdatedDateDesc(referenceRfqId);
        if (CollectionUtils.isEmpty(sourceQuotes)) {
            return 0;
        }

        List<RfqSupplierQuoteEntity> copiedQuotes = new ArrayList<>();
        for (RfqSupplierQuoteEntity sourceQuote : sourceQuotes) {
            if (sourceQuote == null || sourceQuote.getStatus() == RfqSupplierQuoteStatus.CANCELLED) {
                continue;
            }

            copiedQuotes.add(cloneSupplierQuoteForReferenceRfq(targetRfq, sourceQuote, actor));
        }

        if (copiedQuotes.isEmpty()) {
            return 0;
        }

        rfqSupplierQuoteRepository.saveAll(copiedQuotes);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                targetRfq.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "คัดลอกข้อมูล supplier quote จากคำขอราคาเลขที่ " + referenceRfqId + " ไปยัง " + targetRfq.getId(),
                Map.of(
                        "referenceRfqId", referenceRfqId,
                        "copiedSupplierQuoteCount", copiedQuotes.size()
                )
        );

        return copiedQuotes.size();
    }

    private RfqSupplierQuoteEntity cloneSupplierQuoteForReferenceRfq(
            RfqHeaderEntity targetRfq,
            RfqSupplierQuoteEntity sourceQuote,
            String actor
    ) {
        RfqSupplierQuoteEntity quote = new RfqSupplierQuoteEntity();
        quote.setRequestPriceHeader(targetRfq);
        quote.setSupplier(sourceQuote.getSupplier());
        quote.setInquiry(null);
        quote.setRevisionNo(sourceQuote.getRevisionNo());
        quote.setStatus(sourceQuote.getStatus() == null ? RfqSupplierQuoteStatus.RESPONDED : sourceQuote.getStatus());
        quote.setRemark(sourceQuote.getRemark());
        quote.setCreatedBy(actor);
        quote.setUpdatedBy(actor);

        for (RfqSupplierQuoteDetailEntity sourceDetail : Optional.ofNullable(sourceQuote.getDetails()).orElse(List.of())
                .stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteDetailEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteDetailEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList()) {
            quote.addDetail(cloneSupplierQuoteDetailForReferenceRfq(sourceDetail));
        }

        for (RfqSupplierQuoteAdditionalCostEntity sourceAdditionalCost :
                Optional.ofNullable(sourceQuote.getAdditionalCosts()).orElse(List.of()).stream()
                        .sorted(Comparator.comparing(RfqSupplierQuoteAdditionalCostEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(RfqSupplierQuoteAdditionalCostEntity::getId, Comparator.nullsLast(Long::compareTo)))
                        .toList()) {
            quote.addAdditionalCost(cloneSupplierQuoteAdditionalCost(sourceAdditionalCost));
        }

        for (RfqSupplierQuotePackageEntity sourcePackage :
                resolveSupplierQuotePackagesForCopy(sourceQuote).stream()
                        .sorted(Comparator.comparing(RfqSupplierQuotePackageEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(RfqSupplierQuotePackageEntity::getId, Comparator.nullsLast(Long::compareTo)))
                        .toList()) {
            quote.addPackage(cloneSupplierQuotePackage(sourcePackage));
        }

        for (RfqSupplierQuoteLeadTimeEntity sourceLeadTime :
                Optional.ofNullable(sourceQuote.getLeadTimes()).orElse(List.of()).stream()
                        .sorted(Comparator.comparing(RfqSupplierQuoteLeadTimeEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(RfqSupplierQuoteLeadTimeEntity::getId, Comparator.nullsLast(Long::compareTo)))
                        .toList()) {
            quote.addLeadTime(cloneSupplierQuoteLeadTime(sourceLeadTime));
        }

        return quote;
    }

    private RfqSupplierQuoteDetailEntity cloneSupplierQuoteDetailForReferenceRfq(RfqSupplierQuoteDetailEntity sourceDetail) {
        RfqSupplierQuoteDetailEntity detail = new RfqSupplierQuoteDetailEntity();
        detail.setRequestPriceDetail(null);
        detail.setOptionName(sourceDetail.getOptionName());
        detail.setSpec(sourceDetail.getSpec());
        detail.setSortOrder(sourceDetail.getSortOrder());
        detail.setRemark(sourceDetail.getRemark());
        detail.setPackageDimension(sourceDetail.getPackageDimension());
        detail.setPackageWeight(sourceDetail.getPackageWeight());
        detail.setPackageCapacity(sourceDetail.getPackageCapacity());

        for (RfqSupplierQuoteDetailPackageEntity sourcePackage :
                resolveSupplierQuoteDetailPackagesForCopy(sourceDetail).stream()
                        .sorted(Comparator.comparing(RfqSupplierQuoteDetailPackageEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(RfqSupplierQuoteDetailPackageEntity::getId, Comparator.nullsLast(Long::compareTo)))
                        .toList()) {
            detail.addPackage(cloneSupplierQuoteDetailPackage(sourcePackage));
        }

        for (RfqSupplierQuoteTierEntity sourceTier : Optional.ofNullable(sourceDetail.getTiers()).orElse(List.of())
                .stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteTierEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteTierEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList()) {
            detail.addTier(cloneSupplierQuoteTier(sourceTier));
        }

        return detail;
    }

    private List<RfqSupplierQuoteDetailPackageEntity> resolveSupplierQuoteDetailPackagesForCopy(
            RfqSupplierQuoteDetailEntity entity
    ) {
        if (entity == null) {
            return List.of();
        }

        if (!CollectionUtils.isEmpty(entity.getPackages())) {
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

        RfqSupplierQuoteDetailPackageEntity packageEntity = new RfqSupplierQuoteDetailPackageEntity();
        packageEntity.setPackageName(null);
        packageEntity.setPackageDimension(entity.getPackageDimension());
        packageEntity.setPackageWeight(entity.getPackageWeight());
        packageEntity.setPackageCapacity(entity.getPackageCapacity());
        packageEntity.setSortOrder(1);
        return List.of(packageEntity);
    }

    private List<RfqSupplierQuotePackageEntity> resolveSupplierQuotePackagesForCopy(
            RfqSupplierQuoteEntity entity
    ) {
        if (entity == null) {
            return List.of();
        }

        if (!CollectionUtils.isEmpty(entity.getPackages())) {
            return entity.getPackages().stream()
                    .sorted(Comparator.comparing(RfqSupplierQuotePackageEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(RfqSupplierQuotePackageEntity::getId, Comparator.nullsLast(Long::compareTo)))
                    .toList();
        }

        List<RfqSupplierQuotePackageEntity> fallbackPackages = new ArrayList<>();
        int sortOrder = 1;
        for (RfqSupplierQuoteDetailEntity detail : Optional.ofNullable(entity.getDetails()).orElse(List.of()).stream()
                .sorted(Comparator.comparing(RfqSupplierQuoteDetailEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RfqSupplierQuoteDetailEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList()) {
            for (RfqSupplierQuoteDetailPackageEntity detailPackage : resolveSupplierQuoteDetailPackagesForCopy(detail)) {
                RfqSupplierQuotePackageEntity packageEntity = new RfqSupplierQuotePackageEntity();
                packageEntity.setPackageName(detailPackage.getPackageName());
                packageEntity.setPackageDimension(detailPackage.getPackageDimension());
                packageEntity.setPackageWeight(detailPackage.getPackageWeight());
                packageEntity.setPackageCapacity(detailPackage.getPackageCapacity());
                packageEntity.setSortOrder(sortOrder++);
                fallbackPackages.add(packageEntity);
            }
        }

        return fallbackPackages;
    }

    private RfqSupplierQuoteTierEntity cloneSupplierQuoteTier(RfqSupplierQuoteTierEntity sourceTier) {
        RfqSupplierQuoteTierEntity tier = new RfqSupplierQuoteTierEntity();
        tier.setQuantity(sourceTier.getQuantity());
        tier.setProductPrice(scaleMoney(sourceTier.getProductPrice()));
        tier.setShippingCost(scaleMoney(sourceTier.getShippingCost()));
        tier.setProductPriceCurrency(sourceTier.getProductPriceCurrency());
        tier.setShippingCostCurrency(sourceTier.getShippingCostCurrency());
        tier.setCurrency(sourceTier.getCurrency());
        tier.setSortOrder(sourceTier.getSortOrder());
        return tier;
    }

    private RfqSupplierQuoteAdditionalCostEntity cloneSupplierQuoteAdditionalCost(
            RfqSupplierQuoteAdditionalCostEntity sourceAdditionalCost
    ) {
        RfqSupplierQuoteAdditionalCostEntity additionalCost = new RfqSupplierQuoteAdditionalCostEntity();
        additionalCost.setDescription(sourceAdditionalCost.getDescription());
        additionalCost.setUnit(sourceAdditionalCost.getUnit());
        additionalCost.setValue(sourceAdditionalCost.getValue());
        additionalCost.setSortOrder(sourceAdditionalCost.getSortOrder());
        return additionalCost;
    }

    private RfqSupplierQuotePackageEntity cloneSupplierQuotePackage(RfqSupplierQuotePackageEntity sourcePackage) {
        RfqSupplierQuotePackageEntity packageEntity = new RfqSupplierQuotePackageEntity();
        packageEntity.setPackageName(sourcePackage.getPackageName());
        packageEntity.setPackageDimension(sourcePackage.getPackageDimension());
        packageEntity.setPackageWeight(sourcePackage.getPackageWeight());
        packageEntity.setPackageCapacity(sourcePackage.getPackageCapacity());
        packageEntity.setSortOrder(sourcePackage.getSortOrder());
        return packageEntity;
    }

    private RfqSupplierQuoteDetailPackageEntity cloneSupplierQuoteDetailPackage(
            RfqSupplierQuoteDetailPackageEntity sourcePackage
    ) {
        RfqSupplierQuoteDetailPackageEntity packageEntity = new RfqSupplierQuoteDetailPackageEntity();
        packageEntity.setPackageName(sourcePackage.getPackageName());
        packageEntity.setPackageDimension(sourcePackage.getPackageDimension());
        packageEntity.setPackageWeight(sourcePackage.getPackageWeight());
        packageEntity.setPackageCapacity(sourcePackage.getPackageCapacity());
        packageEntity.setSortOrder(sourcePackage.getSortOrder());
        return packageEntity;
    }

    private RfqSupplierQuoteLeadTimeEntity cloneSupplierQuoteLeadTime(RfqSupplierQuoteLeadTimeEntity sourceLeadTime) {
        RfqSupplierQuoteLeadTimeEntity leadTime = new RfqSupplierQuoteLeadTimeEntity();
        leadTime.setLeadTimeConfig(sourceLeadTime.getLeadTimeConfig());
        leadTime.setLeadTimeDayMin(sourceLeadTime.getLeadTimeDayMin());
        leadTime.setLeadTimeDayMax(sourceLeadTime.getLeadTimeDayMax());
        leadTime.setRemark(sourceLeadTime.getRemark());
        leadTime.setSortOrder(sourceLeadTime.getSortOrder());
        return leadTime;
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
        UploadFileResponse uploadedFile = fileStorageService.uploadFile(pictureFile, entity.getId());

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
        RfqHeaderDto dto = requestPriceHeaderMapper.toDto(entity);
        List<QuotationEntity> quotationEntities = quotationRepository.findAllByRfqIdOrderByCreatedDateDesc(entity.getId());
        dto.setQuotations(quotationEntities.stream()
                .map(quotationEntity -> {
                    RfqQuotationDto quotationDto = new RfqQuotationDto();
                    quotationDto.setQuotationNo(quotationEntity.getQuotationNo());
                    quotationDto.setRfqId(quotationEntity.getRfqId());
                    quotationDto.setCreatedDate(quotationEntity.getCreatedDate());
                    quotationDto.setUpdatedDate(quotationEntity.getUpdatedDate());
                    quotationDto.setStatus(quotationEntity.getStatus());
                    quotationDto.setStatusProfile(DocumentStatusResolver.resolveQuotation(quotationEntity.getStatus()));
                    quotationDto.setRevNo(quotationEntity.getRevNo());
                    quotationDto.setGrandTotal(quotationEntity.getGrandTotal());
                    quotationDto.setDocDate(quotationEntity.getDocDate() != null ? quotationEntity.getDocDate().toString() : null);
                    return quotationDto;
                })
                .toList());
        dto.setQuotationNo(quotationEntities.isEmpty() ? null : quotationEntities.get(0).getQuotationNo());
        return dto;
//        dto.setServiceLevelAgreement(slaConfigService.getSlaConfigById(SLA));
//        dto.getServiceLevelAgreement().setDayLeft(slaConfigService.calculateDayLeft(dto.getServiceLevelAgreement(), dto.getRequestedDate().toLocalDate()));
//        return dto;
    }

    private java.util.Map<String, Object> buildActivityDetail(RfqHeaderEntity entity) {
        java.util.Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("requestedDate", entity.getRequestedDate());
        detail.put("status", entity.getStatus());
        detail.put("requestInformation", entity.getRequestInformation());
        detail.put("note", entity.getNote());
        detail.put("contactName", entity.getContactName());
        detail.put("contactPhone", entity.getContactPhone());
        detail.put("contactChannel", entity.getContactChannel());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("referenceRfqId", entity.getReferenceRfqId());
        detail.put("rfqTypeCode", entity.getRfqType() != null ? entity.getRfqType().getId().getCode() : null);
        detail.put("orderTypeCode", entity.getOrderType() != null ? entity.getOrderType().getId().getCode() : null);
        detail.put("shippingMethod", entity.getShippingMethod());
        detail.put("productFamily", entity.getProductFamily());
        detail.put("productUsage", entity.getProductUsage() != null ? entity.getProductUsage().getCode() : null);
        detail.put("systemMechanic", entity.getSystemMechanic() != null ? entity.getSystemMechanic().getCode() : null);
        detail.put("material", entity.getMaterialCode());
        detail.put("capacity", entity.getCapacity());
        detail.put("targetPrice", entity.getTargetPrice());
        detail.put("requestedMoqs", parseRequestedMoq(entity.getRequestedMoq()));
        detail.put("requestSample", entity.getRequestSample());
        detail.put("urgentRequest", entity.getUrgentRequest());
        detail.put("urgentRequestReason", entity.getUrgentRequestReason());
        detail.put("urgentRequestStatus", entity.getUrgentRequestStatus());
        detail.put("urgentRequestedBy", entity.getUrgentRequestedBy());
        detail.put("urgentRequestedDate", entity.getUrgentRequestedDate());
        detail.put("urgentApprovedBy", entity.getUrgentApprovedBy());
        detail.put("urgentApprovedDate", entity.getUrgentApprovedDate());
        detail.put("urgentRejectedBy", entity.getUrgentRejectedBy());
        detail.put("urgentRejectedDate", entity.getUrgentRejectedDate());
        detail.put("urgentRejectReason", entity.getUrgentRejectReason());
        detail.put("description", entity.getDescription());
        detail.put("pictureCount", entity.getPictures() != null ? entity.getPictures().size() : 0);
        return detail;
    }

    private RfqHeaderEntity buildRfqHeaderEntityFromUploadRow(
            Row row,
            Map<String, Integer> headerIndexMap,
            FormulaEvaluator evaluator,
            String userId
    ) throws Exception {
        String actor = userProfileService.getNameFromId(userId);
        String salesId = requiredRfqUploadValue(row, headerIndexMap, evaluator, "salesid");
        String rfqTypeValue = requiredRfqUploadValue(row, headerIndexMap, evaluator, "rfqtype");
        String requestedDateValue = requiredRfqUploadValue(row, headerIndexMap, evaluator, "requesteddate");
        String orderTypeValue = requiredRfqUploadValue(row, headerIndexMap, evaluator, "ordertype");
        String productFamilyValue = requiredRfqUploadValue(row, headerIndexMap, evaluator, "productfamily");
        String productUsageValue = readRfqUploadCell(row, headerIndexMap, evaluator, "productusage");
        String materialValue = readRfqUploadCell(row, headerIndexMap, evaluator, "material");

        RfqHeaderEntity entity = new RfqHeaderEntity();
        entity.setRequestedDate(parseRfqUploadRequestedDate(row, headerIndexMap, "requesteddate", requestedDateValue));
        entity.setStatus(RfqStatus.NEW);
        entity.setIsAccept(Boolean.FALSE);
        entity.setShippingMethod("ALL");
        entity.setContactName(StringUtils.trimToNull(readRfqUploadCell(row, headerIndexMap, evaluator, "contactname")));
        entity.setContactPhone(null);
        entity.setContactChannel(resolveSystemConfigCodeByImportValue(
                SystemConstant.CONTACT_CHANNEL,
                readRfqUploadCell(row, headerIndexMap, evaluator, "contactchannel"),
                "Contact channel"
        ));
        entity.setCapacity(StringUtils.trimToNull(readRfqUploadCell(row, headerIndexMap, evaluator, "capacity")));
        entity.setDescription(StringUtils.trimToNull(readRfqUploadCell(row, headerIndexMap, evaluator, "description")));
        entity.setCreatedBy(actor);
        entity.setUpdatedBy(actor);
        entity.setUrgentRequest(Boolean.FALSE);

        EmployeeEntity sales = resolveSales(salesId);
        EmployeeEntity procurement = resolveDefaultProcurementForSales(sales);

        entity.setSales(sales);
        entity.setCustomer(null);
        entity.setProcurement(procurement);
        entity.setReferenceRfqId(null);
        entity.setReferenceRfq(null);
        entity.setRfqType(resolveSystemConfigByImportValue(SystemConstant.RFQ_TYPE, rfqTypeValue, "RFQ type"));
        entity.setOrderType(resolveSystemConfigByImportValue(SystemConstant.ORDER_TYPE, orderTypeValue, "Order type"));

        String productFamilyCode = resolveProductFamilyCodeByImportValue(productFamilyValue);
        String productSubtype1Code = resolveProductSubtype1CodeByImportValue(productFamilyCode, productUsageValue);
        String productSubtype2Code = resolveProductSubtype2CodeByImportValue(
                productSubtype1Code,
                readRfqUploadCell(row, headerIndexMap, evaluator, "systemmechanic")
        );
        String productMaterialCode = resolveProductMaterialCodeByImportValue(productFamilyCode, materialValue);
        applyProductHierarchy(entity, productFamilyCode, productSubtype1Code, productSubtype2Code, productMaterialCode);

        return entity;
    }

    private EmployeeEntity resolveDefaultProcurementForSales(EmployeeEntity sales) {
        if (sales == null) {
            return null;
        }

        List<EmployeeProcurementMappingEntity> mappings =
                employeeProcurementMappingRepository.findBySalesEmployee_EmployeeId(sales.getEmployeeId());
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }

        return mappings.stream()
                .filter(mapping -> mapping.getProcurementEmployee() != null)
                .sorted(Comparator
                        .comparing((EmployeeProcurementMappingEntity mapping) -> !Boolean.TRUE.equals(mapping.getIsDefault()))
                        .thenComparing(mapping -> StringUtils.defaultString(
                                mapping.getProcurementEmployee() != null ? mapping.getProcurementEmployee().getEmployeeId() : null)))
                .map(EmployeeProcurementMappingEntity::getProcurementEmployee)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Integer> buildRfqUploadHeaderIndexMap(Row headerRow, FormulaEvaluator evaluator) {
        Map<String, Integer> headerIndexMap = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell, evaluator);
            if (StringUtils.isBlank(header)) {
                continue;
            }
            headerIndexMap.put(header.trim().toLowerCase(Locale.ROOT), cell.getColumnIndex());
        }
        return headerIndexMap;
    }

    private void validateRfqUploadHeaders(Map<String, Integer> headerIndexMap) throws InvalidRequestException {
        List<String> requiredHeaders = List.of(
                "rfqtype",
                "requesteddate",
                "salesid",
                "contactname",
                "contactchannel",
                "ordertype",
                "productfamily",
                "systemmechanic",
                "capacity",
                "description"
        );

        List<String> missingHeaders = new ArrayList<>();
        for (String header : requiredHeaders) {
            if (!headerIndexMap.containsKey(header)) {
                missingHeaders.add(header);
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw new InvalidRequestException("RFQ excel headers missing: " + String.join(", ", missingHeaders));
        }
    }

    private boolean isBlankRfqUploadRow(Row row, FormulaEvaluator evaluator, Map<String, Integer> headerIndexMap) {
        for (String header : List.of("rfqtype", "salesid", "productfamily", "description")) {
            if (StringUtils.isNotBlank(readRfqUploadCell(row, headerIndexMap, evaluator, header))) {
                return false;
            }
        }
        return true;
    }

    private String readRfqUploadCell(Row row, Map<String, Integer> headerIndexMap, FormulaEvaluator evaluator, String headerName) {
        Integer columnIndex = headerIndexMap.get(headerName.toLowerCase(Locale.ROOT));
        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell, evaluator);
        return StringUtils.trimToNull(value);
    }

    private String requiredRfqUploadValue(Row row, Map<String, Integer> headerIndexMap, FormulaEvaluator evaluator, String headerName)
            throws InvalidRequestException {
        String value = readRfqUploadCell(row, headerIndexMap, evaluator, headerName);
        if (StringUtils.isBlank(value)) {
            throw new InvalidRequestException(headerName + " is required.");
        }
        return value;
    }

    private ZonedDateTime parseRfqUploadRequestedDate(
            Row row,
            Map<String, Integer> headerIndexMap,
            String headerName,
            String rawValue
    ) throws InvalidRequestException {
        Integer columnIndex = headerIndexMap.get(headerName.toLowerCase(Locale.ROOT));
        if (columnIndex == null) {
            throw new InvalidRequestException(headerName + " is required.");
        }

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            LocalDate localDate = org.apache.poi.ss.usermodel.DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
            return localDate.atTime(LocalTime.MIN).atZone(DateUtil.getTimeZone());
        }

        String normalized = normalizeImportValue(rawValue);
        if (normalized == null) {
            throw new InvalidRequestException(headerName + " is required.");
        }

        if (StringUtils.isNumeric(normalized)) {
            LocalDate localDate = org.apache.poi.ss.usermodel.DateUtil.getLocalDateTime(Double.parseDouble(normalized)).toLocalDate();
            return localDate.atTime(LocalTime.MIN).atZone(DateUtil.getTimeZone());
        }

        List<DateTimeFormatter> formatters = List.of(
                DateUtil.DD_MM_YY,
                DateUtil.DD_MM_YY_2,
                DateUtil.DD_M_YY,
                DateTimeFormatter.ISO_LOCAL_DATE
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate localDate = LocalDate.parse(normalized, formatter);
                return localDate.atTime(LocalTime.MIN).atZone(DateUtil.getTimeZone());
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }

        throw new InvalidRequestException("requestedDate is invalid: " + normalized);
    }

    private SystemConfigEntity resolveSystemConfigByImportValue(SystemConstant groupCode, String value, String fieldLabel)
            throws DataNotFoundException {
        String normalized = normalizeImportValue(value);
        if (normalized == null) {
            return null;
        }

        return systemConfigRepository.findByIdGroupCodeOrderBySortAsc(groupCode).stream()
                .filter(config -> matchesImportValue(
                        normalized,
                        config.getId() != null ? config.getId().getCode() : null,
                        config.getNameTh(),
                        config.getNameEn()
                ))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(fieldLabel + " " + value + " not found."));
    }

    private String resolveSystemConfigCodeByImportValue(SystemConstant groupCode, String value, String fieldLabel)
            throws DataNotFoundException {
        SystemConfigEntity config = resolveSystemConfigByImportValue(groupCode, value, fieldLabel);
        return config != null && config.getId() != null ? config.getId().getCode() : null;
    }

    private String resolveProductFamilyCodeByImportValue(String value) throws DataNotFoundException {
        String normalized = normalizeImportValue(value);
        if (normalized == null) {
            return null;
        }

        return productFamilyEntityRepository.findAllByIsActiveTrueOrderByCodeAsc().stream()
                .filter(entity -> matchesImportValue(normalized, entity.getCode(), entity.getNameTh(), entity.getNameEn()))
                .map(ProductFamilyEntity::getCode)
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Product family " + value + " not found."));
    }

    private String resolveProductSubtype1CodeByImportValue(String productFamilyCode, String value)
            throws DataNotFoundException {
        String normalized = normalizeImportValue(value);
        if (normalized == null) {
            return null;
        }

        List<ProductSubtype1Entity> candidates = StringUtils.isBlank(productFamilyCode)
                ? productSubtype1Repository.findAllByOrderByProductFamilyCodeAscCodeAsc()
                : productSubtype1Repository.findAllByProductFamilyCodeOrderByCodeAsc(productFamilyCode);
        return candidates.stream()
                .filter(entity -> matchesImportValue(normalized, entity.getCode(), entity.getNameTh(), entity.getNameEn()))
                .map(ProductSubtype1Entity::getCode)
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Product subtype1 " + value + " not found."));
    }

    private String resolveProductSubtype2CodeByImportValue(String productSubtype1Code, String value)
            throws DataNotFoundException {
        String normalized = normalizeImportValue(value);
        if (normalized == null || StringUtils.equalsIgnoreCase(normalized, "Optional (ตัวเลือก - ไม่บังคับใช้)")) {
            return null;
        }

        List<ProductSubtype2Entity> candidates = StringUtils.isBlank(productSubtype1Code)
                ? productSubtype2Repository.findAllByOrderByProductSubtype1CodeAscCodeAsc()
                : productSubtype2Repository.findAllByProductSubtype1CodeOrderByCodeAsc(productSubtype1Code);
        return candidates.stream()
                .filter(entity -> matchesImportValue(normalized, entity.getCode(), entity.getNameTh(), entity.getNameEn()))
                .map(ProductSubtype2Entity::getCode)
                .findFirst()
                .orElse(null);
    }

    private String resolveProductMaterialCodeByImportValue(String productFamilyCode, String value)
            throws DataNotFoundException {
        String normalized = normalizeImportValue(value);
        if (normalized == null) {
            return null;
        }

        return productMaterialRepository.findAllByProductFamilyCodeOrderByCodeAsc(productFamilyCode).stream()
                .filter(entity -> matchesImportValue(normalized, entity.getCode(), entity.getNameTh(), entity.getNameEn()))
                .map(ProductMaterialEntity::getCode)
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Product material " + value + " not found."));
    }

    private boolean matchesImportValue(String importValue, String code, String nameTh, String nameEn) {
        return StringUtils.equalsIgnoreCase(importValue, normalizeImportValue(code))
                || StringUtils.equalsIgnoreCase(importValue, normalizeImportValue(nameTh))
                || StringUtils.equalsIgnoreCase(importValue, normalizeImportValue(nameEn))
                || StringUtils.equalsIgnoreCase(importValue, normalizeImportValue(buildDisplayName(nameEn, nameTh)));
    }

    private String buildDisplayName(String nameEn, String nameTh) {
        String en = StringUtils.trimToNull(nameEn);
        String th = StringUtils.trimToNull(nameTh);
        if (en == null) {
            return th;
        }
        if (th == null) {
            return en;
        }
        return en + " (" + th + ")";
    }

    private String normalizeImportValue(String value) {
        if (value == null) {
            return null;
        }
        return StringUtils.normalizeSpace(StringUtils.trimToNull(value));
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

    private String appendRfqNote(
            String existingNote,
            String note,
            String notedBy,
            ZonedDateTime notedDate
    ) throws InvalidRequestException {
        List<RfqNoteEntry> entries = parseRfqNotes(existingNote);
        entries.add(new RfqNoteEntry(note, notedBy, notedDate));

        try {
            return objectMapper.writeValueAsString(entries);
        } catch (Exception exception) {
            throw new InvalidRequestException("Cannot serialize rfq note.");
        }
    }

    private List<RfqNoteEntry> parseRfqNotes(String noteJson) {
        if (StringUtils.isBlank(noteJson)) {
            return new ArrayList<>();
        }

        try {
            RfqNoteEntry[] entries = objectMapper.readValue(noteJson, RfqNoteEntry[].class);
            return new ArrayList<>(Arrays.asList(entries));
        } catch (Exception exception) {
            log.warn("Cannot parse rfq note json", exception);
            return new ArrayList<>();
        }
    }

    private List<BigDecimal> parseRequestedMoq(String requestedMoqJson) {
        if (StringUtils.isBlank(requestedMoqJson)) {
            return new ArrayList<>();
        }

        try {
            BigDecimal[] values = objectMapper.readValue(requestedMoqJson, BigDecimal[].class);
            return new ArrayList<>(Arrays.asList(values));
        } catch (Exception exception) {
            log.warn("Cannot parse requested moq json", exception);
            return new ArrayList<>();
        }
    }

    private void enrichAndFilterCreatedPurchaseOrders(List<RfqHeaderDto> rfqs) {
        if (rfqs == null || rfqs.isEmpty()) {
            return;
        }

        List<String> salesOrderIds = rfqs.stream()
                .map(RfqHeaderDto::getSaleOrderId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        if (salesOrderIds.isEmpty()) {
            rfqs.clear();
            return;
        }

        Set<String> activePurchaseOrderSalesOrderIds = purchaseOrderRepository.findBySalesOrderSalesOrderNoIn(salesOrderIds)
                .stream()
                .filter(purchaseOrder -> purchaseOrder.getStatus() != PurchaseOrderStatus.CANCELLED)
                .map(PurchaseOrderEntity::getSalesOrder)
                .filter(Objects::nonNull)
                .map(SalesOrderEntity::getSalesOrderNo)
                .filter(StringUtils::isNotBlank)
                .collect(java.util.stream.Collectors.toSet());

        rfqs.removeIf(rfq -> {
            boolean isCreatedPurchaseOrder = StringUtils.isNotBlank(rfq.getSaleOrderId())
                    && activePurchaseOrderSalesOrderIds.contains(rfq.getSaleOrderId());
            rfq.setIsCreatedPurchaseOrder(isCreatedPurchaseOrder);
            return !isCreatedPurchaseOrder;
        });
        log.info("rfqs : {}", rfqs);
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
        boolean hasTiers = request.getTiers() != null && !request.getTiers().isEmpty();
        boolean hasTierSplits = request.getTierSplits() != null && !request.getTierSplits().isEmpty();
        if (!hasTiers && !hasTierSplits) {
            throw new InvalidRequestException("tiers or tierSplits are required");
        }

        RfqDetailEntity detailEntity = new RfqDetailEntity();
        detailEntity.setOptionName(StringUtils.trimToNull(request.getOptionName()));
        detailEntity.setSpec(request.getSpec().trim());
        detailEntity.setSortOrder(request.getSortOrder());
        detailEntity.setRemark(StringUtils.trimToNull(request.getRemark()));
        detailEntity.setRecommend(StringUtils.trimToNull(request.getRecommend()));
        detailEntity.setCommission(scaleMoney(request.getCommission()));
        detailEntity.setPackageDimension(StringUtils.trimToNull(request.getPackageDimension()));
        detailEntity.setPackageWeight(StringUtils.trimToNull(request.getPackageWeight()));
        detailEntity.setPackageCapacity(StringUtils.trimToNull(request.getPackageCapacity()));
        SupplierEntity supplier = null;
        if (StringUtils.isNotBlank(request.getSupplierId())) {
            supplier = getSupplierEntity(request.getSupplierId().trim());
            detailEntity.setSupplier(supplier);
        }
        detailEntity.setUpdatedBy(updatedBy);

        if (hasTiers) {
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
                tierEntity.setProductPrice(scaleMoney(tierRequest.getProductPrice()));
                tierEntity.setCommission(scaleMoney(tierRequest.getCommission()));
                tierEntity.setCurrency(tierRequest.getCurrency());
                tierEntity.setLandFreightCost(scaleMoney(tierRequest.getLandFreightCost()));
                tierEntity.setSeaFreightCost(scaleMoney(tierRequest.getSeaFreightCost()));
                boolean isShareFcl = Boolean.TRUE.equals(tierRequest.getIsShareFCL());
                tierEntity.setIsShareFCL(isShareFcl);
                tierEntity.setIsFcl(Boolean.TRUE.equals(tierRequest.getIsFcl()) || isShareFcl);
                tierEntity.setLandTotalPrice(scaleMoney(tierRequest.getLandTotalPrice()));
                tierEntity.setSeaTotalPrice(scaleMoney(tierRequest.getSeaTotalPrice()));
                tierEntity.setSupplierQuoteTierId(tierRequest.getSupplierQuoteTierId());
                tierEntity.setSortOrder(
                        tierRequest.getSortOrder() != null ? tierRequest.getSortOrder() : nextSortOrder++
                );
                detailEntity.addTier(tierEntity);
            }
        }

        if (hasTierSplits) {
            for (CreateRequestPriceDetailRequest.CreateRequestPriceTierSplitRequest tierSplitRequest : request.getTierSplits()) {
                if (tierSplitRequest.getQuantity() == null) {
                    throw new InvalidRequestException("tierSplit.quantity is required");
                }
                if (tierSplitRequest.getSellPrice() == null) {
                    throw new InvalidRequestException("tierSplit.sellPrice is required");
                }

                RfqTierSplitEntity tierSplitEntity = new RfqTierSplitEntity();
                SupplierEntity tierSplitSupplier = supplier;
                if (StringUtils.isNotBlank(tierSplitRequest.getSupplierId())) {
                    tierSplitSupplier = getSupplierEntity(tierSplitRequest.getSupplierId().trim());
                }
                tierSplitEntity.setSupplier(tierSplitSupplier);
                tierSplitEntity.setQuantity(tierSplitRequest.getQuantity());
                tierSplitEntity.setSellPrice(scaleMoney(tierSplitRequest.getSellPrice()));
                tierSplitEntity.setCommission(scaleMoney(tierSplitRequest.getCommission()));
                tierSplitEntity.setCurrency(tierSplitRequest.getCurrency());
                tierSplitEntity.setLandFreightCost(scaleMoney(tierSplitRequest.getLandFreightCost()));
                tierSplitEntity.setLandFreightQty(scaleMoney(tierSplitRequest.getLandFreightQty()));
                tierSplitEntity.setSeaFreightQty(scaleMoney(tierSplitRequest.getSeaFreightQty()));
                tierSplitEntity.setSeaFreightCost(scaleMoney(tierSplitRequest.getSeaFreightCost()));
                boolean isShareFcl = Boolean.TRUE.equals(tierSplitRequest.getIsShareFCL());
                tierSplitEntity.setIsShareFCL(isShareFcl);
                tierSplitEntity.setIsFcl(Boolean.TRUE.equals(tierSplitRequest.getIsFcl()) || isShareFcl);
                detailEntity.addTierSplit(tierSplitEntity);
            }
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

    private void applyRelations(
            RfqHeaderEntity entity,
            String salesId,
            String customerId,
            String rfqTypeCode,
            String orderTypeCode,
            String procurementId,
            String referenceRfqId
    )
            throws DataNotFoundException {
        entity.setSales(resolveSales(salesId));
        entity.setCustomer(resolveCustomer(customerId));
        entity.setRfqType(resolveRfqType(rfqTypeCode));
        entity.setOrderType(resolveOrderType(orderTypeCode));
        entity.setProcurement(resolveProcurement(procurementId));
        entity.setReferenceRfqId(StringUtils.trimToNull(referenceRfqId));
        entity.setReferenceRfq(resolveReferenceRfq(referenceRfqId));
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

        return productFamilyEntityRepository.findByCodeAndIsActiveTrue(productFamilyCode.trim())
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

    private SystemConfigEntity resolveRfqType(String rfqTypeCode) throws DataNotFoundException {
        if (StringUtils.isBlank(rfqTypeCode)) {
            return null;
        }

        SystemConfigEntity rfqType = systemConfigService.getConfigEntity(SystemConstant.RFQ_TYPE, rfqTypeCode.trim());
        if (rfqType == null) {
            throw new DataNotFoundException("RFQ type " + rfqTypeCode + " not found.");
        }
        return rfqType;
    }

    private RfqHeaderEntity resolveReferenceRfq(String referenceRfqId) throws DataNotFoundException {
        if (StringUtils.isBlank(referenceRfqId)) {
            return null;
        }

        return requestPriceHeaderRepository.findById(referenceRfqId.trim())
                .orElseThrow(() -> new DataNotFoundException("Reference RFQ " + referenceRfqId + " not found."));
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

    private void validateSuperAdmin(String userId) throws InvalidRequestException {
        String roleCode = userProfileService.getRoleCodeFromId(userId);
        if (!StringUtils.equals(roleCode, SUPER_ADMIN_ROLE_CODE)) {
            throw new InvalidRequestException("Only SUPER_ADMIN can approve or reject urgent RFQ.");
        }
    }

    private void validateUrgentPendingApproval(RfqHeaderEntity entity) throws InvalidRequestException {
        if (!Boolean.TRUE.equals(entity.getUrgentRequest())) {
            throw new InvalidRequestException("RFQ is not an urgent request.");
        }

        if (!UrgentRequestStatus.PENDING_APPROVAL.equals(entity.getUrgentRequestStatus())) {
            throw new InvalidRequestException("Urgent request is not pending approval.");
        }
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

            UploadFileResponse uploadedFile = fileStorageService.uploadFile(picture, entity.getId());

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

    private RfqTierEntity getTierFromDetail(RfqDetailEntity detail, Long tierId) throws DataNotFoundException {
        return detail.getTiers().stream()
                .filter(tier -> Objects.equals(tier.getId(), tierId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException(
                        "Tier " + tierId + " not found in detail " + detail.getId()
                ));
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
                .and(isAcceptEqual(request.getIsAccept()))
                .and(customerIdEqual(request.getCustomerId()))
                .and(salesIdEqual(request.getSalesId()))
                .and(procurementIdEqual(request.getProcurementId()))
                .and(rfqTypeCodeEqual(request.getRfqTypeCode()))
                .and(orderTypeCodeEqual(
                        request.getOrderTypeCode() != null && !request.getOrderTypeCode().isBlank()
                                ? request.getOrderTypeCode()
                                : request.getOrderType()
                ))
                .and(productFamilyEqual(request.getProductFamily()))
                .and(productSubtype1Equal(request.getProductSubtype1()))
                .and(productMaterialEqual(request.getProductMaterial()))
                .and(requestedDateBetween(request.getRequestedDateStart(), request.getRequestedDateEnd()))
                .and(keywordContain(request.getKeyword()))
                .and(Boolean.TRUE.equals(request.getPrioritizeApprovedUrgent()) ? orderByApprovedUrgentFirst() : null);
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
            case SPECIAL_PRICE_REVIEW -> "รอทบทวนราคาพิเศษ";
            case CANCELED -> "ยกเลิก";
            case CLOSED -> "ปิดงาน";
            case COMPLETED -> "เสร็จสิ้น";
            case REJECTED -> "ปฏิเสธ";
        };
    }

    private String formatExportDateTime(ZonedDateTime value) {
        if (value == null) {
            return "";
        }
        return value.withZoneSameInstant(DateUtil.getTimeZone())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String resolveCustomerName(RfqHeaderDto record) {
        if (record == null || record.getCustomer() == null) {
            return "";
        }
        return StringUtils.defaultIfBlank(
                record.getCustomer().getCustomerName(),
                record.getCustomer().getCompanyName()
        );
    }

    private String resolveEmployeeId(EmployeeDto employee) {
        if (employee == null) {
            return "";
        }
        return StringUtils.defaultString(employee.getEmployeeId());
    }

    private String resolveEmployeeName(EmployeeDto employee) {
        if (employee == null) {
            return "";
        }
        String fullName = StringUtils.trim(String.join(
                " ",
                StringUtils.defaultString(employee.getFirstNameTh()),
                StringUtils.defaultString(employee.getLastNameTh())
        ));
        return StringUtils.firstNonBlank(employee.getNickName(), fullName, "");
    }

    private String resolveSystemConfigName(SystemConfigDto config) {
        if (config == null) {
            return "";
        }
        return StringUtils.firstNonBlank(config.getNameTh(), config.getNameEn(), config.getCode(), "");
    }

    private String resolveProductFamilyName(RfqHeaderDto record) {
        if (record == null || record.getProductFamily() == null) {
            return "";
        }
        return StringUtils.firstNonBlank(
                record.getProductFamily().getNameTh(),
                record.getProductFamily().getNameEn(),
                record.getProductFamily().getCode(),
                ""
        );
    }

    private String resolveProductSubtype1Name(RfqHeaderDto record) {
        if (record == null || record.getProductSubtype1() == null) {
            return "";
        }
        return StringUtils.firstNonBlank(
                record.getProductSubtype1().getNameTh(),
                record.getProductSubtype1().getNameEn(),
                record.getProductSubtype1().getCode(),
                ""
        );
    }

    private String resolveProductMaterialName(RfqHeaderDto record) {
        if (record == null || record.getMaterial() == null) {
            return "";
        }
        return StringUtils.firstNonBlank(
                record.getMaterial().getNameTh(),
                record.getMaterial().getNameEn(),
                record.getMaterial().getCode(),
                ""
        );
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

    private record RfqNoteEntry(
            String note,
            String notedBy,
            ZonedDateTime notedDate
    ) {
    }

    private void sendAwaitingAcceptNotifications(RfqHeaderEntity entity) {
        try {
            EmployeeEntity procurementUser = entity.getProcurement();

            if (procurementUser == null) {
                log.warn("No procurement user found for rfq {}", entity.getId());
                return;
            }

            Optional<UserEntity> userEntityOptional = userRepository.findByEmployeeEntity_EmployeeId(procurementUser.getEmployeeId());
            if (userEntityOptional.isEmpty()) {
                log.warn("No user with LINE binding found for rfq {}", entity.getId());
                return;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("altText", "มีรายการ RFQ รอรับงาน " + entity.getId());
            placeholders.put("title", "RFQ รอรับงาน");
            placeholders.put(
                    "detail",
                    String.format(
                            "RFQ %s ของ %s อยู่ในสถานะรอกดรับงาน",
                            StringUtils.defaultString(entity.getId(), "-"),
                            entity.getSales().getEmployeeId() + ":" + entity.getSales().getNickName())
            );
            placeholders.put("detailUrl", buildRfqDetailUrl(entity.getId()));

            JsonNode message = renderNotificationTemplate(placeholders);
            UserEntity userEntity = userEntityOptional.get();
            try {
                lineMessageService.sendFlexMessage(userEntityOptional.get().getLineUserId(), message);
            } catch (Exception exception) {
                log.warn("Cannot send awaiting validation notification to user {}", userEntity.getId(), exception);
            }

        } catch (Exception exception) {
            log.warn("Cannot send awaiting validation notifications for rfq {}", entity.getId(), exception);
        }
    }

    private void sendRequestInformationNotificationToSales(
            RfqHeaderEntity entity,
            String requestInformationText,
            String requestedBy
    ) {
        try {
            EmployeeEntity salesOwner = entity.getSales();

            if (salesOwner == null || StringUtils.isBlank(salesOwner.getEmployeeId())) {
                log.warn("No sales owner found for rfq {}", entity.getId());
                return;
            }

            Optional<UserEntity> userEntityOptional =
                    userRepository.findByEmployeeEntity_EmployeeId(salesOwner.getEmployeeId());
            if (userEntityOptional.isEmpty() || StringUtils.isBlank(userEntityOptional.get().getLineUserId())) {
                log.warn("No LINE-bound sales owner found for rfq {}", entity.getId());
                return;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("altText", "RFQ " + entity.getId() + " มีคำขอข้อมูลเพิ่มเติม");
            placeholders.put("title", "RFQ ขอข้อมูลเพิ่มเติม");
            placeholders.put(
                    "detail",
                    String.format(
                            "RFQ %s ถูกขอข้อมูลเพิ่มเติมโดย %s%s",
                            StringUtils.defaultString(entity.getId(), "-"),
                            StringUtils.defaultIfBlank(requestedBy, "-"),
                            StringUtils.isNotBlank(requestInformationText)
                                    ? " : " + StringUtils.abbreviate(requestInformationText, 180)
                                    : ""
                    )
            );
            placeholders.put("detailUrl", buildSalesRfqDetailUrl(entity.getId()));

            JsonNode message = renderNotificationTemplate(placeholders);
            try {
                lineMessageService.sendFlexMessage(userEntityOptional.get().getLineUserId(), message);
            } catch (Exception exception) {
                log.warn(
                        "Cannot send request information notification to sales owner {} for rfq {}",
                        userEntityOptional.get().getId(),
                        entity.getId(),
                        exception
                );
            }
        } catch (Exception exception) {
            log.warn("Cannot send request information notification for rfq {}", entity.getId(), exception);
        }
    }

    private void sendAcceptedByProcurementNotificationToSales(
            RfqHeaderEntity entity,
            String acceptedBy
    ) {
        try {
            EmployeeEntity salesOwner = entity.getSales();

            if (salesOwner == null || StringUtils.isBlank(salesOwner.getEmployeeId())) {
                log.warn("No sales owner found for accepted rfq {}", entity.getId());
                return;
            }

            Optional<UserEntity> userEntityOptional =
                    userRepository.findByEmployeeEntity_EmployeeId(salesOwner.getEmployeeId());
            if (userEntityOptional.isEmpty() || StringUtils.isBlank(userEntityOptional.get().getLineUserId())) {
                log.warn("No LINE-bound sales owner found for accepted rfq {}", entity.getId());
                return;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("altText", "RFQ " + entity.getId() + " ถูกจัดซื้อรับงานแล้ว");
            placeholders.put("title", "RFQ ถูกจัดซื้อรับงานแล้ว");
            placeholders.put(
                    "detail",
                    String.format(
                            "RFQ %s ถูกจัดซื้อรับงานแล้วโดย %s และอยู่ในสถานะกำลังดำเนินการ",
                            StringUtils.defaultString(entity.getId(), "-"),
                            StringUtils.defaultIfBlank(acceptedBy, "-")
                    )
            );
            placeholders.put("detailUrl", buildSalesRfqDetailUrl(entity.getId()));

            JsonNode message = renderNotificationTemplate(placeholders);
            try {
                lineMessageService.sendFlexMessage(userEntityOptional.get().getLineUserId(), message);
            } catch (Exception exception) {
                log.warn(
                        "Cannot send accepted-by-procurement notification to sales owner {} for rfq {}",
                        userEntityOptional.get().getId(),
                        entity.getId(),
                        exception
                );
            }
        } catch (Exception exception) {
            log.warn("Cannot send accepted-by-procurement notification for rfq {}", entity.getId(), exception);
        }
    }

    private void sendSalesUpdatedInformationNotificationToProcurement(
            RfqHeaderEntity entity,
            String updatedBy
    ) {
        try {
            EmployeeEntity procurementOwner = entity.getProcurement();

            if (procurementOwner == null || StringUtils.isBlank(procurementOwner.getEmployeeId())) {
                log.warn("No procurement owner found for rfq {}", entity.getId());
                return;
            }

            Optional<UserEntity> userEntityOptional =
                    userRepository.findByEmployeeEntity_EmployeeId(procurementOwner.getEmployeeId());
            if (userEntityOptional.isEmpty() || StringUtils.isBlank(userEntityOptional.get().getLineUserId())) {
                log.warn("No LINE-bound procurement owner found for rfq {}", entity.getId());
                return;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("altText", "RFQ " + entity.getId() + " ถูกอัปเดตข้อมูลโดยเซลล์");
            placeholders.put("title", "RFQ ถูกอัปเดตข้อมูล");
            placeholders.put(
                    "detail",
                    String.format(
                            "เซลล์ได้อัปเดตข้อมูล RFQ %s แล้วโดย %s กรุณาเข้าตรวจสอบรายละเอียด",
                            StringUtils.defaultString(entity.getId(), "-"),
                            StringUtils.defaultIfBlank(updatedBy, "-")
                    )
            );
            placeholders.put("detailUrl", buildRfqDetailUrl(entity.getId()));

            JsonNode message = renderNotificationTemplate(placeholders);
            try {
                lineMessageService.sendFlexMessage(userEntityOptional.get().getLineUserId(), message);
            } catch (Exception exception) {
                log.warn(
                        "Cannot send sales-updated-information notification to procurement owner {} for rfq {}",
                        userEntityOptional.get().getId(),
                        entity.getId(),
                        exception
                );
            }
        } catch (Exception exception) {
            log.warn("Cannot send sales-updated-information notification for rfq {}", entity.getId(), exception);
        }
    }

    private void sendFinalQuotedNotificationToSales(
            RfqHeaderEntity entity,
            String finalQuotedBy
    ) {
        try {
            EmployeeEntity salesOwner = entity.getSales();

            if (salesOwner == null || StringUtils.isBlank(salesOwner.getEmployeeId())) {
                log.warn("No sales owner found for final quoted rfq {}", entity.getId());
                return;
            }

            Optional<UserEntity> userEntityOptional =
                    userRepository.findByEmployeeEntity_EmployeeId(salesOwner.getEmployeeId());
            if (userEntityOptional.isEmpty() || StringUtils.isBlank(userEntityOptional.get().getLineUserId())) {
                log.warn("No LINE-bound sales owner found for final quoted rfq {}", entity.getId());
                return;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("altText", "RFQ เลขที่ " + entity.getId() + " ได้ราคาแล้ว");
            placeholders.put("title", "RFQ ได้ราคาแล้ว");
            placeholders.put(
                    "detail",
                    String.format(
                            "RFQ เลขที่ %s ได้ราคาแล้ว กรุณาเข้าตรวจสอบรายละเอียด",
                            StringUtils.defaultString(entity.getId(), "-")
                    )
            );
            placeholders.put("detailUrl", buildSalesRfqDetailUrl(entity.getId()));

            JsonNode message = renderNotificationTemplate(placeholders);
            try {
                lineMessageService.sendFlexMessage(userEntityOptional.get().getLineUserId(), message);
            } catch (Exception exception) {
                log.warn(
                        "Cannot send final quoted notification to sales owner {} for rfq {}",
                        userEntityOptional.get().getId(),
                        entity.getId(),
                        exception
                );
            }
        } catch (Exception exception) {
            log.warn("Cannot send final quoted notification for rfq {}", entity.getId(), exception);
        }
    }

    private void sendRequestSpecialPriceReviewNotificationToProcurement(
            RfqHeaderEntity entity,
            String requestedBy
    ) {
        try {
            EmployeeEntity procurementOwner = entity.getProcurement();

            if (procurementOwner == null || StringUtils.isBlank(procurementOwner.getEmployeeId())) {
                log.warn("No procurement owner found for special price review rfq {}", entity.getId());
                return;
            }

            Optional<UserEntity> userEntityOptional =
                    userRepository.findByEmployeeEntity_EmployeeId(procurementOwner.getEmployeeId());
            if (userEntityOptional.isEmpty() || !Status.ACTIVE.equals(userEntityOptional.get().getStatus())
                    || StringUtils.isBlank(userEntityOptional.get().getLineUserId())) {
                log.warn("No active LINE-bound procurement owner found for special price review rfq {}", entity.getId());
                return;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("altText", "RFQ " + entity.getId() + " รอทบทวนราคาพิเศษ");
            placeholders.put("title", "รอทบทวนราคาพิเศษ");
            placeholders.put(
                    "detail",
                    String.format(
                            "RFQ %s ถูกขอทบทวนราคาพิเศษโดย %s กรุณาเข้าตรวจสอบรายละเอียด",
                            StringUtils.defaultString(entity.getId(), "-"),
                            StringUtils.defaultIfBlank(requestedBy, "-")
                    )
            );
            placeholders.put("detailUrl", buildPriceInquiryDetailUrl(entity.getId()));

            JsonNode message = renderNotificationTemplate(placeholders);
            try {
                lineMessageService.sendFlexMessage(userEntityOptional.get().getLineUserId(), message);
            } catch (Exception exception) {
                log.warn(
                        "Cannot send special price review notification to procurement owner {} for rfq {}",
                        userEntityOptional.get().getId(),
                        entity.getId(),
                        exception
                );
            }
        } catch (Exception exception) {
            log.warn("Cannot send special price review notification to procurement for rfq {}", entity.getId(), exception);
        }
    }

    private String buildRfqDetailUrl(String rfqId) throws InvalidRequestException {
        return UriComponentsBuilder.fromUriString(buildFrontendBaseUrl())
                .path("/price-inquiry/")
                .path(StringUtils.defaultString(rfqId))
                .build()
                .toUriString();
    }

    private String buildPriceInquiryDetailUrl(String rfqId) throws InvalidRequestException {
        return UriComponentsBuilder.fromUriString(buildFrontendBaseUrl())
                .path("/price-inquiry/")
                .path(StringUtils.defaultString(rfqId))
                .build()
                .toUriString();
    }

    private String buildRfqManagementUrl(
            String procurementId,
            ZonedDateTime requestedDateStart,
            ZonedDateTime requestedDateEnd
    ) throws InvalidRequestException {
        return UriComponentsBuilder.fromUriString(buildFrontendBaseUrl())
                .path("/rfq-management")
                .queryParam("status", RfqStatus.NEW.name())
                .queryParam("isAccept", Boolean.FALSE)
                .queryParam("procurementId", StringUtils.defaultString(procurementId))
                .queryParam(
                        "requestedDateStart",
                        requestedDateStart == null
                                ? ""
                                : requestedDateStart.withZoneSameInstant(DateUtil.getTimeZone()).toLocalDate()
                )
                .queryParam(
                        "requestedDateEnd",
                        requestedDateEnd == null
                                ? ""
                                : requestedDateEnd.withZoneSameInstant(DateUtil.getTimeZone()).toLocalDate()
                )
                .build()
                .toUriString();
    }

    private String buildSalesRfqDetailUrl(String rfqId) throws InvalidRequestException {
        return UriComponentsBuilder.fromUriString(buildFrontendBaseUrl())
                .path("/rfq/")
                .path(StringUtils.defaultString(rfqId))
                .build()
                .toUriString();
    }

    private String buildFrontendBaseUrl() throws InvalidRequestException {
        String loginSuccessUrl = lineConfiguration.getLoginSuccessUrl();
        if (StringUtils.isBlank(loginSuccessUrl)) {
            throw new InvalidRequestException("LINE frontend redirect URL is not configured");
        }

        URI uri = URI.create(loginSuccessUrl);
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    private JsonNode renderNotificationTemplate(Map<String, String> placeholders) throws Exception {
        ClassPathResource resource = new ClassPathResource("line/notification.json");
        try (InputStream inputStream = resource.getInputStream()) {
            String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String rendered = template;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rendered = rendered.replace("${" + entry.getKey() + "}", StringUtils.defaultString(entry.getValue()));
            }
            return objectMapper.readTree(rendered);
        }
    }
}
