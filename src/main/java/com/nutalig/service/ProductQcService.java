package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.constant.QcTypes.*;
import com.nutalig.dto.ProductQcResolutionDto;
import com.nutalig.dto.ProductQcResolutionDto.QcDocumentFileDto;
import com.nutalig.dto.ProductQcResolutionDto.QcRouteDto;
import com.nutalig.entity.*;
import com.nutalig.entity.id.ProductMaterialId;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductQcService {
    private static final String HOLD_ROUTE = "QC-HOLD";

    private final AppProperties appProperties;
    private final ProductFamilyRepository productFamilyRepository;
    private final ProductMaterialRepository productMaterialRepository;
    private final ProductSubtype1Repository productSubtype1Repository;
    private final ProductMaterialQcRuleRepository ruleRepository;
    private final ProductFamilyQcPolicyRepository policyRepository;
    private final QcRouteRepository routeRepository;
    private final QcDocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public ProductQcResolutionDto resolve(String familyCode, String materialCode, String subtype1Code)
            throws DataNotFoundException, InvalidRequestException {
        familyCode = StringUtils.trimToNull(familyCode);
        materialCode = StringUtils.trimToNull(materialCode);
        subtype1Code = StringUtils.trimToNull(subtype1Code);
        if (familyCode == null) {
            throw new InvalidRequestException("Product family is required for QC routing.");
        }
        ProductFamilyEntity family = productFamilyRepository.findById(familyCode)
                .orElseThrow(() -> new DataNotFoundException("Product family not found."));
        if (!Boolean.TRUE.equals(family.getIsActive())) {
            throw new InvalidRequestException("Product family is inactive.");
        }
        familyCode = family.getCode();
        subtype1Code = validateProductContext(familyCode, materialCode, subtype1Code);

        String taxonomyRevision = appProperties.getProductTaxonomy().getActiveRevision();
        if (StringUtils.isBlank(taxonomyRevision)) {
            throw new IllegalStateException("Active product taxonomy revision is not configured.");
        }
        ProductFamilyQcPolicyEntity policy = policyRepository
                .findByTaxonomyRevisionAndProductFamilyCodeAndIsActiveTrue(taxonomyRevision, familyCode)
                .orElse(null);
        ProductMaterialQcRuleEntity rule = null;
        MatchLevel matchLevel = MatchLevel.NONE;
        if (materialCode != null) {
            List<ProductMaterialQcRuleEntity> rules = ruleRepository
                    .findAllByTaxonomyRevisionAndProductFamilyCodeAndProductMaterialCodeAndStatusOrderByIdAsc(
                            taxonomyRevision, familyCode, materialCode, RuleStatus.ACTIVE);
            if (subtype1Code != null) {
                String selectedSubtype = subtype1Code;
                rule = rules.stream().filter(r -> selectedSubtype.equals(r.getProductSubtype1Code()))
                        .findFirst().orElse(null);
                if (rule != null) matchLevel = MatchLevel.SPECIFIC;
            }
            if (rule == null) {
                rule = rules.stream().filter(r -> r.getProductSubtype1Code() == null).findFirst().orElse(null);
                if (rule != null) matchLevel = MatchLevel.DEFAULT;
            }
        }

        String holdReason = materialCode == null ? "MATERIAL_NOT_SELECTED" : rule == null ? "NO_MAPPING" : null;
        QcRouteEntity route = rule == null ? null : activeRoute(rule.getQcRouteCode());
        if (route == null) {
            if (rule != null) holdReason = "QC_ROUTE_INACTIVE";
            route = routeRepository.findById(HOLD_ROUTE)
                    .filter(r -> Boolean.TRUE.equals(r.getIsActive()) && r.getRouteType() == RouteType.HOLD)
                    .orElseThrow(() -> new IllegalStateException("QC-HOLD route is not configured."));
        }
        if (route.getRouteType() == RouteType.HOLD && holdReason == null) {
            holdReason = "PURCHASING_CONFIRMATION_REQUIRED";
        }

        boolean requiresPlan = policy != null && Boolean.TRUE.equals(policy.getSpecialInspectionPlanRequired());
        QcRouteEntity specialRoute = requiresPlan ? activeRoute(policy.getSpecialQcRouteCode()) : null;
        if (specialRoute != null && specialRoute.getRouteType() != RouteType.SPECIAL) {
            throw new IllegalStateException("Family QC policy must reference a SPECIAL route.");
        }
        LinkedHashMap<String, DocumentType> requiredDocumentRoutes = new LinkedHashMap<>();
        HashSet<String> unavailableDocumentRoutes = new HashSet<>();
        if (route.getRouteType() != RouteType.HOLD) {
            requiredDocumentRoutes.put(route.getCode(), route.getRouteType() == RouteType.SPECIAL
                    ? DocumentType.SPECIAL_PLAN_TEMPLATE : DocumentType.MATERIAL_QC);
        }
        if (requiresPlan) {
            requiredDocumentRoutes.put(policy.getSpecialQcRouteCode(), DocumentType.SPECIAL_PLAN_TEMPLATE);
            if (specialRoute == null) unavailableDocumentRoutes.add(policy.getSpecialQcRouteCode());
        }
        // A selected template is not an approved inspection plan for an individual RFQ.
        List<QcDocumentFileDto> documents = requiredDocumentRoutes.isEmpty() ? List.of()
                : documentRepository.findAllByQcRouteCodeInAndIsActiveTrueOrderByIdAsc(requiredDocumentRoutes.keySet())
                .stream().filter(this::hasReleasedCurrentRevision)
                .filter(d -> d.getDocumentType() == requiredDocumentRoutes.get(d.getQcRouteCode()))
                .filter(d -> !unavailableDocumentRoutes.contains(d.getQcRouteCode()))
                .map(this::toDocumentDto).toList();
        List<String> missingDocuments = requiredDocumentRoutes.keySet().stream()
                .filter(code -> documents.stream().noneMatch(d -> code.equals(d.qcRouteCode()))).toList();

        return new ProductQcResolutionDto(taxonomyRevision, familyCode, materialCode, subtype1Code,
                rule == null ? null : rule.getId(), matchLevel, toRouteDto(route),
                rule == null ? null : rule.getPurchasingConfirmationPolicy(), holdReason,
                requiresPlan, specialRoute == null ? null : toRouteDto(specialRoute), documents, missingDocuments);
    }

    private String validateProductContext(String familyCode, String materialCode, String subtype1Code)
            throws DataNotFoundException, InvalidRequestException {
        if (materialCode != null) {
            ProductMaterialId id = new ProductMaterialId();
            id.setCode(materialCode);
            id.setProductFamilyCode(familyCode);
            if (!productMaterialRepository.existsById(id)) {
                throw new DataNotFoundException("Product material not found in the selected family.");
            }
        }
        if (subtype1Code != null) {
            ProductSubtype1Entity subtype = productSubtype1Repository.findById(subtype1Code)
                    .orElseThrow(() -> new DataNotFoundException("Product subtype 1 not found."));
            if (!familyCode.equals(subtype.getProductFamilyCode())) {
                throw new InvalidRequestException("Product subtype 1 does not belong to the selected family.");
            }
            return subtype.getCode();
        }
        return null;
    }

    private QcRouteEntity activeRoute(String code) {
        return code == null ? null : routeRepository.findById(code)
                .filter(r -> Boolean.TRUE.equals(r.getIsActive())).orElse(null);
    }

    private boolean hasReleasedCurrentRevision(QcDocumentEntity document) {
        QcDocumentRevisionEntity revision = document.getCurrentRevision();
        return revision != null && Objects.equals(document.getId(), revision.getQcDocumentId())
                && revision.getStatus() == RevisionStatus.RELEASED
                && (revision.getEffectiveDate() == null
                    || !revision.getEffectiveDate().isAfter(LocalDate.now(DateUtil.getTimeZone())));
    }

    private QcRouteDto toRouteDto(QcRouteEntity route) {
        return new QcRouteDto(route.getCode(), route.getNameTh(), route.getNameEn(),
                route.getRouteType(), route.getQcMaterialGroupCode());
    }

    private QcDocumentFileDto toDocumentDto(QcDocumentEntity document) {
        QcDocumentRevisionEntity r = document.getCurrentRevision();
        return new QcDocumentFileDto(document.getId(), document.getDocumentNo(), document.getQcRouteCode(),
                document.getNameTh(), document.getNameEn(), document.getDocumentType(), r.getId(),
                r.getRevisionNo(), r.getFileName(), r.getOriginalFileName(), r.getContentType(),
                r.getFileSize(), r.getSha256(), r.getEffectiveDate());
    }
}
