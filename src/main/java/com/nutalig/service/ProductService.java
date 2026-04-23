package com.nutalig.service;

import com.nutalig.controller.product.request.CreateProductFamilyRequest;
import com.nutalig.controller.product.request.UpdateProductFamilyRequest;
import com.nutalig.dto.ProductFamilyDto;
import com.nutalig.entity.ProductFamilyEntity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.ProductFamilyMapper;
import com.nutalig.repository.ProductFamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductFamilyRepository productFamilyRepository;
    private final ProductFamilyMapper productFamilyMapper;

    @Transactional(readOnly = true)
    public List<ProductFamilyDto> getAllProductFamily() {
        log.info("Get all product families");

        return productFamilyRepository.findAll().stream()
                .map(productFamilyMapper::toDto)
                .toList();
    }

    @Transactional
    public ProductFamilyDto createProductFamily(CreateProductFamilyRequest request) throws InvalidRequestException {
        log.info("Create product family request: {}", request);

        validateCreateRequest(request);

        boolean existed = productFamilyRepository.findById(request.getCode().trim()).isPresent();
        if (existed) {
            throw new InvalidRequestException("Product family code " + request.getCode() + " already exists.");
        }

        ProductFamilyDto dto = new ProductFamilyDto();
        dto.setCode(request.getCode().trim());
        dto.setNameTh(StringUtils.trimToNull(request.getNameTh()));
        dto.setNameEn(StringUtils.trimToNull(request.getNameEn()));

        ProductFamilyEntity entity = productFamilyRepository.save(productFamilyMapper.toEntity(dto));

        log.info("Create product family success code: {}", entity.getCode());
        return productFamilyMapper.toDto(entity);
    }

    @Transactional
    public ProductFamilyDto updateProductFamily(String code, UpdateProductFamilyRequest request) throws DataNotFoundException {
        log.info("Update product family code: {}, request: {}", code, request);

        ProductFamilyEntity entity = productFamilyRepository.findById(code)
                .orElseThrow(() -> new DataNotFoundException("Product family code " + code + " not found."));

        if (request.getNameTh() != null) {
            entity.setNameTh(StringUtils.trimToNull(request.getNameTh()));
        }

        if (request.getNameEn() != null) {
            entity.setNameEn(StringUtils.trimToNull(request.getNameEn()));
        }

        entity = productFamilyRepository.save(entity);

        log.info("Update product family success code: {}", code);
        return productFamilyMapper.toDto(entity);
    }

    @Transactional
    public String deleteProductFamily(String code) throws DataNotFoundException {
        log.info("Delete product family code: {}", code);

        ProductFamilyEntity entity = productFamilyRepository.findById(code)
                .orElseThrow(() -> new DataNotFoundException("Product family code " + code + " not found."));

        productFamilyRepository.delete(entity);

        log.info("Delete product family success code: {}", code);
        return code;
    }

    private void validateCreateRequest(CreateProductFamilyRequest request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }

        if (StringUtils.isBlank(request.getCode())) {
            throw new InvalidRequestException("Product family code is required.");
        }
    }
}
