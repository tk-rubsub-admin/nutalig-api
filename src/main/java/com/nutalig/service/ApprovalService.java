package com.nutalig.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.LineConfiguration;
import com.nutalig.constant.*;
import com.nutalig.controller.approval.response.ApprovalRejectTokenResolveResponse;
import com.nutalig.dto.ApprovalRequestAuditLogDto;
import com.nutalig.dto.ApprovalRequestDto;
import com.nutalig.dto.ApprovalRequestStepDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.ApprovalRequestAuditLogRepository;
import com.nutalig.repository.ApprovalRequestRepository;
import com.nutalig.repository.ApprovalRequestStepRepository;
import com.nutalig.repository.UserRepository;
import com.nutalig.security.JwtUtil;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
    private static final String URGENT_RFQ_TEMPLATE_CODE = "urgent-rfq-approval";
    private static final String CLAIM_STEP_ID = "stepId";
    private static final String CLAIM_ACTION = "action";
    private static final String CLAIM_SOURCE = "source";
    private static final String ACTION_APPROVE = "approve";
    private static final String ACTION_REJECT = "reject";
    private static final String ACTION_REJECT_FORM = "reject-form";
    private static final String SOURCE_LINE = "line";
    private static final String SOURCE_WEB = "web";

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRequestStepRepository approvalRequestStepRepository;
    private final ApprovalRequestAuditLogRepository approvalRequestAuditLogRepository;
    private final UserRepository userRepository;
    private final GeneratedIdSequenceService generatedIdSequenceService;
    private final ObjectMapper objectMapper;
    private final LineMessageService lineMessageService;
    private final ApprovalTemplateService approvalTemplateService;
    private final ApprovalBusinessService approvalBusinessService;
    private final UserTodoService userTodoService;
    private final UserProfileService userProfileService;
    private final ActivityHistoryService activityHistoryService;
    private final LineConfiguration lineConfiguration;

    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequestDto createUrgentRfqApprovalRequest(RfqHeaderEntity rfqEntity, String userId) throws Exception {
        if (rfqEntity == null) {
            throw new InvalidRequestException("RFQ is required.");
        }

        List<UserEntity> approvers = findApproversByRole(SUPER_ADMIN_ROLE_CODE);
        if (approvers.isEmpty()) {
            throw new InvalidRequestException("No SUPER_ADMIN approver is available.");
        }

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        String actor = userProfileService.getNameFromId(userId);

        ApprovalRequestEntity request = new ApprovalRequestEntity();
        request.setRequestNo(generateApprovalRequestNo());
        request.setEntityType(ActivityEntityType.RFQ);
        request.setReferenceId(rfqEntity.getId());
        request.setRequestType(ApprovalRequestType.URGENT_RFQ);
        request.setTemplateCode(URGENT_RFQ_TEMPLATE_CODE);
        request.setTitle("อนุมัติคำขอราคาเร่งด่วน " + rfqEntity.getId());
        request.setStatus(ApprovalRequestStatus.PENDING);
        request.setCurrentStepNo(1);
        request.setRequestedBy(actor);
        request.setRequestedDate(now);
        request.setCreatedBy(userId);
        request.setUpdatedBy(userId);
        request.setPayloadJson(objectMapper.writeValueAsString(buildUrgentRfqPayload(rfqEntity, actor)));

        ApprovalRequestStepEntity step = new ApprovalRequestStepEntity();
        step.setStepNo(1);
        step.setApproverRoleCode(SUPER_ADMIN_ROLE_CODE);
        step.setStatus(ApprovalStepStatus.PENDING);
        step.setCreatedBy(userId);
        step.setUpdatedBy(userId);
        request.addStep(step);

        request = approvalRequestRepository.save(request);

        recordAudit(
                request,
                step,
                ApprovalAuditEventType.REQUEST_CREATED,
                getUserOrNull(userId),
                null,
                ApprovalSource.SYSTEM,
                "สร้าง approval request สำหรับคำขอราคาเร่งด่วน " + rfqEntity.getId(),
                buildAuditDetail(Map.of(
                        "entityType", request.getEntityType(),
                        "referenceId", request.getReferenceId(),
                        "requestType", request.getRequestType(),
                        "templateCode", request.getTemplateCode()
                )),
                userId
        );
        recordAudit(
                request,
                step,
                ApprovalAuditEventType.STEP_CREATED,
                getUserOrNull(userId),
                null,
                ApprovalSource.SYSTEM,
                "สร้าง approval step ลำดับที่ " + step.getStepNo() + " สำหรับ " + request.getRequestNo(),
                buildAuditDetail(buildMap(
                        "stepNo", step.getStepNo(),
                        "approverRoleCode", step.getApproverRoleCode()
                )),
                userId
        );

        createApprovalTodos(request, step, approvers, userId);
        try {
            sendCurrentStepApprovalCard(request, step, approvers, userId);
        } catch (Exception exception) {
            log.warn("Cannot send approval card for request {}", request.getRequestNo(), exception);
        }

        activityHistoryService.record(
                ActivityEntityType.APPROVAL_REQUEST,
                String.valueOf(request.getId()),
                userId,
                ActivityActorType.USER,
                ActivityAction.REQUEST_APPROVAL,
                ActivitySource.API,
                "สร้างคำขออนุมัติ " + request.getRequestNo(),
                Map.of(
                        "requestType", request.getRequestType(),
                        "entityType", request.getEntityType(),
                        "referenceId", request.getReferenceId()
                )
        );

        return toDto(request);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestDto getLatestApprovalByEntity(ActivityEntityType entityType, String referenceId)
            throws DataNotFoundException {
        ApprovalRequestEntity entity = approvalRequestRepository
                .findFirstByEntityTypeAndReferenceIdAndStatusInOrderByCreatedDateDesc(
                        entityType,
                        referenceId,
                        List.of(
                                ApprovalRequestStatus.PENDING,
                                ApprovalRequestStatus.APPROVED,
                                ApprovalRequestStatus.REJECTED,
                                ApprovalRequestStatus.CANCELLED
                        )
                )
                .orElseThrow(() -> new DataNotFoundException("Approval request for " + entityType + " " + referenceId + " not found."));
        return toDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequestDto approveLatestApprovalByEntity(ActivityEntityType entityType, String referenceId, String userId)
            throws DataNotFoundException, InvalidRequestException {
        ApprovalRequestEntity request = getPendingRequestForEntity(entityType, referenceId);
        ApprovalRequestStepEntity step = getCurrentPendingStep(request);
        validateUserCanAct(step, userId);
        return approveStep(request, step, getUserOrThrow(userId), null, ApprovalSource.WEB);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequestDto rejectLatestApprovalByEntity(
            ActivityEntityType entityType,
            String referenceId,
            String reason,
            String userId
    ) throws DataNotFoundException, InvalidRequestException {
        if (StringUtils.isBlank(reason)) {
            throw new InvalidRequestException("reason is required.");
        }
        ApprovalRequestEntity request = getPendingRequestForEntity(entityType, referenceId);
        ApprovalRequestStepEntity step = getCurrentPendingStep(request);
        validateUserCanAct(step, userId);
        return rejectStep(request, step, getUserOrThrow(userId), null, reason.trim(), ApprovalSource.WEB);
    }

    @Transactional(readOnly = true)
    public ApprovalRejectTokenResolveResponse resolveRejectToken(String token) throws DataNotFoundException, InvalidRequestException {
        ApprovalTokenClaims claims = parseApprovalToken(token, ACTION_REJECT_FORM);
        ApprovalRequestEntity request = getApprovalRequestById(claims.requestId());
        ApprovalRequestStepEntity step = getStepById(request, claims.stepId());

        return ApprovalRejectTokenResolveResponse.builder()
                .requestId(request.getId())
                .stepId(step.getId())
                .requestNo(request.getRequestNo())
                .title(request.getTitle())
                .entityType(request.getEntityType())
                .referenceId(request.getReferenceId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .currentStepNo(request.getCurrentStepNo())
                .approverRoleCode(step.getApproverRoleCode())
                .approverDisplayName(step.getApproverUser() != null ? step.getApproverUser().getDisplayName() : step.getApproverRoleCode())
                .rejectReason(step.getRejectReason())
                .payload(parsePayload(request.getPayloadJson()))
                .requestedDate(request.getRequestedDate())
                .actedAt(step.getActedAt())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequestDto rejectByToken(String token, String reason) throws DataNotFoundException, InvalidRequestException {
        if (StringUtils.isBlank(reason)) {
            throw new InvalidRequestException("reason is required.");
        }

        ApprovalTokenClaims claims = parseApprovalToken(token, ACTION_REJECT_FORM);
        ApprovalRequestEntity request = getApprovalRequestById(claims.requestId());
        ApprovalRequestStepEntity step = getStepById(request, claims.stepId());

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new InvalidRequestException("Approval request is not pending.");
        }
        if (step.getStatus() != ApprovalStepStatus.PENDING) {
            throw new InvalidRequestException("Approval step is not pending.");
        }

        UserEntity actorUser = resolveLineActorOrRoleActor(step, claims.actorLineUserId());
        ApprovalRequestDto response = rejectStep(request, step, actorUser, claims.actorLineUserId(), reason.trim(), ApprovalSource.WEB);
        if (StringUtils.isNotBlank(claims.actorLineUserId())) {
            try {
                lineMessageService.sendTextMessageToLineUser(
                        claims.actorLineUserId(),
                        "คำขออนุมัติ " + request.getRequestNo() + " นี้ถูกปฏิเสธแล้ว"
                );
            } catch (Exception exception) {
                log.warn("Cannot send reject confirmation to line user {}", claims.actorLineUserId(), exception);
            }
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleLinePostback(String lineUserId, String webhookEventId, String postbackData)
            throws Exception {
        Map<String, String> data = parsePostbackData(postbackData);
        if (!"approval".equalsIgnoreCase(data.get("type"))) {
            return;
        }

        String action = data.get("action");
        String actionKey = data.get("key");
        if (StringUtils.isBlank(action) || StringUtils.isBlank(actionKey)) {
            throw new InvalidRequestException("Approval postback payload is invalid.");
        }

        ApprovalRequestStepEntity step = resolveStepByActionKey(action, actionKey);
        ApprovalRequestEntity request = step.getApprovalRequest();

        recordAudit(
                request,
                step,
                ApprovalAuditEventType.POSTBACK_RECEIVED,
                resolveOptionalUserByLineUserId(lineUserId),
                lineUserId,
                ApprovalSource.LINE_POSTBACK,
                "รับ LINE postback สำหรับ approval request " + request.getRequestNo(),
                buildAuditDetail(Map.of(
                        "webhookEventId", webhookEventId,
                        "action", action,
                        "postbackData", postbackData
                )),
                "LINE_POSTBACK"
        );

        if (ACTION_APPROVE.equalsIgnoreCase(action)) {
            approveStep(request, step, resolveLineActorOrRoleActor(step, lineUserId), lineUserId, ApprovalSource.LINE_POSTBACK);
            lineMessageService.sendTextMessageToLineUser(
                    lineUserId,
                    "คำขออนุมัติ " + request.getRequestNo() + " นี้ได้รับการอนุมัติแล้ว"
            );
            return;
        }

        if (ACTION_REJECT.equalsIgnoreCase(action)) {
            String rejectToken = buildApprovalToken(request.getId(), step.getId(), ACTION_REJECT_FORM, lineUserId, SOURCE_WEB, 24 * 60 * 60);
            String rejectUrl = buildRejectFormUrl(rejectToken);

            recordAudit(
                    request,
                    step,
                    ApprovalAuditEventType.REJECT_LINK_OPENED,
                    resolveOptionalUserByLineUserId(lineUserId),
                    lineUserId,
                    ApprovalSource.LINE_POSTBACK,
                    "ส่งลิงก์กรอกเหตุผลปฏิเสธสำหรับ approval request " + request.getRequestNo(),
                    buildAuditDetail(Map.of("rejectUrl", rejectUrl)),
                    "LINE_POSTBACK"
            );

            lineMessageService.sendTextMessageToLineUser(
                    lineUserId,
                    "กรุณาระบุเหตุผลในการไม่อนุมัติผ่านลิงก์นี้\n" + rejectUrl
            );
            return;
        }

        throw new InvalidRequestException("Unsupported approval action.");
    }

    private ApprovalRequestDto approveStep(
            ApprovalRequestEntity request,
            ApprovalRequestStepEntity step,
            UserEntity actorUser,
            String actorLineUserId,
            ApprovalSource source
    ) throws DataNotFoundException, InvalidRequestException {
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new InvalidRequestException("Approval request is not pending.");
        }
        if (step.getStatus() != ApprovalStepStatus.PENDING) {
            throw new InvalidRequestException("Approval step is not pending.");
        }

        step.setStatus(ApprovalStepStatus.APPROVED);
        step.setActedAt(now);
        step.setActedByUser(actorUser);
        step.setActedByLineUserId(StringUtils.defaultIfBlank(actorLineUserId, actorUser.getLineUserId()));
        step.setActionChannel(source);
        step.setUpdatedBy(actorUser.getId());
        approvalRequestStepRepository.save(step);

        request.setStatus(ApprovalRequestStatus.APPROVED);
        request.setApprovedDate(now);
        request.setUpdatedBy(actorUser.getId());
        request.setUpdatedDate(now);
        approvalRequestRepository.save(request);

        recordAudit(
                request,
                step,
                ApprovalAuditEventType.APPROVED,
                actorUser,
                step.getActedByLineUserId(),
                source,
                "อนุมัติ approval request " + request.getRequestNo(),
                buildAuditDetail(Map.of("status", request.getStatus(), "stepStatus", step.getStatus())),
                actorUser.getId()
        );
        recordAudit(
                request,
                step,
                ApprovalAuditEventType.COMPLETED,
                actorUser,
                step.getActedByLineUserId(),
                source,
                "approval request " + request.getRequestNo() + " เสร็จสิ้น",
                buildAuditDetail(Map.of("status", request.getStatus())),
                actorUser.getId()
        );

        approvalBusinessService.handleApproved(request, actorUser.getId(), source);

        activityHistoryService.record(
                ActivityEntityType.APPROVAL_REQUEST,
                String.valueOf(request.getId()),
                actorUser.getId(),
                ActivityActorType.USER,
                ActivityAction.APPROVE,
                source == ApprovalSource.LINE_POSTBACK ? ActivitySource.LINE : ActivitySource.API,
                "อนุมัติคำขอ " + request.getRequestNo(),
                buildMap("entityType", request.getEntityType(), "referenceId", request.getReferenceId())
        );

        return toDto(request);
    }

    private ApprovalRequestDto rejectStep(
            ApprovalRequestEntity request,
            ApprovalRequestStepEntity step,
            UserEntity actorUser,
            String actorLineUserId,
            String reason,
            ApprovalSource source
    ) throws DataNotFoundException, InvalidRequestException {
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new InvalidRequestException("Approval request is not pending.");
        }
        if (step.getStatus() != ApprovalStepStatus.PENDING) {
            throw new InvalidRequestException("Approval step is not pending.");
        }

        step.setStatus(ApprovalStepStatus.REJECTED);
        step.setActedAt(now);
        step.setActedByUser(actorUser);
        step.setActedByLineUserId(StringUtils.defaultIfBlank(actorLineUserId, actorUser.getLineUserId()));
        step.setActionChannel(source);
        step.setRejectReason(reason);
        step.setUpdatedBy(actorUser.getId());
        approvalRequestStepRepository.save(step);

        request.setStatus(ApprovalRequestStatus.REJECTED);
        request.setRejectedDate(now);
        request.setRejectReason(reason);
        request.setUpdatedBy(actorUser.getId());
        request.setUpdatedDate(now);
        approvalRequestRepository.save(request);

        recordAudit(
                request,
                step,
                ApprovalAuditEventType.REJECTED,
                actorUser,
                step.getActedByLineUserId(),
                source,
                "ปฏิเสธ approval request " + request.getRequestNo(),
                buildAuditDetail(Map.of("status", request.getStatus(), "stepStatus", step.getStatus(), "reason", reason)),
                actorUser.getId()
        );

        approvalBusinessService.handleRejected(request, actorUser.getId(), source, reason);

        activityHistoryService.record(
                ActivityEntityType.APPROVAL_REQUEST,
                String.valueOf(request.getId()),
                actorUser.getId(),
                ActivityActorType.USER,
                ActivityAction.REJECT,
                source == ApprovalSource.LINE_POSTBACK ? ActivitySource.LINE : ActivitySource.API,
                "ปฏิเสธคำขอ " + request.getRequestNo(),
                buildMap("entityType", request.getEntityType(), "referenceId", request.getReferenceId(), "reason", reason)
        );

        return toDto(request);
    }

    private void sendCurrentStepApprovalCard(
            ApprovalRequestEntity request,
            ApprovalRequestStepEntity step,
            List<UserEntity> approvers,
            String userId
    ) throws Exception {
        Map<String, Object> payload = parsePayload(request.getPayloadJson());

        for (UserEntity approver : approvers) {
            if (StringUtils.isBlank(approver.getLineUserId())) {
                continue;
            }

            if (StringUtils.isBlank(step.getApproveActionKey())) {
                step.setApproveActionKey(generateActionKey());
            }
            if (StringUtils.isBlank(step.getRejectActionKey())) {
                step.setRejectActionKey(generateActionKey());
            }

            Map<String, String> placeholders = buildUrgentRfqPlaceholders(
                    request,
                    payload,
                    approver,
                    "type=approval&action=" + ACTION_APPROVE + "&key=" + step.getApproveActionKey(),
                    "type=approval&action=" + ACTION_REJECT + "&key=" + step.getRejectActionKey()
            );

            JsonNode message = approvalTemplateService.renderTemplate(request.getTemplateCode(), placeholders);
            lineMessageService.sendFlexMessage(approver.getLineUserId(), message);
        }

        step.setSentAt(ZonedDateTime.now(DateUtil.getTimeZone()));
        step.setUpdatedBy(userId);
        approvalRequestStepRepository.save(step);

        recordAudit(
                request,
                step,
                ApprovalAuditEventType.STEP_SENT,
                getUserOrNull(userId),
                null,
                ApprovalSource.SYSTEM,
                "ส่ง LINE approval card สำหรับ " + request.getRequestNo(),
                buildAuditDetail(Map.of(
                        "recipientCount", approvers.size(),
                        "lineRecipientCount", approvers.stream().filter(user -> StringUtils.isNotBlank(user.getLineUserId())).count()
                )),
                userId
        );
    }

    private void createApprovalTodos(
            ApprovalRequestEntity request,
            ApprovalRequestStepEntity step,
            List<UserEntity> approvers,
            String userId
    ) {
        String targetPath = approvalBusinessService.buildTargetPath(request);
        ZonedDateTime dueDate = ZonedDateTime.now(DateUtil.getTimeZone()).plusDays(1);

        for (UserEntity approver : approvers) {
            userTodoService.buildUserTodoEntity(
                    approver,
                    UserTodoType.PRICE_INQUIRY,
                    request.getTitle(),
                    "มีรายการรออนุมัติ " + request.getRequestNo(),
                    UserTodoStatus.TODO,
                    UserTodoPriority.URGENT,
                    ActivityEntityType.APPROVAL_REQUEST.name(),
                    String.valueOf(request.getId()),
                    targetPath,
                    dueDate,
                    step.getStepNo(),
                    userId
            );
        }
    }

    private String generateApprovalRequestNo() {
        return generatedIdSequenceService.getNextIdWithMonth(BusinessConstant.DocumentPrefix.APPROVAL_REQUEST_PREFIX, 4);
    }

    private Map<String, Object> buildUrgentRfqPayload(RfqHeaderEntity rfqEntity, String actorName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String productFamilyName = rfqEntity.getProductFamilyEntity() != null
                ? rfqEntity.getProductFamilyEntity().getNameTh()
                : rfqEntity.getProductFamily();
        String productMaterialName = rfqEntity.getMaterial() != null
                ? StringUtils.defaultIfBlank(rfqEntity.getMaterial().getNameTh(), rfqEntity.getMaterialCode())
                : rfqEntity.getMaterialCode();
        String productType = StringUtils.join(
                List.of(
                        StringUtils.trimToNull(productFamilyName),
                        StringUtils.trimToNull(productMaterialName)
                ).stream().filter(Objects::nonNull).toList(),
                " / "
        );

        payload.put("rfqId", rfqEntity.getId());
        payload.put("customerName", rfqEntity.getCustomer() != null ? rfqEntity.getCustomer().getCustomerName() : "-");
        payload.put("contactName", rfqEntity.getContactName());
        payload.put("productFamily", productFamilyName);
        payload.put("productMaterial", productMaterialName);
        payload.put("productType", StringUtils.defaultIfBlank(productType, "-"));
        payload.put("capacity", rfqEntity.getCapacity());
        payload.put("salesName", actorName);
        payload.put("urgentReason", rfqEntity.getUrgentRequestReason());
        payload.put("requestedDate", rfqEntity.getRequestedDate() != null ? rfqEntity.getRequestedDate().toString() : null);
        payload.put("orderTypeName", rfqEntity.getOrderType() != null ? rfqEntity.getOrderType().getNameTh() : null);
        payload.put("rfqTypeName", rfqEntity.getRfqType() != null ? rfqEntity.getRfqType().getNameTh() : null);
        payload.put("statusText", "รออนุมัติ");
        return payload;
    }

    private Map<String, String> buildUrgentRfqPlaceholders(
            ApprovalRequestEntity request,
            Map<String, Object> payload,
            UserEntity approver,
            String approvePostbackData,
            String rejectPostbackData
    ) throws InvalidRequestException {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("altText", "มีรายการรอการอนุมัติ " + StringUtils.defaultString(request.getReferenceId()));
        placeholders.put("requestTitle", "คำขออนุมัติ");
        placeholders.put("entityLabel", "คำขอราคาเร่งด่วน");
        placeholders.put("requestNo", String.valueOf(payload.getOrDefault("rfqId", request.getReferenceId())));
        placeholders.put("customerName", String.valueOf(payload.getOrDefault("customerName", "-")));
        placeholders.put("orderTypeName", String.valueOf(payload.getOrDefault("orderTypeName", "-")));
        placeholders.put("rfqTypeName", String.valueOf(payload.getOrDefault("rfqTypeName", "-")));
        placeholders.put("productType", String.valueOf(payload.getOrDefault("productType", "-")));
        placeholders.put("capacity", String.valueOf(payload.getOrDefault("capacity", "-")));
        placeholders.put("requesterName", String.valueOf(payload.getOrDefault("salesName", request.getRequestedBy())));
        placeholders.put("urgentReason", String.valueOf(payload.getOrDefault("urgentReason", "-")));
        placeholders.put("statusText", "รออนุมัติ");
        placeholders.put("approverName", approver.getDisplayName());
        placeholders.put("detailUrl", buildDetailUrl(request));
        placeholders.put("approvePostbackData", approvePostbackData);
        placeholders.put("rejectPostbackData", rejectPostbackData);
        return placeholders;
    }

    private String buildDetailUrl(ApprovalRequestEntity request) throws InvalidRequestException {
        String frontendBaseUrl = buildFrontendBaseUrl();
        if (request.getRequestType() == ApprovalRequestType.URGENT_RFQ) {
            return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                    .path("/price-inquiry/" + request.getReferenceId())
                    .build()
                    .toUriString();
        }
        return frontendBaseUrl;
    }

    private String buildRejectFormUrl(String token) throws InvalidRequestException {
        return UriComponentsBuilder.fromUriString(buildFrontendBaseUrl())
                .path("/approval-reject")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private String buildFrontendBaseUrl() throws InvalidRequestException {
        String loginSuccessUrl = lineConfiguration.getLoginSuccessUrl();
        if (StringUtils.isBlank(loginSuccessUrl)) {
            throw new InvalidRequestException("LINE frontend redirect URL is not configured");
        }
        URI uri = URI.create(loginSuccessUrl);
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    private ApprovalRequestEntity getPendingRequestForEntity(ActivityEntityType entityType, String referenceId)
            throws DataNotFoundException, InvalidRequestException {
        ApprovalRequestEntity request = approvalRequestRepository
                .findFirstByEntityTypeAndReferenceIdAndStatusInOrderByCreatedDateDesc(
                        entityType,
                        referenceId,
                        List.of(ApprovalRequestStatus.PENDING)
                )
                .orElseThrow(() -> new DataNotFoundException("Pending approval request not found."));

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new InvalidRequestException("Approval request is not pending.");
        }
        return request;
    }

    private ApprovalRequestEntity getApprovalRequestById(Long requestId) throws DataNotFoundException {
        return approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new DataNotFoundException("Approval request " + requestId + " not found."));
    }

    private ApprovalRequestStepEntity getCurrentPendingStep(ApprovalRequestEntity request) throws InvalidRequestException {
        return request.getSteps().stream()
                .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Approval step is not pending."));
    }

    private ApprovalRequestStepEntity getStepById(ApprovalRequestEntity request, Long stepId) throws DataNotFoundException {
        return request.getSteps().stream()
                .filter(step -> Objects.equals(step.getId(), stepId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Approval request step " + stepId + " not found."));
    }

    private void validateUserCanAct(ApprovalRequestStepEntity step, String userId) throws InvalidRequestException, DataNotFoundException {
        UserEntity actorUser = getUserOrThrow(userId);
        if (!isUserAllowedForStep(step, actorUser)) {
            throw new InvalidRequestException("User is not allowed to act on this approval step.");
        }
    }

    private boolean isUserAllowedForStep(ApprovalRequestStepEntity step, UserEntity actorUser) {
        if (step.getApproverUser() != null) {
            return StringUtils.equals(step.getApproverUser().getId(), actorUser.getId());
        }
        if (StringUtils.isNotBlank(step.getApproverRoleCode())) {
            return actorUser.getUserRoleEntity() != null
                    && StringUtils.equals(step.getApproverRoleCode(), actorUser.getUserRoleEntity().getRoleCode());
        }
        return false;
    }

    private List<UserEntity> findApproversByRole(String roleCode) {
        return userRepository.findByRoleIn(List.of(roleCode)).stream()
                .filter(user -> Status.ACTIVE.equals(user.getStatus()))
                .toList();
    }

    private UserEntity getUserOrThrow(String userId) throws DataNotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
    }

    private UserEntity getUserOrNull(String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private UserEntity resolveOptionalUserByLineUserId(String lineUserId) {
        if (StringUtils.isBlank(lineUserId)) {
            return null;
        }
        return userRepository.findByLineUserId(lineUserId).orElse(null);
    }

    private UserEntity resolveLineActorOrRoleActor(ApprovalRequestStepEntity step, String lineUserId)
            throws InvalidRequestException, DataNotFoundException {
        UserEntity actorUser = userRepository.findByLineUserId(lineUserId)
                .orElseThrow(() -> new InvalidRequestException("LINE user is not bound to any approver."));

        if (!isUserAllowedForStep(step, actorUser)) {
            throw new InvalidRequestException("LINE user is not allowed to act on this approval step.");
        }

        return actorUser;
    }

    private String buildApprovalToken(
            Long requestId,
            Long stepId,
            String action,
            String actorLineUserId,
            String source,
            long expiresInSeconds
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_STEP_ID, stepId);
        claims.put(CLAIM_ACTION, action);
        claims.put("actorLineUserId", actorLineUserId);
        claims.put(CLAIM_SOURCE, source);
        return JwtUtil.generateToken(String.valueOf(requestId), claims, expiresInSeconds);
    }

    private ApprovalTokenClaims parseApprovalToken(String token, String expectedAction) throws InvalidRequestException {
        if (StringUtils.isBlank(token) || !JwtUtil.isValid(token)) {
            throw new InvalidRequestException("Approval token is invalid or expired.");
        }
        String action = JwtUtil.getClaim(token, CLAIM_ACTION);
        if (!StringUtils.equalsIgnoreCase(expectedAction, action)) {
            throw new InvalidRequestException("Approval token action mismatch.");
        }

        try {
            return new ApprovalTokenClaims(
                    Long.valueOf(JwtUtil.getSubject(token)),
                    Long.valueOf(JwtUtil.getClaim(token, CLAIM_STEP_ID)),
                    action,
                    JwtUtil.getClaim(token, "actorLineUserId")
            );
        } catch (Exception exception) {
            throw new InvalidRequestException("Approval token payload is invalid.");
        }
    }

    private ApprovalRequestStepEntity resolveStepByActionKey(String action, String actionKey) throws InvalidRequestException, DataNotFoundException {
        Optional<ApprovalRequestStepEntity> optionalStep;
        if (ACTION_APPROVE.equalsIgnoreCase(action)) {
            optionalStep = approvalRequestStepRepository.findByApproveActionKey(actionKey);
        } else if (ACTION_REJECT.equalsIgnoreCase(action)) {
            optionalStep = approvalRequestStepRepository.findByRejectActionKey(actionKey);
        } else {
            throw new InvalidRequestException("Unsupported approval action.");
        }

        return optionalStep.orElseThrow(() -> new DataNotFoundException("Approval action key not found."));
    }

    private Map<String, String> parsePostbackData(String postbackData) {
        Map<String, String> data = new HashMap<>();
        if (StringUtils.isBlank(postbackData)) {
            return data;
        }
        String[] pairs = postbackData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                data.put(keyValue[0], keyValue[1]);
            }
        }
        return data;
    }

    private String generateActionKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (StringUtils.isBlank(payloadJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception exception) {
            log.warn("Cannot parse approval payload json", exception);
            return new LinkedHashMap<>();
        }
    }

    private void recordAudit(
            ApprovalRequestEntity request,
            ApprovalRequestStepEntity step,
            ApprovalAuditEventType eventType,
            UserEntity actorUser,
            String actorLineUserId,
            ApprovalSource source,
            String summary,
            String detailJson,
            String actorId
    ) {
        ApprovalRequestAuditLogEntity entity = new ApprovalRequestAuditLogEntity();
        entity.setApprovalRequest(request);
        entity.setApprovalRequestStep(step);
        entity.setEventType(eventType);
        entity.setActorUser(actorUser);
        entity.setActorLineUserId(actorLineUserId);
        entity.setSource(source);
        entity.setSummary(summary);
        entity.setDetailJson(detailJson);
        String auditActorUserId = actorUser != null ? StringUtils.trimToNull(actorUser.getId()) : null;
        entity.setCreatedBy(auditActorUserId);
        entity.setUpdatedBy(auditActorUserId);
        approvalRequestAuditLogRepository.save(entity);
    }

    private String buildAuditDetail(Map<String, ?> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception exception) {
            return null;
        }
    }

    private Map<String, Object> buildMap(Object... entries) {
        Map<String, Object> detail = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            detail.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return detail;
    }

    private ApprovalRequestDto toDto(ApprovalRequestEntity entity) {
        ApprovalRequestDto dto = new ApprovalRequestDto();
        dto.setId(entity.getId());
        dto.setRequestNo(entity.getRequestNo());
        dto.setEntityType(entity.getEntityType());
        dto.setReferenceId(entity.getReferenceId());
        dto.setRequestType(entity.getRequestType());
        dto.setTemplateCode(entity.getTemplateCode());
        dto.setTitle(entity.getTitle());
        dto.setStatus(entity.getStatus());
        dto.setCurrentStepNo(entity.getCurrentStepNo());
        dto.setRequestedBy(entity.getRequestedBy());
        dto.setRequestedDate(entity.getRequestedDate());
        dto.setApprovedDate(entity.getApprovedDate());
        dto.setRejectedDate(entity.getRejectedDate());
        dto.setRejectReason(entity.getRejectReason());
        dto.setPayload(parsePayload(entity.getPayloadJson()));
        dto.setSteps(entity.getSteps().stream().map(this::toStepDto).toList());
        dto.setAuditLogs(entity.getAuditLogs().stream().map(this::toAuditDto).toList());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private ApprovalRequestStepDto toStepDto(ApprovalRequestStepEntity entity) {
        ApprovalRequestStepDto dto = new ApprovalRequestStepDto();
        dto.setId(entity.getId());
        dto.setStepNo(entity.getStepNo());
        dto.setApproverUserId(entity.getApproverUser() != null ? entity.getApproverUser().getId() : null);
        dto.setApproverDisplayName(entity.getApproverUser() != null ? entity.getApproverUser().getDisplayName() : null);
        dto.setApproverRoleCode(entity.getApproverRoleCode());
        dto.setStatus(entity.getStatus());
        dto.setSentAt(entity.getSentAt());
        dto.setActedAt(entity.getActedAt());
        dto.setActedByUserId(entity.getActedByUser() != null ? entity.getActedByUser().getId() : null);
        dto.setActedByDisplayName(entity.getActedByUser() != null ? entity.getActedByUser().getDisplayName() : null);
        dto.setActedByLineUserId(entity.getActedByLineUserId());
        dto.setActionChannel(entity.getActionChannel());
        dto.setLineMessageId(entity.getLineMessageId());
        dto.setRejectReason(entity.getRejectReason());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private ApprovalRequestAuditLogDto toAuditDto(ApprovalRequestAuditLogEntity entity) {
        ApprovalRequestAuditLogDto dto = new ApprovalRequestAuditLogDto();
        dto.setId(entity.getId());
        dto.setEventType(entity.getEventType());
        dto.setActorUserId(entity.getActorUser() != null ? entity.getActorUser().getId() : null);
        dto.setActorDisplayName(entity.getActorUser() != null ? entity.getActorUser().getDisplayName() : null);
        dto.setActorLineUserId(entity.getActorLineUserId());
        dto.setSource(entity.getSource());
        dto.setSummary(entity.getSummary());
        dto.setDetailJson(entity.getDetailJson());
        dto.setCreatedDate(entity.getCreatedDate());
        return dto;
    }

    private record ApprovalTokenClaims(Long requestId, Long stepId, String action, String actorLineUserId) {
    }
}
