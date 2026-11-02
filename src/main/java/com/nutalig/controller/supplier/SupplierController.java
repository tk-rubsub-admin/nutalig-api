package com.nutalig.controller.supplier;

import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.supplier.request.*;
import com.nutalig.controller.supplier.response.SearchSupplierResponse;
import com.nutalig.dto.SupplierCapabilityDto;
import com.nutalig.dto.LeadTimeConfigDto;
import com.nutalig.dto.SupplierDto;
import com.nutalig.dto.SupplierShippingDto;
import com.nutalig.dto.ShippingLocationDto;
import com.nutalig.dto.ShippingRouteDto;
import com.nutalig.dto.SupplierShippingRouteDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/v1/suppliers")
    public GeneralResponse<?> createSupplier(@RequestBody CreateSupplierRequest request) throws InvalidRequestException {
        log.info("=== Start create supplier {} ===", request.getSupplierName());

        String supplierId = supplierService.createSupplier(request);
        record CreateSupplierResponse(String id) {
        }

        log.info("=== End create supplier {} ===", supplierId);
        return new GeneralResponse<>(SUCCESS, new CreateSupplierResponse(supplierId));
    }

    @GetMapping("/v1/suppliers/{supplierId}")
    public GeneralResponse<SupplierDto> getSupplier(@PathVariable String supplierId) throws DataNotFoundException {
        log.info("=== Start get supplier {} ===", supplierId);

        SupplierDto response = supplierService.getSupplierById(supplierId);

        log.info("=== End get supplier {} ===", supplierId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/v1/suppliers/{supplierId}")
    public GeneralResponse<SupplierDto> updateSupplier(
            @PathVariable String supplierId,
            @RequestBody UpdateSupplierRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, supplierService.updateSupplier(supplierId, request));
    }

    @PostMapping(
            path = "/v1/suppliers/{supplierId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public GeneralResponse<SupplierDto> addSupplierAttachments(
            @PathVariable String supplierId,
            @RequestPart("attachments") List<MultipartFile> attachments,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start add supplier attachments {} count {} ===", supplierId, attachments.size());
        SupplierDto response = supplierService.addSupplierAttachments(supplierId, attachments, userId);
        log.info("=== End add supplier attachments {} ===", supplierId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/v1/suppliers/{supplierId}/attachments/{attachmentId}")
    public GeneralResponse<SupplierDto> deleteSupplierAttachment(
            @PathVariable String supplierId,
            @PathVariable Long attachmentId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException {
        log.info("=== Start delete supplier attachment {} {} ===", supplierId, attachmentId);
        SupplierDto response = supplierService.deleteSupplierAttachment(supplierId, attachmentId, userId);
        log.info("=== End delete supplier attachment {} {} ===", supplierId, attachmentId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/suppliers/{supplierId}/capabilities")
    public GeneralResponse<java.util.List<SupplierCapabilityDto>> getSupplierCapabilities(@PathVariable String supplierId)
            throws DataNotFoundException {
        log.info("=== Start get supplier capabilities {} ===", supplierId);

        java.util.List<SupplierCapabilityDto> response = supplierService.getSupplierCapabilities(supplierId);

        log.info("=== End get supplier capabilities {} size {} ===", supplierId, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/supplier-shippings")
    public GeneralResponse<java.util.List<SupplierShippingDto>> getSupplierShippings() {
        java.util.List<SupplierShippingDto> response = supplierService.getSupplierShippings();
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/lead-time-configs")
    public GeneralResponse<java.util.List<LeadTimeConfigDto>> getLeadTimeConfigs() {
        java.util.List<LeadTimeConfigDto> response = supplierService.getLeadTimeConfigs();
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/v1/supplier-shippings")
    public GeneralResponse<java.util.List<SupplierShippingDto>> createSupplierShipping(
            @RequestBody UpsertSupplierShippingRequest request
    ) throws InvalidRequestException {
        java.util.List<SupplierShippingDto> response = supplierService.createSupplierShipping(request);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PatchMapping("/v1/supplier-shippings/{shippingId}")
    public GeneralResponse<java.util.List<SupplierShippingDto>> updateSupplierShipping(
            @PathVariable Long shippingId,
            @RequestBody UpsertSupplierShippingRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        java.util.List<SupplierShippingDto> response = supplierService.updateSupplierShipping(shippingId, request);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/v1/supplier-shippings/{shippingId}")
    public GeneralResponse<java.util.List<SupplierShippingDto>> deleteSupplierShipping(
            @PathVariable Long shippingId
    ) throws DataNotFoundException {
        java.util.List<SupplierShippingDto> response = supplierService.deleteSupplierShipping(shippingId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/v1/shipping-locations")
    public GeneralResponse<List<ShippingLocationDto>> getShippingLocations() {
        return new GeneralResponse<>(SUCCESS, supplierService.getShippingLocations());
    }

    @GetMapping("/v2/shipping-locations")
    public GeneralResponse<List<ShippingLocationDto>> searchShippingLocations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) String countryCode) {
        return new GeneralResponse<>(SUCCESS,
                supplierService.searchShippingLocations(keyword, locationType, countryCode));
    }

    @PostMapping("/v1/shipping-locations")
    public GeneralResponse<List<ShippingLocationDto>> createShippingLocation(
            @RequestBody UpsertShippingLocationRequest request) throws InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, supplierService.createShippingLocation(request));
    }

    @PatchMapping("/v1/shipping-locations/{id}")
    public GeneralResponse<List<ShippingLocationDto>> updateShippingLocation(
            @PathVariable Long id,
            @RequestBody UpsertShippingLocationRequest request) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, supplierService.updateShippingLocation(id, request));
    }

    @DeleteMapping("/v1/shipping-locations/{id}")
    public GeneralResponse<List<ShippingLocationDto>> deleteShippingLocation(@PathVariable Long id)
            throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, supplierService.deleteShippingLocation(id));
    }

    @GetMapping("/v1/shipping-routes")
    public GeneralResponse<List<ShippingRouteDto>> getShippingRoutes() {
        return new GeneralResponse<>(SUCCESS, supplierService.getShippingRoutes());
    }

    @GetMapping("/v2/shipping-routes")
    public GeneralResponse<List<ShippingRouteDto>> searchShippingRoutes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long originLocationId,
            @RequestParam(required = false) Long destinationLocationId) {
        return new GeneralResponse<>(SUCCESS,
                supplierService.searchShippingRoutes(keyword, originLocationId, destinationLocationId));
    }

    @GetMapping("/v2/shipping-routes/check")
    public GeneralResponse<Boolean> checkShippingRoute(
            @RequestParam Long originLocationId,
            @RequestParam Long destinationLocationId) throws InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,
                supplierService.shippingRouteExists(originLocationId, destinationLocationId));
    }

    @PostMapping("/v1/shipping-routes")
    public GeneralResponse<List<ShippingRouteDto>> createShippingRoute(
            @RequestBody UpsertShippingRouteRequest request) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, supplierService.createShippingRoute(request));
    }

    @PatchMapping("/v1/shipping-routes/{id}")
    public GeneralResponse<List<ShippingRouteDto>> updateShippingRoute(
            @PathVariable Long id,
            @RequestBody UpsertShippingRouteRequest request) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, supplierService.updateShippingRoute(id, request));
    }

    @DeleteMapping("/v1/shipping-routes/{id}")
    public GeneralResponse<List<ShippingRouteDto>> deleteShippingRoute(@PathVariable Long id)
            throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, supplierService.deleteShippingRoute(id));
    }

    @GetMapping("/v1/supplier-shipping-routes")
    public GeneralResponse<List<SupplierShippingRouteDto>> getSupplierShippingRoutes(
            @RequestParam(required = false) Long supplierShippingId) {
        return new GeneralResponse<>(SUCCESS, supplierService.getSupplierShippingRoutes(supplierShippingId));
    }

    @GetMapping("/v2/supplier-shippings")
    public GeneralResponse<List<SupplierShippingRouteDto>> searchSupplierShippingsV2(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String shippingMethod,
            @RequestParam(required = false) String shippingMode,
            @RequestParam(required = false) Long supplierShippingId) {
        return new GeneralResponse<>(SUCCESS, supplierService.searchSupplierShippingsV2(
                keyword, shippingMethod, shippingMode, supplierShippingId));
    }

    @PostMapping("/v2/supplier-shippings")
    public GeneralResponse<List<SupplierShippingRouteDto>> createSupplierShippingV2(
            @RequestBody CreateSupplierShippingV2Request request)
            throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, supplierService.createSupplierShippingV2(request));
    }

    @PostMapping("/v1/supplier-shipping-routes")
    public GeneralResponse<List<SupplierShippingRouteDto>> createSupplierShippingRoute(
            @RequestBody UpsertSupplierShippingRouteRequest request)
            throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, supplierService.createSupplierShippingRoute(request));
    }

    @PatchMapping("/v1/supplier-shipping-routes/{id}")
    public GeneralResponse<List<SupplierShippingRouteDto>> updateSupplierShippingRoute(
            @PathVariable Long id,
            @RequestBody UpsertSupplierShippingRouteRequest request)
            throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, supplierService.updateSupplierShippingRoute(id, request));
    }

    @DeleteMapping("/v1/supplier-shipping-routes/{id}")
    public GeneralResponse<List<SupplierShippingRouteDto>> deleteSupplierShippingRoute(@PathVariable Long id)
            throws DataNotFoundException {
        return new GeneralResponse<>(SUCCESS, supplierService.deleteSupplierShippingRoute(id));
    }

    @PostMapping("/v1/suppliers/{supplierId}/capabilities")
    public GeneralResponse<java.util.List<SupplierCapabilityDto>> addSupplierMaterialCapability(
            @PathVariable String supplierId,
            @RequestBody AddSupplierMaterialCapabilityRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start add supplier material capability batch {} size {} ===",
                supplierId, request == null || request.getCapabilities() == null ? 0 : request.getCapabilities().size());

        java.util.List<SupplierCapabilityDto> response = supplierService.addSupplierMaterialCapability(supplierId, request);

        log.info("=== End add supplier material capability batch {} size {} ===", supplierId, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/v1/suppliers/suggest-suppliers")
    public GeneralResponse<java.util.List<SupplierCapabilityDto>> suggestSupplierCapabilities(
            @RequestBody SuggestSupplierCapabilitiesRequest request
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start suggest supplier capabilities supplier {} size {} ===",
                request == null ? null : request.getSupplierId(),
                request == null || request.getCapabilities() == null ? 0 : request.getCapabilities().size());

        java.util.List<SupplierCapabilityDto> response = supplierService.suggestSupplierCapabilities(
                request == null ? null : request.getSupplierId(),
                request == null ? null : request.getCapabilities()
        );

        log.info("=== End suggest supplier capabilities supplier {} size {} ===",
                request == null ? null : request.getSupplierId(), response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/v1/suppliers/{supplierId}/capabilities/families/{productFamilyCode}")
    public GeneralResponse<java.util.List<SupplierCapabilityDto>> deleteSupplierFamilyCapability(
            @PathVariable String supplierId,
            @PathVariable String productFamilyCode
    ) throws DataNotFoundException {
        log.info("=== Start delete supplier family capability {} {} ===", supplierId, productFamilyCode);

        java.util.List<SupplierCapabilityDto> response =
                supplierService.deleteSupplierFamilyCapability(supplierId, productFamilyCode);

        log.info("=== End delete supplier family capability {} {} ===", supplierId, productFamilyCode);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/v1/suppliers/{supplierId}/capabilities/materials/{productFamilyCode}/{productMaterialCode}")
    public GeneralResponse<java.util.List<SupplierCapabilityDto>> deleteSupplierMaterialCapability(
            @PathVariable String supplierId,
            @PathVariable String productFamilyCode,
            @PathVariable String productMaterialCode
    ) throws DataNotFoundException {
        log.info("=== Start delete supplier material capability {} {}:{} ===",
                supplierId, productFamilyCode, productMaterialCode);

        java.util.List<SupplierCapabilityDto> response =
                supplierService.deleteSupplierMaterialCapability(supplierId, productFamilyCode, productMaterialCode);

        log.info("=== End delete supplier material capability {} {}:{} ===",
                supplierId, productFamilyCode, productMaterialCode);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/v1/suppliers/search")
    public GeneralResponse<SearchSupplierResponse> searchSupplier(
            @RequestBody(required = false) SearchSupplierRequest searchSupplierRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search supplier page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        SearchSupplierResponse response = supplierService.searchSupplier(searchSupplierRequest, pageableRequest);

        log.info("=== End search supplier page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
        return new GeneralResponse<>(SUCCESS, response);
    }
}
