package com.nutalig.service;

import com.nutalig.constant.Status;
import com.nutalig.controller.supplier.request.AddSupplierMaterialCapability;
import com.nutalig.controller.supplier.request.AddSupplierFamilyCapabilityRequest;
import com.nutalig.controller.supplier.request.AddSupplierMaterialCapabilityRequest;
import com.nutalig.controller.supplier.request.CreateSupplierContactRequest;
import com.nutalig.controller.supplier.request.CreateSupplierRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.supplier.request.SearchSupplierRequest;
import com.nutalig.controller.supplier.response.SearchSupplierResponse;
import com.nutalig.dto.ProductFamilyDto;
import com.nutalig.dto.ProductMaterialDto;
import com.nutalig.dto.SupplierCapabilityDto;
import com.nutalig.dto.SupplierCapabilityMaterialDto;
import com.nutalig.dto.SupplierDto;
import com.nutalig.entity.ProductFamilyEntity;
import com.nutalig.entity.ProductMaterialEntity;
import com.nutalig.entity.SupplierCapabilityEntity;
import com.nutalig.entity.SupplierContactEntity;
import com.nutalig.entity.SupplierEntity;
import com.nutalig.entity.id.ProductMaterialId;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.ProductFamilyMapper;
import com.nutalig.mapper.ProductMaterialMapper;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.repository.ProductFamilyRepository;
import com.nutalig.repository.ProductMaterialRepository;
import com.nutalig.repository.SupplierCapabilityRepository;
import com.nutalig.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.nutalig.repository.specification.SupplierSpecification.capabilityProductFamilyCodeEqual;
import static com.nutalig.repository.specification.SupplierSpecification.capabilityProductMaterialCodeEqual;
import static com.nutalig.repository.specification.SupplierSpecification.idEqual;
import static com.nutalig.repository.specification.SupplierSpecification.keywordContain;
import static com.nutalig.repository.specification.SupplierSpecification.statusEqual;
import static com.nutalig.repository.specification.SupplierSpecification.contactNameContain;
import static com.nutalig.repository.specification.SupplierSpecification.contactNumberContain;
import static com.nutalig.repository.specification.SupplierSpecification.countryCodeEqual;
import static com.nutalig.repository.specification.SupplierSpecification.supplierCodeEqual;
import static com.nutalig.repository.specification.SupplierSpecification.supplierEmailContain;
import static com.nutalig.repository.specification.SupplierSpecification.supplierNameContain;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierCapabilityRepository supplierCapabilityRepository;
    private final ProductFamilyRepository productFamilyRepository;
    private final ProductMaterialRepository productMaterialRepository;
    private final ProductFamilyMapper productFamilyMapper;
    private final ProductMaterialMapper productMaterialMapper;
    private final SupplierMapper supplierMapper;

    @Transactional
    public String createSupplier(CreateSupplierRequest request) throws InvalidRequestException {
        log.info("Create supplier request {}", request);

        validateCreateRequest(request);

        SupplierEntity entity = supplierMapper.toEntity(request);
        entity.setSupplierName(StringUtils.trimToNull(request.getSupplierName()));
        entity.setSupplierCode(StringUtils.trimToNull(request.getSupplierCode()));
        entity.setSupplierEmail(StringUtils.trimToNull(request.getSupplierEmail()));
        entity.setFullAddress(StringUtils.trimToNull(request.getFullAddress()));
        entity.setFullAddressEn(StringUtils.trimToNull(request.getFullAddressEn()));
        entity.setCountryCode(StringUtils.trimToNull(request.getCountryCode()));
        entity.setProvince(StringUtils.trimToNull(request.getProvince()));
        entity.setCity(StringUtils.trimToNull(request.getCity()));
        entity.setDistrict(StringUtils.trimToNull(request.getDistrict()));
        entity.setTown(StringUtils.trimToNull(request.getTown()));
        entity.setStreet(StringUtils.trimToNull(request.getStreet()));
        entity.setDetailAddress(StringUtils.trimToNull(request.getDetailAddress()));
        entity.setPostalCode(StringUtils.trimToNull(request.getPostalCode()));
        entity.setAdditional(StringUtils.trimToNull(request.getAdditional()));
        entity.setStatus(Status.ACTIVE);

        if (CollectionUtils.isNotEmpty(request.getContacts())) {
            for (CreateSupplierContactRequest contactRequest : request.getContacts()) {
                if (contactRequest == null) {
                    continue;
                }
                if (StringUtils.isAllBlank(
                        contactRequest.getContactName(),
                        contactRequest.getContactNumber(),
                        contactRequest.getWechat()
                )) {
                    continue;
                }

                SupplierContactEntity contact = new SupplierContactEntity();
                contact.setContactName(StringUtils.trimToNull(contactRequest.getContactName()));
                contact.setContactNumber(StringUtils.trimToNull(contactRequest.getContactNumber()));
                contact.setWechat(StringUtils.trimToNull(contactRequest.getWechat()));
                contact.setIsDefault(Boolean.TRUE.equals(contactRequest.getIsDefault()));
                entity.addContact(contact);
            }
        }

        entity = supplierRepository.save(entity);

        log.info("Create supplier success id {}", entity.getId());
        return entity.getId();
    }

    @Transactional(readOnly = true)
    public SupplierDto getSupplierById(String supplierId) throws DataNotFoundException {
        log.info("Get supplier by id {}", supplierId);

        SupplierEntity entity = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new DataNotFoundException("Supplier " + supplierId + " not found."));

        return buildSupplierDto(entity);
    }

    @Transactional(readOnly = true)
    public List<SupplierCapabilityDto> getSupplierCapabilities(String supplierId) throws DataNotFoundException {
        log.info("Get supplier capabilities supplier id {}", supplierId);

        validateSupplierExists(supplierId);
        return buildCapabilityDtos(
                supplierCapabilityRepository.findAllBySupplier_IdAndStatusOrderByProductFamilyCodeAscProductMaterialCodeAsc(
                        supplierId,
                        Status.ACTIVE
                )
        );
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> suggestSuppliers(String productFamilyCode, String productMaterialCode) {
        log.info("Suggest suppliers by family {} material {}", productFamilyCode, productMaterialCode);

        String familyCode = StringUtils.trimToNull(productFamilyCode);
        String materialCode = StringUtils.trimToNull(productMaterialCode);
        if (familyCode == null) {
            return List.of();
        }

        return supplierCapabilityRepository.findSuggestedSuppliers(
                        familyCode,
                        materialCode,
                        Status.ACTIVE,
                        Status.ACTIVE
                ).stream()
                .sorted(Comparator
                        .comparing(SupplierEntity::getSupplierName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(SupplierEntity::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::buildSupplierDto)
                .toList();
    }

    @Transactional
    public List<SupplierCapabilityDto> addSupplierFamilyCapability(
            String supplierId,
            AddSupplierFamilyCapabilityRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("Add supplier family capability supplier id {} request {}", supplierId, request);

        SupplierEntity supplier = validateSupplierExists(supplierId);
        String productFamilyCode = StringUtils.trimToNull(request == null ? null : request.getProductFamilyCode());
        if (productFamilyCode == null) {
            throw new InvalidRequestException("Product family code is required.");
        }

        ProductFamilyEntity productFamily = productFamilyRepository.findById(productFamilyCode)
                .orElseThrow(() -> new DataNotFoundException("Product family code " + productFamilyCode + " not found."));

        if (supplierCapabilityRepository.existsBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeIsNullAndStatus(
                supplierId, productFamilyCode, Status.ACTIVE
        )) {
            throw new InvalidRequestException("Supplier " + supplierId + " already has family capability " + productFamilyCode + ".");
        }

        if (supplierCapabilityRepository.existsBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeIsNotNullAndStatus(
                supplierId, productFamilyCode, Status.ACTIVE
        )) {
            throw new InvalidRequestException(
                    "Supplier " + supplierId + " already has material capabilities under family " + productFamilyCode + "."
            );
        }

        SupplierCapabilityEntity capability = new SupplierCapabilityEntity();
        capability.setSupplier(supplier);
        capability.setProductFamilyCode(productFamily.getCode());
        capability.setProductFamily(productFamily);
        capability.setStatus(Status.ACTIVE);
        supplierCapabilityRepository.save(capability);

        return getSupplierCapabilities(supplierId);
    }

    @Transactional
    public List<SupplierCapabilityDto> addSupplierMaterialCapability(
            String supplierId,
            AddSupplierMaterialCapabilityRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("Add supplier material capability supplier id {} request {}", supplierId, request);

        SupplierEntity supplier = validateSupplierExists(supplierId);
        if (request == null || CollectionUtils.isEmpty(request.getCapabilities())) {
            throw new InvalidRequestException("Capabilities are required.");
        }

        List<SupplierCapabilityEntity> capabilitiesToSave = new ArrayList<>();
        Set<String> pendingCapabilityKeys = new LinkedHashSet<>();

        for (AddSupplierMaterialCapability requestedCapability : request.getCapabilities()) {
            if (requestedCapability == null) {
                throw new InvalidRequestException("Capability item is required.");
            }

            String productFamilyCode = StringUtils.trimToNull(requestedCapability.getProductFamilyCode());
            if (productFamilyCode == null) {
                throw new InvalidRequestException("Product family code is required.");
            }

            if (supplierCapabilityRepository.existsBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeIsNullAndStatus(
                    supplierId, productFamilyCode, Status.ACTIVE
            )) {
                throw new InvalidRequestException(
                        "Supplier " + supplierId + " already has family capability " + productFamilyCode + "."
                );
            }

            ProductFamilyEntity productFamily = productFamilyRepository.findById(productFamilyCode)
                    .orElseThrow(() -> new DataNotFoundException("Product family code " + productFamilyCode + " not found."));

            List<ProductMaterialEntity> productMaterials = resolveProductMaterials(productFamilyCode, requestedCapability);
            for (ProductMaterialEntity productMaterial : productMaterials) {
                String productMaterialCode = productMaterial.getCode();
                String pendingCapabilityKey = buildCapabilityKey(productFamilyCode, productMaterialCode);

                if (!pendingCapabilityKeys.add(pendingCapabilityKey)) {
                    throw new InvalidRequestException(
                            "Duplicate product material capability in request " + productFamilyCode + ":" + productMaterialCode + "."
                    );
                }

                if (supplierCapabilityRepository.existsBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeAndStatus(
                        supplierId, productFamilyCode, productMaterialCode, Status.ACTIVE
                )) {
                    throw new InvalidRequestException(
                            "Supplier " + supplierId + " already has material capability "
                                    + productFamilyCode + ":" + productMaterialCode + "."
                    );
                }

                SupplierCapabilityEntity capability = new SupplierCapabilityEntity();
                capability.setSupplier(supplier);
                capability.setProductFamilyCode(productFamilyCode);
                capability.setProductFamily(productFamily);
                capability.setProductMaterialCode(productMaterialCode);
                capability.setProductMaterial(productMaterial);
                capability.setStatus(Status.ACTIVE);
                capabilitiesToSave.add(capability);
            }
        }

        supplierCapabilityRepository.saveAll(capabilitiesToSave);

        return getSupplierCapabilities(supplierId);
    }

    @Transactional
    public List<SupplierCapabilityDto> deleteSupplierFamilyCapability(String supplierId, String productFamilyCode)
            throws DataNotFoundException {
        log.info("Delete supplier family capability supplier id {} family {}", supplierId, productFamilyCode);

        validateSupplierExists(supplierId);
        SupplierCapabilityEntity capability = supplierCapabilityRepository
                .findBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeIsNullAndStatus(
                        supplierId,
                        productFamilyCode,
                        Status.ACTIVE
                )
                .orElseThrow(() -> new DataNotFoundException(
                        "Supplier family capability " + supplierId + ":" + productFamilyCode + " not found."
                ));
        supplierCapabilityRepository.delete(capability);
        return getSupplierCapabilities(supplierId);
    }

    @Transactional
    public List<SupplierCapabilityDto> deleteSupplierMaterialCapability(
            String supplierId,
            String productFamilyCode,
            String productMaterialCode
    ) throws DataNotFoundException {
        log.info("Delete supplier material capability supplier id {} family {} material {}",
                supplierId, productFamilyCode, productMaterialCode);

        validateSupplierExists(supplierId);
        SupplierCapabilityEntity capability = supplierCapabilityRepository
                .findBySupplier_IdAndProductFamilyCodeAndProductMaterialCodeAndStatus(
                        supplierId,
                        productFamilyCode,
                        productMaterialCode,
                        Status.ACTIVE
                )
                .orElseThrow(() -> new DataNotFoundException(
                        "Supplier material capability " + supplierId + ":" + productFamilyCode + ":" + productMaterialCode
                                + " not found."
                ));
        supplierCapabilityRepository.delete(capability);
        return getSupplierCapabilities(supplierId);
    }

    @Transactional(readOnly = true)
    public SearchSupplierResponse searchSupplier(SearchSupplierRequest request, PageableRequest pageableRequest) {
        SearchSupplierRequest criteria = request == null ? new SearchSupplierRequest() : request;
        log.info("Search supplier by criteria {} page {} size {}", criteria, pageableRequest.getPage(), pageableRequest.getSize());

        pageableRequest.setSortBy("createdDate");
        pageableRequest.setSortDirection(Sort.Direction.DESC);
        Pageable pageable = pageableRequest.build();

        Page<SupplierEntity> supplierPage = supplierRepository.findAll(buildSearchCriteria(criteria), pageable);
        List<SupplierDto> suppliers = supplierPage.getContent().stream()
                .map(this::buildSupplierDto)
                .toList();

        SearchSupplierResponse response = new SearchSupplierResponse();
        response.setSuppliers(suppliers);
        response.setPagination(Pagination.build(supplierPage));
        return response;
    }

    private Specification<SupplierEntity> buildSearchCriteria(SearchSupplierRequest request) {
        return Specification.<SupplierEntity>where(null)
                .and(idEqual(request.getIdEqual()))
                .and(supplierNameContain(request.getNameContain()))
                .and(supplierCodeEqual(request.getSupplierCodeEqual()))
                .and(supplierEmailContain(request.getSupplierEmailContain()))
                .and(statusEqual(parseStatus(request.getStatusEqual())))
                .and(countryCodeEqual(request.getCountryCodeEqual()))
                .and(contactNameContain(request.getContactNameContain()))
                .and(contactNumberContain(request.getContactNumberContain()))
                .and(capabilityProductFamilyCodeEqual(request.getProductFamilyCodeEqual()))
                .and(capabilityProductMaterialCodeEqual(
                        request.getProductFamilyCodeEqual(),
                        request.getProductMaterialCodeEqual()
                ))
                .and(keywordContain(request.getKeyword()));
    }

    private Status parseStatus(String status) {
        String trimmedStatus = StringUtils.trimToNull(status);
        if (trimmedStatus == null) {
            return null;
        }

        try {
            return Status.valueOf(trimmedStatus);
        } catch (IllegalArgumentException exception) {
            log.warn("Ignore invalid supplier status filter {}", trimmedStatus);
            return null;
        }
    }

    private void validateCreateRequest(CreateSupplierRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }

        if (StringUtils.isBlank(request.getSupplierName())) {
            throw new InvalidRequestException("Supplier name is required.");
        }
    }

    private SupplierEntity validateSupplierExists(String supplierId) throws DataNotFoundException {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new DataNotFoundException("Supplier " + supplierId + " not found."));
    }

    private SupplierDto buildSupplierDto(SupplierEntity entity) {
        SupplierDto dto = supplierMapper.toDto(entity);
        dto.setCapabilities(buildCapabilityDtos(entity.getCapabilities()));
        return dto;
    }

    private List<SupplierCapabilityDto> buildCapabilityDtos(List<SupplierCapabilityEntity> capabilities) {
        Map<String, SupplierCapabilityDto> capabilityMap = new LinkedHashMap<>();

        for (SupplierCapabilityEntity capability : capabilities) {
            if (capability == null || capability.getStatus() != Status.ACTIVE) {
                continue;
            }

            SupplierCapabilityDto capabilityDto = capabilityMap.computeIfAbsent(
                    capability.getProductFamilyCode(),
                    key -> {
                        SupplierCapabilityDto dto = new SupplierCapabilityDto();
                        dto.setProductFamilyCode(capability.getProductFamilyCode());
                        dto.setProductFamily(toProductFamilyDto(capability.getProductFamily()));
                        return dto;
                    }
            );

            if (capability.getProductMaterialCode() == null) {
                capabilityDto.setCoversAllMaterials(true);
                continue;
            }

            SupplierCapabilityMaterialDto materialDto = new SupplierCapabilityMaterialDto();
            materialDto.setProductMaterialCode(capability.getProductMaterialCode());
            materialDto.setProductMaterial(toProductMaterialDto(capability.getProductMaterial()));
            capabilityDto.getMaterials().add(materialDto);
        }

        return new ArrayList<>(capabilityMap.values());
    }

    private ProductFamilyDto toProductFamilyDto(ProductFamilyEntity entity) {
        if (entity == null) {
            return null;
        }

        ProductFamilyDto dto = productFamilyMapper.toDto(entity);
        dto.setMaterialList(null);
        dto.setSubtype1List(null);
        return dto;
    }

    private ProductMaterialDto toProductMaterialDto(ProductMaterialEntity entity) {
        if (entity == null) {
            return null;
        }
        return productMaterialMapper.toDto(entity);
    }

    private List<ProductMaterialEntity> resolveProductMaterials(
            String productFamilyCode,
            AddSupplierMaterialCapability requestedCapability
    ) throws DataNotFoundException, InvalidRequestException {
        List<String> requestedMaterialCodes = normalizeMaterialCodes(requestedCapability.getProductMaterialCode());
        if (requestedMaterialCodes.isEmpty()) {
            List<ProductMaterialEntity> productMaterials =
                    productMaterialRepository.findAllByProductFamilyCodeOrderByCodeAsc(productFamilyCode);
            if (CollectionUtils.isEmpty(productMaterials)) {
                throw new InvalidRequestException("No product materials found for family " + productFamilyCode + ".");
            }
            return productMaterials;
        }

        List<ProductMaterialEntity> productMaterials = new ArrayList<>();
        for (String requestedMaterialCode : requestedMaterialCodes) {
            ProductMaterialId productMaterialId = new ProductMaterialId();
            productMaterialId.setProductFamilyCode(productFamilyCode);
            productMaterialId.setCode(requestedMaterialCode);

            ProductMaterialEntity productMaterial = productMaterialRepository.findById(productMaterialId)
                    .orElseThrow(() -> new DataNotFoundException(
                            "Product material code " + requestedMaterialCode + " not found in family " + productFamilyCode + "."
                    ));
            productMaterials.add(productMaterial);
        }
        return productMaterials;
    }

    private List<String> normalizeMaterialCodes(List<String> productMaterialCodes) {
        if (CollectionUtils.isEmpty(productMaterialCodes)) {
            return List.of();
        }

        Set<String> normalizedCodes = new LinkedHashSet<>();
        for (String productMaterialCode : productMaterialCodes) {
            String normalizedCode = StringUtils.trimToNull(productMaterialCode);
            if (normalizedCode != null) {
                normalizedCodes.add(normalizedCode);
            }
        }
        return new ArrayList<>(normalizedCodes);
    }

    private String buildCapabilityKey(String productFamilyCode, String productMaterialCode) {
        return productFamilyCode + "|" + productMaterialCode;
    }
}
