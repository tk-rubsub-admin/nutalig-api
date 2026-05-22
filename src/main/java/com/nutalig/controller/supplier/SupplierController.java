package com.nutalig.controller.supplier;

import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.controller.supplier.request.AddSupplierFamilyCapabilityRequest;
import com.nutalig.controller.supplier.request.AddSupplierMaterialCapabilityRequest;
import com.nutalig.controller.supplier.request.CreateSupplierRequest;
import com.nutalig.controller.supplier.request.SearchSupplierRequest;
import com.nutalig.controller.supplier.response.SearchSupplierResponse;
import com.nutalig.dto.SupplierCapabilityDto;
import com.nutalig.dto.SupplierDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/v1/suppliers/{supplierId}/capabilities")
    public GeneralResponse<java.util.List<SupplierCapabilityDto>> getSupplierCapabilities(@PathVariable String supplierId)
            throws DataNotFoundException {
        log.info("=== Start get supplier capabilities {} ===", supplierId);

        java.util.List<SupplierCapabilityDto> response = supplierService.getSupplierCapabilities(supplierId);

        log.info("=== End get supplier capabilities {} size {} ===", supplierId, response.size());
        return new GeneralResponse<>(SUCCESS, response);
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
