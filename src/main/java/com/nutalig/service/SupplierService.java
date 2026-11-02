package com.nutalig.service;

import com.nutalig.constant.Status;
import com.nutalig.constant.ShippingMethod;
import com.nutalig.constant.ShippingMode;
import com.nutalig.constant.SystemConstant;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pagination;
import com.nutalig.controller.supplier.request.*;
import com.nutalig.controller.supplier.response.SearchSupplierResponse;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.nutalig.entity.id.ProductMaterialId;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.ProductFamilyMapper;
import com.nutalig.mapper.ProductMaterialMapper;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.mapper.SystemConfigMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.nutalig.repository.specification.SupplierSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierAttachmentRepository supplierAttachmentRepository;
    private final SupplierCapabilityRepository supplierCapabilityRepository;
    private final SupplierShippingRepository supplierShippingRepository;
    private final ShippingLocationRepository shippingLocationRepository;
    private final ShippingRouteRepository shippingRouteRepository;
    private final SupplierShippingRouteRepository supplierShippingRouteRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final LeadTimeConfigRepository leadTimeConfigRepository;
    private final ProductFamilyRepository productFamilyRepository;
    private final ProductMaterialRepository productMaterialRepository;
    private final ProductFamilyMapper productFamilyMapper;
    private final ProductMaterialMapper productMaterialMapper;
    private final SupplierMapper supplierMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

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

    @Transactional
    public SupplierDto updateSupplier(String supplierId, UpdateSupplierRequest request)
            throws DataNotFoundException, InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required");
        }
        SupplierEntity entity = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new DataNotFoundException("Supplier " + supplierId + " not found."));
        if (request.getFullAddress() != null) {
            entity.setFullAddress(StringUtils.trimToNull(request.getFullAddress()));
        }
        if (request.getContacts() != null) {
            CreateSupplierContactRequest input = request.getContacts().stream()
                    .filter(Objects::nonNull)
                    .filter(contact -> Boolean.TRUE.equals(contact.getIsDefault()))
                    .findFirst()
                    .orElse(request.getContacts().stream().filter(Objects::nonNull).findFirst().orElse(null));
            if (input != null) {
                SupplierContactEntity contact = entity.getContacts().stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsDefault()))
                        .findFirst().orElse(null);
                if (contact == null) {
                    contact = new SupplierContactEntity();
                    contact.setIsDefault(true);
                    entity.addContact(contact);
                }
                contact.setContactName(StringUtils.trimToNull(input.getContactName()));
                contact.setContactNumber(StringUtils.trimToNull(input.getContactNumber()));
                contact.setWechat(StringUtils.trimToNull(input.getWechat()));
                SupplierContactEntity selectedContact = contact;
                entity.getContacts().stream().filter(item -> item != selectedContact).forEach(item -> item.setIsDefault(false));
            }
        }
        return buildSupplierDto(supplierRepository.save(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierDto addSupplierAttachments(
            String supplierId,
            List<MultipartFile> attachments,
            String userId
    ) throws Exception {
        SupplierEntity supplier = validateSupplierExists(supplierId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        List<MultipartFile> imageFiles = attachments == null
                ? List.of()
                : attachments.stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .toList();
        if (imageFiles.isEmpty()) {
            throw new InvalidRequestException("At least one supplier image is required.");
        }
        for (MultipartFile imageFile : imageFiles) {
            if (!StringUtils.startsWithIgnoreCase(
                    StringUtils.trimToEmpty(imageFile.getContentType()),
                    "image/"
            )) {
                throw new InvalidRequestException("Only image files are allowed.");
            }
        }

        int nextSortOrder = supplier.getAttachments().stream()
                .filter(attachment -> Boolean.TRUE.equals(attachment.getActive()))
                .map(SupplierAttachmentEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        for (MultipartFile imageFile : imageFiles) {
            UploadFileResponse upload = fileStorageService.uploadFile(
                    imageFile,
                    "suppliers/" + supplierId + "/attachments"
            );
            SupplierAttachmentEntity attachment = new SupplierAttachmentEntity();
            attachment.setFileName(upload.getFileName());
            attachment.setOriginalFileName(StringUtils.trimToNull(imageFile.getOriginalFilename()));
            attachment.setFileUrl(upload.getUrl());
            attachment.setContentType(StringUtils.trimToNull(upload.getContentType()));
            attachment.setFileSize(imageFile.getSize());
            attachment.setSortOrder(nextSortOrder++);
            attachment.setActive(Boolean.TRUE);
            attachment.setCreatedBy(user);
            attachment.setUpdatedBy(user);
            attachment.setCreatedDate(now);
            attachment.setUpdatedDate(now);
            supplier.addAttachment(attachment);
        }

        return buildSupplierDto(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierDto deleteSupplierAttachment(String supplierId, Long attachmentId, String userId)
            throws DataNotFoundException {
        SupplierEntity supplier = validateSupplierExists(supplierId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        SupplierAttachmentEntity attachment = supplierAttachmentRepository
                .findByIdAndSupplier_IdAndActiveTrue(attachmentId, supplierId)
                .orElseThrow(() -> new DataNotFoundException(
                        "Supplier attachment " + attachmentId + " not found."
                ));

        attachment.setActive(Boolean.FALSE);
        attachment.setUpdatedBy(user);
        attachment.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        supplierAttachmentRepository.save(attachment);
        return buildSupplierDto(supplier);
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

    @Transactional(readOnly = true)
    public List<SupplierShippingDto> getSupplierShippings() {
        return supplierShippingRepository.findAllByActiveTrueOrderByShippingMethodAscIdAsc().stream()
                .map(this::buildSupplierShippingDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShippingLocationDto> getShippingLocations() {
        return shippingLocationRepository.findAllByActiveTrueOrderByIdAsc().stream()
                .map(this::toShippingLocationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShippingLocationDto> searchShippingLocations(String keyword, String locationType, String countryCode) {
        String normalizedKeyword = StringUtils.lowerCase(StringUtils.trimToNull(keyword), Locale.ROOT);
        String normalizedType = StringUtils.upperCase(StringUtils.trimToNull(locationType), Locale.ROOT);
        String normalizedCountry = StringUtils.upperCase(StringUtils.trimToNull(countryCode), Locale.ROOT);
        return shippingLocationRepository.findAllByActiveTrueOrderByIdAsc().stream()
                .filter(location -> normalizedType == null || normalizedType.equalsIgnoreCase(locationTypeCode(location)))
                .filter(location -> normalizedCountry == null || normalizedCountry.equalsIgnoreCase(location.getCountryCode()))
                .filter(location -> normalizedKeyword == null || Stream.of(
                                location.getCode(), location.getName(), locationTypeCode(location),
                                location.getCountryCode(), location.getProvince(), location.getDistrict(),
                                location.getAddress())
                        .filter(Objects::nonNull)
                        .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(normalizedKeyword)))
                .map(this::toShippingLocationDto)
                .toList();
    }

    @Transactional
    public List<ShippingLocationDto> createShippingLocation(UpsertShippingLocationRequest request)
            throws InvalidRequestException {
        validateShippingLocationRequest(request);
        ShippingLocationEntity entity = new ShippingLocationEntity();
        applyShippingLocation(entity, request);
        shippingLocationRepository.save(entity);
        return getShippingLocations();
    }

    @Transactional
    public List<ShippingLocationDto> updateShippingLocation(Long id, UpsertShippingLocationRequest request)
            throws DataNotFoundException, InvalidRequestException {
        validateShippingLocationRequest(request);
        ShippingLocationEntity entity = shippingLocationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new DataNotFoundException("Shipping location " + id + " not found."));
        applyShippingLocation(entity, request);
        shippingLocationRepository.save(entity);
        return getShippingLocations();
    }

    @Transactional
    public List<ShippingLocationDto> deleteShippingLocation(Long id) throws DataNotFoundException {
        ShippingLocationEntity entity = shippingLocationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new DataNotFoundException("Shipping location " + id + " not found."));
        entity.setActive(Boolean.FALSE);
        shippingLocationRepository.save(entity);
        return getShippingLocations();
    }

    @Transactional(readOnly = true)
    public List<ShippingRouteDto> getShippingRoutes() {
        return shippingRouteRepository.findAllByActiveTrueOrderByIdAsc().stream()
                .map(this::toShippingRouteDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShippingRouteDto> searchShippingRoutes(String keyword, Long originLocationId,
                                                       Long destinationLocationId) {
        String normalizedKeyword = StringUtils.lowerCase(StringUtils.trimToNull(keyword), Locale.ROOT);
        return shippingRouteRepository.findAllByActiveTrueOrderByIdAsc().stream()
                .filter(route -> originLocationId == null
                        || Objects.equals(route.getOriginLocation().getId(), originLocationId))
                .filter(route -> destinationLocationId == null
                        || Objects.equals(route.getDestinationLocation().getId(), destinationLocationId))
                .filter(route -> normalizedKeyword == null || Stream.of(
                                route.getRouteCode(),
                                route.getOriginLocation().getCode(),
                                route.getOriginLocation().getName(),
                                route.getDestinationLocation().getCode(),
                                route.getDestinationLocation().getName())
                        .filter(Objects::nonNull)
                        .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(normalizedKeyword)))
                .map(this::toShippingRouteDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean shippingRouteExists(Long originLocationId, Long destinationLocationId)
            throws InvalidRequestException {
        if (originLocationId == null || destinationLocationId == null) {
            throw new InvalidRequestException("Origin and destination locations are required");
        }
        return shippingRouteRepository.existsByOriginLocation_IdAndDestinationLocation_IdAndActiveTrue(
                originLocationId, destinationLocationId);
    }

    @Transactional
    public List<ShippingRouteDto> createShippingRoute(UpsertShippingRouteRequest request)
            throws DataNotFoundException, InvalidRequestException {
        ShippingRouteEntity entity = new ShippingRouteEntity();
        applyShippingRoute(entity, request);
        shippingRouteRepository.save(entity);
        return getShippingRoutes();
    }

    @Transactional
    public List<ShippingRouteDto> updateShippingRoute(Long id, UpsertShippingRouteRequest request)
            throws DataNotFoundException, InvalidRequestException {
        ShippingRouteEntity entity = shippingRouteRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new DataNotFoundException("Shipping route " + id + " not found."));
        applyShippingRoute(entity, request);
        shippingRouteRepository.save(entity);
        return getShippingRoutes();
    }

    @Transactional
    public List<ShippingRouteDto> deleteShippingRoute(Long id) throws DataNotFoundException {
        ShippingRouteEntity entity = shippingRouteRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new DataNotFoundException("Shipping route " + id + " not found."));
        entity.setActive(Boolean.FALSE);
        shippingRouteRepository.save(entity);
        return getShippingRoutes();
    }

    @Transactional(readOnly = true)
    public List<SupplierShippingRouteDto> getSupplierShippingRoutes(Long supplierShippingId) {
        List<SupplierShippingRouteEntity> routes = supplierShippingId == null
                ? supplierShippingRouteRepository.findAllByActiveTrueOrderByIdAsc()
                : supplierShippingRouteRepository.findAllBySupplierShippingIdAndActiveTrueOrderByIdAsc(supplierShippingId);
        return routes.stream().map(this::toSupplierShippingRouteDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierShippingRouteDto> searchSupplierShippingsV2(
            String keyword,
            String shippingMethod,
            String shippingMode,
            Long supplierShippingId
    ) {
        String normalizedKeyword = StringUtils.trimToNull(keyword);
        String normalizedMethod = StringUtils.upperCase(StringUtils.trimToNull(shippingMethod), Locale.ROOT);
        String normalizedMode = StringUtils.upperCase(StringUtils.trimToNull(shippingMode), Locale.ROOT);
        return supplierShippingRouteRepository.findAllByActiveTrueOrderByIdAsc().stream()
                .filter(item -> supplierShippingId == null
                        || Objects.equals(item.getSupplierShipping().getId(), supplierShippingId))
                .filter(item -> normalizedMethod == null
                        || item.getSupplierShipping().getShippingMethod() != null
                        && item.getSupplierShipping().getShippingMethod().name().equals(normalizedMethod))
                .filter(item -> normalizedMode == null
                        || item.getSupplierShipping().getShippingMode() != null
                        && item.getSupplierShipping().getShippingMode().name().equals(normalizedMode))
                .filter(item -> matchesSupplierShippingRouteKeyword(item, normalizedKeyword))
                .map(this::toSupplierShippingRouteDto)
                .toList();
    }

    @Transactional
    public List<SupplierShippingRouteDto> createSupplierShippingV2(CreateSupplierShippingV2Request request)
            throws DataNotFoundException, InvalidRequestException {
        if (request == null || request.getShippingMethod() == null) {
            throw new InvalidRequestException("shippingMethod is required");
        }
        if (CollectionUtils.isEmpty(request.getShippingRouteIds())) {
            throw new InvalidRequestException("At least one shipping route is required");
        }
        ShippingMode mode = request.getShippingMode() == null ? ShippingMode.STANDARD : request.getShippingMode();
        if (request.getShippingMethod() == ShippingMethod.LAND && mode != ShippingMode.STANDARD) {
            throw new InvalidRequestException("LAND shipping only supports STANDARD mode");
        }
        List<ShippingRouteEntity> routes = new ArrayList<>();
        for (Long routeId : new LinkedHashSet<>(request.getShippingRouteIds())) {
            routes.add(shippingRouteRepository.findByIdAndActiveTrue(routeId)
                    .orElseThrow(() -> new DataNotFoundException("Shipping route " + routeId + " not found.")));
        }
        SupplierShippingEntity shipping = new SupplierShippingEntity();
        shipping.setShippingMethod(request.getShippingMethod());
        shipping.setShippingMode(mode);
        shipping.setShippingName(StringUtils.trimToNull(request.getShippingName()));
        shipping.setCarCode(StringUtils.upperCase(StringUtils.trimToNull(request.getCarCode()), Locale.ROOT));
        shipping.setRemark(StringUtils.trimToNull(request.getRemark()));
        shipping.setActive(Boolean.TRUE);
        supplierShippingRepository.saveAndFlush(shipping);
        List<SupplierShippingRouteEntity> links = routes.stream().map(route -> {
            SupplierShippingRouteEntity link = new SupplierShippingRouteEntity();
            link.setSupplierShipping(shipping);
            link.setShippingRoute(route);
            link.setActive(Boolean.TRUE);
            return link;
        }).toList();
        supplierShippingRouteRepository.saveAll(links);
        return getSupplierShippingRoutes(shipping.getId());
    }

    @Transactional
    public List<SupplierShippingRouteDto> createSupplierShippingRoute(UpsertSupplierShippingRouteRequest request)
            throws DataNotFoundException, InvalidRequestException {
        validateSupplierShippingRouteRequest(request);
        SupplierShippingRouteEntity entity = new SupplierShippingRouteEntity();
        applySupplierShippingRoute(entity, request);
        supplierShippingRouteRepository.save(entity);
        return getSupplierShippingRoutes(request.getSupplierShippingId());
    }

    @Transactional
    public List<SupplierShippingRouteDto> updateSupplierShippingRoute(Long id, UpsertSupplierShippingRouteRequest request)
            throws DataNotFoundException, InvalidRequestException {
        validateSupplierShippingRouteRequest(request);
        SupplierShippingRouteEntity entity = supplierShippingRouteRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new DataNotFoundException("Supplier shipping route " + id + " not found."));
        applySupplierShippingRoute(entity, request);
        supplierShippingRouteRepository.save(entity);
        return getSupplierShippingRoutes(request.getSupplierShippingId());
    }

    @Transactional
    public List<SupplierShippingRouteDto> deleteSupplierShippingRoute(Long id)
            throws DataNotFoundException {
        SupplierShippingRouteEntity entity = supplierShippingRouteRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new DataNotFoundException("Supplier shipping route " + id + " not found."));
        entity.setActive(Boolean.FALSE);
        supplierShippingRouteRepository.save(entity);
        return getSupplierShippingRoutes(entity.getSupplierShipping().getId());
    }

    @Transactional(readOnly = true)
    public List<LeadTimeConfigDto> getLeadTimeConfigs() {
        return leadTimeConfigRepository.findAllByIsActiveTrueOrderByTypeAscNameThAsc().stream()
                .map(this::buildLeadTimeConfigDto)
                .toList();
    }

    @Transactional
    public List<SupplierShippingDto> createSupplierShipping(
            UpsertSupplierShippingRequest request
    ) throws InvalidRequestException {
        validateSupplierShippingRequest(request);

        SupplierShippingEntity shipping = new SupplierShippingEntity();
        applySupplierShipping(shipping, request);
        supplierShippingRepository.save(shipping);

        return getSupplierShippings();
    }

    @Transactional
    public List<SupplierShippingDto> updateSupplierShipping(
            Long shippingId,
            UpsertSupplierShippingRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        validateSupplierShippingRequest(request);

        SupplierShippingEntity shipping = supplierShippingRepository.findByIdAndActiveTrue(shippingId)
                .orElseThrow(() -> new DataNotFoundException("Supplier shipping " + shippingId + " not found."));
        applySupplierShipping(shipping, request);
        supplierShippingRepository.save(shipping);

        return getSupplierShippings();
    }

    @Transactional
    public List<SupplierShippingDto> deleteSupplierShipping(Long shippingId)
            throws DataNotFoundException {
        SupplierShippingEntity shipping = supplierShippingRepository.findByIdAndActiveTrue(shippingId)
                .orElseThrow(() -> new DataNotFoundException("Supplier shipping " + shippingId + " not found."));
        shipping.setActive(Boolean.FALSE);
        if (CollectionUtils.isNotEmpty(shipping.getDestinations())) {
            shipping.getDestinations().forEach(item -> item.setActive(Boolean.FALSE));
        }
        supplierShippingRepository.save(shipping);

        return getSupplierShippings();
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

        ProductFamilyEntity productFamily = productFamilyRepository.findByCodeAndIsActiveTrue(productFamilyCode)
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

            ProductFamilyEntity productFamily = productFamilyRepository.findByCodeAndIsActiveTrue(productFamilyCode)
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
    public List<SupplierCapabilityDto> suggestSupplierCapabilities(
            String supplierId,
            List<AddSupplierMaterialCapability> capabilities
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("Suggest supplier capabilities supplier id {} size {}", supplierId,
                CollectionUtils.isEmpty(capabilities) ? 0 : capabilities.size());

        if (StringUtils.isBlank(supplierId)) {
            throw new InvalidRequestException("Supplier id is required.");
        }

        AddSupplierMaterialCapabilityRequest request = new AddSupplierMaterialCapabilityRequest();
        request.setCapabilities(capabilities);
        return addSupplierMaterialCapability(supplierId.trim(), request);
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
        dto.setAttachments(entity.getAttachments() == null
                ? List.of()
                : entity.getAttachments().stream()
                .filter(attachment -> Boolean.TRUE.equals(attachment.getActive()))
                .sorted(Comparator
                        .comparing(SupplierAttachmentEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SupplierAttachmentEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::buildSupplierAttachmentDto)
                .toList());
        return dto;
    }

    private SupplierAttachmentDto buildSupplierAttachmentDto(SupplierAttachmentEntity entity) {
        SupplierAttachmentDto dto = new SupplierAttachmentDto();
        dto.setId(entity.getId());
        dto.setSupplierId(entity.getSupplier() == null ? null : entity.getSupplier().getId());
        dto.setFileName(entity.getFileName());
        dto.setOriginalFileName(entity.getOriginalFileName());
        dto.setFileUrl(entity.getFileUrl());
        dto.setContentType(entity.getContentType());
        dto.setFileSize(entity.getFileSize());
        dto.setSortOrder(entity.getSortOrder());
        return dto;
    }

    private SupplierShippingDto buildSupplierShippingDto(SupplierShippingEntity entity) {
        SupplierShippingDto dto = supplierMapper.toDto(entity);
        dto.setDestinations(entity.getDestinations() == null
                ? List.of()
                : entity.getDestinations().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .sorted(Comparator
                        .comparing(SupplierShippingDestinationEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SupplierShippingDestinationEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(item -> {
                    SupplierShippingDestinationDto destinationDto = supplierMapper.toDto(item);
                    destinationDto.setSupplierShippingId(entity.getId());
                    return destinationDto;
                })
                .toList());
        return dto;
    }

    private LeadTimeConfigDto buildLeadTimeConfigDto(LeadTimeConfigEntity entity) {
        LeadTimeConfigDto dto = new LeadTimeConfigDto();
        dto.setCode(entity.getCode());
        dto.setType(entity.getType());
        dto.setNameTh(entity.getNameTh());
        dto.setNameEn(entity.getNameEn());
        return dto;
    }

    private void validateSupplierShippingRequest(UpsertSupplierShippingRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("request is required");
        }
        if (request.getShippingMethod() == null) {
            throw new InvalidRequestException("shippingMethod is required");
        }
        if (request.getShippingMethod() != ShippingMethod.LAND && request.getShippingMethod() != ShippingMethod.SEA) {
            throw new InvalidRequestException("Supplier shippingMethod must be LAND or SEA");
        }
        ShippingMode mode = request.getShippingMode() == null ? ShippingMode.STANDARD : request.getShippingMode();
        if (request.getShippingMethod() == ShippingMethod.LAND && mode != ShippingMode.STANDARD) {
            throw new InvalidRequestException("LAND shipping only supports STANDARD mode");
        }
        if (request.getShippingMethod() == ShippingMethod.SEA) {
            String prefix = mode == ShippingMode.FCL ? "TZ" : "TB";
            String carCode = StringUtils.upperCase(StringUtils.trimToEmpty(request.getCarCode()), Locale.ROOT);
            if (!carCode.startsWith(prefix) || carCode.length() <= prefix.length()) {
                throw new InvalidRequestException("SEA " + mode + " carCode must start with " + prefix + " followed by a code");
            }
        }
        if (CollectionUtils.isEmpty(request.getDestinations())) {
            throw new InvalidRequestException("At least one destination is required");
        }
        boolean hasDestinationName = request.getDestinations().stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> StringUtils.isNotBlank(item.getDestinationName()));
        if (!hasDestinationName) {
            throw new InvalidRequestException("At least one destination name is required");
        }
    }

    private void validateShippingLocationRequest(UpsertShippingLocationRequest request)
            throws InvalidRequestException {
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new InvalidRequestException("Shipping location name is required");
        }
    }

    private void applyShippingLocation(ShippingLocationEntity entity, UpsertShippingLocationRequest request)
            throws InvalidRequestException {
        entity.setCode(StringUtils.trimToNull(request.getCode()));
        entity.setName(StringUtils.trimToNull(request.getName()));
        String locationTypeCode = StringUtils.upperCase(StringUtils.trimToNull(request.getLocationType()), Locale.ROOT);
        if (locationTypeCode == null) {
            entity.setLocationType(null);
            entity.setLocationTypeCode(null);
            entity.setLocationTypeGroupCode(null);
        } else {
            SystemConfigEntity locationType = systemConfigRepository
                    .findByIdGroupCodeAndIdCode(SystemConstant.LOCATION_TYPE, locationTypeCode)
                    .orElseThrow(() -> new InvalidRequestException("Unknown LOCATION_TYPE: " + locationTypeCode));
            entity.setLocationType(locationType);
            entity.setLocationTypeCode(locationTypeCode);
            entity.setLocationTypeGroupCode(SystemConstant.LOCATION_TYPE.name());
        }
        entity.setCountryCode(StringUtils.upperCase(StringUtils.trimToNull(request.getCountryCode()), Locale.ROOT));
        entity.setProvince(StringUtils.trimToNull(request.getProvince()));
        entity.setDistrict(StringUtils.trimToNull(request.getDistrict()));
        entity.setAddress(StringUtils.trimToNull(request.getAddress()));
        entity.setActive(Boolean.TRUE);
    }

    private void applyShippingRoute(ShippingRouteEntity entity, UpsertShippingRouteRequest request)
            throws DataNotFoundException, InvalidRequestException {
        if (request == null || request.getOriginLocationId() == null || request.getDestinationLocationId() == null) {
            throw new InvalidRequestException("Origin and destination locations are required");
        }
        if (Objects.equals(request.getOriginLocationId(), request.getDestinationLocationId())) {
            throw new InvalidRequestException("Origin and destination locations must be different");
        }
        ShippingLocationEntity origin = shippingLocationRepository.findByIdAndActiveTrue(request.getOriginLocationId())
                .orElseThrow(() -> new DataNotFoundException("Origin shipping location not found."));
        ShippingLocationEntity destination = shippingLocationRepository.findByIdAndActiveTrue(request.getDestinationLocationId())
                .orElseThrow(() -> new DataNotFoundException("Destination shipping location not found."));
        entity.setRouteCode(StringUtils.trimToNull(request.getRouteCode()));
        entity.setOriginLocation(origin);
        entity.setDestinationLocation(destination);
        entity.setActive(Boolean.TRUE);
    }

    private void validateSupplierShippingRouteRequest(UpsertSupplierShippingRouteRequest request)
            throws InvalidRequestException {
        if (request == null || request.getSupplierShippingId() == null || request.getShippingRouteId() == null) {
            throw new InvalidRequestException("supplierShippingId and shippingRouteId are required");
        }
        if (request.getBaseCost() != null && request.getBaseCost().signum() < 0) {
            throw new InvalidRequestException("baseCost cannot be negative");
        }
        if (request.getLeadTimeDayMin() != null && request.getLeadTimeDayMin() < 0
                || request.getLeadTimeDayMax() != null && request.getLeadTimeDayMax() < 0) {
            throw new InvalidRequestException("Lead time cannot be negative");
        }
        if (request.getLeadTimeDayMin() != null && request.getLeadTimeDayMax() != null
                && request.getLeadTimeDayMin() > request.getLeadTimeDayMax()) {
            throw new InvalidRequestException("Minimum lead time cannot exceed maximum lead time");
        }
        if (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
                && request.getEffectiveFrom().isAfter(request.getEffectiveTo())) {
            throw new InvalidRequestException("effectiveFrom cannot be after effectiveTo");
        }
    }

    private void applySupplierShippingRoute(SupplierShippingRouteEntity entity,
                                             UpsertSupplierShippingRouteRequest request)
            throws DataNotFoundException {
        SupplierShippingEntity shipping = supplierShippingRepository.findByIdAndActiveTrue(request.getSupplierShippingId())
                .orElseThrow(() -> new DataNotFoundException("Supplier shipping not found."));
        ShippingRouteEntity route = shippingRouteRepository.findByIdAndActiveTrue(request.getShippingRouteId())
                .orElseThrow(() -> new DataNotFoundException("Shipping route not found."));
        entity.setSupplierShipping(shipping);
        entity.setShippingRoute(route);
        entity.setCurrency(StringUtils.upperCase(StringUtils.trimToNull(request.getCurrency()), Locale.ROOT));
        entity.setBaseCost(request.getBaseCost());
        entity.setLeadTimeDayMin(request.getLeadTimeDayMin());
        entity.setLeadTimeDayMax(request.getLeadTimeDayMax());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setRemark(StringUtils.trimToNull(request.getRemark()));
        entity.setActive(Boolean.TRUE);
    }

    private ShippingLocationDto toShippingLocationDto(ShippingLocationEntity entity) {
        ShippingLocationDto dto = new ShippingLocationDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setLocationType(entity.getLocationType() == null
                ? null : systemConfigMapper.toDto(entity.getLocationType()));
        dto.setCountryCode(entity.getCountryCode());
        dto.setProvince(entity.getProvince());
        dto.setDistrict(entity.getDistrict());
        dto.setAddress(entity.getAddress());
        dto.setActive(entity.getActive());
        return dto;
    }

    private String locationTypeCode(ShippingLocationEntity entity) {
        return entity.getLocationTypeCode() != null
                ? entity.getLocationTypeCode()
                : entity.getLocationType() == null || entity.getLocationType().getId() == null
                ? null : entity.getLocationType().getId().getCode();
    }

    private ShippingRouteDto toShippingRouteDto(ShippingRouteEntity entity) {
        ShippingRouteDto dto = new ShippingRouteDto();
        dto.setId(entity.getId());
        dto.setRouteCode(entity.getRouteCode());
        dto.setOriginLocationId(entity.getOriginLocation().getId());
        dto.setOriginLocationName(entity.getOriginLocation().getName());
        dto.setDestinationLocationId(entity.getDestinationLocation().getId());
        dto.setDestinationLocationName(entity.getDestinationLocation().getName());
        dto.setActive(entity.getActive());
        return dto;
    }

    private SupplierShippingRouteDto toSupplierShippingRouteDto(SupplierShippingRouteEntity entity) {
        SupplierShippingRouteDto dto = new SupplierShippingRouteDto();
        ShippingRouteEntity route = entity.getShippingRoute();
        dto.setId(entity.getId());
        dto.setSupplierShippingId(entity.getSupplierShipping().getId());
        dto.setSupplierShippingName(entity.getSupplierShipping().getShippingName());
        dto.setShippingMethod(entity.getSupplierShipping().getShippingMethod());
        dto.setShippingMode(entity.getSupplierShipping().getShippingMode());
        dto.setCarCode(entity.getSupplierShipping().getCarCode());
        dto.setShippingRouteId(route.getId());
        dto.setRouteCode(route.getRouteCode());
        dto.setOriginLocationId(route.getOriginLocation().getId());
        dto.setOriginLocationName(route.getOriginLocation().getName());
        dto.setDestinationLocationId(route.getDestinationLocation().getId());
        dto.setDestinationLocationName(route.getDestinationLocation().getName());
        dto.setCurrency(entity.getCurrency());
        dto.setBaseCost(entity.getBaseCost());
        dto.setLeadTimeDayMin(entity.getLeadTimeDayMin());
        dto.setLeadTimeDayMax(entity.getLeadTimeDayMax());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setRemark(entity.getRemark());
        dto.setActive(entity.getActive());
        return dto;
    }

    private boolean matchesSupplierShippingRouteKeyword(SupplierShippingRouteEntity entity, String keyword) {
        if (keyword == null) return true;
        ShippingRouteEntity route = entity.getShippingRoute();
        SupplierShippingEntity shipping = entity.getSupplierShipping();
        return Stream.of(
                        shipping.getShippingName(),
                        shipping.getCarCode(),
                        route.getRouteCode(),
                        route.getOriginLocation().getName(),
                        route.getDestinationLocation().getName()
                )
                .filter(Objects::nonNull)
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)));
    }

    private void applySupplierShipping(SupplierShippingEntity shipping, UpsertSupplierShippingRequest request) {
        shipping.setShippingMethod(request.getShippingMethod());
        shipping.setShippingMode(request.getShippingMode() == null ? ShippingMode.STANDARD : request.getShippingMode());
        shipping.setShippingName(StringUtils.trimToNull(request.getShippingName()));
        shipping.setOriginCountryCode(StringUtils.trimToNull(request.getOriginCountryCode()));
        shipping.setOriginProvince(StringUtils.trimToNull(request.getOriginProvince()));
        shipping.setCurrency(request.getCurrency());
        shipping.setBaseCost(request.getBaseCost());
        shipping.setLeadTimeDayMin(request.getLeadTimeDayMin());
        shipping.setLeadTimeDayMax(request.getLeadTimeDayMax());
        shipping.setRemark(StringUtils.trimToNull(request.getRemark()));
        shipping.setCarCode(StringUtils.upperCase(StringUtils.trimToNull(request.getCarCode()), Locale.ROOT));
        shipping.setActive(Boolean.TRUE);

        List<SupplierShippingDestinationEntity> existingDestinations = new ArrayList<>(shipping.getDestinations());
        existingDestinations.forEach(shipping::removeDestination);

        for (UpsertSupplierShippingDestinationRequest destinationRequest : request.getDestinations()) {
            if (destinationRequest == null || StringUtils.isBlank(destinationRequest.getDestinationName())) {
                continue;
            }
            SupplierShippingDestinationEntity destination = new SupplierShippingDestinationEntity();
            destination.setDestinationCode(StringUtils.trimToNull(destinationRequest.getDestinationCode()));
            destination.setDestinationName(StringUtils.trimToNull(destinationRequest.getDestinationName()));
            destination.setCountryCode(StringUtils.trimToNull(destinationRequest.getCountryCode()));
            destination.setProvince(StringUtils.trimToNull(destinationRequest.getProvince()));
            destination.setDistrict(StringUtils.trimToNull(destinationRequest.getDistrict()));
            destination.setSubdistrict(StringUtils.trimToNull(destinationRequest.getSubdistrict()));
            destination.setPostalCode(StringUtils.trimToNull(destinationRequest.getPostalCode()));
            destination.setFullAddress(StringUtils.trimToNull(destinationRequest.getFullAddress()));
            destination.setAdditionalCost(destinationRequest.getAdditionalCost());
            destination.setSortOrder(destinationRequest.getSortOrder());
            destination.setActive(Boolean.TRUE);
            shipping.addDestination(destination);
        }
    }

    private List<SupplierCapabilityDto> buildCapabilityDtos(List<SupplierCapabilityEntity> capabilities) {
        Map<String, SupplierCapabilityDto> capabilityMap = new LinkedHashMap<>();
        Map<String, List<ProductMaterialEntity>> familyMaterialsMap = new HashMap<>();
        Map<String, Boolean> familyAllMaterialsMap = new HashMap<>();

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
                capabilityDto.setCoversAllMaterials(false);
                capabilityDto.getMaterials().clear();
                List<ProductMaterialEntity> familyMaterials = familyMaterialsMap.computeIfAbsent(
                        capability.getProductFamilyCode(),
                        productFamilyCode -> productMaterialRepository
                                .findAllByProductFamilyCodeOrderByCodeAsc(productFamilyCode)
                );
                for (ProductMaterialEntity productMaterial : familyMaterials) {
                    SupplierCapabilityMaterialDto materialDto = new SupplierCapabilityMaterialDto();
                    materialDto.setProductMaterialCode(productMaterial.getCode());
                    materialDto.setProductMaterial(toProductMaterialDto(productMaterial));
                    capabilityDto.getMaterials().add(materialDto);
                }
                familyAllMaterialsMap.put(capability.getProductFamilyCode(), true);
                continue;
            }

            if (Boolean.TRUE.equals(familyAllMaterialsMap.get(capability.getProductFamilyCode()))) {
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
