package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.PromptTemplateEngine;
import com.nutalig.config.TemplateProperties;
import com.nutalig.constant.Currency;
import com.nutalig.constant.*;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.quotation.request.SearchQuotationRequest;
import com.nutalig.controller.quotation.response.SearchQuotationResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.*;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.dto.document.QuotationDocumentDto;
import com.nutalig.dto.document.QuotationItemDocumentDto;
import com.nutalig.dto.document.TermAndConditionDocumentDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.CustomerMapper;
import com.nutalig.mapper.EmployeeMapper;
import com.nutalig.mapper.RequestPriceHeaderMapper;
import com.nutalig.repository.CustomerRepository;
import com.nutalig.repository.EmployeeRepository;
import com.nutalig.repository.QuotationRepository;
import com.nutalig.repository.RequestPriceHeaderRepository;
import com.nutalig.utils.DateUtil;
import com.nutalig.utils.DocumentStatusResolver;
import com.nutalig.utils.PdfMergeUtil;
import com.nutalig.utils.RfqAttachmentUtil;
import com.nutalig.utils.ThaiBahtText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static com.nutalig.constant.BusinessConstant.MessageTemplateCode.DOWNLOAD_QUOTATION_TH;
import static com.nutalig.constant.BusinessConstant.MessageTemplateCode.QUOTATION_NOT_FOUND_TH;
import static com.nutalig.constant.BusinessConstant.VAT_RATE;
import static com.nutalig.constant.SystemConstant.REPORT_ROW;
import static com.nutalig.repository.specification.QuotationSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationService {

    private final GeneratedIdSequenceService generatedIdSequenceService;
    private final LineMessageService lineMessageService;
    private final ReportService reportService;
    private final FileStorageService fileStorageService;
    private final ActivityHistoryService activityHistoryService;
    private final SystemConfigService systemConfigService;
    private final UserTodoService userTodoService;
    private final UserProfileService userProfileService;
    private final QuotationRepository quotationRepository;
    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerMapper customerMapper;
    private final EmployeeMapper employeeMapper;
    private final RequestPriceHeaderMapper requestPriceHeaderMapper;
    private final ObjectMapper objectMapper;
    private final PromptTemplateEngine promptTemplateEngine;
    private final TemplateProperties templateProperties;

    record QuotationSummary(
            BigDecimal subTotal,
            BigDecimal vat,
            BigDecimal grandTotal
    ) {}

    @Transactional(rollbackFor = Exception.class)
    public void createQuotationFromLine(String userId, String jsonStr) throws Exception {
        log.info("Create quotation from line by {}", userId);
        lineMessageService.sendTextMessage(userId, "ระบบกำลังสร้างใบเสนอราคา");

        QuotationRequestDto quotationRequestDto = objectMapper.readValue(jsonStr, QuotationRequestDto.class);
        log.info("Create quotation from line for {}", quotationRequestDto.getCustomerId());

        // Create Quotation
        QuotationEntity quotationEntity = this.createQuotation(quotationRequestDto, userId);

        String quotationNo = quotationEntity.getQuotationNo();

        String message = """
        สร้างใบเสนอราคาเรียบร้อยแล้ว
        
        เลขที่: %s
        
        ดูใบเสนอราคา:
        https://apps.nutalig.com/quotation/%s
        """.formatted(quotationNo, quotationNo);

        lineMessageService.sendTextMessage(userId, message);

        // Get Quotation doc
//        this.getQuotationDocumentById(quotationEntity.getQuotationNo(), new DocumentRequest(ExportFileFormat.PDF, true, false));
    }

    @Transactional(rollbackFor = Exception.class)
    public QuotationEntity createQuotation(QuotationRequestDto requestDto, String createdBy) throws DataNotFoundException {
        log.info("Create quotation for customer {} by {}", requestDto.getCustomerId(), createdBy);

        CustomerEntity customerEntity = resolveCustomer(requestDto.getCustomerId());

        CustomerAddressEntity customerAddressEntity = null;
        if (StringUtils.isEmpty(requestDto.getCustomerAddressId())) {
            customerAddressEntity = customerEntity.getAddresses()
                    .stream()
                    .filter(addr -> Boolean.TRUE.equals(addr.getIsDefault()))
                    .findFirst()
                    .orElse(null);
        } else {
            customerAddressEntity = customerEntity.getAddresses()
                    .stream()
                    .filter(addr -> addr.getId().toString().equals(requestDto.getCustomerAddressId()))
                    .findFirst()
                    .orElse(null);
        }
        CustomerContactEntity customerContactEntity = null;
        if (StringUtils.isEmpty(requestDto.getCustomerContactId())) {
            customerContactEntity = customerEntity.getContacts()
                    .stream()
                    .filter(con -> Boolean.TRUE.equals(con.getIsDefault()))
                    .findFirst()
                    .orElse(null);
        } else {
            customerContactEntity = customerEntity.getContacts()
                    .stream()
                    .filter(con -> requestDto.getCustomerContactId().equals(con.getId().toString()))
                    .findFirst()
                    .orElse(null);
        }

        String actor = userProfileService.getNameFromId(createdBy);
        EmployeeEntity saleEntity = resolveSales(requestDto.getSalesId());

        String nextId = generatedIdSequenceService.getNextSequence(customerEntity.getId(), 3);
        String docId = customerEntity.getId() + "-" + nextId;
        log.info("Create quotation with id : {}", docId);

        QuotationEntity quotationEntity = new QuotationEntity();
        quotationEntity.setIsShowSummary(requestDto.getIsShowSummary());
        quotationEntity.setQuotationNo(docId);

        LocalDate today = LocalDate.now(DateUtil.getTimeZone());
        quotationEntity.setDocDate(requestDto.getDocDate() == null ? today : requestDto.getDocDate());
        quotationEntity.setExpireDate(requestDto.getEffectiveDate() == null ? today.plusDays(7) : requestDto.getEffectiveDate());
        quotationEntity.setStatus(QuotationStatus.ISSUED);
        quotationEntity.setCurrency(Currency.THB);
        quotationEntity.setCustomer(customerEntity);
        // New quotations render immutable customer data from these snapshots.
        applyCustomerSnapshot(quotationEntity, customerEntity, customerAddressEntity, customerContactEntity,
                requestDto.getCustomerBranchCode(), requestDto.getCustomerSnapshot());
        quotationEntity.setSales(saleEntity);
        quotationEntity.setRfqId(StringUtils.trimToNull(requestDto.getRfqId()));
        quotationEntity.setCoSalesId(requestDto.getCoSaleId());
        quotationEntity.setRemark(requestDto.getRemark());
        quotationEntity.setRevNo(1);
        quotationEntity.setShipping(requestDto.getShipping());

        if (StringUtils.isNotBlank(requestDto.getRfqId())) {
            RfqHeaderEntity rfqEntity = requestPriceHeaderRepository.findById(requestDto.getRfqId().trim())
                    .orElseThrow(() -> new DataNotFoundException("RFQ " + requestDto.getRfqId() + " not found."));
            quotationEntity.setRfq(rfqEntity);
            quotationEntity.setReferenceRfqId(StringUtils.trimToNull(rfqEntity.getReferenceRfqId()));
            quotationEntity.setReferenceRfq(rfqEntity.getReferenceRfq());
        }

        QuotationSummary summary = calculate(requestDto);
        quotationEntity.setDiscount(requestDto.getDiscount());
        quotationEntity.setFreight(requestDto.getFreight());
        quotationEntity.setSubTotal(summary.subTotal);
        quotationEntity.setVat(summary.vat);
        quotationEntity.setGrandTotal(summary.grandTotal);
        quotationEntity.setVatRate(requestDto.getIsVat() ? VAT_RATE : BigDecimal.ZERO);

        int lineNo = 1;
        for (QuotationItemRequestDto itemRequest : Optional.ofNullable(requestDto.getItems()).orElseGet(List::of)) {
            QuotationDetailEntity detailEntity = new QuotationDetailEntity();

            detailEntity.setQuotation(quotationEntity);
            detailEntity.setLineNo(lineNo++);
            detailEntity.setName(itemRequest.getName());
            detailEntity.setType(itemRequest.getType());
            detailEntity.setCapacity(itemRequest.getCapacity());
            detailEntity.setSize(itemRequest.getSize());
            detailEntity.setSpec(itemRequest.getSpec());
            detailEntity.setTierId(itemRequest.getTierId());

            BigDecimal unitPrice = defaultIfNull(itemRequest.getUnitPrice());
            BigDecimal quantity = defaultIfNull(itemRequest.getQuantity());

            BigDecimal amount = unitPrice
                    .multiply(quantity)
                    .setScale(2, RoundingMode.HALF_UP);

            detailEntity.setUnitPrice(unitPrice);
            detailEntity.setQuantity(quantity);
            detailEntity.setAmount(amount);
            detailEntity.setImageUrl(itemRequest.getImagePreview());
            quotationEntity.addItem(detailEntity);
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        quotationEntity.setCreatedDate(now);
        quotationEntity.setCreatedBy(actor);
        quotationEntity.setUpdatedDate(now);
        quotationEntity.setUpdatedBy(actor);

        quotationRepository.save(quotationEntity);
        updateRfqQuotationNo(requestDto.getRfqId(), docId, createdBy);
        recordCreateQuotationActivity(quotationEntity, requestDto, createdBy);

        userTodoService.buildUserTodoEntity(
                userProfileService.getUserEntity(createdBy),
                UserTodoType.QUOTATION,
                "ติดตามใบเสนอราคาเลขที่ " + docId + " กับลูกค้า",
                null,
                UserTodoStatus.TODO,
                UserTodoPriority.LOW,
                "QUOTATION",
                docId,
                null,
                now.plusDays(3),
                1,
                createdBy
        );

        return quotationEntity;
    }

    @Transactional(rollbackFor = Exception.class)
    public QuotationDto updateQuotation(String quotationNo, QuotationRequestDto requestDto, String userId) throws DataNotFoundException {
        log.info("Update quotation {} by {}", quotationNo, userId);

        QuotationEntity quotationEntity = quotationRepository.findById(quotationNo)
                .orElseThrow(() -> new DataNotFoundException("Quotation " + quotationNo + " not found."));

        String actor = userProfileService.getNameFromId(userId);
        Integer oldRevNo = quotationEntity.getRevNo();

        if (requestDto.getRemark() != null) {
            quotationEntity.setRemark(requestDto.getRemark());
        }
        if (requestDto.getDiscount() != null) {
            quotationEntity.setDiscount(requestDto.getDiscount());
        }
        if (requestDto.getFreight() != null) {
            quotationEntity.setFreight(requestDto.getFreight());
        }
        if (requestDto.getIsVat() != null) {
            quotationEntity.setVatRate(Boolean.TRUE.equals(requestDto.getIsVat()) ? VAT_RATE : BigDecimal.ZERO);
        }
        if (requestDto.getItems() != null) {
            replaceQuotationItems(quotationEntity, requestDto.getItems());
        }
        if (requestDto.getCustomerSnapshot() != null) {
            applyCustomerSnapshot(quotationEntity, null, null, null, null, requestDto.getCustomerSnapshot());
        }

        List<QuotationItemRequestDto> itemsForCalculate = requestDto.getItems() != null
                ? requestDto.getItems()
                : toItemRequests(quotationEntity.getItems());
        Boolean isVat = requestDto.getIsVat() != null
                ? requestDto.getIsVat()
                : quotationEntity.getVatRate() != null && quotationEntity.getVatRate().compareTo(BigDecimal.ZERO) > 0;

        QuotationSummary summary = calculate(
                itemsForCalculate,
                quotationEntity.getDiscount(),
                quotationEntity.getFreight(),
                isVat
        );
        quotationEntity.setSubTotal(summary.subTotal);
        quotationEntity.setVat(summary.vat);
        quotationEntity.setGrandTotal(summary.grandTotal);
        quotationEntity.setRevNo(defaultRevNo(oldRevNo) + 1);
        quotationEntity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        quotationEntity.setUpdatedBy(actor);

        quotationEntity = quotationRepository.save(quotationEntity);
        recordUpdateQuotationActivity(quotationEntity, requestDto, userId, oldRevNo);

        return mapToDto(quotationEntity);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuotationDto syncCustomerSnapshot(String quotationNo, String userId) throws DataNotFoundException {
        QuotationEntity quotation = quotationRepository.findById(quotationNo)
                .orElseThrow(() -> new DataNotFoundException("Quotation " + quotationNo + " not found."));
        CustomerEntity customer = quotation.getCustomer();
        if (customer == null) throw new DataNotFoundException("Customer for quotation " + quotationNo + " not found.");
        CustomerAddressEntity address = customer.getAddresses().stream()
                .filter(value -> Boolean.TRUE.equals(value.getIsDefault())).findFirst().orElse(null);
        CustomerContactEntity contact = customer.getContacts().stream()
                .filter(value -> Boolean.TRUE.equals(value.getIsDefault())).findFirst().orElse(null);
        applyCustomerSnapshot(quotation, customer, address, contact, null, null);
        quotation.setUpdatedBy(userProfileService.getNameFromId(userId));
        quotation.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        return mapToDto(quotationRepository.save(quotation));
    }

    private void recordCreateQuotationActivity(QuotationEntity quotationEntity, QuotationRequestDto requestDto, String createdBy) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("status", quotationEntity.getStatus());
        detail.put("customerId", quotationEntity.getCustomer() != null ? quotationEntity.getCustomer().getId() : null);
        detail.put("salesId", quotationEntity.getSales() != null ? quotationEntity.getSales().getEmployeeId() : null);
        detail.put("rfqId", requestDto.getRfqId());
        detail.put("referenceRfqId", quotationEntity.getReferenceRfqId());
        detail.put("revNo", quotationEntity.getRevNo());
        detail.put("itemCount", quotationEntity.getItems() != null ? quotationEntity.getItems().size() : 0);
        detail.put("subTotal", quotationEntity.getSubTotal());
        detail.put("vat", quotationEntity.getVat());
        detail.put("grandTotal", quotationEntity.getGrandTotal());

        activityHistoryService.record(
                ActivityEntityType.QUOTATION,
                quotationEntity.getQuotationNo(),
                createdBy,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้างใบเสนอราคาเลขที่ " + quotationEntity.getQuotationNo(),
                detail
        );
    }

    private void recordUpdateQuotationActivity(QuotationEntity quotationEntity, QuotationRequestDto requestDto, String userId, Integer oldRevNo) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("status", quotationEntity.getStatus());
        detail.put("customerId", quotationEntity.getCustomer() != null ? quotationEntity.getCustomer().getId() : null);
        detail.put("salesId", quotationEntity.getSales() != null ? quotationEntity.getSales().getEmployeeId() : null);
        detail.put("oldRevNo", oldRevNo);
        detail.put("newRevNo", quotationEntity.getRevNo());
        detail.put("remarkUpdated", requestDto.getRemark() != null);
        detail.put("itemsUpdated", requestDto.getItems() != null);
        detail.put("itemCount", quotationEntity.getItems() != null ? quotationEntity.getItems().size() : 0);
        detail.put("subTotal", quotationEntity.getSubTotal());
        detail.put("vat", quotationEntity.getVat());
        detail.put("grandTotal", quotationEntity.getGrandTotal());

        activityHistoryService.record(
                ActivityEntityType.QUOTATION,
                quotationEntity.getQuotationNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "แก้ไขใบเสนอราคาเลขที่ " + quotationEntity.getQuotationNo(),
                detail
        );
    }

    private void replaceQuotationItems(QuotationEntity quotationEntity, List<QuotationItemRequestDto> itemRequests) {
        Map<String, QuotationDetailEntity> existingItemsById = new HashMap<>();
        for (QuotationDetailEntity item : quotationEntity.getItems()) {
            if (item.getId() != null) {
                existingItemsById.put(item.getId().toString(), item);
            }
        }

        quotationEntity.getItems().clear();

        int lineNo = 1;
        for (QuotationItemRequestDto itemRequest : itemRequests) {
            QuotationDetailEntity existingItem = StringUtils.isNotBlank(itemRequest.getId())
                    ? existingItemsById.get(itemRequest.getId())
                    : null;

            QuotationDetailEntity detailEntity = new QuotationDetailEntity();
            detailEntity.setQuotation(quotationEntity);
            detailEntity.setLineNo(lineNo++);
            detailEntity.setName(itemRequest.getName());
            detailEntity.setType(itemRequest.getType());
            detailEntity.setCapacity(itemRequest.getCapacity());
            detailEntity.setSize(itemRequest.getSize());
            detailEntity.setSpec(itemRequest.getSpec());

            BigDecimal unitPrice = defaultIfNull(itemRequest.getUnitPrice());
            BigDecimal quantity = defaultIfNull(itemRequest.getQuantity());
            detailEntity.setUnitPrice(unitPrice);
            detailEntity.setQuantity(quantity);
            detailEntity.setAmount(unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP));
            detailEntity.setImageUrl(StringUtils.defaultIfBlank(
                    itemRequest.getImagePreview(),
                    existingItem != null ? existingItem.getImageUrl() : null
            ));

            quotationEntity.getItems().add(detailEntity);
        }
    }

    private List<QuotationItemRequestDto> toItemRequests(Set<QuotationDetailEntity> details) {
        List<QuotationItemRequestDto> itemRequests = new ArrayList<>();
        for (QuotationDetailEntity detail : details) {
            QuotationItemRequestDto item = new QuotationItemRequestDto();
            item.setId(detail.getId() != null ? detail.getId().toString() : null);
            item.setName(detail.getName());
            item.setType(detail.getType());
            item.setCapacity(detail.getCapacity());
            item.setSize(detail.getSize());
            item.setSpec(detail.getSpec());
            item.setUnitPrice(detail.getUnitPrice());
            item.setQuantity(detail.getQuantity());
            item.setAmount(detail.getAmount());
            item.setImagePreview(detail.getImageUrl());
            itemRequests.add(item);
        }
        return itemRequests;
    }

    private int defaultRevNo(Integer revNo) {
        return revNo == null ? 0 : revNo;
    }

    private void updateRfqQuotationNo(String rfqId, String quotationNo, String updatedBy) throws DataNotFoundException {
        if (StringUtils.isBlank(rfqId)) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        RfqHeaderEntity rfq = requestPriceHeaderRepository.findById(rfqId)
                .orElseThrow(() -> new DataNotFoundException("RFQ " + rfqId + " not found."));
        rfq.setQuotationNo(quotationNo);
        rfq.setQuotedDate(now);
        rfq.setUpdatedDate(now);
        requestPriceHeaderRepository.save(rfq);

        activityHistoryService.record(
                ActivityEntityType.RFQ,
                rfq.getId(),
                updatedBy,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้างใบเสนอราคาเลขที่ " + quotationNo,
                null
        );
    }

    @Transactional(readOnly = true)
    public SearchQuotationResponse searchQuotation(SearchQuotationRequest searchQuotationRequest, PageableRequest pageableRequest) {
        log.info("Search quotation by criteria(s) : {}", searchQuotationRequest);

        pageableRequest.setSortBy("docDate");
        pageableRequest.setSortDirection(Sort.Direction.DESC);
        Pageable pageable = pageableRequest.build();

        Page<QuotationEntity> quotationEntityPage = quotationRepository.findAll(buildSearchCriteria(searchQuotationRequest), pageable);
        Page<QuotationDto> quotaionDtoPage = quotationEntityPage.map(this::mapToDto);
        List<QuotationDto> quotationList = quotaionDtoPage.getContent();
        log.info("Search quotation size : {}", quotaionDtoPage.getTotalElements());

        SearchQuotationResponse response = new SearchQuotationResponse();
        response.setQuotationList(quotationList);
        response.setPagination(Pagination.build(quotaionDtoPage));

        return response;
    }

    @Transactional(readOnly = true)
    public QuotationDto getQuotationDetailById(String quotationNo) throws DataNotFoundException {
        log.info("Get quotation detail by {}", quotationNo);

        QuotationEntity quotationEntity = quotationRepository.findById(quotationNo)
                .orElseThrow(() -> new DataNotFoundException("Quotation " + quotationNo + " not found."));

        return mapToDto(quotationEntity);
    }

    @Transactional(readOnly = true)
    public DownloadDocumentDto getQuotationDocumentById(String quotationNo, DocumentRequest documentRequest) throws Exception {
        log.info("Get quotation by {}", quotationNo);

        QuotationEntity quotationEntity = quotationRepository.findById(quotationNo)
                .orElseThrow(() -> new DataNotFoundException("Quotation " + quotationNo + " not found."));

        String fileName = quotationEntity.getQuotationNo();
        byte[] termAndCondPages = (byte[]) reportService.getTermAndConditionDocument(
                buildTermAndConditionDocumentDto(
                        quotationEntity.getVatRate().compareTo(BigDecimal.ZERO) > 0,
                        quotationEntity.getSales(),
                        documentRequest.getLang()
                ),
                documentRequest.getFormat(),
                documentRequest.getLang()
        );
        if (documentRequest.getFormat().equals(ExportFileFormat.PDF)) {
            List<byte[]> pdfBytesList = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                pdfBytesList.add((byte[]) reportService.getQuotationDocument(
                        buildQuotationDocumentDto(quotationEntity, Boolean.FALSE, documentRequest.getLang()),
                        documentRequest.getFormat(),
                        documentRequest.getLang()
                ));
                pdfBytesList.add(termAndCondPages);
            }
            if (documentRequest.getIsCopy()) {
                pdfBytesList.add((byte[]) reportService.getQuotationDocument(
                        buildQuotationDocumentDto(quotationEntity, Boolean.TRUE, documentRequest.getLang()),
                        documentRequest.getFormat(),
                        documentRequest.getLang()
                ));
                pdfBytesList.add(termAndCondPages);
            }

            byte[] mergedPdf = PdfMergeUtil.merge(pdfBytesList);
            return new DownloadDocumentDto(fileName, documentRequest.getFormat(), List.of(new DownloadDocumentDto.FileItem(fileName + "." + documentRequest.getFormat(), Base64.getEncoder().encodeToString(mergedPdf) , "application/pdf")));
        } else if (documentRequest.getFormat().equals(ExportFileFormat.JPG)) {
            List<byte[]> pages = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                List<byte[]> originalPages = (List<byte[]>) reportService.getQuotationDocument(
                        buildQuotationDocumentDto(quotationEntity, Boolean.FALSE, documentRequest.getLang()),
                        documentRequest.getFormat(),
                        documentRequest.getLang()
                );
                pages.addAll(originalPages);
                pages.add(termAndCondPages);
            }
            if (documentRequest.getIsCopy()) {
                List<byte[]> copyPages = (List<byte[]>) reportService.getQuotationDocument(
                        buildQuotationDocumentDto(quotationEntity, Boolean.TRUE, documentRequest.getLang()),
                        documentRequest.getFormat(),
                        documentRequest.getLang()
                );
                pages.addAll(copyPages);
                pages.add(termAndCondPages);
            }
            List<DownloadDocumentDto.FileItem> files = new ArrayList<>();
            for (int i = 0; i< pages.size(); i++) {
                String pageFileName = fileName + "_page_" + 1 + "." + documentRequest.getFormat();
                files.add(new DownloadDocumentDto.FileItem(pageFileName, Base64.getEncoder().encodeToString(pages.get(i)), "image/jpeg"));
            }
            return new DownloadDocumentDto(fileName, documentRequest.getFormat(), files);
        }

        return null;
    }

    @Transactional
    public UploadFileResponse generateQuotationPdfUrl(String quotationNo, DocumentRequest documentRequest) throws Exception {
        if (documentRequest == null) {
            documentRequest = new DocumentRequest(ExportFileFormat.PDF, Boolean.TRUE, Boolean.FALSE);
        }
        if (!Boolean.TRUE.equals(documentRequest.getIsOriginal()) && !Boolean.TRUE.equals(documentRequest.getIsCopy())) {
            throw new InvalidRequestException("At least one of isOriginal or isCopy must be true.");
        }
        return resolveQuotationPdfFile(quotationNo);
    }

    @Transactional
    public void getAndDownloadQuotation(String userId, String quotationNo) throws Exception {
        log.info("Download quotation for line {}", quotationNo);

        Map<String, String> variables = new LinkedHashMap<>();
        try {
            lineMessageService.sendTextMessage(userId, "ระบบกำลังดาวน์โหลดใบเสนอราคา");
            UploadFileResponse uploadFileResponse = resolveQuotationPdfFile(quotationNo);

            variables.put("quotationNo", quotationNo);
            variables.put("link", uploadFileResponse.getUrl());

            String msg = promptTemplateEngine.render(templateProperties.getTexts().get(DOWNLOAD_QUOTATION_TH), variables);

            log.info("Send message {} to user {}", msg, userId);

            lineMessageService.sendTextMessage(userId, msg);
        } catch (DataNotFoundException ex) {
            variables.put("quotationNo", quotationNo);

            String notFoundMsg = promptTemplateEngine.render(templateProperties.getTexts().get(QUOTATION_NOT_FOUND_TH), variables);
            lineMessageService.sendTextMessage(userId, notFoundMsg);
        }
    }

    private UploadFileResponse resolveQuotationPdfFile(String quotationNo) throws Exception {
        String fileName = quotationNo + ".pdf";
        if (fileStorageService.fileExists(fileName)) {
            log.info("{} is exist", fileName);
            return new UploadFileResponse(fileName, fileStorageService.getPublicFileUrl(fileName), "application/pdf");
        }

        log.info("Generate quotation {}", quotationNo);
        DocumentRequest documentRequest = new DocumentRequest(ExportFileFormat.PDF, true, false);
        DownloadDocumentDto documentDto = getQuotationDocumentById(quotationNo, documentRequest);
        DownloadDocumentDto.FileItem pdfFile = documentDto.getFiles().getFirst();
        byte[] content = Base64.getDecoder().decode(pdfFile.getBase64());
        return fileStorageService.uploadGeneratedFile(content, quotationNo, pdfFile.getContentType());
    }

    private TermAndConditionDocumentDto buildTermAndConditionDocumentDto(
            Boolean isVat,
            EmployeeEntity sales,
            TemplateLanguage language
    ) {
        TermAndConditionDocumentDto dto = new TermAndConditionDocumentDto();
        dto.setSalesName(sales.getFirstNameTh() + " " + sales.getLastNameTh());
        if (!isVat) {
            List<SystemConfigDto> noVatConfig = systemConfigService.getSystemConfigByGroupCode(SystemConstant.REPORT_NO_VAT);
            dto.setBankName(systemConfigService.getConfig(noVatConfig, "BANK_NAME", language));
            dto.setAccountName(systemConfigService.getConfig(noVatConfig, "ACCOUNT_NAME", language));
            dto.setAccountNo(systemConfigService.getConfig(noVatConfig, "ACCOUNT_NO", language));
            dto.setBranchName(systemConfigService.getConfig(noVatConfig, "BRANCH_NAME", language));
        } else {
            List<SystemConfigDto> vatConfig = systemConfigService.getSystemConfigByGroupCode(SystemConstant.REPORT_VAT);
            dto.setBankName(systemConfigService.getConfig(vatConfig, "BANK_NAME", language));
            dto.setAccountName(systemConfigService.getConfig(vatConfig, "ACCOUNT_NAME", language));
            dto.setAccountNo(systemConfigService.getConfig(vatConfig, "ACCOUNT_NO", language));
            dto.setBranchName(systemConfigService.getConfig(vatConfig, "BRANCH_NAME", language));
        }
        return dto;
    }

    private QuotationDocumentDto  buildQuotationDocumentDto(
            QuotationEntity quotationEntity,
            Boolean aFalse,
            TemplateLanguage language
    ) {
        QuotationDocumentDto dto = new QuotationDocumentDto();
        dto.setDocNo(quotationEntity.getQuotationNo());
        dto.setDocDate(quotationEntity.getDocDate().format(DateUtil.DD_MM_YY));
        dto.setIsCopy(aFalse);

        if (quotationEntity.getIsShowSummary()) {
            dto.setDiscount(quotationEntity.getDiscount());
            dto.setGrandTotal(quotationEntity.getGrandTotal());
            dto.setFreight(quotationEntity.getFreight());
            dto.setSubTotal(quotationEntity.getSubTotal());
            dto.setVat(quotationEntity.getVat());
            dto.setThaiBahtText(ThaiBahtText.convertBahtText(quotationEntity.getGrandTotal()));
        } else {
            dto.setDiscount(null);
            dto.setGrandTotal(null);
            dto.setFreight(null);
            dto.setSubTotal(null);
            dto.setVat(null);
            dto.setThaiBahtText(null);
        }
        dto.setRemark(quotationEntity.getRemark());
        dto.setThaiBahtText(ThaiBahtText.convertBahtText(quotationEntity.getGrandTotal()));
        QuotationCustomerSnapshotDto customerSnapshot = toCustomerSnapshot(quotationEntity);
        dto.setCustName(StringUtils.defaultString(customerSnapshot.getCustomerName()));
        dto.setCustTaxId(customerSnapshot.getTaxId());
        dto.setCustAddress(customerSnapshot.getAddress());
        dto.setCustMobileNo(customerSnapshot.getContactNumber());
        dto.setSalesId(quotationEntity.getSales().getEmployeeId());
        dto.setSalesName(quotationEntity.getSales().getFirstNameTh() + " " + quotationEntity.getSales().getLastNameTh());
        dto.setSalesMobileNo(quotationEntity.getSales().getPhoneNumber());
        dto.setSalesNickname(quotationEntity.getSales().getNickName());
        dto.setShipping(quotationEntity.getShipping());
        dto.setCoSalesId(quotationEntity.getCoSalesId());

        if (quotationEntity.getVatRate().compareTo(BigDecimal.ZERO) == 0) {
            List<SystemConfigDto> noVatConfig = systemConfigService.getSystemConfigByGroupCode(SystemConstant.REPORT_NO_VAT);
            dto.setBankName(systemConfigService.getConfig(noVatConfig, "BANK_NAME", language));
            dto.setAccountName(systemConfigService.getConfig(noVatConfig, "ACCOUNT_NAME", language));
            dto.setAccountNo(systemConfigService.getConfig(noVatConfig, "ACCOUNT_NO", language));
            dto.setBranchName(systemConfigService.getConfig(noVatConfig, "BRANCH_NAME", language));
        } else {
            List<SystemConfigDto> vatConfig = systemConfigService.getSystemConfigByGroupCode(SystemConstant.REPORT_VAT);
            dto.setBankName(systemConfigService.getConfig(vatConfig, "BANK_NAME", language));
            dto.setAccountName(systemConfigService.getConfig(vatConfig, "ACCOUNT_NAME", language));
            dto.setAccountNo(systemConfigService.getConfig(vatConfig, "ACCOUNT_NO", language));
            dto.setBranchName(systemConfigService.getConfig(vatConfig, "BRANCH_NAME", language));
        }

        List<QuotationItemDocumentDto> itemDocuments = getItemDocumentDtos(quotationEntity);
        dto.setItems(itemDocuments);

        return dto;
    }

    private String buildFullAddress(CustomerAddressEntity address) {
        if (address == null) {
            return null;
        }

        boolean isBangkok = "กรุงเทพมหานคร".equals(address.getProvince());

        String subdistrictPrefix = isBangkok ? "แขวง" : "ตำบล";
        String districtPrefix = isBangkok ? "เขต" : "อำเภอ";

        StringBuilder sb = new StringBuilder();

        append(sb, address.getAddressLine1());
        append(sb, address.getAddressLine2());

        if (address.getSubdistrict() != null) {
            append(sb, subdistrictPrefix + address.getSubdistrict());
        }

        if (address.getDistrict() != null) {
            append(sb, districtPrefix + address.getDistrict());
        }

        if (address.getProvince() != null) {
            if (isBangkok) {
                append(sb, address.getProvince());
            } else {
                append(sb, "จังหวัด" + address.getProvince());
            }
        }
        if (StringUtils.isNotEmpty(address.getPostcode())) {
            append(sb, address.getPostcode());
        }

        return sb.toString().trim();
    }

    private void applyCustomerSnapshot(QuotationEntity quotation, CustomerEntity customer,
                                       CustomerAddressEntity address, CustomerContactEntity contact,
                                       String requestedBranchCode, QuotationCustomerSnapshotDto supplied) {
        if (supplied != null && StringUtils.isNotBlank(supplied.getCustomerName())) {
            quotation.setCustomerNameSnapshot(StringUtils.trimToNull(supplied.getCustomerName()));
            quotation.setCustomerTaxIdSnapshot(StringUtils.trimToNull(supplied.getTaxId()));
            quotation.setCustomerBranchCodeSnapshot(StringUtils.trimToNull(supplied.getBranchCode()));
            quotation.setCustomerBranchNameSnapshot(StringUtils.trimToNull(supplied.getBranchName()));
            quotation.setCustomerAddressSnapshot(StringUtils.trimToNull(supplied.getAddress()));
            quotation.setCustomerContactSnapshot(StringUtils.trimToNull(supplied.getContactName()));
            quotation.setCustomerPhoneSnapshot(StringUtils.trimToNull(supplied.getContactNumber()));
            return;
        }
        CustomerBranchEntity branch = customer.getBranches().stream()
                .filter(value -> StringUtils.equals(value.getBranchCode(), requestedBranchCode))
                .findFirst()
                .orElseGet(() -> customer.getBranches().stream().filter(value -> Boolean.TRUE.equals(value.getIsDefault()))
                        .findFirst().orElse(null));
        quotation.setCustomerNameSnapshot(customer.getCustomerName());
        quotation.setCustomerTaxIdSnapshot(customer.getTaxId());
        quotation.setCustomerBranchCodeSnapshot(branch == null ? customer.getBranchNumber() : branch.getBranchCode());
        quotation.setCustomerBranchNameSnapshot(branch == null ? customer.getBranchName() : branch.getBranchName());
        quotation.setCustomerAddressSnapshot(buildFullAddress(address));
        quotation.setCustomerContactSnapshot(contact == null ? null : contact.getContactName());
        quotation.setCustomerPhoneSnapshot(contact == null ? null : contact.getContactNumber());
    }

    private QuotationCustomerSnapshotDto toCustomerSnapshot(QuotationEntity quotation) {
        QuotationCustomerSnapshotDto snapshot = new QuotationCustomerSnapshotDto();
        if (quotation.getCustomerNameSnapshot() != null) {
            snapshot.setCustomerName(quotation.getCustomerNameSnapshot());
            snapshot.setTaxId(quotation.getCustomerTaxIdSnapshot());
            snapshot.setBranchCode(quotation.getCustomerBranchCodeSnapshot());
            snapshot.setBranchName(quotation.getCustomerBranchNameSnapshot());
            snapshot.setAddress(quotation.getCustomerAddressSnapshot());
            snapshot.setContactName(quotation.getCustomerContactSnapshot());
            snapshot.setContactNumber(quotation.getCustomerPhoneSnapshot());
            return snapshot;
        }
        CustomerEntity customer = quotation.getCustomer();
        snapshot.setCustomerName(customer == null ? null : customer.getCustomerName());
        snapshot.setTaxId(customer == null ? null : customer.getTaxId());
        snapshot.setBranchCode(customer == null ? null : customer.getBranchNumber());
        snapshot.setBranchName(customer == null ? null : customer.getBranchName());
        snapshot.setAddress(buildFullAddress(quotation.getCustomerAddress()));
        snapshot.setContactName(quotation.getCustomerContact() == null ? null : quotation.getCustomerContact().getContactName());
        snapshot.setContactNumber(quotation.getCustomerContact() == null ? null : quotation.getCustomerContact().getContactNumber());
        return snapshot;
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(value.trim());
        }
    }

    @NotNull
    private List<QuotationItemDocumentDto> getItemDocumentDtos(QuotationEntity quotationEntity) {
        List<QuotationItemDocumentDto> itemDocuments = new ArrayList<>();
        for (QuotationDetailEntity detail : quotationEntity.getItems()) {
            QuotationItemDocumentDto item = new QuotationItemDocumentDto();

            if (StringUtils.isNotEmpty(detail.getImageUrl())) {
                item.setImage(loadImageAsInputStream(detail.getImageUrl(), fileStorageService));
            }
            item.setNo(detail.getLineNo());
            item.setName(detail.getName());
            item.setType(detail.getType());
            item.setCapacity(detail.getCapacity());
            item.setSize(detail.getSize());
            item.setSpec(detail.getSpec());
            item.setPrice(detail.getUnitPrice());
            item.setQuantity(detail.getQuantity());
            item.setAmount(detail.getAmount());

            itemDocuments.add(item);
        }

        SystemConfigEntity rowCount = systemConfigService.getConfigEntity(REPORT_ROW, "QT");
        int row = rowCount != null ? Integer.valueOf(rowCount.getNameTh()) : 4;
        while (itemDocuments.size() < row) {
            itemDocuments.add(new QuotationItemDocumentDto());
        }

        return itemDocuments;
    }

    private static InputStream loadImageAsInputStream(String imageUrl, FileStorageService fileStorageService) {
        try {
            String fileName = RfqAttachmentUtil.extractFileName(imageUrl);
            if (StringUtils.isBlank(fileName)) {
                return null;
            }

            InputStream localFile = fileStorageService.openUploadedFile(fileName);
            if (localFile != null) {
                return localFile;
            }

            return new URL(imageUrl).openStream();
        } catch (Exception e) {
            log.warn("Cannot load image from url: {}", imageUrl, e);
            return null;
        }
    }

    private QuotationSummary calculate(QuotationRequestDto request) {
        return calculate(request.getItems(), request.getDiscount(), request.getFreight(), request.getIsVat());
    }

    private QuotationSummary calculate(
            List<QuotationItemRequestDto> items,
            BigDecimal requestDiscount,
            BigDecimal requestFreight,
            Boolean isVat
    ) {
        BigDecimal subTotal = BigDecimal.ZERO;

        for (QuotationItemRequestDto item : Optional.ofNullable(items).orElseGet(List::of)) {
            BigDecimal lineTotal = defaultIfNull(item.getUnitPrice())
                    .multiply(defaultIfNull(item.getQuantity()));
            subTotal = subTotal.add(lineTotal);
        }

        BigDecimal discount = defaultIfNull(requestDiscount);
        BigDecimal freight = defaultIfNull(requestFreight);

        BigDecimal taxableAmount = subTotal.subtract(discount);

        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        BigDecimal vat = BigDecimal.ZERO;

        if (Boolean.TRUE.equals(isVat)) {
            vat = taxableAmount
                    .multiply(VAT_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal grandTotal = taxableAmount
                .add(vat)
                .add(freight)
                .setScale(2, RoundingMode.HALF_UP);

        return new QuotationSummary(
                subTotal.setScale(2, RoundingMode.HALF_UP),
                vat,
                grandTotal
        );
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private CustomerEntity resolveCustomer(String input) throws DataNotFoundException {

        // 1️⃣ search by ID
        Optional<CustomerEntity> byId = customerRepository.findById(input);
        if (byId.isPresent()) {
            return byId.get();
        }

        // 2️⃣ search by companyName
        Optional<CustomerEntity> byCompany =
                customerRepository.findFirstByCompanyNameContainingIgnoreCase(input);

        if (byCompany.isPresent()) {
            return byCompany.get();
        }

        // 3️⃣ search by customerName
        Optional<CustomerEntity> byCustomer =
                customerRepository.findFirstByCustomerNameContainingIgnoreCase(input);

        if (byCustomer.isPresent()) {
            return byCustomer.get();
        }

        throw new DataNotFoundException("Customer " + input + " not found.");
    }

    private EmployeeEntity resolveSales(String input) throws DataNotFoundException {

        if (input == null || input.isBlank()) {
            throw new DataNotFoundException("Sales id is empty");
        }

        // 1️⃣ search by salesId
        Optional<EmployeeEntity> byId =
                employeeRepository.findById(input);

        if (byId.isPresent()) {
            return byId.get();
        }

        throw new DataNotFoundException("Internal sale " + input + " not found.");
    }

    private Specification<QuotationEntity> buildSearchCriteria(SearchQuotationRequest searchQuotationRequest) {
        Specification<QuotationEntity> specification = Specification.where(null);
        return specification
                .and(docNoEqual(searchQuotationRequest.getDocNoEqual()))
                .and(customerIdEqual(searchQuotationRequest.getCustomerIdEqual()))
                .and(salesIdEqual(searchQuotationRequest.getSalesId()))
                .and(statusEqual(searchQuotationRequest.getStatusEqual()))
                .and(docDateBetween(searchQuotationRequest.getDocDateStart(), searchQuotationRequest.getDocDateEnd()))
                .and(keywordContains(searchQuotationRequest.getKeyword()));
    }

    private QuotationDto mapToDto(QuotationEntity entity) {
        CustomerDto customerDto = customerMapper.toDto(entity.getCustomer());
        CustomerAddressDto customerAddressDto = customerMapper.toAddressDto(entity.getCustomerAddress());
        CustomerContactDto customerContactDto = customerMapper.toContactDto(entity.getCustomerContact());
        EmployeeDto salesDto = employeeMapper.toDto(entity.getSales());

        QuotationDto dto = new QuotationDto();
        dto.setRfqId(entity.getRfqId());
        dto.setReferenceRfqId(entity.getReferenceRfqId());
        dto.setReferenceRfq(requestPriceHeaderMapper.toReferenceDto(entity.getReferenceRfq()));
        dto.setDocDate(entity.getDocDate().format(DateUtil.DD_MM_YY));
        dto.setEffectiveDate(entity.getExpireDate().format(DateUtil.DD_MM_YY));
        dto.setCustomer(customerDto);
        dto.setCustomerAddress(customerAddressDto);
        dto.setCustomerContact(customerContactDto);
        dto.setCustomerSnapshot(toCustomerSnapshot(entity));
        dto.setSaleAccount(salesDto);
        dto.setCoSaleId(entity.getCoSalesId());
        dto.setQuotationNo(entity.getQuotationNo());
        dto.setStatus(entity.getStatus());
        dto.setStatusProfile(DocumentStatusResolver.resolveQuotation(entity.getStatus()));
        dto.setRemark(entity.getRemark());
        dto.setDiscount(entity.getDiscount());
        dto.setFreight(entity.getFreight());
        dto.setSubTotal(entity.getSubTotal());
        dto.setVat(entity.getVat());
        dto.setVatRate(entity.getVatRate());
        dto.setIsShowSummary(entity.getIsShowSummary());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setRevNo(entity.getRevNo());
        List<QuotationItemRequestDto> items = new ArrayList<>();
        for (QuotationDetailEntity detail : entity.getItems()) {
            QuotationItemRequestDto item = new QuotationItemRequestDto();
            item.setId(detail.getId() != null ? detail.getId().toString() : null);
            item.setTierId(detail.getTierId());
            item.setName(detail.getName());
            item.setImagePreview(detail.getImageUrl());
            item.setSpec(detail.getSpec());
            item.setSize(detail.getSize());
            item.setType(detail.getType());
            item.setCapacity(detail.getCapacity());
            item.setUnitPrice(detail.getUnitPrice());
            item.setQuantity(detail.getQuantity());
            item.setAmount(detail.getAmount());
            items.add(item);
        }
        dto.setItems(items);
        return dto;
    }

}
