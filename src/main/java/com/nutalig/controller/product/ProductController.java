package com.nutalig.controller.product;

import com.nutalig.controller.product.request.CreateProductFamilyRequest;
import com.nutalig.controller.product.request.UpdateProductFamilyRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.ProductFamilyDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/product-families")
    public GeneralResponse<List<ProductFamilyDto>> getAllProductFamily() {
        log.info("=== Start get all product family ===");

        List<ProductFamilyDto> response = productService.getAllProductFamily();

        log.info("=== End get all product family size {} ===", response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/product-families")
    public GeneralResponse<ProductFamilyDto> createProductFamily(@RequestBody CreateProductFamilyRequest request)
            throws InvalidRequestException {
        log.info("=== Start create product family code {} ===", request.getCode());

        ProductFamilyDto response = productService.createProductFamily(request);

        log.info("=== End create product family code {} ===", response.getCode());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PutMapping("/product-families/{code}")
    public GeneralResponse<ProductFamilyDto> updateProductFamily(
            @PathVariable("code") String code,
            @RequestBody UpdateProductFamilyRequest request
    ) throws DataNotFoundException {
        log.info("=== Start update product family code {} ===", code);

        ProductFamilyDto response = productService.updateProductFamily(code, request);

        log.info("=== End update product family code {} ===", code);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/product-families/{code}")
    public GeneralResponse<String> deleteProductFamily(@PathVariable("code") String code) throws DataNotFoundException {
        log.info("=== Start delete product family code {} ===", code);

        String response = productService.deleteProductFamily(code);

        log.info("=== End delete product family code {} ===", code);
        return new GeneralResponse<>(SUCCESS, response);
    }
}
