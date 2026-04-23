package com.nutalig.controller.customer.response;

import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.CustomerDto;
import lombok.Data;

import java.util.List;

@Data
public class SearchCustomerResponse {

    private List<CustomerDto> customers;
    private Pagination pagination;
}
