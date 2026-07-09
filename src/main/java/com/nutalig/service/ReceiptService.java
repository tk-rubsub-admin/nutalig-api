package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.controller.receipt.request.CreateReceiptRequest;
import com.nutalig.controller.receipt.request.SearchReceiptRequest;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.*;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.dto.document.ReceiptDocumentDto;
import com.nutalig.dto.document.ReceiptItemDocumentDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.CustomerMapper;
import com.nutalig.mapper.EmployeeMapper;
import com.nutalig.mapper.UserMapper;
import com.nutalig.repository.InvoicePaymentRepository;
import com.nutalig.repository.InvoiceRepository;
import com.nutalig.repository.ReceiptRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.utils.DateUtil;
import com.nutalig.utils.PdfMergeUtil;
import com.nutalig.utils.ThaiBahtText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static com.nutalig.constant.BusinessConstant.DocumentPrefix.DEPOSIT_RECEIPT_PREFIX;
import static com.nutalig.constant.BusinessConstant.DocumentPrefix.DEPOSIT_RECEIPT_TAX_PREFIX;
import static com.nutalig.constant.BusinessConstant.DocumentPrefix.RECEIPT_PREFIX;
import static com.nutalig.constant.BusinessConstant.DocumentPrefix.RECEIPT_TAX_PREFIX;
import static com.nutalig.repository.specification.ReceiptSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final GeneratedIdSequenceService generatedIdSequenceService;
    private final ActivityHistoryService activityHistoryService;
    private final SystemConfigService systemConfigService;
    private final ReportService reportService;
    private final ReceiptRepository receiptRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;
    private final EmployeeMapper employeeMapper;
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public ReceiptEntity createReceipt(CreateReceiptRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        validateCreateRequest(request);

        InvoiceEntity invoice = invoiceRepository.findById(request.getInvoiceNo())
                .orElseThrow(() -> new DataNotFoundException("Invoice " + request.getInvoiceNo() + " not found."));
        InvoicePaymentEntity invoicePayment = invoicePaymentRepository.findById(request.getInvoicePaymentId())
                .orElseThrow(() -> new DataNotFoundException("Invoice payment " + request.getInvoicePaymentId() + " not found."));
        if (invoicePayment.getInvoice() == null || !StringUtils.equals(invoice.getInvoiceNo(), invoicePayment.getInvoice().getInvoiceNo())) {
            throw new InvalidRequestException("invoicePayment does not belong to invoice");
        }
        if (StringUtils.isNotBlank(invoicePayment.getReceiptNo())) {
            throw new InvalidRequestException("invoicePayment already linked with receipt " + invoicePayment.getReceiptNo());
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        LocalDate docDate = request.getDocDate() != null
                ? request.getDocDate()
                : (invoicePayment.getPaymentDate() != null ? invoicePayment.getPaymentDate().toLocalDate() : null);
        if (docDate == null) {
            docDate = LocalDate.now(DateUtil.getTimeZone());
        }
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        ReceiptEntity entity = new ReceiptEntity();
        entity.setReceiptType(request.getReceiptType());
        entity.setReceiptNo(generateReceiptNo(request.getReceiptType()));
        entity.setStatus(ReceiptStatus.ISSUED);
        entity.setInvoice(invoice);
        entity.setInvoicePayment(invoicePayment);
        entity.setSalesOrder(invoice.getSalesOrder());
        entity.setQuotationNo(invoice.getQuotationNo());
        entity.setDocDate(docDate);
        entity.setPaidDate(invoicePayment.getPaymentDate());
        entity.setCurrency(invoice.getCurrency());
        entity.setCustomer(invoice.getCustomer());
        entity.setCustomerAddress(invoice.getCustomerAddress());
        entity.setCustomerContact(invoice.getCustomerContact());
        entity.setSales(invoice.getSales());
        entity.setCoSalesId(invoice.getCoSalesId());
        entity.setPaymentMethod(invoicePayment.getPaymentMethod());
        entity.setChequeBank(invoicePayment.getChequeBank());
        entity.setChequeNo(invoicePayment.getChequeNo());
        entity.setChequeDate(invoicePayment.getChequeDate());
        entity.setChequeBranch(invoicePayment.getChequeBranch());
        entity.setSlipFileName(invoicePayment.getSlipFileName());
        entity.setSlipFileUrl(invoicePayment.getSlipFileUrl());
        entity.setRemark(StringUtils.trimToNull(request.getRemark()));
        entity.setRevNo(1);
        entity.setCustomerNameSnapshot(invoice.getCustomerNameSnapshot());
        entity.setCustomerTaxIdSnapshot(invoice.getCustomerTaxIdSnapshot());
        entity.setCustomerAddressSnapshot(invoice.getCustomerAddressSnapshot());
        entity.setCustomerContactSnapshot(invoice.getCustomerContactSnapshot());
        entity.setCustomerPhoneSnapshot(invoice.getCustomerPhoneSnapshot());
        entity.setSalesNameSnapshot(invoice.getSalesNameSnapshot());
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
        entity.setCreatedDate(now);
        entity.setUpdatedDate(now);

        applyTotals(entity, invoice, invoicePayment.getAmount());

        int lineNo = 1;
        for (InvoiceDetailEntity invoiceDetail : invoice.getItems()) {
            ReceiptDetailEntity detail = new ReceiptDetailEntity();
            detail.setInvoiceDetail(invoiceDetail);
            detail.setLineNo(lineNo++);
            detail.setName(invoiceDetail.getName());
            detail.setType(invoiceDetail.getType());
            detail.setCapacity(invoiceDetail.getCapacity());
            detail.setSize(invoiceDetail.getSize());
            detail.setSpec(invoiceDetail.getSpec());
            detail.setUnitPrice(defaultIfNull(invoiceDetail.getUnitPrice()));
            detail.setQuantity(defaultIfNull(invoiceDetail.getQuantity()));
            detail.setAmount(defaultIfNull(invoiceDetail.getAmount()));
            detail.setImageUrl(invoiceDetail.getImageUrl());
            entity.addItem(detail);
        }

        invoicePayment.setReceiptNo(entity.getReceiptNo());
        invoicePayment.setUpdatedBy(user);
        invoicePayment.setUpdatedDate(now);

        receiptRepository.save(entity);
        recordCreateReceiptActivity(entity, userId);
        return entity;
    }

    @Transactional(readOnly = true)
    public ReceiptDto getReceiptById(String receiptNo) throws DataNotFoundException {
        ReceiptEntity entity = receiptRepository.findById(receiptNo)
                .orElseThrow(() -> new DataNotFoundException("Receipt " + receiptNo + " not found."));
        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReceiptDto voidReceipt(String receiptNo, String userId)
            throws DataNotFoundException, InvalidRequestException {
        ReceiptEntity receipt = receiptRepository.findById(receiptNo)
                .orElseThrow(() -> new DataNotFoundException("Receipt " + receiptNo + " not found."));
        if (receipt.getStatus() == ReceiptStatus.VOID) {
            throw new InvalidRequestException("Receipt " + receiptNo + " is already void.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        ReceiptStatus beforeStatus = receipt.getStatus();

        receipt.setStatus(ReceiptStatus.VOID);
        receipt.setUpdatedBy(user);
        receipt.setUpdatedDate(now);

        InvoicePaymentEntity invoicePayment = receipt.getInvoicePayment();
        if (invoicePayment != null && StringUtils.equals(invoicePayment.getReceiptNo(), receipt.getReceiptNo())) {
            invoicePayment.setReceiptNo(null);
            invoicePayment.setUpdatedBy(user);
            invoicePayment.setUpdatedDate(now);
        }

        recordVoidReceiptActivity(receipt, beforeStatus, userId);
        return mapToDto(receipt);
    }

    @Transactional(readOnly = true)
    public Pageable<ReceiptDto> searchReceipts(SearchReceiptRequest request, PageableRequest pageableRequest) {
        SearchReceiptRequest criteria = Optional.ofNullable(request).orElseGet(SearchReceiptRequest::new);
        if (pageableRequest.getSortBy() == null || pageableRequest.getSortDirection() == null) {
            pageableRequest.setSortBy("docDate");
            pageableRequest.setSortDirection(Sort.Direction.DESC);
        }

        Page<ReceiptDto> page = receiptRepository
                .findAll(buildSearchCriteria(criteria), pageableRequest.build())
                .map(this::mapToDto);

        Pageable<ReceiptDto> response = new Pageable<>();
        response.setRecords(page.getContent());
        response.setPagination(Pagination.build(page));
        return response;
    }

    @Transactional(readOnly = true)
    public DownloadDocumentDto getReceiptDocumentById(String receiptNo, DocumentRequest documentRequest) throws Exception {
        log.info("Get receipt document by {}", receiptNo);

        ReceiptEntity receiptEntity = receiptRepository.findById(receiptNo)
                .orElseThrow(() -> new DataNotFoundException("Receipt " + receiptNo + " not found."));
        String fileName = receiptEntity.getReceiptNo();

        if (documentRequest.getFormat().equals(ExportFileFormat.PDF)) {
            List<byte[]> pdfBytesList = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                pdfBytesList.add((byte[]) reportService.getReceiptDocument(buildReceiptDocumentDto(receiptEntity, Boolean.FALSE), documentRequest.getFormat()));
            }
            if (documentRequest.getIsCopy()) {
                pdfBytesList.add((byte[]) reportService.getReceiptDocument(buildReceiptDocumentDto(receiptEntity, Boolean.TRUE), documentRequest.getFormat()));
            }

            byte[] mergedPdf = PdfMergeUtil.merge(pdfBytesList);
            return new DownloadDocumentDto(fileName, documentRequest.getFormat(), List.of(new DownloadDocumentDto.FileItem(fileName + "." + documentRequest.getFormat(), Base64.getEncoder().encodeToString(mergedPdf) , "application/pdf")));
        } else if (documentRequest.getFormat().equals(ExportFileFormat.JPG)) {
            List<byte[]> pages = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                List<byte[]> originalPages = (List<byte[]>) reportService.getReceiptDocument(buildReceiptDocumentDto(receiptEntity, Boolean.FALSE), documentRequest.getFormat());
                pages.addAll(originalPages);
            }
            if (documentRequest.getIsCopy()) {
                List<byte[]> copyPages = (List<byte[]>) reportService.getReceiptDocument(buildReceiptDocumentDto(receiptEntity, Boolean.TRUE), documentRequest.getFormat());
                pages.addAll(copyPages);
            }
            List<DownloadDocumentDto.FileItem> files = new ArrayList<>();
            for (int i = 0; i< pages.size(); i++) {
                String pageFileName = fileName + "_page_" + (i + 1) + "." + documentRequest.getFormat();
                files.add(new DownloadDocumentDto.FileItem(pageFileName, Base64.getEncoder().encodeToString(pages.get(i)), "image/jpeg"));
            }
            return new DownloadDocumentDto(fileName, documentRequest.getFormat(), files);
        }

        return null;
    }

    private Specification<ReceiptEntity> buildSearchCriteria(SearchReceiptRequest request) {
        return Specification.<ReceiptEntity>where(null)
                .and(receiptNoEqual(request.getReceiptNo()))
                .and(invoiceNoEqual(request.getInvoiceNo()))
                .and(customerIdEqual(request.getCustomerId()))
                .and(salesIdEqual(request.getSalesId()))
                .and(receiptTypeEqual(request.getReceiptType()))
                .and(statusEqual(request.getStatus()))
                .and(statusIn(request.getStatuses()))
                .and(docDateBetween(request.getDocDateStart(), request.getDocDateEnd()))
                .and(keywordContains(request.getKeyword()));
    }

    private void validateCreateRequest(CreateReceiptRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required");
        }
        if (StringUtils.isBlank(request.getInvoiceNo())) {
            throw new InvalidRequestException("invoiceNo is required");
        }
        if (request.getInvoicePaymentId() == null) {
            throw new InvalidRequestException("invoicePaymentId is required");
        }
        if (request.getReceiptType() == null) {
            throw new InvalidRequestException("receiptType is required");
        }
    }

    private void applyTotals(ReceiptEntity entity, InvoiceEntity invoice, BigDecimal paymentAmount) {
        BigDecimal paidAmount = defaultIfNull(paymentAmount);
        if (isTaxInvoiceReceipt(entity.getReceiptType()) && defaultIfNull(invoice.getVatRate()).compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisor = BigDecimal.ONE.add(defaultIfNull(invoice.getVatRate()));
            BigDecimal baseAmount = paidAmount.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal vatAmount = paidAmount.subtract(baseAmount);
            entity.setSubTotal(baseAmount);
            entity.setDiscount(BigDecimal.ZERO);
            entity.setAmount(baseAmount);
            entity.setVatRate(defaultIfNull(invoice.getVatRate()));
            entity.setVat(vatAmount);
            entity.setGrandTotal(baseAmount.add(vatAmount));
            return;
        }

        entity.setSubTotal(paidAmount);
        entity.setDiscount(BigDecimal.ZERO);
        entity.setAmount(paidAmount);
        entity.setVatRate(BigDecimal.ZERO);
        entity.setVat(BigDecimal.ZERO);
        entity.setGrandTotal(paidAmount);
    }

    private boolean isTaxInvoiceReceipt(ReceiptType receiptType) {
        return receiptType == ReceiptType.RECEIPT_TAX_INVOICE
                || receiptType == ReceiptType.DEPOSIT_TAX_INVOICE;
    }

    private String generateReceiptNo(ReceiptType receiptType) {
        String prefix = switch (receiptType) {
            case RECEIPT -> RECEIPT_PREFIX;
            case DEPOSIT_RECEIPT -> DEPOSIT_RECEIPT_PREFIX;
            case RECEIPT_TAX_INVOICE -> RECEIPT_TAX_PREFIX;
            case DEPOSIT_TAX_INVOICE -> DEPOSIT_RECEIPT_TAX_PREFIX;
        };
        return generatedIdSequenceService.getNextIdWithMonth(prefix, 4);
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ReceiptDocumentDto buildReceiptDocumentDto(ReceiptEntity receiptEntity, Boolean isCopy) {
        ReceiptDocumentDto dto = new ReceiptDocumentDto();
        dto.setReceiptType(receiptEntity.getReceiptType());
        dto.setDocNo(receiptEntity.getReceiptNo());
        dto.setDocDate(receiptEntity.getDocDate() != null ? receiptEntity.getDocDate().format(DateUtil.DD_MM_YY) : null);
        dto.setIsCopy(isCopy);
        dto.setSalesOrderNo(receiptEntity.getSalesOrder() != null ? receiptEntity.getSalesOrder().getSalesOrderNo() : null);
        dto.setInvoiceNo(receiptEntity.getInvoice() != null ? receiptEntity.getInvoice().getInvoiceNo() : null);
        dto.setAmount(receiptEntity.getAmount());
        dto.setDiscount(receiptEntity.getDiscount());
        dto.setGrandTotal(receiptEntity.getGrandTotal());
        dto.setFreight(BigDecimal.ZERO);
        dto.setSubTotal(receiptEntity.getInvoice().getSubTotal());
        dto.setVat(receiptEntity.getVat());
        dto.setRemark(receiptEntity.getRemark());
        dto.setThaiBahtText(ThaiBahtText.convertBahtText(defaultIfNull(receiptEntity.getGrandTotal())));
        dto.setPaymentMethod(receiptEntity.getPaymentMethod());
        dto.setChequeBank(defaultEmptyIfNull(receiptEntity.getChequeBank()));
        dto.setChequeNo(defaultEmptyIfNull(receiptEntity.getChequeNo()));
        dto.setChequeDate(receiptEntity.getChequeDate() != null ? receiptEntity.getChequeDate().format(DateUtil.DD_MM_YY) : "");
        dto.setChequeBranch(defaultEmptyIfNull(receiptEntity.getChequeBranch()));

        dto.setCustName(
                receiptEntity.getCustomer() != null
                        ? receiptEntity.getCustomer().getCustomerName()
                        : receiptEntity.getCustomerNameSnapshot()
        );
        dto.setCustTaxId(
                receiptEntity.getCustomer() != null
                        ? receiptEntity.getCustomer().getTaxId()
                        : receiptEntity.getCustomerTaxIdSnapshot()
        );
        dto.setCustAddress(
                receiptEntity.getCustomerAddress() != null
                        ? buildFullAddress(receiptEntity.getCustomerAddress())
                        : receiptEntity.getCustomerAddressSnapshot()
        );
        dto.setSalesId(receiptEntity.getSales() != null ? receiptEntity.getSales().getEmployeeId() : null);

        if (defaultIfNull(receiptEntity.getVatRate()).compareTo(BigDecimal.ZERO) == 0) {
            List<SystemConfigDto> noVatConfig = systemConfigService.getSystemConfigByGroupCode(SystemConstant.REPORT_NO_VAT);
            dto.setBankName(systemConfigService.getConfig(noVatConfig, "BANK_NAME"));
            dto.setAccountName(systemConfigService.getConfig(noVatConfig, "ACCOUNT_NAME"));
            dto.setAccountNo(systemConfigService.getConfig(noVatConfig, "ACCOUNT_NO"));
        } else {
            List<SystemConfigDto> vatConfig = systemConfigService.getSystemConfigByGroupCode(SystemConstant.REPORT_VAT);
            dto.setBankName(systemConfigService.getConfig(vatConfig, "BANK_NAME"));
            dto.setAccountName(systemConfigService.getConfig(vatConfig, "ACCOUNT_NAME"));
            dto.setAccountNo(systemConfigService.getConfig(vatConfig, "ACCOUNT_NO"));
        }

        dto.setItems(getReceiptItemDocumentDtos(receiptEntity));
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

    private void appendSnapshot(StringBuilder sb, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(value.trim());
    }

    private List<ReceiptItemDocumentDto> getReceiptItemDocumentDtos(ReceiptEntity receiptEntity) {
        List<ReceiptItemDocumentDto> itemDocuments = new ArrayList<>();
        for (ReceiptDetailEntity detail : receiptEntity.getItems()) {
            ReceiptItemDocumentDto item = new ReceiptItemDocumentDto();
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

        while (itemDocuments.size() < 3) {
            itemDocuments.add(new ReceiptItemDocumentDto());
        }

        return itemDocuments;
    }

    private ReceiptDto mapToDto(ReceiptEntity entity) {
        ReceiptDto dto = new ReceiptDto();
        dto.setReceiptNo(entity.getReceiptNo());
        dto.setReceiptType(entity.getReceiptType());
        dto.setStatus(entity.getStatus());
        dto.setInvoiceNo(entity.getInvoice() != null ? entity.getInvoice().getInvoiceNo() : null);
        dto.setInvoicePaymentId(entity.getInvoicePayment() != null ? entity.getInvoicePayment().getId() : null);
        dto.setSalesOrderNo(entity.getSalesOrder() != null ? entity.getSalesOrder().getSalesOrderNo() : null);
        dto.setQuotationNo(entity.getQuotationNo());
        dto.setDocDate(entity.getDocDate() != null ? entity.getDocDate().format(DateUtil.DD_MM_YY) : null);
        dto.setPaidDate(entity.getPaidDate());
        dto.setCurrency(entity.getCurrency());
        dto.setCustomer(customerMapper.toDto(entity.getCustomer()));
        dto.setCustomerAddress(customerMapper.toAddressDto(entity.getCustomerAddress()));
        dto.setCustomerContact(customerMapper.toContactDto(entity.getCustomerContact()));
        dto.setSaleAccount(employeeMapper.toDto(entity.getSales()));
        dto.setCoSaleId(entity.getCoSalesId());
        dto.setSubTotal(entity.getSubTotal());
        dto.setDiscount(entity.getDiscount());
        dto.setAmount(entity.getAmount());
        dto.setVatRate(entity.getVatRate());
        dto.setVat(entity.getVat());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setChequeBank(entity.getChequeBank());
        dto.setChequeNo(entity.getChequeNo());
        dto.setChequeDate(entity.getChequeDate() != null ? entity.getChequeDate().format(DateUtil.DD_MM_YY) : "..............................");
        dto.setChequeBranch(entity.getChequeBranch());
        dto.setSlipFileName(entity.getSlipFileName());
        dto.setSlipFileUrl(entity.getSlipFileUrl());
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

        List<ReceiptDetailDto> items = new ArrayList<>();
        for (ReceiptDetailEntity detail : entity.getItems()) {
            ReceiptDetailDto item = new ReceiptDetailDto();
            item.setId(detail.getId());
            item.setInvoiceDetailId(detail.getInvoiceDetail() != null ? detail.getInvoiceDetail().getId() : null);
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
        return dto;
    }

    private void recordCreateReceiptActivity(ReceiptEntity receipt, String userId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("receiptNo", receipt.getReceiptNo());
        after.put("receiptType", receipt.getReceiptType());
        after.put("status", receipt.getStatus());
        after.put("invoiceNo", receipt.getInvoice() != null ? receipt.getInvoice().getInvoiceNo() : null);
        after.put("invoicePaymentId", receipt.getInvoicePayment() != null ? receipt.getInvoicePayment().getId() : null);
        after.put("docDate", receipt.getDocDate());
        after.put("paidDate", receipt.getPaidDate());
        after.put("grandTotal", receipt.getGrandTotal());
        after.put("paymentMethod", receipt.getPaymentMethod());
        detail.put("after", after);

        activityHistoryService.record(
                ActivityEntityType.RECEIPT,
                receipt.getReceiptNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้าง Receipt เลขที่ " + receipt.getReceiptNo(),
                detail
        );
    }

    private void recordVoidReceiptActivity(ReceiptEntity receipt, ReceiptStatus beforeStatus, String userId) {
        Map<String, Object> detail = new LinkedHashMap<>();

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", beforeStatus);
        before.put("invoicePaymentId", receipt.getInvoicePayment() != null ? receipt.getInvoicePayment().getId() : null);
        before.put("receiptNoOnPayment", receipt.getReceiptNo());

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", receipt.getStatus());
        after.put("invoicePaymentId", receipt.getInvoicePayment() != null ? receipt.getInvoicePayment().getId() : null);
        after.put("receiptNoOnPayment", receipt.getInvoicePayment() != null ? receipt.getInvoicePayment().getReceiptNo() : null);

        detail.put("before", before);
        detail.put("after", after);

        activityHistoryService.record(
                ActivityEntityType.RECEIPT,
                receipt.getReceiptNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.STATUS_CHANGE,
                ActivitySource.API,
                "Void Receipt เลขที่ " + receipt.getReceiptNo(),
                detail
        );
    }

    private String defaultEmptyIfNull(String text) {
        if (StringUtils.isNotEmpty(text)) {
            return text;
        }
        return "..............................";
    }
}
