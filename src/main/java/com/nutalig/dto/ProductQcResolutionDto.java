package com.nutalig.dto;

import com.nutalig.constant.QcTypes.*;
import java.time.LocalDate;
import java.util.List;

/** Routing information only; it does not confirm RFQ QC or approve an inspection. */
public record ProductQcResolutionDto(
        String taxonomyRevision,
        String productFamilyCode,
        String productMaterialCode,
        String productSubtype1Code,
        Long ruleId,
        MatchLevel matchLevel,
        QcRouteDto route,
        PurchasingConfirmationPolicy purchasingConfirmationPolicy,
        String holdReason,
        boolean specialInspectionPlanRequired,
        QcRouteDto specialQcRoute,
        List<QcDocumentFileDto> documents,
        List<String> missingDocumentRouteCodes) {

    public record QcRouteDto(String code, String nameTh, String nameEn,
                             RouteType routeType, String qcMaterialGroupCode) {}

    public record QcDocumentFileDto(Long documentId, String documentNo,
            String qcRouteCode, String nameTh, String nameEn, DocumentType documentType,
            Long revisionId, String revisionNo, String fileName, String originalFileName,
            String contentType, Long fileSize, String sha256, LocalDate effectiveDate) {}
}
