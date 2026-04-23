package com.nutalig.controller.customer;

import com.nutalig.controller.customer.request.CreateCustomerRequest;
import com.nutalig.controller.customer.request.SearchCustomerRequest;
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
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/v1/customers")
    public GeneralResponse createCustomer(@RequestBody CreateCustomerRequest request, @RequestHeader("userId") String createdBy)
            throws InvalidRequestException {
        log.info("=== Start create customer ===");

        String customerId = customerService.createCustomer(request, createdBy);
        record CreateCustomerResponse(String id) {

        }

        log.info("=== End create customer ===");
        return new GeneralResponse<>(SUCCESS, new CreateCustomerResponse(customerId));
    }

    @GetMapping("/v1/customers/{id}")
    public GeneralResponse<CustomerDto> getCustomer(@PathVariable("id") String custId) throws DataNotFoundException {
        log.info("=== Start get customer ===");

        CustomerDto customerDto = customerService.getCustomerById(custId);

        log.info("=== End get customer ===");
        return new GeneralResponse<>(SUCCESS, customerDto);
    }

    @GetMapping("/v1/customers")
    public GeneralResponse<SearchCustomerResponse> getCustomers(
            SearchCustomerRequest searchCustomerRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start get customers page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        SearchCustomerResponse response = customerService.searchCustomer(searchCustomerRequest, pageableRequest);

        log.info("=== End get customers page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/v1/customers/search")
    public GeneralResponse<SearchCustomerResponse> searchCustomer(
            @RequestBody(required = false) SearchCustomerRequest searchCustomerRequest,
            @Valid PageableRequest pageableRequest
    ) {
        log.info("=== Start search customer page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());

        SearchCustomerResponse response = customerService.searchCustomer(searchCustomerRequest, pageableRequest);

        log.info("=== End search customer page {} size {} ===", pageableRequest.getPage(), pageableRequest.getSize());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/v1/customers/all")
    public GeneralResponse<List<CustomerDto>> getAllCustomer(@RequestBody(required = false) SearchCustomerRequest searchCustomerRequest) {
        log.info("=== Start get all customer ===");

        List<CustomerDto> response = customerService.getAllCustomer(searchCustomerRequest);

        log.info("=== End get all customer ===");
        return new GeneralResponse<>(SUCCESS, response);
    }

}
