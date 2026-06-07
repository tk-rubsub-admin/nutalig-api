package com.nutalig.controller.customer;

import com.nutalig.controller.customer.request.CreateCustomerRequest;
import com.nutalig.controller.customer.request.CreateCustomerAddressRequest;
import com.nutalig.controller.customer.request.CreateCustomerContactRequest;
import com.nutalig.controller.customer.request.SearchCustomerRequest;
import com.nutalig.controller.customer.request.UpdateCustomerRequest;
import com.nutalig.controller.customer.response.SearchCustomerResponse;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.CustomerDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
