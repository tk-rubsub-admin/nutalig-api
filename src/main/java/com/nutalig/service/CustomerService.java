package com.nutalig.service;

import com.nutalig.constant.Status;
import com.nutalig.constant.SystemConstant;
import com.nutalig.controller.customer.request.CreateCustomerAddressRequest;
import com.nutalig.controller.customer.request.CreateCustomerContactRequest;
import com.nutalig.controller.customer.request.CreateCustomerRequest;
import com.nutalig.controller.customer.request.SearchCustomerRequest;
import com.nutalig.controller.customer.request.UpdateCustomerRequest;
import com.nutalig.controller.customer.response.SearchCustomerResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.CustomerDto;
import com.nutalig.entity.CustomerAddressEntity;
import com.nutalig.entity.CustomerContactEntity;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.CustomerMapper;
import com.nutalig.repository.CustomerRepository;
import com.nutalig.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.nutalig.repository.specification.CustomerSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final CustomerMapper customerMapper;

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

        entity.setStatus(Status.ACTIVE);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);

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

        log.info("Create customer {} with id : {}", request.getCustomerName(), entity.getId());
        return entity.getId();
    }

    @Transactional
    public SearchCustomerResponse searchCustomer(SearchCustomerRequest searchCustomerRequest, PageableRequest pageableRequest) {
        log.info("Search customer by criteria(s) {}", searchCustomerRequest);

        pageableRequest.setSortBy("createdDate");
        pageableRequest.setSortDirection(Sort.Direction.DESC);
        Pageable pageable = pageableRequest.build();

        Page<CustomerEntity> customerEntityPage = customerRepository.findAll(buildSearchCriteria(searchCustomerRequest), pageable);
        Page<CustomerDto> customerDtoPage = customerEntityPage.map(customerMapper::toDto);
        List<CustomerDto> customerDtoList = customerDtoPage.getContent();

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
            return customerMapper.toDto(entity);
        }

        if (request.getCustomerType() != null) {
            entity.setCustomerType(getOptionalSystemConfig(
                    SystemConstant.CUSTOMER_TYPE,
                    request.getCustomerType(),
                    "Customer type"
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
        if (request.getSalesAccount() != null) {
            entity.setSalesAccount(StringUtils.trimToNull(request.getSalesAccount()));
        }
        if (request.getCoSalesAccount() != null) {
            entity.setCoSalesAccount(StringUtils.trimToNull(request.getCoSalesAccount()));
        }

        entity.setUpdatedBy(userId);
        entity = customerRepository.save(entity);

        log.info("Update customer success id {}", customerId);
        return customerMapper.toDto(entity);
    }

    @Transactional
    public CustomerDto deleteCustomerAddress(String customerId, Long addressId) throws DataNotFoundException {
        log.info("Delete customer address {} from customer {}", addressId, customerId);

        CustomerEntity entity = customerRepository.findById(customerId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));

        boolean removed = entity.getAddresses().removeIf(address -> Objects.equals(address.getId(), addressId));
        if (!removed) {
            throw new DataNotFoundException("Customer address " + addressId + " not found.");
        }

        entity = customerRepository.save(entity);

        log.info("Delete customer address success {} from customer {}", addressId, customerId);
        return customerMapper.toDto(entity);
    }

    @Transactional
    public CustomerDto deleteCustomerContact(String customerId, Long contactId) throws DataNotFoundException {
        log.info("Delete customer contact {} from customer {}", contactId, customerId);

        CustomerEntity entity = customerRepository.findById(customerId)
                .orElseThrow(() -> new DataNotFoundException("Customer " + customerId + " not found."));

        boolean removed = entity.getContacts().removeIf(contact -> Objects.equals(contact.getId(), contactId));
        if (!removed) {
            throw new DataNotFoundException("Customer contact " + contactId + " not found.");
        }

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

        log.info("Add customer contact success to customer {}", customerId);
        return customerMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getAllCustomer(SearchCustomerRequest criteria) {
        log.info("Get all customer with criteria : {}", criteria);
        List<CustomerDto> customerDtoList = customerRepository.findAllBasicCustomer();
        return customerDtoList;
    }

    private Specification<CustomerEntity> buildSearchCriteria(SearchCustomerRequest request) {
        Specification<CustomerEntity> specification = Specification.where(null);
        return specification
                .and(idEqual(request.getIdEqual()))
                .and(customerNameContain(request.getNameContain()))
                .and(customerTypeEqual(request.getTypeEqual()))
                .and(saleAccountEqual(request.getSaleAccountEqual()))
                .and(keywordContain(request.getKeyword()))
                ;
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


}
