package com.nutalig.controller.customer;

import com.nutalig.controller.customer.request.*;
import com.nutalig.controller.customer.response.SearchCustomerResponse;
import com.nutalig.controller.customer.response.UploadCustomerResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.CustomerDashboardDto;
import com.nutalig.dto.CustomerDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.CustomerService;
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
@RequestMapping("/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public GeneralResponse createCustomer(@RequestBody CreateCustomerRequest request, @RequestHeader("userId") String createdBy)
            throws InvalidRequestException {
        log.info("=== Start create customer ===");

        String customerId = customerService.createCustomer(request, createdBy);
        record CreateCustomerResponse(String id) {

        }

        log.info("=== End create customer ===");
        return new GeneralResponse<>(SUCCESS, new CreateCustomerResponse(customerId));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GeneralResponse<UploadCustomerResponse> uploadCustomers(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("userId") String userId
    ) throws Exception {
        log.info("=== Start upload customers ===");

        UploadCustomerResponse response = customerService.uploadCustomers(file, userId);

        log.info("=== End upload customers ===");
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/{id}")
    public GeneralResponse<CustomerDto> getCustomer(@PathVariable("id") String custId) throws DataNotFoundException {
        log.info("=== Start get customer ===");

        CustomerDto customerDto = customerService.getCustomerById(custId);

        log.info("=== End get customer ===");
        return new GeneralResponse<>(SUCCESS, customerDto);
    }

    @PatchMapping("/{customerId}")
    public GeneralResponse<CustomerDto> updateCustomer(
            @PathVariable String customerId,
            @RequestBody UpdateCustomerRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start update customer {} ===", customerId);

        CustomerDto response = customerService.updateCustomer(customerId, request, userId);

        log.info("=== End update customer {} ===", customerId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/{customerId}")
    public GeneralResponse<CustomerDto> deleteCustomer(
            @PathVariable String customerId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException {
        log.info("=== Start delete customer {} ===", customerId);

        CustomerDto response = customerService.deleteCustomer(customerId, userId);

        log.info("=== End delete customer {} ===", customerId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping
    public GeneralResponse<SearchCustomerResponse> getCustomers(
            SearchCustomerRequest searchCustomerRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start get customers page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        SearchCustomerResponse response = customerService.searchCustomer(searchCustomerRequest, pageableRequest);

        log.info("=== End get customers page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/search")
    public GeneralResponse<SearchCustomerResponse> searchCustomer(
            @RequestBody(required = false) SearchCustomerRequest searchCustomerRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search customer page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        SearchCustomerResponse response = customerService.searchCustomer(searchCustomerRequest, pageableRequest);

        log.info("=== End search customer page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/all")
    public GeneralResponse<List<CustomerDto>> getAllCustomer(@RequestBody(required = false) SearchCustomerRequest searchCustomerRequest) {
        log.info("=== Start get all customer ===");

        List<CustomerDto> response = customerService.getAllCustomer(searchCustomerRequest);

        log.info("=== End get all customer ===");
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/dashboard")
    public GeneralResponse<CustomerDashboardDto> getCustomerDashboard(
            @RequestParam(required = false) String salesId
    ) {
        log.info("=== Start get customer dashboard with salesId {} ===", salesId);

        CustomerDashboardDto response = customerService.getCustomerDashboard(salesId);

        log.info("=== End get customer dashboard with salesId {} ===", salesId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{customerId}/addresses")
    public GeneralResponse<CustomerDto> addCustomerAddress(
            @PathVariable String customerId,
            @RequestBody CreateCustomerAddressRequest request
    ) throws DataNotFoundException {
        log.info("=== Start add customer address to customer {} ===", customerId);

        CustomerDto response = customerService.addCustomerAddress(customerId, request);

        log.info("=== End add customer address to customer {} ===", customerId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/{customerId}/addresses/{addressId}")
    public GeneralResponse<CustomerDto> deleteCustomerAddress(
            @PathVariable String customerId,
            @PathVariable Long addressId
    ) throws DataNotFoundException {
        log.info("=== Start delete customer address {} from customer {} ===", addressId, customerId);

        CustomerDto response = customerService.deleteCustomerAddress(customerId, addressId);

        log.info("=== End delete customer address {} from customer {} ===", addressId, customerId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/{customerId}/contacts/{contactId}")
    public GeneralResponse<CustomerDto> deleteCustomerContact(
            @PathVariable String customerId,
            @PathVariable Long contactId
    ) throws DataNotFoundException {
        log.info("=== Start delete customer contact {} from customer {} ===", contactId, customerId);

        CustomerDto response = customerService.deleteCustomerContact(customerId, contactId);

        log.info("=== End delete customer contact {} from customer {} ===", contactId, customerId);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/{customerId}/contacts")
    public GeneralResponse<CustomerDto> addCustomerContact(
            @PathVariable String customerId,
            @RequestBody CreateCustomerContactRequest request
    ) throws DataNotFoundException {
        log.info("=== Start add customer contact to customer {} ===", customerId);

        CustomerDto response = customerService.addCustomerContact(customerId, request);

        log.info("=== End add customer contact to customer {} ===", customerId);
        return new GeneralResponse<>(SUCCESS, response);
    }

}
