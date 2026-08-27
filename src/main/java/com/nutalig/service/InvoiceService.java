package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.config.LineConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.invoice.request.CreateInvoiceRequest;
import com.nutalig.controller.invoice.request.SearchInvoiceRequest;
import com.nutalig.controller.invoice.request.UpdateInvoiceRequest;
import com.nutalig.controller.invoice.response.InvoiceAwaitingValidationResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.InvoiceDetailDto;
import com.nutalig.dto.InvoiceDto;
import com.nutalig.dto.InvoicePaymentDto;
import com.nutalig.dto.InvoicePaymentSlipFileDto;
import com.nutalig.dto.SystemConfigDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.dto.document.InvoiceDocumentDto;
import com.nutalig.dto.document.InvoiceItemDocumentDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.CustomerMapper;
import com.nutalig.mapper.EmployeeMapper;
import com.nutalig.mapper.UserMapper;
import com.nutalig.repository.InvoiceRepository;
import com.nutalig.repository.RequestPriceHeaderRepository;
import com.nutalig.repository.SalesOrderRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.security.JwtUtil;
import com.nutalig.utils.DateUtil;
import com.nutalig.utils.DocumentStatusResolver;
import com.nutalig.utils.PdfMergeUtil;
import com.nutalig.utils.ThaiBahtText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static com.nutalig.constant.BusinessConstant.DocumentPrefix.INVOICE_PREFIX;
import static com.nutalig.constant.SystemConstant.REPORT_ROW;
import static com.nutalig.repository.specification.InvoiceSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private static final String CLAIM_ACTION = "action";
    private static final String CLAIM_PAYMENT_ID = "paymentId";
    private static final String ACTION_AWAITING_VALIDATION_VIEW = "awaiting-validation-view";
    private static final String AWAITING_VALIDATION_SUBJECT_SEPARATOR = "|";
    private static final long AWAITING_VALIDATION_TOKEN_EXPIRATION_SECONDS = 24 * 60 * 60;
    private static final String PUBLIC_TOKEN_ACTOR = "PUBLIC_TOKEN";
    private static final String ACCOUNTING_ADMIN_POSITION_CODE = "ACCOUNTING_ADMIN";

    private final GeneratedIdSequenceService generatedIdSequenceService;
    private final ActivityHistoryService activityHistoryService;
    private final SystemConfigService systemConfigService;
    private final ReportService reportService;
    private final FileStorageService fileStorageService;
    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderService salesOrderService;
    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;
    private final EmployeeMapper employeeMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final LineMessageService lineMessageService;
    private final LineConfiguration lineConfiguration;

    @Transactional(rollbackFor = Exception.class)
    public InvoiceEntity createInvoice(CreateInvoiceRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        validateCreateRequest(request);
        SalesOrderEntity salesOrder = salesOrderRepository.findById(request.getSalesOrderNo())
                .orElseThrow(() -> new DataNotFoundException("Sales order " + request.getSalesOrderNo() + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        LocalDate docDate = request.getDocDate() != null ? request.getDocDate() : LocalDate.now(DateUtil.getTimeZone());
        LocalDate dueDate = request.getDueDate() != null
                ? request.getDueDate()
                : (salesOrder.getExpireDate() != null ? salesOrder.getExpireDate() : docDate.plusDays(7));
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        InvoiceEntity entity = new InvoiceEntity();
        entity.setInvoiceNo(generateInvoiceNo());
        entity.setSalesOrder(salesOrder);
        entity.setQuotationNo(resolveQuotationNo(salesOrder.getSalesOrderNo()));
        entity.setDocDate(docDate);
        entity.setDueDate(dueDate);
        entity.setDeliveryDate(request.getDeliveryDate());
        entity.setStatus(InvoiceStatus.ISSUED);
        entity.setCurrency(salesOrder.getCurrency());
        entity.setCustomer(salesOrder.getCustomer());
        entity.setCustomerAddress(salesOrder.getCustomerAddress());
        entity.setCustomerContact(salesOrder.getCustomerContact());
        entity.setSales(salesOrder.getSales());
        entity.setCoSalesId(salesOrder.getCoSalesId());
        entity.setSubTotal(defaultIfNull(salesOrder.getSubTotal()));
        entity.setDiscount(defaultIfNull(salesOrder.getDiscount()));
        entity.setFreight(defaultIfNull(salesOrder.getFreight()));
        entity.setAmount(defaultIfNull(salesOrder.getAmount()));
        entity.setCommission(defaultIfNull(salesOrder.getCommission()));
        entity.setVatRate(defaultIfNull(salesOrder.getVatRate()));
        entity.setVat(defaultIfNull(salesOrder.getVat()));
        entity.setGrandTotal(defaultIfNull(salesOrder.getGrandTotal()));
        entity.setPaidTotal(BigDecimal.ZERO);
        entity.setOutstandingTotal(defaultIfNull(salesOrder.getGrandTotal()));
        entity.setRemark(StringUtils.defaultIfBlank(request.getRemark(), salesOrder.getRemark()));
        entity.setRevNo(1);
        entity.setCustomerNameSnapshot(salesOrder.getCustomer() != null ? salesOrder.getCustomer().getCustomerName() : null);
        entity.setCustomerTaxIdSnapshot(salesOrder.getCustomer() != null ? salesOrder.getCustomer().getTaxId() : null);
        entity.setCustomerAddressSnapshot(buildCustomerAddressSnapshot(salesOrder.getCustomerAddress()));
        entity.setCustomerContactSnapshot(salesOrder.getCustomerContact() != null ? salesOrder.getCustomerContact().getContactName() : null);
        entity.setCustomerPhoneSnapshot(salesOrder.getCustomerContact() != null ? salesOrder.getCustomerContact().getContactNumber() : null);
        entity.setSalesNameSnapshot(buildSalesNameSnapshot(salesOrder.getSales()));
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
        entity.setCreatedDate(now);
        entity.setUpdatedDate(now);
        applySummaryOverride(entity, request);

        int lineNo = 1;
        for (SalesOrderDetailEntity salesOrderItem : salesOrder.getItems()) {
            InvoiceDetailEntity detail = new InvoiceDetailEntity();
            detail.setInvoice(entity);
            detail.setSalesOrderDetail(salesOrderItem);
            detail.setLineNo(lineNo++);
            detail.setName(salesOrderItem.getName());
            detail.setType(salesOrderItem.getType());
            detail.setCapacity(salesOrderItem.getCapacity());
            detail.setSize(salesOrderItem.getSize());
            detail.setSpec(salesOrderItem.getSpec());
            detail.setUnitPrice(defaultIfNull(salesOrderItem.getUnitPrice()));
            detail.setQuantity(defaultIfNull(salesOrderItem.getQuantity()));
            detail.setAmount(defaultIfNull(salesOrderItem.getAmount()));
            detail.setImageUrl(salesOrderItem.getImageUrl());
            entity.addItem(detail);
        }

        invoiceRepository.save(entity);
        salesOrderService.recalculatePaymentSummary(salesOrder.getSalesOrderNo());
        recordCreateInvoiceActivity(entity, userId);
        return entity;
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(String invoiceNo) throws DataNotFoundException {
        InvoiceEntity entity = invoiceRepository.findById(invoiceNo)
                .orElseThrow(() -> new DataNotFoundException("Invoice " + invoiceNo + " not found."));
        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoiceDto updateInvoice(String invoiceNo, UpdateInvoiceRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required");
        }

        InvoiceEntity invoice = invoiceRepository.findById(invoiceNo)
                .orElseThrow(() -> new DataNotFoundException("Invoice " + invoiceNo + " not found."));
        if (!EnumSet.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID).contains(invoice.getStatus())) {
            throw new InvalidRequestException("Invoice status must be ISSUED or PARTIALLY_PAID.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        LocalDate beforeDeliveryDate = invoice.getDeliveryDate();
        LocalDate nextDeliveryDate = request.getDeliveryDate();
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        invoice.setDeliveryDate(nextDeliveryDate);
        invoice.setRevNo(defaultRevNo(invoice.getRevNo()) + 1);
        invoice.setUpdatedBy(user);
        invoice.setUpdatedDate(now);

        InvoiceEntity saved = invoiceRepository.save(invoice);

        Map<String, Object> detail = new LinkedHashMap<>();
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("deliveryDate", beforeDeliveryDate);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("deliveryDate", nextDeliveryDate);
        detail.put("before", before);
        detail.put("after", after);

        activityHistoryService.record(
                ActivityEntityType.INVOICE,
                saved.getInvoiceNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "อัปเดตวันที่ส่งสินค้าของ Invoice เลขที่ " + saved.getInvoiceNo(),
                detail
        );

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceAwaitingValidationResponse resolveAwaitingValidationToken(String token) throws DataNotFoundException, InvalidRequestException {
        AwaitingValidationTokenClaims claims = parseAwaitingValidationToken(token);
        InvoiceEntity entity = invoiceRepository.findById(claims.invoiceNo())
                .orElseThrow(() -> new DataNotFoundException("Invoice " + claims.invoiceNo() + " not found."));
        resolvePayment(entity, claims.paymentId());
        return InvoiceAwaitingValidationResponse.builder()
                .invoice(mapToDto(entity))
                .paymentId(claims.paymentId())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoiceAwaitingValidationResponse approveAwaitingValidationByToken(String token)
            throws DataNotFoundException, InvalidRequestException {
        AwaitingValidationTokenClaims claims = parseAwaitingValidationToken(token);
        InvoiceEntity invoice = invoiceRepository.findById(claims.invoiceNo())
                .orElseThrow(() -> new DataNotFoundException("Invoice " + claims.invoiceNo() + " not found."));
        InvoicePaymentEntity payment = resolvePayment(invoice, claims.paymentId());

        if (payment.getStatus() != InvoicePaymentStatus.PENDING) {
            throw new InvalidRequestException("Invoice payment is already processed.");
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        payment.setStatus(InvoicePaymentStatus.APPROVE);
        payment.setUpdatedBy(null);
        payment.setUpdatedDate(now);
        invoice.setStatus(resolveValidatedInvoiceStatus(invoice));
        invoice.setUpdatedBy(null);
        invoice.setUpdatedDate(now);

        markSalesOrderReadyForProcurementIfDepositPaid(invoice, PUBLIC_TOKEN_ACTOR);
        InvoiceEntity saved = invoiceRepository.save(invoice);
        salesOrderService.recalculatePaymentSummary(saved.getSalesOrder().getSalesOrderNo());
        recordAwaitingValidationDecisionActivity(
                saved,
                payment,
                ActivityAction.APPROVE,
                "อนุมัติการรับชำระเงินผ่านลิงก์สาธารณะ"
        );
        return InvoiceAwaitingValidationResponse.builder()
                .invoice(mapToDto(saved))
                .paymentId(payment.getId())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoiceAwaitingValidationResponse rejectAwaitingValidationByToken(String token)
            throws DataNotFoundException, InvalidRequestException {
        AwaitingValidationTokenClaims claims = parseAwaitingValidationToken(token);
        InvoiceEntity invoice = invoiceRepository.findById(claims.invoiceNo())
                .orElseThrow(() -> new DataNotFoundException("Invoice " + claims.invoiceNo() + " not found."));
        InvoicePaymentEntity payment = resolvePayment(invoice, claims.paymentId());

        if (payment.getStatus() != InvoicePaymentStatus.PENDING) {
            throw new InvalidRequestException("Invoice payment is already processed.");
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        BigDecimal nextPaidTotal = defaultIfNull(invoice.getPaidTotal()).subtract(defaultIfNull(payment.getAmount()));
        if (nextPaidTotal.compareTo(BigDecimal.ZERO) < 0) {
            nextPaidTotal = BigDecimal.ZERO;
        }

        BigDecimal nextOutstandingTotal = defaultIfNull(invoice.getGrandTotal()).subtract(nextPaidTotal);
        if (nextOutstandingTotal.compareTo(BigDecimal.ZERO) < 0) {
            nextOutstandingTotal = BigDecimal.ZERO;
        }

        payment.setStatus(InvoicePaymentStatus.REJECT);
        payment.setUpdatedBy(null);
        payment.setUpdatedDate(now);
        invoice.setPaidTotal(nextPaidTotal);
        invoice.setOutstandingTotal(nextOutstandingTotal);
        invoice.setStatus(resolveValidatedInvoiceStatus(invoice));
        invoice.setUpdatedBy(null);
        invoice.setUpdatedDate(now);

        InvoiceEntity saved = invoiceRepository.save(invoice);
        salesOrderService.recalculatePaymentSummary(saved.getSalesOrder().getSalesOrderNo());
        recordAwaitingValidationDecisionActivity(
                saved,
                payment,
                ActivityAction.REJECT,
                "ปฏิเสธการรับชำระเงินผ่านลิงก์สาธารณะ"
        );
        return InvoiceAwaitingValidationResponse.builder()
                .invoice(mapToDto(saved))
                .paymentId(payment.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoicesBySalesOrderId(String salesOrderId) {
        return invoiceRepository.findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(salesOrderId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Pageable<InvoiceDto> searchInvoices(SearchInvoiceRequest request, PageableRequest pageableRequest) {
        SearchInvoiceRequest criteria = Optional.ofNullable(request).orElseGet(SearchInvoiceRequest::new);
        if (pageableRequest.getSortBy() == null || pageableRequest.getSortDirection() == null) {
            pageableRequest.setSortBy("docDate");
            pageableRequest.setSortDirection(Sort.Direction.DESC);
        }

        Page<InvoiceDto> page = invoiceRepository
                .findAll(buildSearchCriteria(criteria), pageableRequest.build())
                .map(this::mapToDto);

        Pageable<InvoiceDto> response = new Pageable<>();
        response.setRecords(page.getContent());
        response.setPagination(Pagination.build(page));
        return response;
    }

    private Specification<InvoiceEntity> buildSearchCriteria(SearchInvoiceRequest request) {
        return Specification.<InvoiceEntity>where(null)
                .and(invoiceNoEqual(request.getInvoiceNo()))
                .and(customerIdEqual(request.getCustomerId()))
                .and(salesIdEqual(request.getSalesId()))
                .and(statusEqual(request.getStatus()))
                .and(statusIn(request.getStatuses()))
                .and(docDateBetween(request.getDocDateStart(), request.getDocDateEnd()))
                .and(keywordContains(request.getKeyword()));
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoiceDto receivePayment(
            String invoiceNo,
            ZonedDateTime paymentDate,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String chequeBank,
            String chequeNo,
            LocalDate chequeDate,
            String chequeBranch,
            MultipartFile[] slipFiles,
            MultipartFile slipFile,
            String userId
    ) throws Exception {
        if (paymentDate == null) {
            throw new InvalidRequestException("paymentDate is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("amount must be greater than zero");
        }
        if (paymentMethod == null) {
            throw new InvalidRequestException("paymentMethod is required");
        }
        List<MultipartFile> uploadFiles = resolveSlipFiles(slipFiles, slipFile);
        if (paymentMethod == PaymentMethod.TRANSFER && uploadFiles.isEmpty()) {
            throw new InvalidRequestException("slipFiles is required for transfer payment");
        }
        if (paymentMethod == PaymentMethod.CHEQUE) {
            if (StringUtils.isBlank(chequeBank)) {
                throw new InvalidRequestException("chequeBank is required for cheque payment");
            }
            if (StringUtils.isBlank(chequeNo)) {
                throw new InvalidRequestException("chequeNo is required for cheque payment");
            }
            if (chequeDate == null) {
                throw new InvalidRequestException("chequeDate is required for cheque payment");
            }
        }

        InvoiceEntity invoice = invoiceRepository.findById(invoiceNo)
                .orElseThrow(() -> new DataNotFoundException("Invoice " + invoiceNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        BigDecimal currentOutstanding = defaultIfNull(invoice.getOutstandingTotal());
        if (currentOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Invoice has no outstanding balance");
        }
        if (amount.compareTo(currentOutstanding) > 0) {
            throw new InvalidRequestException("amount cannot exceed outstanding total");
        }

        BigDecimal beforePaidTotal = defaultIfNull(invoice.getPaidTotal());
        BigDecimal beforeOutstandingTotal = currentOutstanding;
        InvoiceStatus beforeStatus = invoice.getStatus();

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        InvoicePaymentEntity payment = new InvoicePaymentEntity();
        payment.setPaymentDate(paymentDate);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setChequeBank(StringUtils.trimToNull(chequeBank));
        payment.setChequeNo(StringUtils.trimToNull(chequeNo));
        payment.setChequeDate(chequeDate);
        payment.setChequeBranch(StringUtils.trimToNull(chequeBranch));
        payment.setStatus(InvoicePaymentStatus.PENDING);
        payment.setCreatedBy(user);
        payment.setUpdatedBy(user);
        payment.setCreatedDate(now);
        payment.setUpdatedDate(now);

        List<InvoicePaymentAttachmentEntity> attachments = new ArrayList<>();
        for (int index = 0; index < uploadFiles.size(); index++) {
            MultipartFile file = uploadFiles.get(index);
            if (file == null || file.isEmpty()) {
                continue;
            }

            UploadFileResponse uploadedSlip = fileStorageService.uploadFile(file);
            InvoicePaymentAttachmentEntity attachment = new InvoicePaymentAttachmentEntity();
            attachment.setFileName(uploadedSlip.getFileName());
            attachment.setOriginalFileName(file.getOriginalFilename());
            attachment.setFileUrl(uploadedSlip.getUrl());
            attachment.setContentType(uploadedSlip.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setSortOrder(index);
            payment.addSlipFile(attachment);
            attachments.add(attachment);
        }

        if (!attachments.isEmpty()) {
            InvoicePaymentAttachmentEntity firstAttachment = attachments.get(0);
            payment.setSlipFileName(firstAttachment.getFileName());
            payment.setSlipFileUrl(firstAttachment.getFileUrl());
        }

        BigDecimal nextPaidTotal = beforePaidTotal.add(amount);
        BigDecimal nextOutstandingTotal = defaultIfNull(invoice.getGrandTotal()).subtract(nextPaidTotal);
        if (nextOutstandingTotal.compareTo(BigDecimal.ZERO) < 0) {
            nextOutstandingTotal = BigDecimal.ZERO;
        }

        invoice.setPaidTotal(nextPaidTotal);
        invoice.setOutstandingTotal(nextOutstandingTotal);
        invoice.setStatus(InvoiceStatus.AWAITING_VALIDATION);
        invoice.setUpdatedBy(user);
        invoice.setUpdatedDate(now);
        markSalesOrderReadyForProcurementIfDepositPaid(invoice, userId);

        invoice.addPayment(payment);
        InvoiceEntity saved = invoiceRepository.saveAndFlush(invoice);
        recordReceivePaymentActivity(saved, payment, beforePaidTotal, beforeOutstandingTotal, beforeStatus, userId);
//        sendAwaitingValidationNotifications(saved, savedPayment);
        return mapToDto(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoiceDto sendAwaitingValidationNotification(String invoiceNo, Long paymentId, String userId) throws Exception {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceNo)
                .orElseThrow(() -> new DataNotFoundException("Invoice " + invoiceNo + " not found."));
        userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        InvoicePaymentEntity payment = invoice.getPayments().stream()
                .filter(item -> Objects.equals(item.getId(), paymentId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Invoice payment " + paymentId + " not found."));

        if (invoice.getStatus() != InvoiceStatus.AWAITING_VALIDATION) {
            throw new InvalidRequestException("Invoice status must be AWAITING_VALIDATION.");
        }

        sendAwaitingValidationNotifications(invoice, payment);

        activityHistoryService.record(
                ActivityEntityType.INVOICE,
                invoice.getInvoiceNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.WEB,
                "ส่งแจ้งเตือนตรวจสอบการรับชำระเงิน Invoice เลขที่ " + invoice.getInvoiceNo(),
                Map.of(
                        "paymentId", payment.getId(),
                        "paymentAmount", payment.getAmount(),
                        "paymentMethod", payment.getPaymentMethod()
                )
        );

        return mapToDto(invoice);
    }

    @Transactional(readOnly = true)
    public DownloadDocumentDto getInvoiceDocumentById(String invoiceNo, DocumentRequest documentRequest) throws Exception {
        log.info("Get invoice document by {}", invoiceNo);

        InvoiceEntity invoiceEntity = invoiceRepository.findById(invoiceNo)
                .orElseThrow(() -> new DataNotFoundException("Invoice " + invoiceNo + " not found."));
        String fileName = invoiceEntity.getInvoiceNo();

        if (documentRequest.getFormat().equals(ExportFileFormat.PDF)) {
            List<byte[]> pdfBytesList = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                pdfBytesList.add((byte[]) reportService.getInvoiceDocument(
                        buildInvoiceDocumentDto(invoiceEntity, Boolean.FALSE, documentRequest.getLang()),
                        documentRequest.getFormat(),
                        documentRequest.getLang()
                ));
            }
            if (documentRequest.getIsCopy()) {
                pdfBytesList.add((byte[]) reportService.getInvoiceDocument(
                        buildInvoiceDocumentDto(invoiceEntity, Boolean.TRUE, documentRequest.getLang()),
                        documentRequest.getFormat(),
                        documentRequest.getLang()
                ));
            }

            byte[] mergedPdf = PdfMergeUtil.merge(pdfBytesList);
            return new DownloadDocumentDto(fileName, documentRequest.getFormat(), List.of(new DownloadDocumentDto.FileItem(fileName + "." + documentRequest.getFormat(), Base64.getEncoder().encodeToString(mergedPdf) , "application/pdf")));
        } else if (documentRequest.getFormat().equals(ExportFileFormat.JPG)) {
            List<byte[]> pages = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                List<byte[]> originalPages = (List<byte[]>) reportService.getInvoiceDocument(
                        buildInvoiceDocumentDto(invoiceEntity, Boolean.FALSE, documentRequest.getLang()),
                        documentRequest.getFormat(),
                        documentRequest.getLang()
                );
                pages.addAll(originalPages);
            }
            if (documentRequest.getIsCopy()) {
                List<byte[]> copyPages = (List<byte[]>) reportService.getInvoiceDocument(
                        buildInvoiceDocumentDto(invoiceEntity, Boolean.TRUE, documentRequest.getLang()),
                        documentRequest.getFormat(),
                        documentRequest.getLang()
                );
                pages.addAll(copyPages);
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

    private void validateCreateRequest(CreateInvoiceRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required");
        }
        if (StringUtils.isBlank(request.getSalesOrderNo())) {
            throw new InvalidRequestException("salesOrderNo is required");
        }
    }

    private String generateInvoiceNo() {
        return generatedIdSequenceService.getNextIdWithMonth(INVOICE_PREFIX, 4);
    }

    private String resolveQuotationNo(String salesOrderNo) {
        return requestPriceHeaderRepository.findFirstBySaleOrderId(salesOrderNo)
                .map(RfqHeaderEntity::getQuotationNo)
                .orElse(null);
    }

    private String buildCustomerAddressSnapshot(CustomerAddressEntity address) {
        if (address == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        appendSnapshot(builder, address.getAddressLine1());
        appendSnapshot(builder, address.getAddressLine2());
        appendSnapshot(builder, address.getSubdistrict());
        appendSnapshot(builder, address.getDistrict());
        appendSnapshot(builder, address.getProvince());
        appendSnapshot(builder, address.getPostcode());
        appendSnapshot(builder, address.getCountry());
        return builder.toString().trim();
    }

    private String buildSalesNameSnapshot(EmployeeEntity sales) {
        if (sales == null) {
            return null;
        }
        return String.join(" ", List.of(
                StringUtils.defaultString(sales.getFirstNameTh()),
                StringUtils.defaultString(sales.getLastNameTh())
        )).trim();
    }

    private void appendSnapshot(StringBuilder builder, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    private void recordCreateInvoiceActivity(InvoiceEntity entity, String userId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("salesOrderNo", entity.getSalesOrder() != null ? entity.getSalesOrder().getSalesOrderNo() : null);
        detail.put("quotationNo", entity.getQuotationNo());
        detail.put("status", entity.getStatus());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("itemCount", entity.getItems() != null ? entity.getItems().size() : 0);
        detail.put("subTotal", entity.getSubTotal());
        detail.put("vat", entity.getVat());
        detail.put("grandTotal", entity.getGrandTotal());

        activityHistoryService.record(
                ActivityEntityType.INVOICE,
                entity.getInvoiceNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้าง Invoice เลขที่ " + entity.getInvoiceNo(),
                detail
        );
    }

    private void applySummaryOverride(InvoiceEntity entity, CreateInvoiceRequest request) {
        if (request == null) {
            return;
        }

        boolean hasSummaryOverride = request.getSubTotal() != null
                || request.getDiscount() != null
                || request.getAmount() != null
                || request.getVat() != null
                || request.getGrandTotal() != null;

        if (!hasSummaryOverride) {
            return;
        }

        if (request.getSubTotal() != null) {
            entity.setSubTotal(request.getSubTotal());
        }

        if (request.getDiscount() != null) {
            entity.setDiscount(request.getDiscount());
        }

        entity.setFreight(BigDecimal.ZERO);

        BigDecimal remainingAmount = defaultIfNull(entity.getSubTotal())
                .subtract(defaultIfNull(entity.getDiscount()));
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        BigDecimal depositAmount = request.getAmount() != null ? request.getAmount() : remainingAmount;
        if (depositAmount.compareTo(BigDecimal.ZERO) < 0) {
            depositAmount = BigDecimal.ZERO;
        }
        entity.setAmount(depositAmount);

        if (request.getVat() != null) {
            entity.setVat(request.getVat());
        }

        BigDecimal calculatedGrandTotal = depositAmount.add(defaultIfNull(entity.getVat()));
        entity.setGrandTotal(calculatedGrandTotal);
        entity.setOutstandingTotal(calculatedGrandTotal);

        if (depositAmount.compareTo(BigDecimal.ZERO) > 0 && defaultIfNull(entity.getVat()).compareTo(BigDecimal.ZERO) > 0) {
            entity.setVatRate(entity.getVat().divide(depositAmount, 4, RoundingMode.HALF_UP));
        } else if (defaultIfNull(entity.getVat()).compareTo(BigDecimal.ZERO) <= 0) {
            entity.setVatRate(BigDecimal.ZERO);
        }
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultRevNo(Integer revNo) {
        return revNo == null ? 0 : revNo;
    }

    private void markSalesOrderReadyForProcurementIfDepositPaid(InvoiceEntity invoice, String userId) {
        if (invoice == null || invoice.getStatus() != InvoiceStatus.PAID || !isDepositInvoice(invoice)) {
            return;
        }

        SalesOrderEntity salesOrder = invoice.getSalesOrder();
        if (salesOrder == null || salesOrder.getProcurementStatus() == ProcurementStatus.READY_FOR_PO) {
            return;
        }

        ProcurementStatus beforeStatus = salesOrder.getProcurementStatus();
        salesOrder.setProcurementStatus(ProcurementStatus.READY_FOR_PO);
        salesOrderRepository.save(salesOrder);
        recordReadyForProcurementActivity(salesOrder, beforeStatus, userId);
    }

    private boolean isDepositInvoice(InvoiceEntity invoice) {
        if (invoice == null) {
            return false;
        }

        BigDecimal remainingAmount = defaultIfNull(invoice.getSubTotal())
                .subtract(defaultIfNull(invoice.getDiscount()));
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        BigDecimal expectedDepositAmount = remainingAmount
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal actualAmount = defaultIfNull(invoice.getAmount()).setScale(2, RoundingMode.HALF_UP);

        return actualAmount.compareTo(expectedDepositAmount) == 0;
    }

    private void recordReadyForProcurementActivity(
            SalesOrderEntity salesOrder,
            ProcurementStatus beforeStatus,
            String userId
    ) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("procurementStatus", beforeStatus);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("procurementStatus", salesOrder.getProcurementStatus());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", after);
        detail.put("trigger", "DEPOSIT_INVOICE_PAID");

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                salesOrder.getSalesOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.STATUS_CHANGE,
                ActivitySource.API,
                "แจ้งพร้อมสร้าง Purchase Order สำหรับ Sales Order เลขที่ " + salesOrder.getSalesOrderNo(),
                detail
        );
    }

    private InvoiceDto mapToDto(InvoiceEntity entity) {
        InvoiceDto dto = new InvoiceDto();
        dto.setInvoiceNo(entity.getInvoiceNo());
        dto.setSalesOrderNo(entity.getSalesOrder() != null ? entity.getSalesOrder().getSalesOrderNo() : null);
        dto.setQuotationNo(entity.getQuotationNo());
        dto.setDocDate(entity.getDocDate() != null ? entity.getDocDate().format(DateUtil.DD_MM_YY) : null);
        dto.setDueDate(entity.getDueDate() != null ? entity.getDueDate().format(DateUtil.DD_MM_YY) : null);
        dto.setDeliveryDate(entity.getDeliveryDate() != null ? entity.getDeliveryDate().format(DateUtil.DD_MM_YY) : null);
        dto.setStatus(entity.getStatus());
        dto.setStatusProfile(DocumentStatusResolver.resolveInvoice(entity.getStatus(), entity.getPayments()));
        dto.setCurrency(entity.getCurrency());
        dto.setCustomer(customerMapper.toDto(entity.getCustomer()));
        dto.setCustomerAddress(customerMapper.toAddressDto(entity.getCustomerAddress()));
        dto.setCustomerContact(customerMapper.toContactDto(entity.getCustomerContact()));
        dto.setSaleAccount(employeeMapper.toDto(entity.getSales()));
        dto.setCoSaleId(entity.getCoSalesId());
        dto.setSubTotal(entity.getSubTotal());
        dto.setDiscount(entity.getDiscount());
        dto.setFreight(entity.getFreight());
        dto.setAmount(entity.getAmount());
        dto.setCommission(entity.getCommission());
        dto.setVatRate(entity.getVatRate());
        dto.setVat(entity.getVat());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setPaidTotal(entity.getPaidTotal());
        dto.setOutstandingTotal(entity.getOutstandingTotal());
        dto.setRemark(entity.getRemark());
        dto.setRevNo(entity.getRevNo());
        dto.setCustomerNameSnapshot(entity.getCustomerNameSnapshot());
        dto.setCustomerTaxIdSnapshot(entity.getCustomerTaxIdSnapshot());
        dto.setCustomerAddressSnapshot(entity.getCustomerAddressSnapshot());
        dto.setCustomerContactSnapshot(entity.getCustomerContactSnapshot());
        dto.setCustomerPhoneSnapshot(entity.getCustomerPhoneSnapshot());
        dto.setSalesNameSnapshot(entity.getSalesNameSnapshot());
        dto.setCreatedBy(userMapper.toDto(entity.getCreatedBy()));
        dto.setUpdatedBy(userMapper.toDto(entity.getUpdatedBy()));

        List<InvoiceDetailDto> items = new ArrayList<>();
        for (InvoiceDetailEntity detail : entity.getItems()) {
            InvoiceDetailDto item = new InvoiceDetailDto();
            item.setId(detail.getId());
            item.setSalesOrderDetailId(detail.getSalesOrderDetail() != null ? detail.getSalesOrderDetail().getId() : null);
            item.setLineNo(detail.getLineNo());
            item.setName(detail.getName());
            item.setType(detail.getType());
            item.setCapacity(detail.getCapacity());
            item.setSize(detail.getSize());
            item.setSpec(detail.getSpec());
            item.setUnitPrice(detail.getUnitPrice());
            item.setQuantity(detail.getQuantity());
            item.setAmount(detail.getAmount());
            item.setImageUrl(detail.getImageUrl());
            items.add(item);
        }
        dto.setItems(items);

        List<InvoicePaymentDto> payments = new ArrayList<>();
        for (InvoicePaymentEntity payment : entity.getPayments()) {
            InvoicePaymentDto paymentDto = new InvoicePaymentDto();
            paymentDto.setId(payment.getId());
            paymentDto.setPaymentDate(payment.getPaymentDate());
            paymentDto.setAmount(payment.getAmount());
            paymentDto.setPaymentMethod(payment.getPaymentMethod());
            paymentDto.setChequeBank(payment.getChequeBank());
            paymentDto.setChequeNo(payment.getChequeNo());
            paymentDto.setChequeDate(payment.getChequeDate());
            paymentDto.setChequeBranch(payment.getChequeBranch());
            paymentDto.setSlipFileName(payment.getSlipFileName());
            paymentDto.setSlipFileUrl(payment.getSlipFileUrl());
            paymentDto.setSlipFiles(payment.getSlipFiles() == null
                    ? List.of()
                    : payment.getSlipFiles().stream().map(this::mapToSlipFileDto).toList());
            paymentDto.setReceiptNo(payment.getReceiptNo());
            paymentDto.setStatus(payment.getStatus());
            paymentDto.setCreatedBy(userMapper.toDto(payment.getCreatedBy()));
            paymentDto.setUpdatedBy(userMapper.toDto(payment.getUpdatedBy()));
            paymentDto.setCreatedDate(payment.getCreatedDate());
            paymentDto.setUpdatedDate(payment.getUpdatedDate());
            payments.add(paymentDto);
        }
        dto.setPayments(payments);
        return dto;
    }

    private InvoiceDocumentDto buildInvoiceDocumentDto(
            InvoiceEntity invoiceEntity,
            Boolean aFalse,
            TemplateLanguage language
    ) {
        InvoiceDocumentDto dto = new InvoiceDocumentDto();
        dto.setDocNo(invoiceEntity.getInvoiceNo());
        dto.setDocDate(invoiceEntity.getDocDate() != null ? invoiceEntity.getDocDate().format(DateUtil.DD_MM_YY) : null);
        dto.setIsCopy(aFalse);
        dto.setQuotationNo(invoiceEntity.getQuotationNo());
        dto.setSalesOrderNo(invoiceEntity.getSalesOrder().getSalesOrderNo());
        dto.setDueDate(invoiceEntity.getDueDate() != null ?invoiceEntity.getDueDate().format(DateUtil.DD_MM_YY) : null);
        dto.setDeliveryDate(invoiceEntity.getDeliveryDate() != null ? invoiceEntity.getDeliveryDate().format(DateUtil.DD_MM_YY) : null);
        dto.setAmount(invoiceEntity.getAmount());
        dto.setDiscount(invoiceEntity.getDiscount());
        dto.setGrandTotal(invoiceEntity.getGrandTotal());
        dto.setFreight(invoiceEntity.getFreight());
        dto.setSubTotal(invoiceEntity.getSubTotal());
        dto.setVat(invoiceEntity.getVat());
        dto.setRemark(invoiceEntity.getRemark());
        dto.setThaiBahtText(ThaiBahtText.convertBahtText(defaultIfNull(invoiceEntity.getGrandTotal())));

        dto.setCustName(
                invoiceEntity.getCustomer() != null
                        ? invoiceEntity.getCustomer().getCustomerName()
                        : invoiceEntity.getCustomerNameSnapshot()
        );
        dto.setCustTaxId(
                invoiceEntity.getCustomer() != null
                        ? invoiceEntity.getCustomer().getTaxId()
                        : invoiceEntity.getCustomerTaxIdSnapshot()
        );
        dto.setCustAddress(
                invoiceEntity.getCustomerAddress() != null
                        ? buildFullAddress(invoiceEntity.getCustomerAddress())
                        : invoiceEntity.getCustomerAddressSnapshot()
        );
        dto.setCustMobileNo(
                invoiceEntity.getCustomerContact() != null
                        ? invoiceEntity.getCustomerContact().getContactNumber()
                        : invoiceEntity.getCustomerPhoneSnapshot()
        );
        dto.setSalesId(invoiceEntity.getSales() != null ? invoiceEntity.getSales().getEmployeeId() : null);
        dto.setSalesName(
                invoiceEntity.getSales() != null
                        ? buildSalesNameSnapshot(invoiceEntity.getSales())
                        : invoiceEntity.getSalesNameSnapshot()
        );
        dto.setSalesMobileNo(invoiceEntity.getSales() != null ? invoiceEntity.getSales().getPhoneNumber() : null);
        dto.setSalesNickname(invoiceEntity.getSales() != null ? invoiceEntity.getSales().getNickName() : null);
        dto.setCoSalesId(invoiceEntity.getCoSalesId());

        if (defaultIfNull(invoiceEntity.getVatRate()).compareTo(BigDecimal.ZERO) == 0) {
            List<SystemConfigDto> noVatConfig = systemConfigService.getSystemConfigByGroupCode(SystemConstant.REPORT_NO_VAT);
            dto.setBankName(systemConfigService.getConfig(noVatConfig, "BANK_NAME", language));
            dto.setAccountName(systemConfigService.getConfig(noVatConfig, "ACCOUNT_NAME", language));
            dto.setAccountNo(systemConfigService.getConfig(noVatConfig, "ACCOUNT_NO", language));
        } else {
            List<SystemConfigDto> vatConfig = systemConfigService.getSystemConfigByGroupCode(SystemConstant.REPORT_VAT);
            dto.setBankName(systemConfigService.getConfig(vatConfig, "BANK_NAME", language));
            dto.setAccountName(systemConfigService.getConfig(vatConfig, "ACCOUNT_NAME", language));
            dto.setAccountNo(systemConfigService.getConfig(vatConfig, "ACCOUNT_NO", language));
        }

        dto.setItems(getItemDocumentDtos(invoiceEntity));

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
        appendSnapshot(sb, address.getAddressLine1());
        appendSnapshot(sb, address.getAddressLine2());

        if (address.getSubdistrict() != null) {
            appendSnapshot(sb, subdistrictPrefix + address.getSubdistrict());
        }

        if (address.getDistrict() != null) {
            appendSnapshot(sb, districtPrefix + address.getDistrict());
        }

        if (address.getProvince() != null) {
            appendSnapshot(sb, isBangkok ? address.getProvince() : "จังหวัด" + address.getProvince());
        }

        appendSnapshot(sb, address.getPostcode());
        return sb.toString().trim();
    }

    private List<InvoiceItemDocumentDto> getItemDocumentDtos(InvoiceEntity invoiceEntity) {
        List<InvoiceItemDocumentDto> itemDocuments = new ArrayList<>();
        for (InvoiceDetailEntity detail : invoiceEntity.getItems()) {
            InvoiceItemDocumentDto item = new InvoiceItemDocumentDto();
            item.setSku("PRE ORDER");
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

        SystemConfigEntity rowCount = systemConfigService.getConfigEntity(REPORT_ROW, "INV");
        int row = rowCount != null ? Integer.valueOf(rowCount.getNameTh()) : 4;
        while (itemDocuments.size() < row) {
            itemDocuments.add(new InvoiceItemDocumentDto());
        }

        return itemDocuments;
    }

    private void recordReceivePaymentActivity(
            InvoiceEntity invoice,
            InvoicePaymentEntity payment,
            BigDecimal beforePaidTotal,
            BigDecimal beforeOutstandingTotal,
            InvoiceStatus beforeStatus,
            String userId
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", beforeStatus);
        before.put("paidTotal", beforePaidTotal);
        before.put("outstandingTotal", beforeOutstandingTotal);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", invoice.getStatus());
        after.put("paidTotal", invoice.getPaidTotal());
        after.put("outstandingTotal", invoice.getOutstandingTotal());
        after.put("paymentDate", payment.getPaymentDate());
        after.put("paymentAmount", payment.getAmount());
        after.put("paymentMethod", payment.getPaymentMethod());
        after.put("chequeBank", payment.getChequeBank());
        after.put("chequeNo", payment.getChequeNo());
        after.put("chequeDate", payment.getChequeDate());
        after.put("chequeBranch", payment.getChequeBranch());
        after.put("slipFileName", payment.getSlipFileName());
        after.put("slipFileUrl", payment.getSlipFileUrl());
        after.put("slipFiles", payment.getSlipFiles() == null
                ? List.of()
                : payment.getSlipFiles().stream().map(this::mapToSlipFileActivity).toList());

        detail.put("before", before);
        detail.put("after", after);

        activityHistoryService.record(
                ActivityEntityType.INVOICE,
                invoice.getInvoiceNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.RECEIVE_PAYMENT,
                ActivitySource.API,
                "รับชำระเงิน Invoice เลขที่ " + invoice.getInvoiceNo(),
                detail
        );
    }

    private List<MultipartFile> resolveSlipFiles(MultipartFile[] slipFiles, MultipartFile slipFile) {
        List<MultipartFile> files = new ArrayList<>();
        if (slipFiles != null) {
            for (MultipartFile file : slipFiles) {
                if (file != null && !file.isEmpty()) {
                    files.add(file);
                }
            }
        }
        if (slipFile != null && !slipFile.isEmpty()) {
            files.add(slipFile);
        }
        return files;
    }

    private InvoicePaymentSlipFileDto mapToSlipFileDto(InvoicePaymentAttachmentEntity attachment) {
        InvoicePaymentSlipFileDto dto = new InvoicePaymentSlipFileDto();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setOriginalFileName(attachment.getOriginalFileName());
        dto.setFileUrl(attachment.getFileUrl());
        dto.setContentType(attachment.getContentType());
        dto.setFileSize(attachment.getFileSize());
        dto.setSortOrder(attachment.getSortOrder());
        dto.setCreatedDate(attachment.getCreatedDate());
        dto.setUpdatedDate(attachment.getUpdatedDate());
        return dto;
    }

    private Map<String, Object> mapToSlipFileActivity(InvoicePaymentAttachmentEntity attachment) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("id", attachment.getId());
        file.put("fileName", attachment.getFileName());
        file.put("originalFileName", attachment.getOriginalFileName());
        file.put("fileUrl", attachment.getFileUrl());
        file.put("contentType", attachment.getContentType());
        file.put("fileSize", attachment.getFileSize());
        file.put("sortOrder", attachment.getSortOrder());
        return file;
    }

    private void sendAwaitingValidationNotifications(InvoiceEntity invoice, InvoicePaymentEntity payment) {
        try {
            List<UserEntity> adminUsers = userRepository.findByRoleIn(List.of("ADMIN")).stream()
                    .filter(user -> Status.ACTIVE.equals(user.getStatus()))
                    .filter(user -> StringUtils.isNotBlank(user.getLineUserId()))
                    .filter(user -> user.getEmployeeEntity() != null
                            && user.getEmployeeEntity().getPosition() != null
                            && user.getEmployeeEntity().getPosition().getId() != null
                            && ACCOUNTING_ADMIN_POSITION_CODE.equals(user.getEmployeeEntity().getPosition().getId().getCode()))
                    .toList();

            if (adminUsers.isEmpty()) {
                log.warn("No active ADMIN users with LINE binding found for invoice {}", invoice.getInvoiceNo());
                return;
            }
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("altText", "มีรายการรับชำระเงินรอตรวจสอบ " + invoice.getInvoiceNo());
            placeholders.put("title", "รอตรวจสอบการรับชำระเงิน");
            placeholders.put(
                    "detail",
                    String.format(
                            "Invoice %s ของ %s มียอดรับชำระ %s บาท และอยู่ในสถานะรอตรวจสอบ",
                            StringUtils.defaultString(invoice.getInvoiceNo(), "-"),
                            invoice.getCustomer() != null
                                    ? StringUtils.defaultString(invoice.getCustomer().getCustomerName(), "-")
                                    : "-",
                            String.format(
                                    "%,.2f",
                                    defaultIfNull(invoice.getPaidTotal()).setScale(2, RoundingMode.HALF_UP)
                            )
                    )
            );
            placeholders.put("detailUrl", buildAwaitingValidationUrl(invoice.getInvoiceNo(), payment.getId()));

            JsonNode message = renderNotificationTemplate(placeholders);
            for (UserEntity adminUser : adminUsers) {
                try {
                    lineMessageService.sendFlexMessage(adminUser.getLineUserId(), message);
                } catch (Exception exception) {
                    log.warn("Cannot send awaiting validation notification to admin {}", adminUser.getId(), exception);
                }
            }
        } catch (Exception exception) {
            log.warn("Cannot send awaiting validation notifications for invoice {}", invoice.getInvoiceNo(), exception);
        }
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

    private String buildAwaitingValidationUrl(String invoiceNo, Long paymentId) throws InvalidRequestException {
        String token = buildAwaitingValidationToken(invoiceNo, paymentId);
        return UriComponentsBuilder.fromUriString(buildFrontendBaseUrl())
                .path("/invoice-awaiting-validation")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private String buildAwaitingValidationToken(String invoiceNo, Long paymentId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ACTION, ACTION_AWAITING_VALIDATION_VIEW);
        claims.put(CLAIM_PAYMENT_ID, paymentId);
        String subject = invoiceNo + AWAITING_VALIDATION_SUBJECT_SEPARATOR + paymentId;
        String token = JwtUtil.generateToken(subject, claims, AWAITING_VALIDATION_TOKEN_EXPIRATION_SECONDS);
        log.info(
                "Build awaiting validation token invoiceNo={}, paymentId={}, subject={}, token={}",
                invoiceNo,
                paymentId,
                subject,
                token
        );
        return token;
    }

    private AwaitingValidationTokenClaims parseAwaitingValidationToken(String token) throws InvalidRequestException {
        if (StringUtils.isBlank(token) || !JwtUtil.isValid(token)) {
            throw new InvalidRequestException("Awaiting validation token is invalid or expired.");
        }

        String action = JwtUtil.getClaim(token, CLAIM_ACTION);
        String subject = JwtUtil.getSubject(token);
        String paymentIdValue = JwtUtil.getClaim(token, CLAIM_PAYMENT_ID);
        log.info(
                "Parse awaiting validation token action={}, subject={}, paymentId={}, token={}",
                action,
                subject,
                paymentIdValue,
                token
        );
        if (!StringUtils.equalsIgnoreCase(ACTION_AWAITING_VALIDATION_VIEW, action)) {
            throw new InvalidRequestException("Awaiting validation token action mismatch.");
        }

        try {
            subject = StringUtils.trimToNull(subject);
            if (StringUtils.isNotBlank(subject) && subject.contains(AWAITING_VALIDATION_SUBJECT_SEPARATOR)) {
                String[] parts = subject.split("\\Q" + AWAITING_VALIDATION_SUBJECT_SEPARATOR + "\\E", 2);
                if (parts.length == 2 && StringUtils.isNotBlank(parts[0]) && StringUtils.isNotBlank(parts[1])) {
                    return new AwaitingValidationTokenClaims(parts[0], Long.valueOf(parts[1]));
                }
            }

            String invoiceNo = subject;
            if (StringUtils.isBlank(invoiceNo) || StringUtils.isBlank(paymentIdValue)) {
                throw new InvalidRequestException("Awaiting validation token payload is invalid.");
            }

            return new AwaitingValidationTokenClaims(invoiceNo, Long.valueOf(paymentIdValue));
        } catch (NumberFormatException exception) {
            throw new InvalidRequestException("Awaiting validation token payload is invalid.");
        }
    }

    private InvoicePaymentEntity resolvePayment(InvoiceEntity invoice, Long paymentId) throws DataNotFoundException {
        return invoice.getPayments().stream()
                .filter(item -> Objects.equals(item.getId(), paymentId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Invoice payment " + paymentId + " not found."));
    }

    private InvoiceStatus resolveValidatedInvoiceStatus(InvoiceEntity invoice) {
        BigDecimal paidTotal = defaultIfNull(invoice.getPaidTotal());
        BigDecimal outstandingTotal = defaultIfNull(invoice.getOutstandingTotal());

        if (paidTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return InvoiceStatus.ISSUED;
        }
        if (outstandingTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return InvoiceStatus.PAID;
        }
        return InvoiceStatus.PARTIALLY_PAID;
    }

    private void recordAwaitingValidationDecisionActivity(
            InvoiceEntity invoice,
            InvoicePaymentEntity payment,
            ActivityAction action,
            String summary
    ) {
        activityHistoryService.record(
                ActivityEntityType.INVOICE,
                invoice.getInvoiceNo(),
                PUBLIC_TOKEN_ACTOR,
                ActivityActorType.SYSTEM,
                action,
                ActivitySource.WEB,
                summary + " Invoice เลขที่ " + invoice.getInvoiceNo(),
                Map.of(
                        "paymentId", payment.getId(),
                        "paymentAmount", payment.getAmount(),
                        "paymentStatus", payment.getStatus(),
                        "invoiceStatus", invoice.getStatus()
                )
        );
    }

    private record AwaitingValidationTokenClaims(String invoiceNo, Long paymentId) {}

    private String buildFrontendBaseUrl() throws InvalidRequestException {
        String loginSuccessUrl = lineConfiguration.getLoginSuccessUrl();
        if (StringUtils.isBlank(loginSuccessUrl)) {
            throw new InvalidRequestException("LINE frontend redirect URL is not configured");
        }
        URI uri = URI.create(loginSuccessUrl);
        return uri.getScheme() + "://" + uri.getAuthority();
    }
}
