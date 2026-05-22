package com.nutalig.controller.supplier.response;

import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.SupplierDto;
import lombok.Data;

import java.util.List;

@Data
public class SearchSupplierResponse {

    private List<SupplierDto> suppliers;
    private Pagination pagination;
}
