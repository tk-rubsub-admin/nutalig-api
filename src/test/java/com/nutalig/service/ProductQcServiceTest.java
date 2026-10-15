package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.constant.QcTypes.*;
import com.nutalig.entity.*;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductQcServiceTest {
    @Mock ProductFamilyRepository families;
    @Mock ProductMaterialRepository materials;
    @Mock ProductSubtype1Repository subtypes;
    @Mock ProductMaterialQcRuleRepository rules;
    @Mock ProductFamilyQcPolicyRepository policies;
    @Mock QcRouteRepository routes;
    @Mock QcDocumentRepository documents;
    AppProperties properties;
    ProductQcService service;

    @BeforeEach
    void setup() {
        properties = new AppProperties();
        service = new ProductQcService(properties, families, materials, subtypes, rules, policies, routes, documents);
        ProductFamilyEntity family = new ProductFamilyEntity();
        family.setCode("PF001"); family.setIsActive(true);
        when(families.findById("PF001")).thenReturn(Optional.of(family));
        lenient().when(materials.existsById(any())).thenReturn(true);
        ProductSubtype1Entity subtype = new ProductSubtype1Entity();
        subtype.setCode("PST0001"); subtype.setProductFamilyCode("PF001");
        lenient().when(subtypes.findById("PST0001")).thenReturn(Optional.of(subtype));
        lenient().when(routes.findById("QC-HOLD")).thenReturn(Optional.of(route("QC-HOLD", RouteType.HOLD, true)));
    }

    @Test
    void specificRuleOverridesMaterialDefault() throws Exception {
        givenRules(rule(null, "MAT-QC-02"), rule("PST0001", "MAT-QC-05"));
        when(routes.findById("MAT-QC-05")).thenReturn(Optional.of(route("MAT-QC-05", RouteType.MATERIAL, true)));
        var result = service.resolve("PF001", "PMAT0004", "PST0001");
        assertEquals("MAT-QC-05", result.route().code());
        assertEquals(MatchLevel.SPECIFIC, result.matchLevel());
        assertNull(result.holdReason());
        assertEquals(List.of("MAT-QC-05"), result.missingDocumentRouteCodes());
        verify(routes, never()).findById("MAT-QC-02");
    }

    @Test
    void explicitHoldDoesNotFallBackToReadyDefault() throws Exception {
        givenRules(rule(null, "MAT-QC-02"), rule("PST0001", "QC-HOLD"));
        var result = service.resolve("PF001", "PMAT0004", "PST0001");
        assertEquals("QC-HOLD", result.route().code());
        assertEquals("PURCHASING_CONFIRMATION_REQUIRED", result.holdReason());
        assertEquals(MatchLevel.SPECIFIC, result.matchLevel());
        verify(routes, never()).findById("MAT-QC-02");
    }

    @Test
    void fallsBackToDefaultWhenSelectedFormatHasNoSpecificRule() throws Exception {
        givenRules(rule(null, "MAT-QC-02"));
        when(routes.findById("MAT-QC-02")).thenReturn(Optional.of(route("MAT-QC-02", RouteType.MATERIAL, true)));
        assertEquals(MatchLevel.DEFAULT, service.resolve("PF001", "PMAT0004", "PST0001").matchLevel());
        assertEquals(MatchLevel.DEFAULT, service.resolve("PF001", "PMAT0004", null).matchLevel());
    }

    @Test
    void unconfiguredMaterialReturnsHoldWithoutInferringFromItsName() throws Exception {
        givenRules();
        var result = service.resolve("PF001", "PMAT0004", "PST0001");
        assertEquals("QC-HOLD", result.route().code());
        assertEquals("NO_MAPPING", result.holdReason());
        assertEquals(MatchLevel.NONE, result.matchLevel());
        verify(rules).findAllByTaxonomyRevisionAndProductFamilyCodeAndProductMaterialCodeAndStatusOrderByIdAsc(
                "REV10", "PF001", "PMAT0004", RuleStatus.ACTIVE);
    }

    @Test
    void missingMaterialRetainsFamilyPlanRequirementWithoutClearingHold() throws Exception {
        givenSpecialPolicy();
        var result = service.resolve("PF001", null, null);
        assertEquals("MATERIAL_NOT_SELECTED", result.holdReason());
        assertTrue(result.specialInspectionPlanRequired());
        assertEquals("SPECIAL-F13", result.specialQcRoute().code());
        assertEquals(List.of("SPECIAL-F13"), result.missingDocumentRouteCodes());
        verifyNoInteractions(rules);
    }

    @Test
    void inactiveSpecificRouteDoesNotFallBackToAnotherRule() throws Exception {
        givenRules(rule(null, "MAT-QC-02"), rule("PST0001", "MAT-QC-05"));
        when(routes.findById("MAT-QC-05")).thenReturn(Optional.of(route("MAT-QC-05", RouteType.MATERIAL, false)));
        var result = service.resolve("PF001", "PMAT0004", "PST0001");
        assertEquals("QC-HOLD", result.route().code());
        assertEquals("QC_ROUTE_INACTIVE", result.holdReason());
        verify(routes, never()).findById("MAT-QC-02");
    }

    @Test
    void rejectsSubtypeFromAnotherFamily() {
        ProductSubtype1Entity subtype = new ProductSubtype1Entity();
        subtype.setProductFamilyCode("PF013");
        when(subtypes.findById("PST0092")).thenReturn(Optional.of(subtype));
        assertThrows(InvalidRequestException.class, () -> service.resolve("PF001", "PMAT0004", "PST0092"));
        verifyNoInteractions(rules);
    }

    @Test
    void returnsOnlyReleasedEffectiveCurrentDocumentVersions() throws Exception {
        givenRules(rule(null, "MAT-QC-02"));
        when(routes.findById("MAT-QC-02")).thenReturn(Optional.of(route("MAT-QC-02", RouteType.MATERIAL, true)));
        var published = document(1L, RevisionStatus.RELEASED);
        var draft = document(2L, RevisionStatus.DRAFT);
        var future = document(3L, RevisionStatus.RELEASED);
        future.getCurrentRevision().setEffectiveDate(LocalDate.of(2999, 1, 1));
        var retired = document(4L, RevisionStatus.RETIRED);
        var wrongKind = document(5L, RevisionStatus.RELEASED);
        wrongKind.setDocumentType(DocumentType.SPECIAL_PLAN_TEMPLATE);
        when(documents.findAllByQcRouteCodeInAndIsActiveTrueOrderByIdAsc(anyCollection()))
                .thenReturn(List.of(published, draft, future, retired, wrongKind));
        var result = service.resolve("PF001", "PMAT0004", null);
        assertEquals(1, result.documents().size());
        assertEquals("Rev.01", result.documents().getFirst().revisionNo());
        assertEquals("qc/current.pdf", result.documents().getFirst().fileName());
        assertTrue(result.missingDocumentRouteCodes().isEmpty());
    }

    @Test
    void deduplicatesSpecialRouteWhenBothMaterialAndFamilyRequireIt() throws Exception {
        givenRules(rule(null, "SPECIAL-F13"));
        givenSpecialPolicy();
        var result = service.resolve("PF001", "PMAT0004", null);
        assertEquals(List.of("SPECIAL-F13"), result.missingDocumentRouteCodes());
        verify(documents).findAllByQcRouteCodeInAndIsActiveTrueOrderByIdAsc(argThat(c -> c.size() == 1));
    }

    @Test
    void usesConfiguredTaxonomyRevision() throws Exception {
        properties.getProductTaxonomy().setActiveRevision("REV11");
        var result = service.resolve("PF001", "PMAT0004", null);
        assertEquals("REV11", result.taxonomyRevision());
        verify(rules).findAllByTaxonomyRevisionAndProductFamilyCodeAndProductMaterialCodeAndStatusOrderByIdAsc(
                "REV11", "PF001", "PMAT0004", RuleStatus.ACTIVE);
    }

    @Test
    void usesCanonicalCodesReturnedByCaseInsensitiveMasterLookup() throws Exception {
        var canonicalFamily = families.findById("PF001");
        var canonicalSubtype = subtypes.findById("PST0001");
        when(families.findById("pf001")).thenReturn(canonicalFamily);
        when(subtypes.findById("pst0001")).thenReturn(canonicalSubtype);
        givenRules(rule(null, "MAT-QC-02"), rule("PST0001", "MAT-QC-05"));
        when(routes.findById("MAT-QC-05")).thenReturn(Optional.of(route("MAT-QC-05", RouteType.MATERIAL, true)));
        var result = service.resolve("pf001", "PMAT0004", "pst0001");
        assertEquals("PF001", result.productFamilyCode());
        assertEquals("PST0001", result.productSubtype1Code());
        assertEquals("MAT-QC-05", result.route().code());
    }

    private void givenRules(ProductMaterialQcRuleEntity... values) {
        when(rules.findAllByTaxonomyRevisionAndProductFamilyCodeAndProductMaterialCodeAndStatusOrderByIdAsc(
                "REV10", "PF001", "PMAT0004", RuleStatus.ACTIVE)).thenReturn(List.of(values));
    }

    private void givenSpecialPolicy() {
        var policy = new ProductFamilyQcPolicyEntity();
        policy.setSpecialInspectionPlanRequired(true);
        policy.setSpecialQcRouteCode("SPECIAL-F13");
        when(policies.findByTaxonomyRevisionAndProductFamilyCodeAndIsActiveTrue("REV10", "PF001"))
                .thenReturn(Optional.of(policy));
        when(routes.findById("SPECIAL-F13")).thenReturn(Optional.of(route("SPECIAL-F13", RouteType.SPECIAL, true)));
    }

    private ProductMaterialQcRuleEntity rule(String subtype, String route) {
        var rule = new ProductMaterialQcRuleEntity();
        rule.setProductSubtype1Code(subtype); rule.setQcRouteCode(route);
        rule.setPurchasingConfirmationPolicy(PurchasingConfirmationPolicy.ALWAYS);
        return rule;
    }

    private QcRouteEntity route(String code, RouteType type, boolean active) {
        var route = new QcRouteEntity();
        route.setCode(code); route.setRouteType(type); route.setIsActive(active);
        return route;
    }

    private QcDocumentEntity document(Long id, RevisionStatus status) {
        var document = new QcDocumentEntity();
        document.setId(id); document.setQcRouteCode("MAT-QC-02");
        document.setDocumentType(DocumentType.MATERIAL_QC);
        var revision = new QcDocumentRevisionEntity();
        revision.setId(id); revision.setQcDocumentId(id); revision.setStatus(status);
        revision.setRevisionNo("Rev.01"); revision.setFileName("qc/current.pdf");
        document.setCurrentRevision(revision);
        return document;
    }
}
