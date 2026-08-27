package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.controller.customer.request.*;
import com.nutalig.controller.customer.response.SearchCustomerResponse;
import com.nutalig.controller.customer.response.UploadCustomerErrorResponse;
import com.nutalig.controller.customer.response.UploadCustomerResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.CustomerDashboardBreakdownDto;
import com.nutalig.dto.CustomerDashboardDto;
import com.nutalig.dto.CustomerDto;
import com.nutalig.dto.UserDto;
import com.nutalig.entity.CustomerAddressEntity;
import com.nutalig.entity.CustomerContactEntity;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.CustomerMapper;
import com.nutalig.repository.InvoiceRepository;
import com.nutalig.repository.CustomerRepository;
import com.nutalig.repository.QuotationRepository;
import com.nutalig.repository.ReceiptRepository;
import com.nutalig.repository.SalesOrderRepository;
import com.nutalig.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

import static com.nutalig.repository.specification.CustomerSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final CustomerMapper customerMapper;
    private final UserProfileService userProfileService;
    private final ActivityHistoryService activityHistoryService;

    @Transactional
    public String createCustomer(CreateCustomerRequest request, String userId) throws InvalidRequestException {
        log.info("Create customer {} by {}", request.getCustomerName(), userId);

        CustomerEntity entity = customerMapper.toEntity(request);

        if (StringUtils.isNotBlank(request.getCustomerType())) {
            SystemConfigEntity customerType = systemConfigRepository
                    .findByIdGroupCodeAndIdCode(SystemConstant.CUSTOMER_TYPE, request.getCustomerType())
                    .orElseThrow(() -> new InvalidRequestException(
                            "Config " + request.getCustomerType() + " not found."
                    ));
            entity.setCustomerType(customerType);
        }

        if (StringUtils.isNotBlank(request.getCreditTerm())) {
            SystemConfigEntity customerCreditTerm = systemConfigRepository
                    .findByIdGroupCodeAndIdCode(SystemConstant.CUSTOMER_CREDIT_TERM, request.getCreditTerm())
                    .orElseThrow(() -> new InvalidRequestException(
                            "Config " + request.getCreditTerm() + " not found."
                    ));
            entity.setCustomerCreditTerm(customerCreditTerm);
        }

        if (StringUtils.isNotBlank(request.getPaymentTerm())) {
            SystemConfigEntity customerPaymentTerm = systemConfigRepository
                    .findByIdGroupCodeAndIdCode(SystemConstant.CUSTOMER_PAYMENT_TERM, request.getPaymentTerm())
                    .orElseThrow(() -> new InvalidRequestException(
                            "Config " + request.getPaymentTerm() + " not found."
                    ));
            entity.setCustomerPaymentTerm(customerPaymentTerm);
        }

        entity.setCustomerBillingCondition(StringUtils.trimToNull(request.getBillingCondition()));
        entity.setCustomerPaymentCycle(StringUtils.trimToNull(request.getPaymentCycle()));

        if (StringUtils.isNotBlank(request.getCustomerSegment())) {
            SystemConfigEntity customerSegment = systemConfigRepository
                    .findByIdGroupCodeAndIdCode(SystemConstant.CUSTOMER_SEGMENT, request.getCustomerSegment())
                    .orElseThrow(() -> new InvalidRequestException(
                            "Config " + request.getCustomerSegment() + " not found."
                    ));
            entity.setCustomerSegment(customerSegment);
        } else {
            SystemConfigEntity customerSegment = systemConfigRepository
                    .findByIdGroupCodeAndIdCode(SystemConstant.CUSTOMER_SEGMENT, "RETAIL")
                    .orElseThrow(() -> new InvalidRequestException(
                            "Config " + request.getCustomerSegment() + " not found."
                    ));
            entity.setCustomerSegment(customerSegment);
        }

        if (StringUtils.isNotBlank(request.getCustomerTier())) {
            SystemConfigEntity customerTier = systemConfigRepository
                    .findByIdGroupCodeAndIdCode(SystemConstant.CUSTOMER_TIER, request.getCustomerTier())
                    .orElseThrow(() -> new InvalidRequestException(
                            "Config " + request.getCustomerSegment() + " not found."
                    ));
            entity.setCustomerTier(customerTier);
        } else {
            SystemConfigEntity customerTier = systemConfigRepository
                    .findByIdGroupCodeAndIdCode(SystemConstant.CUSTOMER_TIER, "TIER_4")
                    .orElseThrow(() -> new InvalidRequestException(
                            "Config " + request.getCustomerSegment() + " not found."
                    ));
            entity.setCustomerTier(customerTier);
        }

        entity.setStatus(Status.ACTIVE);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        applySalesAccounts(entity, request.getSalesAccounts(), request.getSalesAccount());

        // address
        if (request.getAddress() != null) {
            CustomerAddressEntity address = getCustomerAddressEntity(request);

            entity.addAddress(address);
        }

        // contacts
        if (CollectionUtils.isNotEmpty(request.getContacts())) {
            for (CreateCustomerContactRequest contactReq : request.getContacts()) {
                if (StringUtils.isAllBlank(contactReq.getContactName(), contactReq.getContactNumber())) {
                    continue;
                }

                CustomerContactEntity contact = new CustomerContactEntity();
                contact.setContactName(contactReq.getContactName());
                contact.setContactNumber(contactReq.getContactNumber());

                entity.addContact(contact);
            }
        }

        entity = customerRepository.save(entity);

        activityHistoryService.record(
                ActivityEntityType.CUSTOMER,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้างลูกค้า " + entity.getId(),
                buildCustomerHistorySnapshot(entity)
        );
        recordCustomerAddressCreateHistory(entity, userId, "สร้างที่อยู่ลูกค้า ");
        recordCustomerContactCreateHistory(entity, userId, "สร้างผู้ติดต่อของลูกค้า ");

        log.info("Create customer {} with id : {}", request.getCustomerName(), entity.getId());
        return entity.getId();
    }

    @Transactional
    public UploadCustomerResponse uploadCustomers(MultipartFile file, String userId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Customer file is required.");
        }

        log.info("Upload customers from file {} by {}", file.getOriginalFilename(), userId);

        UploadCustomerResponse response = new UploadCustomerResponse();
        List<UploadCustomerErrorResponse> errors = new ArrayList<>();
        int totalRows = 0;
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new InvalidRequestException("Customer excel sheet not found.");
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new InvalidRequestException("Customer excel header row not found.");
            }

            Map<String, Integer> headerIndexMap = buildHeaderIndexMap(headerRow, evaluator);
            validateCustomerHeaders(headerIndexMap);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankCustomerRow(row, evaluator, headerIndexMap)) {
                    continue;
                }

                totalRows++;

                try {
                    String customerName = readCell(row, headerIndexMap, evaluator, "customer_name");
                    log.info("Processing row {} for customer {}", rowIndex, customerName);
                    if (StringUtils.isBlank(customerName)) {
                        throw new InvalidRequestException("customer_name is required.");
                    }

//                    CustomerEntity existingCustomer = findExistingCustomer(row, headerIndexMap, evaluator);
//                    if (existingCustomer != null) {
//                        skippedCount++;
//                        continue;
//                    }

                    CustomerEntity entity = buildCustomerEntityFromRow(row, headerIndexMap, evaluator, userId);
                    customerRepository.saveAndFlush(entity);
                    activityHistoryService.record(
                            ActivityEntityType.CUSTOMER,
                            entity.getId(),
                            userId,
                            ActivityActorType.USER,
                            ActivityAction.CREATE,
                            ActivitySource.API,
                            "อัปโหลดลูกค้า " + entity.getId(),
                            Map.of(
                                    "source", "UPLOAD_CUSTOMER_EXCEL",
                                    "rowNumber", rowIndex + 1,
                                    "customer", buildCustomerHistorySnapshot(entity)
                            )
                    );
//                    recordCustomerAddressCreateHistory(entity, userId, "อัปโหลดที่อยู่ลูกค้า ");
                    recordCustomerContactCreateHistory(entity, userId, "อัปโหลดผู้ติดต่อของลูกค้า ");
                    createdCount++;
                } catch (Exception ex) {
                    failedCount++;
                    errors.add(new UploadCustomerErrorResponse(
                            rowIndex + 1,
                            StringUtils.defaultString(readCell(row, headerIndexMap, evaluator, "customer_name"), "-"),
                            ex.getMessage()
                    ));
                    log.warn("Skip customer row {} because {}", rowIndex + 1, ex.getMessage());
                }
            }
        }

        response.setTotalRows(totalRows);
        response.setCreatedCount(createdCount);
        response.setSkippedCount(skippedCount);
        response.setFailedCount(failedCount);
        response.setErrors(errors);
        log.info("Upload customers completed created={} skipped={} failed={}", response.getCreatedCount(), response.getSkippedCount(), response.getFailedCount());
        return response;
    }

    @Transactional
    public SearchCustomerResponse searchCustomer(
            SearchCustomerRequest searchCustomerRequest,
            PageableRequest pageableRequest,
            Authentication authentication
    ) {
        log.info("Search customer by criteria(s) {}", searchCustomerRequest);

        pageableRequest.setSortBy("id");
        pageableRequest.setSortDirection(Sort.Direction.DESC);
        Pageable pageable = pageableRequest.build();

        Page<CustomerEntity> customerEntityPage = customerRepository.findAll(buildSearchCriteria(searchCustomerRequest), pageable);
        Page<CustomerDto> customerDtoPage = customerEntityPage.map(customerMapper::toDto);
        List<CustomerDto> customerDtoList = customerDtoPage.getContent();
        applySalesVisibleOrderTotals(customerDtoList, authentication);

        log.info("Search customer size : {}", customerDtoPage.getTotalElements());

        SearchCustomerResponse response = new SearchCustomerResponse();
        response.setCustomers(customerDtoList);
        response.setPagination(Pagination.build(customerDtoPage));

        return response;
    }

    @Transactional
    public CustomerDto getCustomerById(String custId) throws DataNotFoundException {
        log.info("Get customer by id : {}", custId);

        CustomerEntity entity = customerRepository.findById(custId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + custId + " not found."));

        return customerMapper.toDto(entity);
    }

    @Transactional
    public CustomerDto updateCustomer(String customerId, UpdateCustomerRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        log.info("Update customer {} request {}", customerId, request);

        CustomerEntity entity = customerRepository.findById(customerId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));

        if (request == null) {
            entity.setUpdatedBy(userId);
            entity = customerRepository.save(entity);
            activityHistoryService.record(
                    ActivityEntityType.CUSTOMER,
                    entity.getId(),
                    userId,
                    ActivityActorType.USER,
                    ActivityAction.UPDATE,
                    ActivitySource.API,
                    "แก้ไขลูกค้า " + entity.getId(),
                    buildCustomerHistorySnapshot(entity)
            );
            return customerMapper.toDto(entity);
        }

        Map<String, Object> beforeDetail = buildCustomerHistorySnapshot(entity);

        if (request.getCustomerType() != null) {
            entity.setCustomerType(getOptionalSystemConfig(
                    SystemConstant.CUSTOMER_TYPE,
                    request.getCustomerType(),
                    "Customer type"
            ));
        }
        if (request.getCustomerTier() != null) {
            entity.setCustomerTier(getOptionalSystemConfig(
                    SystemConstant.CUSTOMER_TIER,
                    request.getCustomerTier(),
                    "Customer tier"
            ));
        }
        if (request.getCustomerSegment() != null) {
            entity.setCustomerSegment(getOptionalSystemConfig(
                    SystemConstant.CUSTOMER_SEGMENT,
                    request.getCustomerSegment(),
                    "Customer segment"
            ));
        }
        if (request.getCustomerName() != null) {
            entity.setCustomerName(StringUtils.trimToNull(request.getCustomerName()));
        }
        if (request.getEmail() != null) {
            entity.setEmail(StringUtils.trimToNull(request.getEmail()));
        }
        if (request.getTaxId() != null) {
            entity.setTaxId(StringUtils.trimToNull(request.getTaxId()));
        }
        if (request.getBranchNumber() != null) {
            entity.setBranchNumber(StringUtils.trimToNull(request.getBranchNumber()));
        }
        if (request.getBranchName() != null) {
            entity.setBranchName(StringUtils.trimToNull(request.getBranchName()));
        }
        if (request.getCreditTerm() != null) {
            entity.setCustomerCreditTerm(getOptionalSystemConfig(
                    SystemConstant.CUSTOMER_CREDIT_TERM,
                    request.getCreditTerm(),
                    "Customer credit term"
            ));
        }
        if (request.getPaymentTerm() != null) {
            entity.setCustomerPaymentTerm(getOptionalSystemConfig(
                    SystemConstant.CUSTOMER_PAYMENT_TERM,
                    request.getPaymentTerm(),
                    "Customer payment term"
            ));
        }
        if (request.getBillingCondition() != null) {
            entity.setCustomerBillingCondition(StringUtils.trimToNull(request.getBillingCondition()));
        }
        if (request.getPaymentCycle() != null) {
            entity.setCustomerPaymentCycle(StringUtils.trimToNull(request.getPaymentCycle()));
        }
        if (request.getSalesAccount() != null) {
            applySalesAccounts(entity, request.getSalesAccounts(), request.getSalesAccount());
        } else if (request.getSalesAccounts() != null) {
            applySalesAccounts(entity, request.getSalesAccounts(), null);
        }
        if (request.getCoSalesAccount() != null) {
            entity.setCoSalesAccount(StringUtils.trimToNull(request.getCoSalesAccount()));
        }

        entity.setUpdatedBy(userId);
        entity = customerRepository.save(entity);

        Map<String, Object> afterDetail = buildCustomerHistorySnapshot(entity);
        Map<String, Object> updateDetail = new LinkedHashMap<>();
        updateDetail.put("before", beforeDetail);
        updateDetail.put("after", afterDetail);

        activityHistoryService.record(
                ActivityEntityType.CUSTOMER,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "แก้ไขลูกค้า " + entity.getId(),
                updateDetail
        );

        log.info("Update customer success id {}", customerId);
        return customerMapper.toDto(entity);
    }

    @Transactional
    public CustomerDto deleteCustomerAddress(String customerId, Long addressId)
            throws DataNotFoundException, InvalidRequestException {
        log.info("Delete customer address {} from customer {}", addressId, customerId);

        CustomerEntity entity = customerRepository.findById(customerId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));

        CustomerAddressEntity addressToDelete = entity.getAddresses().stream()
                .filter(address -> Objects.equals(address.getId(), addressId))
                .findFirst()
                .orElse(null);
        if (addressToDelete == null) {
            throw new DataNotFoundException("Customer address " + addressId + " not found.");
        }

        validateCustomerAddressCanBeDeleted(addressId);

        boolean removed = entity.getAddresses().remove(addressToDelete);
        if (!removed) {
            throw new DataNotFoundException("Customer address " + addressId + " not found.");
        }

        activityHistoryService.record(
                ActivityEntityType.CUSTOMER,
                entity.getId(),
                StringUtils.defaultIfBlank(activityHistoryService.resolveCurrentUserId(), "SYSTEM"),
                ActivityActorType.USER,
                ActivityAction.DELETE,
                ActivitySource.API,
                "ลบที่อยู่ลูกค้า " + addressId + " ของ " + entity.getId(),
                buildCustomerAddressDeleteHistorySnapshot(addressToDelete)
        );
        entity = customerRepository.save(entity);

        log.info("Delete customer address success {} from customer {}", addressId, customerId);
        return customerMapper.toDto(entity);
    }

    @Transactional
    public CustomerDto deleteCustomerContact(String customerId, Long contactId) throws DataNotFoundException {
        log.info("Delete customer contact {} from customer {}", contactId, customerId);

        CustomerEntity entity = customerRepository.findById(customerId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));

        CustomerContactEntity contactToDelete = entity.getContacts().stream()
                .filter(contact -> Objects.equals(contact.getId(), contactId))
                .findFirst()
                .orElse(null);
        boolean removed = contactToDelete != null && entity.getContacts().remove(contactToDelete);
        if (!removed) {
            throw new DataNotFoundException("Customer contact " + contactId + " not found.");
        }

        activityHistoryService.record(
                ActivityEntityType.CUSTOMER,
                entity.getId(),
                StringUtils.defaultIfBlank(activityHistoryService.resolveCurrentUserId(), "SYSTEM"),
                ActivityActorType.USER,
                ActivityAction.DELETE,
                ActivitySource.API,
                "ลบผู้ติดต่อลูกค้า " + contactId + " ของ " + entity.getId(),
                buildCustomerContactDeleteHistorySnapshot(contactToDelete)
        );
        entity = customerRepository.save(entity);

        log.info("Delete customer contact success {} from customer {}", contactId, customerId);
        return customerMapper.toDto(entity);
    }

    @Transactional
    public CustomerDto addCustomerAddress(String customerId, CreateCustomerAddressRequest request)
            throws DataNotFoundException {
        log.info("Add customer address to customer {} request {}", customerId, request);

        CustomerEntity entity = customerRepository.findById(customerId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));

        CustomerAddressEntity address = new CustomerAddressEntity();
        address.setAddressType(request.getAddressType());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        address.setLabel(request.getLabel());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setSubdistrict(request.getSubdistrict());
        address.setDistrict(request.getDistrict());
        address.setProvince(request.getProvince());
        address.setPostcode(request.getPostcode());
        address.setCountry(request.getCountry());

        entity.addAddress(address);
        entity = customerRepository.save(entity);

        recordCustomerAddressCreateHistory(entity.getId(), address, "เพิ่มที่อยู่ลูกค้า ");
        log.info("Add customer address success to customer {}", customerId);
        return customerMapper.toDto(entity);
    }

    @Transactional
    public CustomerDto addCustomerContact(String customerId, CreateCustomerContactRequest request)
            throws DataNotFoundException {
        log.info("Add customer contact to customer {} request {}", customerId, request);

        CustomerEntity entity = customerRepository.findById(customerId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));

        CustomerContactEntity contact = new CustomerContactEntity();
        contact.setContactName(request.getContactName());
        contact.setContactNumber(request.getContactNumber());

        entity.addContact(contact);
        entity = customerRepository.save(entity);

        recordCustomerContactCreateHistory(entity.getId(), contact, "เพิ่มผู้ติดต่อของลูกค้า ");
        log.info("Add customer contact success to customer {}", customerId);
        return customerMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomer(SearchCustomerRequest criteria) {
        log.info("Get all customer with criteria : {}", criteria);
        List<CustomerDto> customerDtoList = customerRepository.findAllByStatusOrderByCreatedDateDesc(Status.ACTIVE)
                .stream()
                .map(customerMapper::toDto)
                .toList();
        return customerDtoList;
    }

    @Transactional(readOnly = true)
    public CustomerDashboardDto getCustomerDashboard(String salesId) {
        log.info("Get customer dashboard with salesId : {}", salesId);

        List<CustomerEntity> customers = StringUtils.isBlank(salesId)
                ? customerRepository.findAllByStatusOrderByCreatedDateDesc(Status.ACTIVE)
                : customerRepository.findAllByStatusAndSalesAccountOrderByCreatedDateDesc(Status.ACTIVE, salesId);
        List<CustomerDto> customerDtos = customers.stream()
                .map(customerMapper::toDto)
                .toList();

        CustomerDashboardDto dashboard = new CustomerDashboardDto();
        dashboard.setGeneratedAt(java.time.ZonedDateTime.now());
        dashboard.setTotalCustomers((long) customers.size());
        dashboard.setCompanyCustomers(countCustomersByCode(customers, customer -> customer.getCustomerType(), "COMPANY"));
        dashboard.setIndividualCustomers(countCustomersByCode(customers, customer -> customer.getCustomerType(), "INDIVIDUAL", "INDIVIDUA"));
        dashboard.setDefaultAddressCustomers(customers.stream()
                .filter(customer -> customer.getAddresses() != null && customer.getAddresses().stream().anyMatch(address -> Boolean.TRUE.equals(address.getIsDefault())))
                .count());
        dashboard.setTypeBreakdown(buildBreakdown(customers, customer -> customer.getCustomerType()));
        dashboard.setTierBreakdown(buildBreakdown(customers, customer -> customer.getCustomerTier()));
        dashboard.setSegmentBreakdown(buildBreakdown(customers, customer -> customer.getCustomerSegment()));
        dashboard.setRecentCustomers(customerDtos.subList(0, Math.min(8, customerDtos.size())));

        return dashboard;
    }

    @Transactional
    public CustomerDto deleteCustomer(String customerId, String userId) throws DataNotFoundException {
        log.info("Delete customer {} by {}", customerId, userId);

        CustomerEntity entity = customerRepository.findById(customerId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));

        Map<String, Object> beforeDetail = buildCustomerHistorySnapshot(entity);

        entity.setStatus(Status.INACTIVE);
        entity.setUpdatedBy(userId);
        entity = customerRepository.save(entity);

        Map<String, Object> afterDetail = buildCustomerHistorySnapshot(entity);
        Map<String, Object> deleteDetail = new LinkedHashMap<>();
        deleteDetail.put("before", beforeDetail);
        deleteDetail.put("after", afterDetail);

        activityHistoryService.record(
                ActivityEntityType.CUSTOMER,
                entity.getId(),
                userId,
                ActivityActorType.USER,
                ActivityAction.DELETE,
                ActivitySource.API,
                "ลบลูกค้า " + entity.getId(),
                deleteDetail
        );

        return customerMapper.toDto(entity);
    }

    private Specification<CustomerEntity> buildSearchCriteria(SearchCustomerRequest request) {
        Specification<CustomerEntity> specification = Specification.where(null);
        return specification
                .and(statusEqual(Status.ACTIVE))
                .and(idEqual(request.getIdEqual()))
                .and(customerNameContain(request.getNameContain()))
                .and(customerTypeEqual(request.getTypeEqual()))
                .and(customerTierEqual(request.getTierEqual()))
                .and(customerSegmentEqual(request.getSegmentEqual()))
                .and(saleAccountEqual(request.getSaleAccountEqual()))
                .and(keywordContain(request.getKeyword()))
                ;
    }

    private void applySalesVisibleOrderTotals(List<CustomerDto> customers, Authentication authentication) {
        if (CollectionUtils.isEmpty(customers) || authentication == null || !(authentication.getPrincipal() instanceof UserDto userDto)) {
            return;
        }

        String roleCode = userDto.getRole() != null ? userDto.getRole().getRoleCode() : null;
        String salesId = StringUtils.trimToNull(userDto.getEmployeeId());
        if (!StringUtils.equalsIgnoreCase(roleCode, "SALES") || salesId == null) {
            return;
        }

        for (CustomerDto customer : customers) {
            if (!isCustomerOwnedBySales(customer, salesId)) {
                customer.setTotalSalesOrderAmount(BigDecimal.ZERO);
            }
        }
    }

    private boolean isCustomerOwnedBySales(CustomerDto customer, String salesId) {
        if (customer == null || StringUtils.isBlank(salesId)) {
            return false;
        }

        if (CollectionUtils.isNotEmpty(customer.getSalesAccounts()) && customer.getSalesAccounts().contains(salesId)) {
            return true;
        }

        return StringUtils.equals(customer.getSalesAccount(), salesId);
    }

    private Map<String, Object> buildCustomerHistorySnapshot(CustomerEntity entity) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("customerId", entity.getId());
        detail.put("customerName", entity.getCustomerName());
        detail.put("status", entity.getStatus());
        detail.put("customerType", entity.getCustomerType() != null ? entity.getCustomerType().getId().getCode() : null);
        detail.put("customerTier", entity.getCustomerTier() != null ? entity.getCustomerTier().getId().getCode() : null);
        detail.put("customerSegment", entity.getCustomerSegment() != null ? entity.getCustomerSegment().getId().getCode() : null);
        detail.put("creditTerm", entity.getCustomerCreditTerm() != null ? entity.getCustomerCreditTerm().getId().getCode() : null);
        detail.put("paymentTerm", entity.getCustomerPaymentTerm() != null ? entity.getCustomerPaymentTerm().getId().getCode() : null);
        detail.put("billingCondition", entity.getCustomerBillingCondition());
        detail.put("paymentCycle", entity.getCustomerPaymentCycle());
        detail.put("companyName", entity.getCompanyName());
        detail.put("branchNumber", entity.getBranchNumber());
        detail.put("branchName", entity.getBranchName());
        detail.put("email", entity.getEmail());
        detail.put("salesAccount", entity.getSalesAccount());
        detail.put("salesAccounts", getEffectiveSalesAccounts(entity));
        detail.put("coSalesAccount", entity.getCoSalesAccount());
        return detail;
    }

    private void applySalesAccounts(CustomerEntity entity, List<String> salesAccounts, String salesAccount) {
        List<String> normalizedSalesAccounts = normalizeSalesAccounts(salesAccounts, salesAccount);
        entity.setSalesAccounts(new ArrayList<>(normalizedSalesAccounts));
        entity.setSalesAccount(normalizedSalesAccounts.isEmpty() ? null : normalizedSalesAccounts.get(0));
    }

    private List<String> normalizeSalesAccounts(List<String> salesAccounts, String salesAccount) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (CollectionUtils.isNotEmpty(salesAccounts)) {
            salesAccounts.stream()
                    .map(StringUtils::trimToNull)
                    .filter(Objects::nonNull)
                    .forEach(normalized::add);
        }
        String primarySalesAccount = StringUtils.trimToNull(salesAccount);
        if (primarySalesAccount != null) {
            normalized.add(primarySalesAccount);
        }
        return new ArrayList<>(normalized);
    }

    private List<String> getEffectiveSalesAccounts(CustomerEntity entity) {
        if (CollectionUtils.isNotEmpty(entity.getSalesAccounts())) {
            return new ArrayList<>(entity.getSalesAccounts());
        }
        if (StringUtils.isNotBlank(entity.getSalesAccount())) {
            return List.of(entity.getSalesAccount());
        }
        return new ArrayList<>();
    }

    private List<CustomerDashboardBreakdownDto> buildBreakdown(
            List<CustomerEntity> customers,
            Function<CustomerEntity, SystemConfigEntity> mapper
    ) {
        Map<String, BreakdownAccumulator> counts = new LinkedHashMap<>();
        for (CustomerEntity customer : customers) {
            SystemConfigEntity config = mapper.apply(customer);
            if (config == null || config.getId() == null || StringUtils.isBlank(config.getId().getCode())) {
                continue;
            }

            String code = config.getId().getCode();
            BreakdownAccumulator accumulator = counts.computeIfAbsent(code, key -> new BreakdownAccumulator());
            accumulator.count++;
            accumulator.nameTh = StringUtils.defaultIfBlank(accumulator.nameTh, config.getNameTh());
            accumulator.nameEn = StringUtils.defaultIfBlank(accumulator.nameEn, config.getNameEn());
        }

        return counts.entrySet().stream()
                .map(entry -> {
                    CustomerDashboardBreakdownDto dto = new CustomerDashboardBreakdownDto();
                    dto.setCode(entry.getKey());
                    dto.setCount(entry.getValue().count);
                    dto.setNameTh(entry.getValue().nameTh);
                    dto.setNameEn(entry.getValue().nameEn);
                    return dto;
                })
                .sorted((a, b) -> {
                    int compareCount = Long.compare(b.getCount() == null ? 0L : b.getCount(), a.getCount() == null ? 0L : a.getCount());
                    if (compareCount != 0) {
                        return compareCount;
                    }
                    return StringUtils.defaultString(a.getCode()).compareTo(StringUtils.defaultString(b.getCode()));
                })
                .toList();
    }

    private long countCustomersByCode(
            List<CustomerEntity> customers,
            Function<CustomerEntity, SystemConfigEntity> mapper,
            String... codes
    ) {
        List<String> codeList = List.of(codes);
        return customers.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .map(config -> config.getId() != null ? config.getId().getCode() : null)
                .filter(code -> code != null && codeList.stream().anyMatch(item -> item.equalsIgnoreCase(code)))
                .count();
    }

    private static class BreakdownAccumulator {
        private long count;
        private String nameTh;
        private String nameEn;
    }

    private void recordCustomerAddressCreateHistory(CustomerEntity customer, String actorId, String summaryPrefix) {
        if (customer == null || CollectionUtils.isEmpty(customer.getAddresses())) {
            return;
        }

        for (CustomerAddressEntity address : customer.getAddresses()) {
            recordCustomerAddressHistory(
                    customer.getId(),
                    actorId,
                    ActivityAction.CREATE,
                    summaryPrefix + addressTitle(address),
                    buildEmptyCustomerAddressHistorySnapshot(),
                    buildCustomerAddressHistorySnapshot(address)
            );
        }
    }

    private void validateCustomerAddressCanBeDeleted(Long addressId) throws InvalidRequestException {
        if (addressId == null) {
            return;
        }

        List<String> usedBy = new ArrayList<>();
        if (quotationRepository.countByCustomerAddress_Id(addressId) > 0) {
            usedBy.add("ใบเสนอราคา");
        }
        if (salesOrderRepository.countByCustomerAddress_Id(addressId) > 0) {
            usedBy.add("ใบยืนยันสั่งซื้อ");
        }
        if (invoiceRepository.countByCustomerAddress_Id(addressId) > 0) {
            usedBy.add("ใบแจ้งหนี้");
        }
        if (receiptRepository.countByCustomerAddress_Id(addressId) > 0) {
            usedBy.add("ใบเสร็จรับเงิน");
        }

        if (!usedBy.isEmpty()) {
            throw new InvalidRequestException(
                    "ไม่สามารถลบที่อยู่นี้ได้ เนื่องจากถูกใช้งานใน " + String.join(", ", usedBy)
            );
        }
    }

    private void recordCustomerAddressCreateHistory(String customerId, CustomerAddressEntity address, String summaryPrefix) {
        if (StringUtils.isBlank(customerId) || address == null) {
            return;
        }

        recordCustomerAddressHistory(
                customerId,
                activityHistoryService.resolveCurrentUserId(),
                ActivityAction.CREATE,
                summaryPrefix + addressTitle(address),
                buildEmptyCustomerAddressHistorySnapshot(),
                buildCustomerAddressHistorySnapshot(address)
        );
    }

    private void recordCustomerContactCreateHistory(CustomerEntity customer, String actorId, String summaryPrefix) {
        if (customer == null || CollectionUtils.isEmpty(customer.getContacts())) {
            return;
        }

        for (CustomerContactEntity contact : customer.getContacts()) {
            recordCustomerContactHistory(
                    customer.getId(),
                    actorId,
                    ActivityAction.CREATE,
                    summaryPrefix + contactTitle(contact),
                    buildEmptyCustomerContactHistorySnapshot(),
                    buildCustomerContactHistorySnapshot(contact)
            );
        }
    }

    private void recordCustomerContactCreateHistory(String customerId, CustomerContactEntity contact, String summaryPrefix) {
        if (StringUtils.isBlank(customerId) || contact == null) {
            return;
        }

        recordCustomerContactHistory(
                customerId,
                activityHistoryService.resolveCurrentUserId(),
                ActivityAction.CREATE,
                summaryPrefix + contactTitle(contact),
                buildEmptyCustomerContactHistorySnapshot(),
                buildCustomerContactHistorySnapshot(contact)
        );
    }

    private void recordCustomerAddressHistory(
            String customerId,
            String actorId,
            ActivityAction action,
            String summary,
            Map<String, Object> before,
            Map<String, Object> after
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", after);
        activityHistoryService.record(
                ActivityEntityType.CUSTOMER,
                customerId,
                StringUtils.defaultIfBlank(StringUtils.defaultIfBlank(actorId, activityHistoryService.resolveCurrentUserId()), "SYSTEM"),
                ActivityActorType.USER,
                action,
                ActivitySource.API,
                summary,
                detail
        );
    }

    private void recordCustomerContactHistory(
            String customerId,
            String actorId,
            ActivityAction action,
            String summary,
            Map<String, Object> before,
            Map<String, Object> after
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", after);
        activityHistoryService.record(
                ActivityEntityType.CUSTOMER,
                customerId,
                StringUtils.defaultIfBlank(StringUtils.defaultIfBlank(actorId, activityHistoryService.resolveCurrentUserId()), "SYSTEM"),
                ActivityActorType.USER,
                action,
                ActivitySource.API,
                summary,
                detail
        );
    }

    private Map<String, Object> buildCustomerAddressHistorySnapshot(CustomerAddressEntity address) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (address == null) {
            return detail;
        }
        detail.put("addressId", address.getId());
        detail.put("addressType", address.getAddressType());
        detail.put("isDefault", address.getIsDefault());
        detail.put("label", address.getLabel());
        detail.put("addressLine1", address.getAddressLine1());
        detail.put("addressLine2", address.getAddressLine2());
        detail.put("subdistrict", address.getSubdistrict());
        detail.put("district", address.getDistrict());
        detail.put("province", address.getProvince());
        detail.put("postcode", address.getPostcode());
        detail.put("country", address.getCountry());
        return detail;
    }

    private Map<String, Object> buildEmptyCustomerAddressHistorySnapshot() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("addressId", null);
        detail.put("addressType", null);
        detail.put("isDefault", null);
        detail.put("label", null);
        detail.put("addressLine1", null);
        detail.put("addressLine2", null);
        detail.put("subdistrict", null);
        detail.put("district", null);
        detail.put("province", null);
        detail.put("postcode", null);
        detail.put("country", null);
        return detail;
    }

    private Map<String, Object> buildCustomerContactHistorySnapshot(CustomerContactEntity contact) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (contact == null) {
            return detail;
        }
        detail.put("contactId", contact.getId());
        detail.put("contactName", contact.getContactName());
        detail.put("contactNumber", contact.getContactNumber());
        detail.put("isDefault", contact.getIsDefault());
        return detail;
    }

    private Map<String, Object> buildEmptyCustomerContactHistorySnapshot() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("contactId", null);
        detail.put("contactName", null);
        detail.put("contactNumber", null);
        detail.put("isDefault", null);
        return detail;
    }

    private Map<String, Object> buildCustomerAddressDeleteHistorySnapshot(CustomerAddressEntity address) {
        return buildCustomerAddressDeleteHistorySnapshot(address == null ? null : address.getId(), address);
    }

    private Map<String, Object> buildCustomerAddressDeleteHistorySnapshot(Long addressId, CustomerAddressEntity address) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", buildCustomerAddressHistorySnapshot(address));
        detail.put("after", buildEmptyCustomerAddressHistorySnapshot());
        detail.put("addressId", addressId);
        return detail;
    }

    private Map<String, Object> buildCustomerContactDeleteHistorySnapshot(CustomerContactEntity contact) {
        return buildCustomerContactDeleteHistorySnapshot(contact == null ? null : contact.getId(), contact);
    }

    private Map<String, Object> buildCustomerContactDeleteHistorySnapshot(Long contactId, CustomerContactEntity contact) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", buildCustomerContactHistorySnapshot(contact));
        detail.put("after", buildEmptyCustomerContactHistorySnapshot());
        detail.put("contactId", contactId);
        return detail;
    }

    private String addressTitle(CustomerAddressEntity address) {
        if (address == null) {
            return "-";
        }
        return StringUtils.defaultIfBlank(address.getLabel(),
                StringUtils.defaultIfBlank(address.getAddressLine1(),
                        StringUtils.defaultIfBlank(address.getProvince(), "-")));
    }

    private String contactTitle(CustomerContactEntity contact) {
        if (contact == null) {
            return "-";
        }
        return StringUtils.defaultIfBlank(contact.getContactName(),
                StringUtils.defaultIfBlank(contact.getContactNumber(), "-"));
    }

    @NotNull
    private static CustomerAddressEntity getCustomerAddressEntity(CreateCustomerRequest request) {
        CustomerAddressEntity address = new CustomerAddressEntity();
        address.setAddressType(request.getAddress().getAddressType());
        address.setIsDefault(Boolean.TRUE.equals(request.getAddress().getIsDefault()));
        address.setLabel(request.getAddress().getLabel());
        address.setAddressLine1(request.getAddress().getAddressLine1());
        address.setAddressLine2(request.getAddress().getAddressLine2());
        address.setSubdistrict(request.getAddress().getSubdistrict());
        address.setDistrict(request.getAddress().getDistrict());
        address.setProvince(request.getAddress().getProvince());
        address.setPostcode(request.getAddress().getPostcode());
        address.setCountry(request.getAddress().getCountry());
        return address;
    }

    private SystemConfigEntity getOptionalSystemConfig(SystemConstant groupCode, String code, String fieldName)
            throws InvalidRequestException {
        String trimmedCode = StringUtils.trimToNull(code);
        if (trimmedCode == null) {
            return null;
        }

        return systemConfigRepository.findByIdGroupCodeAndIdCode(groupCode, trimmedCode)
                .orElseThrow(() -> new InvalidRequestException(fieldName + " " + trimmedCode + " not found."));
    }

    private CustomerEntity buildCustomerEntityFromRow(Row row, Map<String, Integer> headerIndexMap, FormulaEvaluator evaluator, String userId)
            throws InvalidRequestException {
        CustomerEntity entity = new CustomerEntity();

        entity.setCustomerName(readCell(row, headerIndexMap, evaluator, "customer_name"));
        entity.setCompanyName(readCell(row, headerIndexMap, evaluator, "company_name"));
        entity.setEmail(readCell(row, headerIndexMap, evaluator, "email"));
        entity.setTaxId(readCell(row, headerIndexMap, evaluator, "tax_id"));
        entity.setBranchNumber(readCell(row, headerIndexMap, evaluator, "branch_number"));
        entity.setBranchName(readCell(row, headerIndexMap, evaluator, "branch_name"));
        entity.setSalesAccount(readCell(row, headerIndexMap, evaluator, "sales_account"));
        entity.setCoSalesAccount(readCell(row, headerIndexMap, evaluator, "co_sales_account"));
        entity.setCreatedBy(userProfileService.getNameFromId(userId));
        entity.setUpdatedBy(userProfileService.getNameFromId(userId));

        String statusValue = StringUtils.defaultIfBlank(readCell(row, headerIndexMap, evaluator, "status"), Status.ACTIVE.name());
        entity.setStatus(parseStatus(statusValue));

        String customerType = readCell(row, headerIndexMap, evaluator, "customer_type");
        if (StringUtils.isBlank(customerType)) {
            throw new InvalidRequestException("customer_type is required.");
        }
        entity.setCustomerType(getOptionalSystemConfig(
                SystemConstant.CUSTOMER_TYPE,
                customerType,
                "Customer type"
        ));

        String creditTerm = readCell(row, headerIndexMap, evaluator, "customer_credit_term");
        entity.setCustomerCreditTerm(getOptionalSystemConfig(
                SystemConstant.CUSTOMER_CREDIT_TERM,
                creditTerm,
                "Customer credit term"
        ));
        String segment = readCell(row, headerIndexMap, evaluator, "customer_segment");
        entity.setCustomerSegment(getOptionalSystemConfig(
                SystemConstant.CUSTOMER_SEGMENT,
                segment,
                "Customer Segment"
        ));
        String tier = readCell(row, headerIndexMap, evaluator, "customer_tier");
        entity.setCustomerTier(getOptionalSystemConfig(
                SystemConstant.CUSTOMER_TIER,
                tier,
                "Customer Tier"
        ));

        String contactName = readCell(row, headerIndexMap, evaluator, "contact_name");
        String contactNumber = readCell(row, headerIndexMap, evaluator, "contact_number");
        if (StringUtils.isNotBlank(contactName) || StringUtils.isNotBlank(contactNumber)) {
            CustomerContactEntity contactEntity = new CustomerContactEntity();
            contactEntity.setContactName(contactName);
            contactEntity.setContactNumber(contactNumber);
            contactEntity.setIsDefault(true);
            entity.addContact(contactEntity);
        }

        return entity;
    }

    private CustomerEntity findExistingCustomer(Row row, Map<String, Integer> headerIndexMap, FormulaEvaluator evaluator) {
        String taxId = StringUtils.trimToNull(readCell(row, headerIndexMap, evaluator, "tax_id"));
        if (taxId != null) {
            return customerRepository.findFirstByTaxIdIgnoreCase(taxId).orElse(null);
        }

        String customerName = StringUtils.trimToNull(readCell(row, headerIndexMap, evaluator, "customer_name"));
        String branchNumber = StringUtils.trimToNull(readCell(row, headerIndexMap, evaluator, "branch_number"));
        String companyName = StringUtils.trimToNull(readCell(row, headerIndexMap, evaluator, "company_name"));
        if (customerName == null) {
            return null;
        }

        return customerRepository
                .findFirstByCustomerNameIgnoreCaseAndBranchNumberIgnoreCaseAndCompanyNameIgnoreCase(
                        customerName,
                        branchNumber,
                        companyName
                )
                .orElse(null);
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow, FormulaEvaluator evaluator) {
        Map<String, Integer> headerIndexMap = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell, evaluator);
            if (StringUtils.isBlank(header)) {
                continue;
            }
            headerIndexMap.put(header.trim().toLowerCase(), cell.getColumnIndex());
        }
        return headerIndexMap;
    }

    private void validateCustomerHeaders(Map<String, Integer> headerIndexMap) throws InvalidRequestException {
        List<String> requiredHeaders = List.of(
                "customer_name",
                "status",
                "email",
                "customer_type",
                "customer_credit_term",
                "tax_id",
                "company_name",
                "branch_number",
                "branch_name",
                "sales_account",
                "co_sales_account",
                "contact_name",
                "contact_number",
                "is_default"
        );

        List<String> missingHeaders = new ArrayList<>();
        for (String header : requiredHeaders) {
            if (!headerIndexMap.containsKey(header)) {
                missingHeaders.add(header);
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw new InvalidRequestException("Customer excel headers missing: " + String.join(", ", missingHeaders));
        }
    }

    private boolean isBlankCustomerRow(Row row, FormulaEvaluator evaluator, Map<String, Integer> headerIndexMap) {
        for (String header : List.of("customer_name", "customer_type", "tax_id", "company_name")) {
            String value = readCell(row, headerIndexMap, evaluator, header);
            if (StringUtils.isNotBlank(value)) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Row row, Map<String, Integer> headerIndexMap, FormulaEvaluator evaluator, String headerName) {
        Integer columnIndex = headerIndexMap.get(headerName.toLowerCase());
        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();
        try {
            String value = formatter.formatCellValue(cell, evaluator);
            return StringUtils.trimToNull(value);
        } catch (Exception ex) {
            if (cell.getCellType() == CellType.FORMULA) {
                String fallbackValue = resolveFormulaCellValue(row, headerIndexMap, cell.getCellFormula());
                if (fallbackValue != null) {
                    return StringUtils.trimToNull(fallbackValue);
                }
            }
            throw ex;
        }
    }

    private String resolveFormulaCellValue(Row row, Map<String, Integer> headerIndexMap, String formula) {
        if (StringUtils.isBlank(formula)) {
            return null;
        }

        String normalized = formula.replace(" ", "").toUpperCase();
        if (normalized.startsWith("IF(") && normalized.contains("\"COMPANY\"")) {
            String sourceValue = readCellWithoutEvaluator(row, headerIndexMap, "customer_type");
            boolean isCompany = "COMPANY".equalsIgnoreCase(StringUtils.trimToNull(sourceValue));

            String[] parts = splitIfFormula(formula);
            if (parts == null || parts.length < 3) {
                return null;
            }
            return isCompany ? unquoteFormulaValue(parts[1]) : unquoteFormulaValue(parts[2]);
        }

        return null;
    }

    private String[] splitIfFormula(String formula) {
        int firstComma = findTopLevelComma(formula);
        if (firstComma < 0) {
            return null;
        }

        int secondComma = findTopLevelComma(formula, firstComma + 1);
        if (secondComma < 0) {
            return null;
        }

        String thenPart = formula.substring(firstComma + 1, secondComma).trim();
        String elsePart = formula.substring(secondComma + 1, formula.lastIndexOf(')')).trim();
        return new String[]{null, thenPart, elsePart};
    }

    private int findTopLevelComma(String formula) {
        return findTopLevelComma(formula, 0);
    }

    private int findTopLevelComma(String formula, int startIndex) {
        int depth = 0;
        for (int i = startIndex; i < formula.length(); i++) {
            char ch = formula.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
            } else if (ch == ',' && depth == 1) {
                return i;
            }
        }
        return -1;
    }

    private String unquoteFormulaValue(String value) {
        String trimmed = StringUtils.trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        if ("\"\"".equals(trimmed)) {
            return "";
        }

        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        return trimmed;
    }

    private String readCellWithoutEvaluator(Row row, Map<String, Integer> headerIndexMap, String headerName) {
        Integer columnIndex = headerIndexMap.get(headerName.toLowerCase());
        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();
        return StringUtils.trimToNull(formatter.formatCellValue(cell));
    }

    private Status parseStatus(String value) throws InvalidRequestException {
        String normalized = StringUtils.trimToNull(value);
        if (normalized == null) {
            return Status.ACTIVE;
        }

        try {
            return Status.valueOf(normalized.toUpperCase());
        } catch (Exception ex) {
            throw new InvalidRequestException("Invalid customer status: " + normalized);
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase();
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "yes".equals(normalized)
                || "y".equals(normalized)
                || "ใช่".equals(normalized);
    }


}
