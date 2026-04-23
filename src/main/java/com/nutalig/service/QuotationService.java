package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.constant.*;
import com.nutalig.constant.Currency;
import com.nutalig.controller.customer.response.SearchCustomerResponse;
import com.nutalig.controller.quotation.request.SearchQuotationRequest;
import com.nutalig.controller.quotation.response.SearchQuotationResponse;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.*;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.dto.document.QuotationDocumentDto;
import com.nutalig.dto.document.QuotationItemDocumentDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.mapper.CustomerMapper;
import com.nutalig.repository.*;
import com.nutalig.repository.specification.QuotationSpecification;
import com.nutalig.utils.DateUtil;
import com.nutalig.utils.PdfMergeUtil;
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

import static com.nutalig.repository.specification.QuotationSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationService {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.07");

    private final GeneratedIdSequenceService generatedIdSequenceService;
    private final LineMessageService lineMessageService;
    private final ReportService reportService;
    private final QuotationRepository quotationRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SalesRepository salesRepository;
    private final SystemConfigService systemConfigService;
    private final CustomerMapper customerMapper;
    private final ObjectMapper objectMapper;

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
                    .get();
        } else {
            customerAddressEntity = customerEntity.getAddresses()
                    .stream()
                    .filter(addr -> addr.getId().toString().equals(requestDto.getCustomerAddressId()))
                    .findFirst()
                    .get();
        }
        CustomerContactEntity customerContactEntity = null;
        if (StringUtils.isEmpty(requestDto.getCustomerContactId())) {
            customerContactEntity = customerEntity.getContacts()
                    .stream()
                    .filter(con -> Boolean.TRUE.equals(con.getIsDefault()))
                    .findFirst()
                    .get();
        } else {
            customerContactEntity = customerEntity.getContacts()
                    .stream()
                    .filter(con -> requestDto.getCustomerContactId().equals(con.getId().toString()))
                    .findFirst()
                    .get();
        }

        UserEntity createdByEntity = userRepository.findById(createdBy)
                .orElseThrow(() -> new DataNotFoundException("User " + createdBy + " not found."));

        SalesEntity salesEntity = resolveSales(requestDto.getSalesId());

        String nextId = generatedIdSequenceService.getNextSequence(customerEntity.getId(), 3);
        String docId = customerEntity.getId() + "/" + nextId;
        log.info("Create quotation with id : {}", docId);

        QuotationEntity quotationEntity = new QuotationEntity();
        quotationEntity.setQuotationNo(docId);

        LocalDate today = LocalDate.now(DateUtil.getTimeZone());
        quotationEntity.setDocDate(requestDto.getDocDate() == null ? today : requestDto.getDocDate());
        quotationEntity.setExpireDate(requestDto.getEffectiveDate() == null ? today.plusDays(7) : requestDto.getEffectiveDate());
        quotationEntity.setStatus(QuotationStatus.DRAFT);
        quotationEntity.setCurrency(Currency.THB);
        quotationEntity.setCustomer(customerEntity);
        quotationEntity.setCustomerAddress(customerAddressEntity);
        quotationEntity.setCustomerContact(customerContactEntity);
        quotationEntity.setSales(salesEntity);
        quotationEntity.setCoSalesId(requestDto.getCoSaleId());
        quotationEntity.setRemark(requestDto.getRemark());

        QuotationSummary summary = calculate(requestDto);
        quotationEntity.setDiscount(requestDto.getDiscount());
        quotationEntity.setFreight(requestDto.getFreight());
        quotationEntity.setSubTotal(summary.subTotal);
        quotationEntity.setVat(summary.vat);
        quotationEntity.setGrandTotal(summary.grandTotal);
        quotationEntity.setVatRate(requestDto.getIsVat() ? VAT_RATE : BigDecimal.ZERO);

        int lineNo = 1;
        for (QuotationItemRequestDto itemRequest : requestDto.getItems()) {
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

            BigDecimal amount = unitPrice
                    .multiply(quantity)
                    .setScale(2, RoundingMode.HALF_UP);

            detailEntity.setUnitPrice(unitPrice);
            detailEntity.setQuantity(quantity);
            detailEntity.setAmount(amount);
            detailEntity.setImageUrl(itemRequest.getImageUrl());

            quotationEntity.getItems().add(detailEntity);
        }

        quotationEntity.setCreatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        quotationEntity.setCreatedBy(createdByEntity);
        quotationEntity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        quotationEntity.setUpdatedBy(createdByEntity);

        quotationRepository.save(quotationEntity);

        return quotationEntity;
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
    public DownloadDocumentDto getQuotationDocumentById(String quotationNo, DocumentRequest documentRequest) throws Exception {
        log.info("Get quotation by {}", quotationNo);

        QuotationEntity quotationEntity = quotationRepository.findById(quotationNo)
                .orElseThrow(() -> new DataNotFoundException("Quotation " + quotationNo + " not found."));

        String fileName = quotationEntity.getQuotationNo();
        if (documentRequest.getFormat().equals(ExportFileFormat.PDF)) {
            List<byte[]> pdfBytesList = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                pdfBytesList.add((byte[]) reportService.getQuotationDocument(buildQuotationDocumentDto(quotationEntity, Boolean.FALSE), documentRequest.getFormat()));
            }
            if (documentRequest.getIsCopy()) {
                pdfBytesList.add((byte[]) reportService.getQuotationDocument(buildQuotationDocumentDto(quotationEntity, Boolean.TRUE), documentRequest.getFormat()));
            }

            byte[] mergedPdf = PdfMergeUtil.merge(pdfBytesList);
            return new DownloadDocumentDto(fileName, documentRequest.getFormat(), List.of(new DownloadDocumentDto.FileItem(fileName + "." + documentRequest.getFormat(), Base64.getEncoder().encodeToString(mergedPdf) , "application/pdf")));
        } else if (documentRequest.getFormat().equals(ExportFileFormat.JPG)) {
            List<byte[]> pages = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                List<byte[]> originalPages = (List<byte[]>) reportService.getQuotationDocument(buildQuotationDocumentDto(quotationEntity, Boolean.FALSE), documentRequest.getFormat());
                pages.addAll(originalPages);
            }
            if (documentRequest.getIsCopy()) {
                List<byte[]> copyPages = (List<byte[]>) reportService.getQuotationDocument(buildQuotationDocumentDto(quotationEntity, Boolean.TRUE), documentRequest.getFormat());
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

    private QuotationDocumentDto  buildQuotationDocumentDto(QuotationEntity quotationEntity, Boolean aFalse) {
        QuotationDocumentDto dto = new QuotationDocumentDto();
        dto.setDocNo(quotationEntity.getQuotationNo());
        dto.setDocDate(quotationEntity.getDocDate().format(DateUtil.DD_MM_YY));
        dto.setIsCopy(aFalse);
        dto.setDiscount(quotationEntity.getDiscount());
        dto.setGrandTotal(quotationEntity.getGrandTotal());
        dto.setFreight(quotationEntity.getFreight());
        dto.setSubTotal(quotationEntity.getSubTotal());
        dto.setVat(quotationEntity.getVat());
        dto.setRemark(quotationEntity.getRemark());
        dto.setThaiBahtText(ThaiBahtText.convertBahtText(quotationEntity.getGrandTotal()));
        dto.setCustName(quotationEntity.getCustomer().getCustomerName());
        dto.setCustTaxId(quotationEntity.getCustomer().getTaxId());
        dto.setCustAddress(buildFullAddress(quotationEntity.getCustomerAddress()));
        dto.setCustMobileNo(quotationEntity.getCustomerContact().getContactNumber());
        dto.setSalesId(quotationEntity.getSales().getSalesId());
        dto.setSalesName(quotationEntity.getSales().getName());
        dto.setSalesMobileNo(quotationEntity.getSales().getMobileNo());
        dto.setSalesNickname(quotationEntity.getSales().getNickname());
        dto.setCoSalesId(quotationEntity.getCoSalesId());

        if (quotationEntity.getVatRate().compareTo(BigDecimal.ZERO) == 0) {
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

        append(sb, address.getPostcode());

        return sb.toString().trim();
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
    private static List<QuotationItemDocumentDto> getItemDocumentDtos(QuotationEntity quotationEntity) {
        List<QuotationItemDocumentDto> itemDocuments = new ArrayList<>();
        for (QuotationDetailEntity detail : quotationEntity.getItems()) {
            QuotationItemDocumentDto item = new QuotationItemDocumentDto();

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
        while (itemDocuments.size() < 7) {
            itemDocuments.add(new QuotationItemDocumentDto());
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

    private QuotationSummary calculate(QuotationRequestDto request) {

        BigDecimal subTotal = BigDecimal.ZERO;

        for (QuotationItemRequestDto item : request.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(item.getQuantity());
            subTotal = subTotal.add(lineTotal);
        }

        BigDecimal discount = defaultIfNull(request.getDiscount());
        BigDecimal freight = defaultIfNull(request.getFreight());

        BigDecimal taxableAmount = subTotal.subtract(discount);

        if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        BigDecimal vat = BigDecimal.ZERO;

        if (Boolean.TRUE.equals(request.getIsVat())) {
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

    private SalesEntity resolveSales(String input) throws DataNotFoundException {

        if (input == null || input.isBlank()) {
            throw new DataNotFoundException("Sales id is empty");
        }

        // 1️⃣ search by salesId
        Optional<SalesEntity> byId =
                salesRepository.findBySalesIdAndType(input, SalesType.INTERNAL_SALES);

        if (byId.isPresent()) {
            return byId.get();
        }

        // 2️⃣ search by nickname
        Optional<SalesEntity> byNickname =
                salesRepository.findFirstByNicknameContainingIgnoreCaseAndType(input, SalesType.INTERNAL_SALES);

        if (byNickname.isPresent()) {
            return byNickname.get();
        }

        // 3️⃣ search by name
        Optional<SalesEntity> byName =
                salesRepository.findFirstByNameContainingIgnoreCaseAndType(input, SalesType.INTERNAL_SALES);

        if (byName.isPresent()) {
            return byName.get();
        }

        throw new DataNotFoundException("Internal sale " + input + " not found.");
    }

    private Specification<QuotationEntity> buildSearchCriteria(SearchQuotationRequest searchQuotationRequest) {
        Specification<QuotationEntity> specification = Specification.where(null);
        return specification
                .and(docNoEqual(searchQuotationRequest.getDocNoEqual()))
                .and(customerIdEqual(searchQuotationRequest.getCustomerIdEqual()))
                .and(statusEqual(searchQuotationRequest.getStatusEqual()))
                .and(docDateBetween(searchQuotationRequest.getDocDateStart(), searchQuotationRequest.getDocDateEnd()));
    }

    private QuotationDto mapToDto(QuotationEntity entity) {
        CustomerDto customerDto = customerMapper.toDto(entity.getCustomer());
        CustomerAddressDto customerAddressDto = customerMapper.toAddressDto(entity.getCustomerAddress());
        CustomerContactDto customerContactDto = customerMapper.toContactDto(entity.getCustomerContact());

        QuotationDto dto = new QuotationDto();
        dto.setDocDate(entity.getDocDate().format(DateUtil.DD_MM_YY));
        dto.setEffectiveDate(entity.getExpireDate().format(DateUtil.DD_MM_YY));
        dto.setCustomer(customerDto);
        dto.setCustomerAddress(customerAddressDto);
        dto.setCustomerContact(customerContactDto);
        dto.setCoSaleId(entity.getCoSalesId());
        dto.setQuotationNo(entity.getQuotationNo());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());
        dto.setDiscount(entity.getDiscount());
        dto.setFreight(entity.getFreight());
        dto.setSubTotal(entity.getSubTotal());
        dto.setVat(entity.getVat());
        dto.setGrandTotal(entity.getGrandTotal());

        List<QuotationItemRequestDto> items = new ArrayList<>();
        for (QuotationDetailEntity detail : entity.getItems()) {
            QuotationItemRequestDto item = new QuotationItemRequestDto();
            item.setId(detail.getId().toString());
            item.setName(detail.getName());
            item.setImageUrl(detail.getImageUrl());
            item.setSpec(detail.getSpec());
            item.setSize(detail.getSize());
            item.setType(detail.getType());
            item.setCapacity(detail.getCapacity());
            item.setUnitPrice(detail.getUnitPrice());
            item.setQuantity(detail.getQuantity());
            items.add(item);
        }
        dto.setItems(items);
        return dto;
    }
}
