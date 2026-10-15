package com.nutalig.constant;

public final class QcTypes {
    private QcTypes() {}

    public enum RouteType { MATERIAL, SPECIAL, HOLD }
    public enum DocumentType { MATERIAL_QC, SPECIAL_PLAN_TEMPLATE }
    public enum RevisionStatus { DRAFT, RELEASED, RETIRED }
    public enum RuleStatus { DRAFT, ACTIVE, RETIRED }
    public enum PurchasingConfirmationPolicy { ALWAYS, IF_UNCERTAIN }
    public enum MatchLevel { SPECIFIC, DEFAULT, NONE }
}
