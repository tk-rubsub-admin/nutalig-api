package com.nutalig.service;

import com.nutalig.constant.Currency;
import com.nutalig.constant.*;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.salesorder.request.*;
import com.nutalig.dto.SalesOrderAttachmentDto;
import com.nutalig.dto.SalesOrderDetailDto;
import com.nutalig.dto.SalesOrderDto;
import com.nutalig.dto.SystemConfigDto;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.dto.document.SalesOrderDocumentDto;
import com.nutalig.dto.document.SalesOrderItemDocumentDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.CustomerMapper;
import com.nutalig.mapper.EmployeeMapper;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.mapper.UserMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import com.nutalig.utils.DocumentStatusResolver;
import com.nutalig.utils.PdfMergeUtil;
import com.nutalig.utils.ThaiBahtText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static com.nutalig.constant.BusinessConstant.DocumentPrefix.SALES_ORDER_PREFIX;
import static com.nutalig.constant.BusinessConstant.VAT_RATE;
import static com.nutalig.repository.specification.SalesOrderSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderService {
    private static final Set<SalesOrderStatus> EXCLUDED_CUSTOMER_ORDER_TOTAL_STATUSES =
            EnumSet.of(SalesOrderStatus.REJECTED, SalesOrderStatus.CANCELLED);

    private final GeneratedIdSequenceService generatedIdSequenceService;
    private final SalesOrderRepository salesOrderRepository;
    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final RequestPriceTierRepository requestPriceTierRepository;
    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;
    private final EmployeeMapper employeeMapper;
    private final SupplierMapper supplierMapper;
    private final UserMapper userMapper;
    private final ActivityHistoryService activityHistoryService;
    private final SystemConfigService systemConfigService;
    private final ReportService reportService;
    private final FileStorageService fileStorageService;
    private final SalesOrderAttachmentRepository salesOrderAttachmentRepository;

    record SalesOrderSummary(
            BigDecimal subTotal,
            BigDecimal vat,
            BigDecimal grandTotal
    ) {}

    @Transactional(rollbackFor = Exception.class)
    public SalesOrderEntity createSalesOrder(CreateSalesOrderRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        validateCreateRequest(request);
        log.info("Create sales order for customer {} by {}", request.getCustomerId(), userId);

        CustomerEntity customer = resolveCustomer(request.getCustomerId());
        CustomerAddressEntity customerAddress = resolveCustomerAddress(customer, request.getCustomerAddressId());
        CustomerContactEntity customerContact = resolveCustomerContact(customer, request.getCustomerContactId());
        EmployeeEntity sales = resolveSales(request.getSalesId());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        String salesOrderNo = generateSalesOrderNo();
        LocalDate today = LocalDate.now(DateUtil.getTimeZone());
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        SalesOrderStatus status = resolveCreateStatus(request.getStatus());

        SalesOrderEntity entity = new SalesOrderEntity();
        entity.setSalesOrderNo(salesOrderNo);
        entity.setDocDate(request.getDocDate() == null ? today : request.getDocDate());
        entity.setExpireDate(request.getExpireDate() == null ? today.plusDays(7) : request.getExpireDate());
        entity.setStatus(status);
        entity.setCurrency(Currency.THB);
        entity.setCustomer(customer);
        entity.setCustomerAddress(customerAddress);
        entity.setCustomerContact(customerContact);
        entity.setSales(sales);
        entity.setCoSalesId(request.getCoSaleId());
        entity.setCoSaleCommission(request.getCoSaleCommission());
        entity.setDiscount(defaultIfNull(request.getDiscount()));
        entity.setFreight(defaultIfNull(request.getFreight()));
        entity.setShippingType(normalizeShippingType(request.getShippingType()));
        entity.setRequestCoa(Boolean.TRUE.equals(request.getRequestCoa()));
        entity.setRequestPo(Boolean.TRUE.equals(request.getRequestPo()));
        entity.setVatRate(Boolean.TRUE.equals(request.getIsVat()) ? VAT_RATE : BigDecimal.ZERO);
        entity.setProcurementStatus(ProcurementStatus.NOT_READY);
        entity.setRemark(request.getRemark());
        entity.setRevNo(1);
        entity.setCreatedDate(now);
        entity.setCreatedBy(user);
        entity.setUpdatedDate(now);
        entity.setUpdatedBy(user);

        int lineNo = 1;
        for (CreateSalesOrderDetailRequest itemRequest : request.getItems()) {
            SalesOrderDetailEntity detail = buildDetail(entity, itemRequest, lineNo++);
            entity.getItems().add(detail);
        }

        SalesOrderSummary summary = calculate(request.getItems(), entity.getDiscount(), entity.getFreight(), request.getIsVat());
        entity.setSubTotal(summary.subTotal());
        entity.setAmount(summary.subTotal.subtract(entity.getFreight()));
        entity.setVat(summary.vat());
        entity.setGrandTotal(summary.grandTotal());

        salesOrderRepository.save(entity);
        refreshCustomerOrderTotal(customer);

        if (SalesOrderStatus.CREATED.equals(request.getStatus())) {
            linkRfq(request.getRfqId(), salesOrderNo, userId, now);
        }

        recordCreateSalesOrderActivity(entity, request, userId);

        return entity;
    }

    @Transactional(readOnly = true)
    public SalesOrderDto getSalesOrderById(String salesOrderNo) throws DataNotFoundException {
        SalesOrderEntity entity = salesOrderRepository.findById(salesOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Sales order " + salesOrderNo + " not found."));

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public SalesOrderDto updateSalesOrder(String salesOrderNo, UpdateSalesOrderRequest request, String userId)
            throws DataNotFoundException {
        log.info("Update sales order {} by {}", salesOrderNo, userId);

        SalesOrderEntity entity = salesOrderRepository.findById(salesOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Sales order " + salesOrderNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        Integer oldRevNo = entity.getRevNo();
        Map<String, Object> before = buildSalesOrderSnapshot(entity);

        if (request.getDocDate() != null) {
            entity.setDocDate(request.getDocDate());
        }
        if (request.getExpireDate() != null) {
            entity.setExpireDate(request.getExpireDate());
        }
        if (request.getCoSaleId() != null) {
            entity.setCoSalesId(request.getCoSaleId());
        }
        if (request.getDiscount() != null) {
            entity.setDiscount(defaultIfNull(request.getDiscount()));
        }
        if (request.getFreight() != null) {
            entity.setFreight(defaultIfNull(request.getFreight()));
        }
        if (request.getShippingType() != null) {
            entity.setShippingType(normalizeShippingType(request.getShippingType()));
        }
        if (request.getRequestCoa() != null) {
            entity.setRequestCoa(request.getRequestCoa());
        }
        if (request.getRequestPo() != null) {
            entity.setRequestPo(request.getRequestPo());
        }
        if (request.getRemark() != null) {
            entity.setRemark(request.getRemark());
        }
        if (request.getIsVat() != null) {
            entity.setVatRate(Boolean.TRUE.equals(request.getIsVat()) ? VAT_RATE : BigDecimal.ZERO);
        }
        if (request.getItems() != null) {
            replaceSalesOrderItems(entity, request.getItems());
        }
        if (request.getAmount() != null) {
            entity.setAmount(request.getAmount());
        }
        if (request.getCommission() != null) {
            entity.setCommission(request.getCommission());
        }
        if (request.getCoSaleCommission() != null) {
            entity.setCoSaleCommission(request.getCoSaleCommission());
        }

        if (SalesOrderStatus.DRAFT.equals(entity.getStatus())) {
            entity.setStatus(SalesOrderStatus.CREATED);
        }

        List<UpdateSalesOrderDetailRequest> itemsForCalculate = request.getItems() != null
                ? request.getItems()
                : toUpdateItemRequests(entity.getItems());
        Boolean isVat = request.getIsVat() != null
                ? request.getIsVat()
                : entity.getVatRate() != null && entity.getVatRate().compareTo(BigDecimal.ZERO) > 0;

        SalesOrderSummary summary = calculateForUpdate(itemsForCalculate, entity.getDiscount(), entity.getFreight(), isVat);
        entity.setSubTotal(summary.subTotal());
        entity.setVat(summary.vat());
        entity.setGrandTotal(summary.grandTotal());
        entity.setRevNo(defaultRevNo(oldRevNo) + 1);
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        entity.setUpdatedBy(user);

        salesOrderRepository.save(entity);
        refreshCustomerOrderTotal(entity.getCustomer());
        recordUpdateSalesOrderActivity(entity, request, userId, oldRevNo, before);

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public SalesOrderDto addAttachments(String salesOrderNo, List<MultipartFile> attachments, String userId)
            throws Exception {
        if (attachments == null || attachments.isEmpty()) {
            throw new InvalidRequestException("Attachments are required");
        }

        SalesOrderEntity entity = salesOrderRepository.findById(salesOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Sales order " + salesOrderNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        attachFiles(entity, attachments, user, now);

        entity.setUpdatedBy(user);
        entity.setUpdatedDate(now);
        salesOrderRepository.save(entity);

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                entity.getSalesOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.WEB,
                "เพิ่มไฟล์แนบของใบยืนยันสั่งซื้อเลขที่ " + entity.getSalesOrderNo(),
                null
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public SalesOrderDto deleteAttachment(String salesOrderNo, Long attachmentId, String userId)
            throws DataNotFoundException {
        SalesOrderEntity entity = salesOrderRepository.findById(salesOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Sales order " + salesOrderNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        SalesOrderAttachmentEntity attachment = salesOrderAttachmentRepository
                .findByIdAndSalesOrderSalesOrderNoAndActiveTrue(attachmentId, salesOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Sales order attachment " + attachmentId + " not found."));

        attachment.setActive(Boolean.FALSE);
        attachment.setUpdatedBy(user);
        attachment.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        salesOrderAttachmentRepository.save(attachment);

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                entity.getSalesOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.WEB,
                "ลบไฟล์แนบของใบยืนยันสั่งซื้อเลขที่ " + entity.getSalesOrderNo(),
                Map.of(
                        "attachmentId", attachment.getId(),
                        "fileName", attachment.getFileName(),
                        "originalFileName", attachment.getOriginalFileName()
                )
        );

        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public Pageable<SalesOrderDto> searchSalesOrders(
            SearchSalesOrderRequest request,
            PageableRequest pageableRequest
    ) {
        SearchSalesOrderRequest criteria = Optional.ofNullable(request).orElseGet(SearchSalesOrderRequest::new);
        if (pageableRequest.getSortBy() == null || pageableRequest.getSortDirection() == null) {
            pageableRequest.setSortBy("docDate");
            pageableRequest.setSortDirection(Sort.Direction.DESC);
        }

        Page<SalesOrderDto> page = salesOrderRepository
                .findAll(buildSearchCriteria(criteria), pageableRequest.build())
                .map(this::mapToDto);

        Pageable<SalesOrderDto> response = new Pageable<>();
        response.setRecords(page.getContent());
        response.setPagination(Pagination.build(page));
        return response;
    }

    @Transactional(readOnly = true)
    public DownloadDocumentDto getSalesOrderDocumentById(String salesOrderNo, DocumentRequest documentRequest) throws Exception {
        log.info("Get sales order by {}", salesOrderNo);

        SalesOrderEntity salesOrderEntity = salesOrderRepository.findById(salesOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Sales Order " + salesOrderNo + " not found."));

        String fileName = salesOrderEntity.getSalesOrderNo();
        if (documentRequest.getFormat().equals(ExportFileFormat.PDF)) {
            List<byte[]> pdfBytesList = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                pdfBytesList.add((byte[]) reportService.getSalesOrderDocument(buildSalesOrderDocumentDto(salesOrderEntity, Boolean.FALSE), documentRequest.getFormat()));
            }
            if (documentRequest.getIsCopy()) {
                pdfBytesList.add((byte[]) reportService.getSalesOrderDocument(buildSalesOrderDocumentDto(salesOrderEntity, Boolean.TRUE), documentRequest.getFormat()));
            }

            byte[] mergedPdf = PdfMergeUtil.merge(pdfBytesList);
            return new DownloadDocumentDto(fileName, documentRequest.getFormat(), List.of(new DownloadDocumentDto.FileItem(fileName + "." + documentRequest.getFormat(), Base64.getEncoder().encodeToString(mergedPdf) , "application/pdf")));
        } else if (documentRequest.getFormat().equals(ExportFileFormat.JPG)) {
            List<byte[]> pages = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                List<byte[]> originalPages = (List<byte[]>) reportService.getSalesOrderDocument(buildSalesOrderDocumentDto(salesOrderEntity, Boolean.FALSE), documentRequest.getFormat());
                pages.addAll(originalPages);
            }
            if (documentRequest.getIsCopy()) {
                List<byte[]> copyPages = (List<byte[]>) reportService.getSalesOrderDocument(buildSalesOrderDocumentDto(salesOrderEntity, Boolean.TRUE), documentRequest.getFormat());
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

    private SalesOrderDocumentDto buildSalesOrderDocumentDto(SalesOrderEntity salesOrderEntity, Boolean aFalse) {
        SalesOrderDocumentDto dto = new SalesOrderDocumentDto();
        dto.setDocNo(salesOrderEntity.getSalesOrderNo());
        dto.setDocDate(salesOrderEntity.getDocDate().format(DateUtil.DD_MM_YY));
        dto.setIsCopy(aFalse);
        dto.setDiscount(salesOrderEntity.getDiscount());
        dto.setGrandTotal(salesOrderEntity.getGrandTotal());
        dto.setFreight(salesOrderEntity.getFreight());
        dto.setSubTotal(salesOrderEntity.getSubTotal());
        dto.setVat(salesOrderEntity.getVat());
        dto.setRemark(salesOrderEntity.getRemark());
        dto.setThaiBahtText(ThaiBahtText.convertBahtText(salesOrderEntity.getGrandTotal()));
        dto.setCustName(salesOrderEntity.getCustomer().getCustomerName());
        dto.setCustTaxId(salesOrderEntity.getCustomer().getTaxId());
        dto.setCustAddress(buildFullAddress(salesOrderEntity.getCustomerAddress()));
        dto.setCustMobileNo(salesOrderEntity.getCustomerContact().getContactNumber());
        dto.setSalesId(salesOrderEntity.getSales().getEmployeeId());
        dto.setSalesName(salesOrderEntity.getSales().getFirstNameTh() + " " + salesOrderEntity.getSales().getLastNameTh());
        dto.setSalesMobileNo(salesOrderEntity.getSales().getPhoneNumber());
        dto.setSalesNickname(salesOrderEntity.getSales().getNickName());
        dto.setCoSalesId(salesOrderEntity.getCoSalesId());

        if (salesOrderEntity.getVatRate().compareTo(BigDecimal.ZERO) == 0) {
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

        List<SalesOrderItemDocumentDto> itemDocuments = getItemDocumentDtos(salesOrderEntity);
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

        append(sb, address.getPostcode());

        return sb.toString().trim();
    }

    private void refreshCustomerOrderTotal(CustomerEntity customer) {
        if (customer == null || StringUtils.isBlank(customer.getId())) {
            return;
        }

        BigDecimal total = salesOrderRepository.sumGrandTotalByCustomerIdAndStatusNotIn(
                customer.getId(),
                EXCLUDED_CUSTOMER_ORDER_TOTAL_STATUSES
        );
        customer.setTotalSalesOrderAmount(defaultIfNull(total));
        customerRepository.save(customer);
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(value.trim());
        }
    }

    private void validateCreateRequest(CreateSalesOrderRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required");
        }
        if (StringUtils.isBlank(request.getCustomerId())) {
            throw new InvalidRequestException("customerId is required");
        }
        if (StringUtils.isBlank(request.getSalesId())) {
            throw new InvalidRequestException("salesId is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidRequestException("items is required");
        }
        for (CreateSalesOrderDetailRequest item : request.getItems()) {
            if (item == null || StringUtils.isBlank(item.getSupplierId())) {
                throw new InvalidRequestException("items.supplierId is required");
            }
        }
    }

    private Specification<SalesOrderEntity> buildSearchCriteria(SearchSalesOrderRequest request) {
        return Specification.<SalesOrderEntity>where(null)
                .and(salesOrderNoEqual(request.getSalesOrderNo()))
                .and(customerIdEqual(request.getCustomerId()))
                .and(salesIdEqual(request.getSalesId()))
                .and(statusEqual(request.getStatus()))
                .and(statusIn(request.getStatuses()))
                .and(docDateBetween(request.getDocDateStart(), request.getDocDateEnd()))
                .and(keywordContains(request.getKeyword()));
    }

    private SalesOrderStatus resolveCreateStatus(SalesOrderStatus status) throws InvalidRequestException {
        if (status == null) {
            return SalesOrderStatus.CREATED;
        }
        if (status == SalesOrderStatus.DRAFT || status == SalesOrderStatus.CREATED) {
            return status;
        }
        throw new InvalidRequestException("status must be DRAFT or CREATED");
    }

    private SalesOrderDetailEntity buildDetail(
            SalesOrderEntity salesOrder,
            CreateSalesOrderDetailRequest request,
            int lineNo
    ) throws DataNotFoundException {
        SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new DataNotFoundException("Supplier " + request.getSupplierId() + " not found."));

        BigDecimal unitPrice = defaultIfNull(request.getUnitPrice());
        BigDecimal quantity = defaultIfNull(request.getQuantity());

        SalesOrderDetailEntity detail = new SalesOrderDetailEntity();
        detail.setSalesOrder(salesOrder);
        detail.setLineNo(lineNo);
        detail.setSupplier(supplier);
        detail.setName(request.getName());
        detail.setType(request.getType());
        detail.setCapacity(request.getCapacity());
        detail.setSize(request.getSize());
        detail.setSpec(request.getSpec());
        detail.setUnitPrice(unitPrice);
        detail.setQuantity(quantity);
        detail.setAmount(unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP));
        detail.setImageUrl(request.getImageUrl());
        detail.setRfqDetailId(request.getRfqDetailId());
        detail.setRfqTierId(request.getRfqTierId());
        detail.setQuotationDetailId(request.getQuotationDetailId());
        detail.setShippingMethod(normalizeShippingType(request.getShippingMethod()));
        detail.setSupplierCurrency(request.getSupplierCurrency());
        detail.setSupplierUnitPrice(request.getSupplierUnitPrice());
        detail.setSupplierShippingCost(request.getSupplierShippingCost());
        detail.setSupplierTotalUnitCost(request.getSupplierTotalUnitCost());
        detail.setSupplierQuoteTierId(request.getSupplierQuoteTierId());
        return detail;
    }

    private void replaceSalesOrderItems(SalesOrderEntity entity, List<UpdateSalesOrderDetailRequest> itemRequests)
            throws DataNotFoundException {
        entity.getItems().clear();
        int lineNo = 1;

        for (UpdateSalesOrderDetailRequest itemRequest : Optional.ofNullable(itemRequests).orElseGet(List::of)) {
            SupplierEntity supplier = resolveSupplier(itemRequest.getSupplierId());
            BigDecimal unitPrice = defaultIfNull(itemRequest.getUnitPrice());
            BigDecimal quantity = defaultIfNull(itemRequest.getQuantity());

            SalesOrderDetailEntity detail = new SalesOrderDetailEntity();
            detail.setSalesOrder(entity);
            detail.setLineNo(lineNo++);
            detail.setSupplier(supplier);
            detail.setName(itemRequest.getName());
            detail.setType(itemRequest.getType());
            detail.setCapacity(itemRequest.getCapacity());
            detail.setSize(itemRequest.getSize());
            detail.setSpec(itemRequest.getSpec());
            detail.setUnitPrice(unitPrice);
            detail.setQuantity(quantity);
            detail.setAmount(unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP));
            detail.setImageUrl(itemRequest.getImageUrl());
            detail.setRfqDetailId(itemRequest.getRfqDetailId());
            detail.setRfqTierId(itemRequest.getRfqTierId());
            detail.setQuotationDetailId(itemRequest.getQuotationDetailId());
            detail.setShippingMethod(normalizeShippingType(itemRequest.getShippingMethod()));
            detail.setSupplierCurrency(itemRequest.getSupplierCurrency());
            detail.setSupplierUnitPrice(itemRequest.getSupplierUnitPrice());
            detail.setSupplierShippingCost(itemRequest.getSupplierShippingCost());
            detail.setSupplierTotalUnitCost(itemRequest.getSupplierTotalUnitCost());
            detail.setSupplierQuoteTierId(itemRequest.getSupplierQuoteTierId());
            entity.addItem(detail);
        }
    }

    private List<UpdateSalesOrderDetailRequest> toUpdateItemRequests(Set<SalesOrderDetailEntity> items) {
        List<UpdateSalesOrderDetailRequest> requests = new ArrayList<>();
        for (SalesOrderDetailEntity item : items) {
            UpdateSalesOrderDetailRequest request = new UpdateSalesOrderDetailRequest();
            request.setId(item.getId());
            request.setSupplierId(item.getSupplier() != null ? item.getSupplier().getId() : null);
            request.setName(item.getName());
            request.setType(item.getType());
            request.setCapacity(item.getCapacity());
            request.setSize(item.getSize());
            request.setSpec(item.getSpec());
            request.setUnitPrice(item.getUnitPrice());
            request.setQuantity(item.getQuantity());
            request.setImageUrl(item.getImageUrl());
            request.setRfqDetailId(item.getRfqDetailId());
            request.setRfqTierId(item.getRfqTierId());
            request.setQuotationDetailId(item.getQuotationDetailId());
            request.setShippingMethod(item.getShippingMethod());
            request.setSupplierCurrency(item.getSupplierCurrency());
            request.setSupplierUnitPrice(item.getSupplierUnitPrice());
            request.setSupplierShippingCost(item.getSupplierShippingCost());
            request.setSupplierTotalUnitCost(item.getSupplierTotalUnitCost());
            request.setSupplierQuoteTierId(item.getSupplierQuoteTierId());
            requests.add(request);
        }
        return requests;
    }

    private SalesOrderSummary calculate(
            List<CreateSalesOrderDetailRequest> items,
            BigDecimal requestDiscount,
            BigDecimal requestFreight,
            Boolean isVat
    ) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CreateSalesOrderDetailRequest item : Optional.ofNullable(items).orElseGet(List::of)) {
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
            vat = taxableAmount.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal grandTotal = taxableAmount.add(vat).setScale(2, RoundingMode.HALF_UP);
        return new SalesOrderSummary(subTotal.setScale(2, RoundingMode.HALF_UP), vat, grandTotal);
    }

    private SalesOrderSummary calculateForUpdate(
            List<UpdateSalesOrderDetailRequest> items,
            BigDecimal requestDiscount,
            BigDecimal requestFreight,
            Boolean isVat
    ) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (UpdateSalesOrderDetailRequest item : Optional.ofNullable(items).orElseGet(List::of)) {
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
            vat = taxableAmount.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal grandTotal = taxableAmount.add(vat).add(freight).setScale(2, RoundingMode.HALF_UP);
        return new SalesOrderSummary(subTotal.setScale(2, RoundingMode.HALF_UP), vat, grandTotal);
    }

    private void linkRfq(String rfqId, String salesOrderNo, String userId, ZonedDateTime updatedDate)
            throws DataNotFoundException, InvalidRequestException {
        if (StringUtils.isBlank(rfqId)) {
            return;
        }

        RfqHeaderEntity rfq = requestPriceHeaderRepository.findById(rfqId)
                .orElseThrow(() -> new DataNotFoundException("RFQ " + rfqId + " not found."));
        if (StringUtils.isNotBlank(rfq.getSaleOrderId()) && !salesOrderNo.equals(rfq.getSaleOrderId())) {
            throw new InvalidRequestException("RFQ " + rfqId + " already linked to sale order " + rfq.getSaleOrderId());
        }

        rfq.setSaleOrderId(salesOrderNo);
        rfq.setUpdatedBy(userId);
        rfq.setUpdatedDate(updatedDate);
        requestPriceHeaderRepository.save(rfq);
    }

    private void recordCreateSalesOrderActivity(SalesOrderEntity entity, CreateSalesOrderRequest request, String userId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("rfqId", request.getRfqId());
        detail.put("status", entity.getStatus());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("shippingType", entity.getShippingType());
        detail.put("coSaleCommission", entity.getCoSaleCommission());
        detail.put("requestCoa", entity.getRequestCoa());
        detail.put("requestPo", entity.getRequestPo());
        detail.put("procurementStatus", entity.getProcurementStatus());
        detail.put("itemCount", entity.getItems() != null ? entity.getItems().size() : 0);
        detail.put("attachmentCount", entity.getAttachments() != null ? entity.getAttachments().size() : 0);
        detail.put("subTotal", entity.getSubTotal());
        detail.put("vat", entity.getVat());
        detail.put("grandTotal", entity.getGrandTotal());

        String summary = SalesOrderStatus.DRAFT.equals(entity.getStatus()) ? "บันทึกฉบับร่าง Sales Order เลขที่ " + entity.getSalesOrderNo() : "สร้าง Sales Order เลขที่ " + entity.getSalesOrderNo();

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                entity.getSalesOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้าง Sales Order เลขที่ " + entity.getSalesOrderNo(),
                detail
        );
    }

    private void recordUpdateSalesOrderActivity(
            SalesOrderEntity entity,
            UpdateSalesOrderRequest request,
            String userId,
            Integer oldRevNo,
            Map<String, Object> before
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", buildSalesOrderSnapshot(entity));
        detail.put("oldRevNo", oldRevNo);
        detail.put("newRevNo", entity.getRevNo());
        detail.put("itemsUpdated", request.getItems() != null);

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                entity.getSalesOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "แก้ไข Sales Order เลขที่ " + entity.getSalesOrderNo(),
                detail
        );
    }

    private Map<String, Object> buildSalesOrderSnapshot(SalesOrderEntity entity) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("salesOrderNo", entity.getSalesOrderNo());
        detail.put("docDate", entity.getDocDate());
        detail.put("expireDate", entity.getExpireDate());
        detail.put("status", entity.getStatus());
        detail.put("customerId", entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        detail.put("salesId", entity.getSales() != null ? entity.getSales().getEmployeeId() : null);
        detail.put("coSaleId", entity.getCoSalesId());
        detail.put("discount", entity.getDiscount());
        detail.put("freight", entity.getFreight());
        detail.put("coSaleCommission", entity.getCoSaleCommission());
        detail.put("shippingType", entity.getShippingType());
        detail.put("requestCoa", entity.getRequestCoa());
        detail.put("requestPo", entity.getRequestPo());
        detail.put("procurementStatus", entity.getProcurementStatus());
        detail.put("vatRate", entity.getVatRate());
        detail.put("remark", entity.getRemark());
        detail.put("subTotal", entity.getSubTotal());
        detail.put("vat", entity.getVat());
        detail.put("grandTotal", entity.getGrandTotal());
        detail.put("itemCount", entity.getItems() != null ? entity.getItems().size() : 0);
        detail.put("attachmentCount", entity.getAttachments() != null ? entity.getAttachments().size() : 0);
        return detail;
    }

    private void attachFiles(
            SalesOrderEntity entity,
            List<MultipartFile> attachments,
            UserEntity user,
            ZonedDateTime now
    ) throws Exception {
        int nextSortOrder = entity.getAttachments().stream()
                .filter(attachment -> Boolean.TRUE.equals(attachment.getActive()))
                .map(SalesOrderAttachmentEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }

            UploadFileResponse upload = fileStorageService.uploadFile(attachment);
            SalesOrderAttachmentEntity attachmentEntity = new SalesOrderAttachmentEntity();
            attachmentEntity.setFileName(upload.getFileName());
            attachmentEntity.setOriginalFileName(StringUtils.trimToNull(attachment.getOriginalFilename()));
            attachmentEntity.setFileUrl(upload.getUrl());
            attachmentEntity.setContentType(StringUtils.trimToNull(upload.getContentType()));
            attachmentEntity.setFileSize(attachment.getSize());
            attachmentEntity.setSortOrder(nextSortOrder++);
            attachmentEntity.setActive(Boolean.TRUE);
            attachmentEntity.setCreatedBy(user);
            attachmentEntity.setUpdatedBy(user);
            attachmentEntity.setCreatedDate(now);
            attachmentEntity.setUpdatedDate(now);
            entity.addAttachment(attachmentEntity);
        }
    }

    private String generateSalesOrderNo() {
        return generatedIdSequenceService.getNextIdWithMonth(SALES_ORDER_PREFIX, 6);
    }

    private CustomerEntity resolveCustomer(String input) throws DataNotFoundException {
        return customerRepository.findById(input)
                .orElseThrow(() -> new DataNotFoundException("Customer " + input + " not found."));
    }

    private CustomerAddressEntity resolveCustomerAddress(CustomerEntity customer, String customerAddressId)
            throws DataNotFoundException {
        return customer.getAddresses().stream()
                .filter(address -> StringUtils.isBlank(customerAddressId)
                        ? Boolean.TRUE.equals(address.getIsDefault())
                        : customerAddressId.equals(address.getId().toString()))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Customer address " + customerAddressId + " not found."));
    }

    private CustomerContactEntity resolveCustomerContact(CustomerEntity customer, String customerContactId)
            throws DataNotFoundException {
        return customer.getContacts().stream()
                .filter(contact -> StringUtils.isBlank(customerContactId)
                        ? Boolean.TRUE.equals(contact.getIsDefault())
                        : customerContactId.equals(contact.getId().toString()))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Customer contact " + customerContactId + " not found."));
    }

    private EmployeeEntity resolveSales(String input) throws DataNotFoundException {
        return employeeRepository.findById(input)
                .orElseThrow(() -> new DataNotFoundException("Internal sale " + input + " not found."));
    }

    private SupplierEntity resolveSupplier(String input) throws DataNotFoundException {
        if (StringUtils.isBlank(input)) {
            throw new DataNotFoundException("Supplier is required.");
        }

        return supplierRepository.findById(input)
                .orElseThrow(() -> new DataNotFoundException("Supplier " + input + " not found."));
    }

    private String normalizeShippingType(String shippingType) {
        return StringUtils.isBlank(shippingType) ? null : shippingType.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultRevNo(Integer value) {
        return value == null ? 0 : value;
    }

    private SalesOrderDto mapToDto(SalesOrderEntity entity) {
        SalesOrderDto dto = new SalesOrderDto();
        dto.setSalesOrderNo(entity.getSalesOrderNo());
        dto.setDocDate(entity.getDocDate() != null ? entity.getDocDate().format(DateUtil.DD_MM_YY) : null);
        dto.setExpireDate(entity.getExpireDate() != null ? entity.getExpireDate().format(DateUtil.DD_MM_YY) : null);
        dto.setStatus(entity.getStatus());
        dto.setStatusProfile(DocumentStatusResolver.resolveSalesOrder(entity.getStatus()));
        dto.setCurrency(entity.getCurrency());
        dto.setCustomer(customerMapper.toDto(entity.getCustomer()));
        dto.setCustomerAddress(customerMapper.toAddressDto(entity.getCustomerAddress()));
        dto.setCustomerContact(customerMapper.toContactDto(entity.getCustomerContact()));
        dto.setSaleAccount(employeeMapper.toDto(entity.getSales()));
        dto.setCoSaleId(entity.getCoSalesId());
        dto.setSubTotal(entity.getSubTotal());
        dto.setDiscount(entity.getDiscount());
        dto.setFreight(entity.getFreight());
        dto.setVat(entity.getVat());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setAmount(entity.getAmount());
        if (entity.getCommission() != null) {
            dto.setCommission(entity.getCommission());
        } else {
            BigDecimal fallbackCommission = entity.getItems().stream()
                    .findFirst()
                    .map(SalesOrderDetailEntity::getRfqTierId)
                    .flatMap(requestPriceTierRepository::findById)
                    .map(RfqTierEntity::getCommission)
                    .orElse(null);
            dto.setCommission(fallbackCommission);
        }
        dto.setCoSaleCommission(entity.getCoSaleCommission());
        dto.setProcurementStatus(entity.getProcurementStatus());
        dto.setShippingType(entity.getShippingType());
        dto.setRequestCoa(Boolean.TRUE.equals(entity.getRequestCoa()));
        dto.setRequestPo(Boolean.TRUE.equals(entity.getRequestPo()));
        dto.setVatRate(entity.getVatRate());
        dto.setRemark(entity.getRemark());
        dto.setRfqId(resolveRfq(entity.getSalesOrderNo()));
        dto.setCreatedBy(userMapper.toDto(entity.getCreatedBy()));
        dto.setUpdatedBy(userMapper.toDto(entity.getUpdatedBy()));
        dto.setRevNo(entity.getRevNo());

        List<SalesOrderAttachmentDto> attachments = new ArrayList<>();
        for (SalesOrderAttachmentEntity attachment : entity.getAttachments()) {
            if (!Boolean.TRUE.equals(attachment.getActive())) {
                continue;
            }
            SalesOrderAttachmentDto attachmentDto = new SalesOrderAttachmentDto();
            attachmentDto.setId(attachment.getId());
            attachmentDto.setSalesOrderNo(entity.getSalesOrderNo());
            attachmentDto.setFileName(attachment.getFileName());
            attachmentDto.setOriginalFileName(attachment.getOriginalFileName());
            attachmentDto.setFileUrl(attachment.getFileUrl());
            attachmentDto.setContentType(attachment.getContentType());
            attachmentDto.setFileSize(attachment.getFileSize());
            attachmentDto.setRemark(attachment.getRemark());
            attachmentDto.setSortOrder(attachment.getSortOrder());
            attachments.add(attachmentDto);
        }
        dto.setAttachments(attachments);

        List<SalesOrderDetailDto> items = new ArrayList<>();
        for (SalesOrderDetailEntity detail : entity.getItems()) {
            SalesOrderDetailDto item = new SalesOrderDetailDto();
            item.setId(detail.getId());
            item.setLineNo(detail.getLineNo());
            item.setSupplier(supplierMapper.toDto(detail.getSupplier()));
            item.setName(detail.getName());
            item.setType(detail.getType());
            item.setCapacity(detail.getCapacity());
            item.setSize(detail.getSize());
            item.setSpec(detail.getSpec());
            item.setUnitPrice(detail.getUnitPrice());
            item.setQuantity(detail.getQuantity());
            item.setAmount(detail.getAmount());
            item.setImageUrl(detail.getImageUrl());
            item.setRfqDetailId(detail.getRfqDetailId());
            item.setRfqTierId(detail.getRfqTierId());
            item.setQuotationDetailId(detail.getQuotationDetailId());
            item.setShippingMethod(detail.getShippingMethod());
            item.setSupplierCurrency(detail.getSupplierCurrency());
            item.setSupplierUnitPrice(detail.getSupplierUnitPrice());
            item.setSupplierShippingCost(detail.getSupplierShippingCost());
            item.setSupplierTotalUnitCost(detail.getSupplierTotalUnitCost());
            item.setSupplierQuoteTierId(detail.getSupplierQuoteTierId());
            items.add(item);
        }
        dto.setItems(items);
        return dto;
    }

    @NotNull
    private static List<SalesOrderItemDocumentDto> getItemDocumentDtos(SalesOrderEntity salesOrderEntity) {
        List<SalesOrderItemDocumentDto> itemDocuments = new ArrayList<>();
        for (SalesOrderDetailEntity detail : salesOrderEntity.getItems()) {
            SalesOrderItemDocumentDto item = new SalesOrderItemDocumentDto();

            if (StringUtils.isNotEmpty(detail.getImageUrl())) {
                item.setImage(loadImageAsInputStream(detail.getImageUrl()));
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
        while (itemDocuments.size() < 5) {
            itemDocuments.add(new SalesOrderItemDocumentDto());
        }

        return itemDocuments;
    }

    private static InputStream loadImageAsInputStream(String imageUrl) {
        try {
            return new URL(imageUrl).openStream();
        } catch (Exception e) {
            log.warn("Cannot load image from url: {}", imageUrl, e);
            return null;
        }
    }

    private String resolveRfq(String salesOrderNo) {
        return requestPriceHeaderRepository.findFirstBySaleOrderId(salesOrderNo)
                .map(RfqHeaderEntity::getId)
                .orElse(null);
    }
}
