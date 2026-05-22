package com.nutalig.controller.product;

import com.nutalig.controller.product.request.CreateProductFamilyRequest;
import com.nutalig.controller.product.request.CreateProductSubtype1Request;
import com.nutalig.controller.product.request.CreateProductSubtype2Request;
import com.nutalig.controller.product.request.UpdateProductFamilyRequest;
import com.nutalig.controller.product.request.UpdateProductSubtype1Request;
import com.nutalig.controller.product.request.UpdateProductSubtype2Request;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.ProductFamilyDto;
import com.nutalig.dto.ProductMaterialDto;
import com.nutalig.dto.ProductSubtype1Dto;
import com.nutalig.dto.ProductSubtype2Dto;
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
    public GeneralResponse<String> deleteProductFamily(@PathVariable("code") String code)
            throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start delete product family code {} ===", code);

        String response = productService.deleteProductFamily(code);

        log.info("=== End delete product family code {} ===", code);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/product-families/{familyCode}/subtype1")
    public GeneralResponse<List<ProductSubtype1Dto>> getProductSubtype1ByFamily(
            @PathVariable("familyCode") String familyCode
    ) {
        log.info("=== Start get product subtype1 by family code {} ===", familyCode);

        List<ProductSubtype1Dto> response = productService.getProductSubtype1ByFamily(familyCode);

        log.info("=== End get product subtype1 by family code {} size {} ===", familyCode, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/product-families/{familyCode}/materials")
    public GeneralResponse<List<ProductMaterialDto>> getProductMaterialByFamily(
            @PathVariable("familyCode") String familyCode
    ) {
        log.info("=== Start get product material by family code {} ===", familyCode);

        List<ProductMaterialDto> response = productService.getProductMaterialByFamily(familyCode);

        log.info("=== End get product material by family code {} size {} ===", familyCode, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/product-subtype1")
    public GeneralResponse<ProductSubtype1Dto> createProductSubtype1(@RequestBody CreateProductSubtype1Request request)
            throws InvalidRequestException, DataNotFoundException {
        log.info("=== Start create product subtype1 code {} ===", request.getCode());

        ProductSubtype1Dto response = productService.createProductSubtype1(request);

        log.info("=== End create product subtype1 code {} ===", response.getCode());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PutMapping("/product-subtype1/{code}")
    public GeneralResponse<ProductSubtype1Dto> updateProductSubtype1(
            @PathVariable("code") String code,
            @RequestBody UpdateProductSubtype1Request request
    ) throws DataNotFoundException {
        log.info("=== Start update product subtype1 code {} ===", code);

        ProductSubtype1Dto response = productService.updateProductSubtype1(code, request);

        log.info("=== End update product subtype1 code {} ===", code);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/product-subtype1/{code}")
    public GeneralResponse<String> deleteProductSubtype1(@PathVariable("code") String code)
            throws DataNotFoundException, InvalidRequestException {
        log.info("=== Start delete product subtype1 code {} ===", code);

        String response = productService.deleteProductSubtype1(code);

        log.info("=== End delete product subtype1 code {} ===", code);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @GetMapping("/product-subtype1/{subtype1Code}/subtype2")
    public GeneralResponse<List<ProductSubtype2Dto>> getProductSubtype2BySubtype1(
            @PathVariable("subtype1Code") String subtype1Code
    ) {
        log.info("=== Start get product subtype2 by subtype1 code {} ===", subtype1Code);

        List<ProductSubtype2Dto> response = productService.getProductSubtype2BySubtype1(subtype1Code);

        log.info("=== End get product subtype2 by subtype1 code {} size {} ===", subtype1Code, response.size());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PostMapping("/product-subtype2")
    public GeneralResponse<ProductSubtype2Dto> createProductSubtype2(@RequestBody CreateProductSubtype2Request request)
            throws InvalidRequestException, DataNotFoundException {
        log.info("=== Start create product subtype2 code {} ===", request.getCode());

        ProductSubtype2Dto response = productService.createProductSubtype2(request);

        log.info("=== End create product subtype2 code {} ===", response.getCode());
        return new GeneralResponse<>(SUCCESS, response);
    }

    @PutMapping("/product-subtype2/{code}")
    public GeneralResponse<ProductSubtype2Dto> updateProductSubtype2(
            @PathVariable("code") String code,
            @RequestBody UpdateProductSubtype2Request request
    ) throws DataNotFoundException {
        log.info("=== Start update product subtype2 code {} ===", code);

        ProductSubtype2Dto response = productService.updateProductSubtype2(code, request);

        log.info("=== End update product subtype2 code {} ===", code);
        return new GeneralResponse<>(SUCCESS, response);
    }

    @DeleteMapping("/product-subtype2/{code}")
    public GeneralResponse<String> deleteProductSubtype2(@PathVariable("code") String code)
            throws DataNotFoundException {
        log.info("=== Start delete product subtype2 code {} ===", code);

        String response = productService.deleteProductSubtype2(code);

        log.info("=== End delete product subtype2 code {} ===", code);
        return new GeneralResponse<>(SUCCESS, response);
    }
}
