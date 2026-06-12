package com.nutalig.service;

import com.nutalig.controller.product.request.*;
import com.nutalig.dto.ProductFamilyDto;
import com.nutalig.dto.ProductMaterialDto;
import com.nutalig.dto.ProductSubtype1Dto;
import com.nutalig.dto.ProductSubtype2Dto;
import com.nutalig.entity.ProductFamilyEntity;
import com.nutalig.entity.ProductSubtype1Entity;
import com.nutalig.entity.ProductSubtype2Entity;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.ProductFamilyMapper;
import com.nutalig.mapper.ProductMaterialMapper;
import com.nutalig.mapper.ProductSubtype1Mapper;
import com.nutalig.mapper.ProductSubtype2Mapper;
import com.nutalig.repository.ProductFamilyRepository;
import com.nutalig.repository.ProductMaterialRepository;
import com.nutalig.repository.ProductSubtype1Repository;
import com.nutalig.repository.ProductSubtype2Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductFamilyRepository productFamilyRepository;
    private final ProductMaterialRepository productMaterialRepository;
    private final ProductSubtype1Repository productSubtype1Repository;
    private final ProductSubtype2Repository productSubtype2Repository;
    private final ProductFamilyMapper productFamilyMapper;
    private final ProductMaterialMapper productMaterialMapper;
    private final ProductSubtype1Mapper productSubtype1Mapper;
    private final ProductSubtype2Mapper productSubtype2Mapper;

    @Transactional(readOnly = true)
    public List<ProductFamilyDto> getAllProductFamily() {
        log.info("Get all product families");

        List<ProductFamilyDto> families = productFamilyRepository.findAll().stream()
                .map(productFamilyMapper::toDto)
                .toList();

        List<ProductSubtype1Dto> subtype1List = productSubtype1Repository.findAllByOrderByProductFamilyCodeAscCodeAsc().stream()
                .map(productSubtype1Mapper::toDto)
                .toList();

        List<ProductMaterialDto> productMaterialList = productMaterialRepository.findAll().stream()
                .map(productMaterialMapper::toDto)
                .toList();

        List<ProductSubtype2Dto> subtype2List = productSubtype2Repository.findAllByOrderByProductSubtype1CodeAscCodeAsc().stream()
                .map(productSubtype2Mapper::toDto)
                .toList();

        Map<String, List<ProductSubtype2Dto>> subtype2Map = new LinkedHashMap<>();
        for (ProductSubtype2Dto subtype2 : subtype2List) {
            subtype2Map.computeIfAbsent(subtype2.getProductSubtype1Code(), key -> new ArrayList<>()).add(subtype2);
        }

        Map<String, List<ProductSubtype1Dto>> subtype1Map = new LinkedHashMap<>();
        for (ProductSubtype1Dto subtype1 : subtype1List) {
            subtype1.setSubtype2List(subtype2Map.getOrDefault(subtype1.getCode(), List.of()));
            subtype1Map.computeIfAbsent(subtype1.getProductFamilyCode(), key -> new ArrayList<>()).add(subtype1);
        }

        Map<String, List<ProductMaterialDto>> productMaterialMap = new LinkedHashMap<>();
        for (ProductMaterialDto productMaterial : productMaterialList) {
            productMaterialMap.computeIfAbsent(productMaterial.getProductFamilyCode(), key -> new ArrayList<>())
                    .add(productMaterial);
        }

        for (ProductFamilyDto family : families) {
            family.setMaterialList(productMaterialMap.getOrDefault(family.getCode(), List.of()));
            family.setSubtype1List(subtype1Map.getOrDefault(family.getCode(), List.of()));
        }

        return families;
    }

    @Transactional(readOnly = true)
    public List<ProductSubtype1Dto> getProductSubtype1ByFamily(String familyCode) {
        log.info("Get product subtype1 by family code: {}", familyCode);

        return productSubtype1Repository.findAllByProductFamilyCodeOrderByCodeAsc(familyCode).stream()
                .map(productSubtype1Mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductMaterialDto> getProductMaterialByFamily(String familyCode) {
        log.info("Get product material by family code: {}", familyCode);

        return productMaterialRepository.findAllByProductFamilyCodeOrderByCodeAsc(familyCode).stream()
                .map(productMaterialMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductSubtype2Dto> getProductSubtype2BySubtype1(String subtype1Code) {
        log.info("Get product subtype2 by subtype1 code: {}", subtype1Code);

        return productSubtype2Repository.findAllByProductSubtype1CodeOrderByCodeAsc(subtype1Code).stream()
                .map(productSubtype2Mapper::toDto)
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
    public String deleteProductFamily(String code) throws DataNotFoundException, InvalidRequestException {
        log.info("Delete product family code: {}", code);

        ProductFamilyEntity entity = productFamilyRepository.findById(code)
                .orElseThrow(() -> new DataNotFoundException("Product family code " + code + " not found."));

        if (productSubtype1Repository.existsByProductFamilyCode(code)) {
            throw new InvalidRequestException("Product family code " + code + " is in use by subtype1.");
        }

        productFamilyRepository.delete(entity);

        log.info("Delete product family success code: {}", code);
        return code;
    }

    @Transactional
    public ProductSubtype1Dto createProductSubtype1(CreateProductSubtype1Request request)
            throws InvalidRequestException, DataNotFoundException {
        log.info("Create product subtype1 request: {}", request);

        validateCreateSubtype1Request(request);
        validateProductFamilyExists(request.getProductFamilyCode().trim());

        boolean existed = productSubtype1Repository.findById(request.getCode().trim()).isPresent();
        if (existed) {
            throw new InvalidRequestException("Product subtype1 code " + request.getCode() + " already exists.");
        }

        ProductSubtype1Dto dto = new ProductSubtype1Dto();
        dto.setCode(request.getCode().trim());
        dto.setProductFamilyCode(request.getProductFamilyCode().trim());
        dto.setNameTh(StringUtils.trimToNull(request.getNameTh()));
        dto.setNameEn(StringUtils.trimToNull(request.getNameEn()));
        dto.setSubtype2Required(Boolean.TRUE.equals(request.getSubtype2Required()));

        ProductSubtype1Entity entity = productSubtype1Repository.save(productSubtype1Mapper.toEntity(dto));

        log.info("Create product subtype1 success code: {}", entity.getCode());
        return productSubtype1Mapper.toDto(entity);
    }

    @Transactional
    public ProductSubtype1Dto updateProductSubtype1(String code, UpdateProductSubtype1Request request)
            throws DataNotFoundException {
        log.info("Update product subtype1 code: {}, request: {}", code, request);

        ProductSubtype1Entity entity = productSubtype1Repository.findById(code)
                .orElseThrow(() -> new DataNotFoundException("Product subtype1 code " + code + " not found."));

        if (request.getProductFamilyCode() != null) {
            validateProductFamilyExistsUnchecked(request.getProductFamilyCode().trim());
            entity.setProductFamilyCode(request.getProductFamilyCode().trim());
        }

        if (request.getNameTh() != null) {
            entity.setNameTh(StringUtils.trimToNull(request.getNameTh()));
        }

        if (request.getNameEn() != null) {
            entity.setNameEn(StringUtils.trimToNull(request.getNameEn()));
        }

        if (request.getSubtype2Required() != null) {
            entity.setSubtype2Required(request.getSubtype2Required());
        }

        entity = productSubtype1Repository.save(entity);

        log.info("Update product subtype1 success code: {}", code);
        return productSubtype1Mapper.toDto(entity);
    }

    @Transactional
    public String deleteProductSubtype1(String code) throws DataNotFoundException, InvalidRequestException {
        log.info("Delete product subtype1 code: {}", code);

        ProductSubtype1Entity entity = productSubtype1Repository.findById(code)
                .orElseThrow(() -> new DataNotFoundException("Product subtype1 code " + code + " not found."));

        if (productSubtype2Repository.existsByProductSubtype1Code(code)) {
            throw new InvalidRequestException("Product subtype1 code " + code + " is in use by subtype2.");
        }

        productSubtype1Repository.delete(entity);

        log.info("Delete product subtype1 success code: {}", code);
        return code;
    }

    @Transactional
    public ProductSubtype2Dto createProductSubtype2(CreateProductSubtype2Request request)
            throws InvalidRequestException, DataNotFoundException {
        log.info("Create product subtype2 request: {}", request);

        validateCreateSubtype2Request(request);
        validateProductSubtype1Exists(request.getProductSubtype1Code().trim());

        boolean existed = productSubtype2Repository.findById(request.getCode().trim()).isPresent();
        if (existed) {
            throw new InvalidRequestException("Product subtype2 code " + request.getCode() + " already exists.");
        }

        ProductSubtype2Dto dto = new ProductSubtype2Dto();
        dto.setCode(request.getCode().trim());
        dto.setProductSubtype1Code(request.getProductSubtype1Code().trim());
        dto.setNameTh(StringUtils.trimToNull(request.getNameTh()));
        dto.setNameEn(StringUtils.trimToNull(request.getNameEn()));

        ProductSubtype2Entity entity = productSubtype2Repository.save(productSubtype2Mapper.toEntity(dto));

        log.info("Create product subtype2 success code: {}", entity.getCode());
        return productSubtype2Mapper.toDto(entity);
    }

    @Transactional
    public ProductSubtype2Dto updateProductSubtype2(String code, UpdateProductSubtype2Request request)
            throws DataNotFoundException {
        log.info("Update product subtype2 code: {}, request: {}", code, request);

        ProductSubtype2Entity entity = productSubtype2Repository.findById(code)
                .orElseThrow(() -> new DataNotFoundException("Product subtype2 code " + code + " not found."));

        if (request.getProductSubtype1Code() != null) {
            validateProductSubtype1ExistsUnchecked(request.getProductSubtype1Code().trim());
            entity.setProductSubtype1Code(request.getProductSubtype1Code().trim());
        }

        if (request.getNameTh() != null) {
            entity.setNameTh(StringUtils.trimToNull(request.getNameTh()));
        }

        if (request.getNameEn() != null) {
            entity.setNameEn(StringUtils.trimToNull(request.getNameEn()));
        }

        entity = productSubtype2Repository.save(entity);

        log.info("Update product subtype2 success code: {}", code);
        return productSubtype2Mapper.toDto(entity);
    }

    @Transactional
    public String deleteProductSubtype2(String code) throws DataNotFoundException {
        log.info("Delete product subtype2 code: {}", code);

        ProductSubtype2Entity entity = productSubtype2Repository.findById(code)
                .orElseThrow(() -> new DataNotFoundException("Product subtype2 code " + code + " not found."));

        productSubtype2Repository.delete(entity);

        log.info("Delete product subtype2 success code: {}", code);
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

    private void validateCreateSubtype1Request(CreateProductSubtype1Request request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }

        if (StringUtils.isBlank(request.getCode())) {
            throw new InvalidRequestException("Product subtype1 code is required.");
        }

        if (StringUtils.isBlank(request.getProductFamilyCode())) {
            throw new InvalidRequestException("Product family code is required.");
        }
    }

    private void validateCreateSubtype2Request(CreateProductSubtype2Request request) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("Request is required.");
        }

        if (StringUtils.isBlank(request.getCode())) {
            throw new InvalidRequestException("Product subtype2 code is required.");
        }

        if (StringUtils.isBlank(request.getProductSubtype1Code())) {
            throw new InvalidRequestException("Product subtype1 code is required.");
        }
    }

    private void validateProductFamilyExists(String familyCode) throws DataNotFoundException {
        validateProductFamilyExistsUnchecked(familyCode);
    }

    private void validateProductFamilyExistsUnchecked(String familyCode) throws DataNotFoundException {
        productFamilyRepository.findById(familyCode)
                .orElseThrow(() -> new DataNotFoundException("Product family code " + familyCode + " not found."));
    }

    private void validateProductSubtype1Exists(String subtype1Code) throws DataNotFoundException {
        validateProductSubtype1ExistsUnchecked(subtype1Code);
    }

    private void validateProductSubtype1ExistsUnchecked(String subtype1Code) throws DataNotFoundException {
        productSubtype1Repository.findById(subtype1Code)
                .orElseThrow(() -> new DataNotFoundException("Product subtype1 code " + subtype1Code + " not found."));
    }
}
