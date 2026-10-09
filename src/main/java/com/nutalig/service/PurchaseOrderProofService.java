package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderProofRequest;
import com.nutalig.dto.ApprovalRequestDto;
import com.nutalig.dto.PurchaseOrderProofAttachmentDto;
import com.nutalig.dto.PurchaseOrderProofDto;
import com.nutalig.dto.PurchaseOrderProofRevisionDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.*;
import com.nutalig.mapper.SystemConfigMapper;
import com.nutalig.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderProofService {

    private static final int MAX_FILES = 10;
    private static final long MAX_IMAGE_SIZE = 20L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 200L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/webm"
    );
    private static final Set<String> PRIVILEGED_VIEW_ROLES = Set.of(
            "SUPER_ADMIN", "ADMIN", "PROCUREMENT", "PROCUREMENT_MANAGER", "PROCUREMENT_ADMIN"
    );
    private static final String DIGITAL_PROOF_CODE = "DIGITAL_PROOF";
    private static final String ON_PRESS_COLOR_CHECK_CODE = "ON_PRESS_COLOR_CHECK";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderProofRepository proofRepository;
    private final PurchaseOrderProofRevisionRepository revisionRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigMapper systemConfigMapper;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalService approvalService;
    private final FileStorageService fileStorageService;
    private final ActivityHistoryService activityHistoryService;
    private final LineMessageService lineMessageService;
    private final PurchaseOrderMilestoneService purchaseOrderMilestoneService;

    @Transactional(readOnly = true)
    public List<PurchaseOrderProofDto> getProofs(String purchaseOrderNo, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        validateCanViewPurchaseOrder(purchaseOrder, userId);
        return proofRepository.findAllByPurchaseOrder_PurchaseOrderNoOrderByIdAsc(purchaseOrderNo)
                .stream().map(proof -> toDto(proof, userId)).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderProofDto getProof(Long proofId, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderProofEntity proof = getDetailedProof(proofId);
        validateCanViewPurchaseOrder(proof.getPurchaseOrder(), userId);
        return toDto(proof, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderProofDto createAndSubmit(
            String purchaseOrderNo,
            CreatePurchaseOrderProofRequest request,
            List<MultipartFile> attachments,
            String userId
    ) throws Exception {
        validateRequest(request, attachments);
        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        validateProductionRunning(purchaseOrder);
        SystemConfigEntity proofType = resolveProofType(request.getProofType());
        String proofTypeCode = proofType.getId().getCode();
        validateProofNotSkipped(purchaseOrder, proofTypeCode);
        if (proofRepository.findByPurchaseOrder_PurchaseOrderNoAndProofType_Id_Code(
                purchaseOrderNo, proofTypeCode).isPresent()) {
            throw new InvalidRequestException("Proof type already exists. Submit a new revision instead.");
        }
        if (ON_PRESS_COLOR_CHECK_CODE.equals(proofTypeCode)) {
            if (!purchaseOrderMilestoneService.isProofResolved(
                    purchaseOrder, PurchaseOrderMilestoneCode.DIGITAL_PROOF)) {
                throw new InvalidRequestException("Digital Proof must be approved or skipped before On-Press Color Check.");
            }
        }

        UserEntity salesUser = resolveSalesUser(purchaseOrder);
        PurchaseOrderProofEntity proof = new PurchaseOrderProofEntity();
        proof.setPurchaseOrder(purchaseOrder);
        proof.setProofType(proofType);
        proof.setRequired(Boolean.TRUE);
        proof.setStatus(PurchaseOrderProofStatus.DRAFT);
        proof.setCurrentRevision(0);
        proof.setAssignedSalesUser(salesUser);
        proof.setCreatedBy(userId);
        proof.setUpdatedBy(userId);
        proof = proofRepository.save(proof);

        return submitRevision(proof, request, attachments, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderProofDto resubmit(
            Long proofId,
            CreatePurchaseOrderProofRequest request,
            List<MultipartFile> attachments,
            String userId
    ) throws Exception {
        PurchaseOrderProofEntity proof = proofRepository.findByIdForUpdate(proofId)
                .orElseThrow(() -> new DataNotFoundException("Purchase order proof " + proofId + " not found."));
        validateProductionRunning(proof.getPurchaseOrder());
        validateProofNotSkipped(proof.getPurchaseOrder(), proofTypeCode(proof));
        if (proof.getStatus() != PurchaseOrderProofStatus.CHANGES_REQUESTED
                && proof.getStatus() != PurchaseOrderProofStatus.CANCELLED) {
            throw new InvalidRequestException("Only a proof with requested changes or a cancelled proof can be resubmitted.");
        }
        if (StringUtils.isNotBlank(request.getProofType())
                && !StringUtils.equalsIgnoreCase(request.getProofType(), proofTypeCode(proof))) {
            throw new InvalidRequestException("Proof type cannot be changed when resubmitting.");
        }
        request.setProofType(proofTypeCode(proof));
        validateRequest(request, attachments);
        return submitRevision(proof, request, attachments, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderProofDto approve(Long proofId, String comment, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderProofEntity proof = getDetailedProof(proofId);
        PurchaseOrderProofRevisionEntity revision = getCurrentRevision(proof);
        if (proof.getStatus() != PurchaseOrderProofStatus.PENDING_APPROVAL
                || revision.getStatus() != PurchaseOrderProofStatus.PENDING_APPROVAL) {
            throw new InvalidRequestException("Proof is not pending approval.");
        }
        revision.setSalesComment(StringUtils.trimToNull(comment));
        revision.setUpdatedBy(userId);
        revisionRepository.save(revision);
        approvalService.approveLatestApprovalByEntityAndType(
                ActivityEntityType.PURCHASE_ORDER_PROOF,
                String.valueOf(proofId),
                ApprovalRequestType.PURCHASE_ORDER_PROOF,
                userId
        );
        notifyRequester(revision, "งานพรูฟ " + proof.getPurchaseOrder().getPurchaseOrderNo()
                + " Revision " + revision.getRevisionNo() + " ได้รับการอนุมัติแล้ว");
        return toDto(getDetailedProof(proofId), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderProofDto requestChanges(Long proofId, String reason, String userId)
            throws DataNotFoundException, InvalidRequestException {
        if (StringUtils.isBlank(reason)) {
            throw new InvalidRequestException("reason is required.");
        }
        PurchaseOrderProofEntity proof = getDetailedProof(proofId);
        PurchaseOrderProofRevisionEntity revision = getCurrentRevision(proof);
        if (proof.getStatus() != PurchaseOrderProofStatus.PENDING_APPROVAL
                || revision.getStatus() != PurchaseOrderProofStatus.PENDING_APPROVAL) {
            throw new InvalidRequestException("Proof is not pending approval.");
        }
        approvalService.rejectLatestApprovalByEntityAndType(
                ActivityEntityType.PURCHASE_ORDER_PROOF,
                String.valueOf(proofId),
                ApprovalRequestType.PURCHASE_ORDER_PROOF,
                reason.trim(),
                userId
        );
        notifyRequester(revision, "งานพรูฟ " + proof.getPurchaseOrder().getPurchaseOrderNo()
                + " Revision " + revision.getRevisionNo() + " ถูกขอให้แก้ไข: " + reason.trim());
        return toDto(getDetailedProof(proofId), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderProofDto cancel(Long proofId, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderProofEntity proof = getDetailedProof(proofId);
        if (proof.getStatus() != PurchaseOrderProofStatus.PENDING_APPROVAL
                && proof.getStatus() != PurchaseOrderProofStatus.DRAFT) {
            throw new InvalidRequestException("Only a draft or pending proof can be cancelled.");
        }
        PurchaseOrderProofRevisionEntity revision = getCurrentRevision(proof);
        if (proof.getStatus() == PurchaseOrderProofStatus.PENDING_APPROVAL) {
            approvalService.cancelLatestApprovalByEntityAndType(
                    ActivityEntityType.PURCHASE_ORDER_PROOF,
                    String.valueOf(proofId),
                    ApprovalRequestType.PURCHASE_ORDER_PROOF,
                    userId
            );
        }
        proof.setStatus(PurchaseOrderProofStatus.CANCELLED);
        proof.setRequired(Boolean.FALSE);
        proof.setUpdatedBy(userId);
        revision.setStatus(PurchaseOrderProofStatus.CANCELLED);
        revision.setUpdatedBy(userId);
        revisionRepository.save(revision);
        proofRepository.save(proof);
        purchaseOrderMilestoneService.markProofStatus(
                proof.getPurchaseOrder(), proofTypeCode(proof),
                PurchaseOrderMilestoneStatus.CANCELLED,
                "ยกเลิกคำขอพรูฟ", userId
        );
        activityHistoryService.record(ActivityEntityType.PURCHASE_ORDER,
                proof.getPurchaseOrder().getPurchaseOrderNo(), userId, ActivityActorType.USER,
                ActivityAction.UPDATE, ActivitySource.API,
                "ยกเลิกงานพรูฟ " + proofTypeLabel(proof),
                Map.of("proofId", proof.getId(), "revisionNo", revision.getRevisionNo()));
        return toDto(getDetailedProof(proofId), userId);
    }

    private PurchaseOrderProofDto submitRevision(
            PurchaseOrderProofEntity proof,
            CreatePurchaseOrderProofRequest request,
            List<MultipartFile> attachments,
            String userId
    ) throws Exception {
        UserEntity requester = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        int revisionNo = proof.getCurrentRevision() + 1;
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        PurchaseOrderProofRevisionEntity revision = new PurchaseOrderProofRevisionEntity();
        revision.setRevisionNo(revisionNo);
        revision.setTitle(StringUtils.trim(request.getTitle()));
        revision.setProcurementNote(StringUtils.trimToNull(request.getProcurementNote()));
        revision.setStatus(PurchaseOrderProofStatus.PENDING_APPROVAL);
        revision.setRequestedByUser(requester);
        revision.setRequestedAt(now);
        revision.setDueDate(request.getDueDate() != null ? request.getDueDate() : now.plusDays(1));
        revision.setCreatedBy(userId);
        revision.setUpdatedBy(userId);
        // Initialize the existing revision collection before inserting the new row.
        proof.addRevision(revision);

        int sortOrder = 0;
        for (MultipartFile file : attachments) {
            PurchaseOrderProofMediaType mediaType = resolveMediaType(file);
            String relativeDir = "purchase-order-proofs/" + proof.getPurchaseOrder().getPurchaseOrderNo()
                    + "/" + proofTypeCode(proof).toLowerCase() + "/revision-" + revisionNo;
            UploadFileResponse uploaded = fileStorageService.uploadFile(file, relativeDir);
            PurchaseOrderProofAttachmentEntity attachment = new PurchaseOrderProofAttachmentEntity();
            attachment.setFileName(uploaded.getFileName());
            attachment.setOriginalFileName(file.getOriginalFilename());
            attachment.setFileUrl(uploaded.getUrl());
            attachment.setContentType(uploaded.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setMediaType(mediaType);
            attachment.setSortOrder(sortOrder++);
            revision.addAttachment(attachment);
        }

        // Persist this revision first. Merging a proof with a transient revision can create
        // a managed copy, leaving the original revision without an ID and inserting it twice.
        revision = revisionRepository.saveAndFlush(revision);
        proof.setCurrentRevision(revisionNo);
        proof.setRequired(Boolean.TRUE);
        proof.setStatus(PurchaseOrderProofStatus.PENDING_APPROVAL);
        proof.setUpdatedBy(userId);
        proofRepository.saveAndFlush(proof);
        ApprovalRequestDto approval = approvalService.createPurchaseOrderProofApprovalRequest(proof, revision, userId);
        revision.setApprovalRequest(approvalRequestRepository.getReferenceById(approval.getId()));
        revisionRepository.saveAndFlush(revision);
        purchaseOrderMilestoneService.markProofStatus(
                proof.getPurchaseOrder(), proofTypeCode(proof),
                PurchaseOrderMilestoneStatus.IN_PROGRESS,
                null, userId
        );

        activityHistoryService.record(ActivityEntityType.PURCHASE_ORDER,
                proof.getPurchaseOrder().getPurchaseOrderNo(), userId, ActivityActorType.USER,
                ActivityAction.REQUEST_APPROVAL, ActivitySource.API,
                "ส่งงานพรูฟ " + proofTypeLabel(proof) + " Revision " + revisionNo + " ให้เซลล์อนุมัติ",
                Map.of("proofId", proof.getId(), "revisionId", revision.getId(),
                        "revisionNo", revisionNo, "proofType", proofTypeCode(proof),
                        "attachmentCount", attachments.size(), "assignedSalesUserId", proof.getAssignedSalesUser().getId()));
        return toDto(getDetailedProof(proof.getId()), userId);
    }

    private void validateRequest(CreatePurchaseOrderProofRequest request, List<MultipartFile> attachments)
            throws InvalidRequestException {
        if (request == null || StringUtils.isBlank(request.getProofType())) {
            throw new InvalidRequestException("proofType is required.");
        }
        if (StringUtils.isBlank(request.getTitle())) {
            throw new InvalidRequestException("title is required.");
        }
        if (attachments == null || attachments.isEmpty()) {
            throw new InvalidRequestException("At least one image or video is required.");
        }
        if (attachments.size() > MAX_FILES) {
            throw new InvalidRequestException("A maximum of " + MAX_FILES + " files is allowed.");
        }
        for (MultipartFile file : attachments) {
            resolveMediaType(file);
        }
    }

    private PurchaseOrderProofMediaType resolveMediaType(MultipartFile file) throws InvalidRequestException {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Attachment cannot be empty.");
        }
        String contentType = StringUtils.lowerCase(StringUtils.trimToEmpty(file.getContentType()));
        if (IMAGE_TYPES.contains(contentType)) {
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new InvalidRequestException("Image files must not exceed 20 MB.");
            }
            return PurchaseOrderProofMediaType.IMAGE;
        }
        if (VIDEO_TYPES.contains(contentType)) {
            if (file.getSize() > MAX_VIDEO_SIZE) {
                throw new InvalidRequestException("Video files must not exceed 200 MB.");
            }
            return PurchaseOrderProofMediaType.VIDEO;
        }
        throw new InvalidRequestException("Only JPG, PNG, WEBP, GIF, MP4, MOV and WEBM files are supported.");
    }

    private void validateProductionRunning(PurchaseOrderEntity purchaseOrder) throws InvalidRequestException {
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.PRODUCTION_RUNNING) {
            throw new InvalidRequestException("Proof can only be submitted while purchase order production is running.");
        }
    }

    private void validateProofNotSkipped(PurchaseOrderEntity purchaseOrder, String proofTypeCode)
            throws InvalidRequestException {
        if (DIGITAL_PROOF_CODE.equals(proofTypeCode)
                && purchaseOrderMilestoneService.isProofSkipped(
                purchaseOrder, PurchaseOrderMilestoneCode.DIGITAL_PROOF)) {
            throw new InvalidRequestException("Digital Proof has already been skipped.");
        }
    }

    private UserEntity resolveSalesUser(PurchaseOrderEntity purchaseOrder)
            throws InvalidRequestException, DataNotFoundException {
        if (purchaseOrder.getSalesOrder() == null || purchaseOrder.getSalesOrder().getSales() == null) {
            throw new InvalidRequestException("Purchase order does not have an assigned sales employee.");
        }
        String employeeId = purchaseOrder.getSalesOrder().getSales().getEmployeeId();
        UserEntity user = userRepository.findByEmployeeEntity_EmployeeId(employeeId)
                .orElseThrow(() -> new DataNotFoundException("User for sales employee " + employeeId + " not found."));
        if (!Status.ACTIVE.equals(user.getStatus())) {
            throw new InvalidRequestException("Assigned sales user is not active.");
        }
        return user;
    }

    private PurchaseOrderProofEntity getDetailedProof(Long proofId) throws DataNotFoundException {
        return proofRepository.findDetailedById(proofId)
                .orElseThrow(() -> new DataNotFoundException("Purchase order proof " + proofId + " not found."));
    }

    private PurchaseOrderProofRevisionEntity getCurrentRevision(PurchaseOrderProofEntity proof)
            throws DataNotFoundException {
        return proof.getRevisions().stream()
                .filter(revision -> revision.getRevisionNo().equals(proof.getCurrentRevision()))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Current proof revision not found."));
    }

    private void validateCanViewPurchaseOrder(PurchaseOrderEntity purchaseOrder, String userId)
            throws DataNotFoundException, InvalidRequestException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        String role = roleOf(user);
        if (PRIVILEGED_VIEW_ROLES.contains(role) || "SALES_MANAGER".equals(role)) {
            return;
        }
        String employeeId = user.getEmployeeEntity() != null ? user.getEmployeeEntity().getEmployeeId() : null;
        String salesId = purchaseOrder.getSalesOrder() != null && purchaseOrder.getSalesOrder().getSales() != null
                ? purchaseOrder.getSalesOrder().getSales().getEmployeeId() : null;
        if (!"SALES".equals(role) || !StringUtils.equals(employeeId, salesId)) {
            throw new InvalidRequestException("You do not have access to this proof.");
        }
    }

    private String roleOf(UserEntity user) {
        return user.getUserRoleEntity() != null ? user.getUserRoleEntity().getRoleCode() : null;
    }

    private void notifyRequester(PurchaseOrderProofRevisionEntity revision, String message) {
        if (revision.getRequestedByUser() == null) return;
        try {
            lineMessageService.sendTextMessage(revision.getRequestedByUser().getId(), message);
        } catch (Exception exception) {
            log.warn("Cannot notify proof requester {}", revision.getRequestedByUser().getId(), exception);
        }
    }

    private PurchaseOrderProofDto toDto(PurchaseOrderProofEntity entity, String userId) {
        PurchaseOrderProofDto dto = new PurchaseOrderProofDto();
        PurchaseOrderEntity purchaseOrder = entity.getPurchaseOrder();
        SalesOrderEntity salesOrder = purchaseOrder != null ? purchaseOrder.getSalesOrder() : null;
        dto.setId(entity.getId());
        dto.setPurchaseOrderNo(purchaseOrder != null ? purchaseOrder.getPurchaseOrderNo() : null);
        dto.setSalesOrderNo(salesOrder != null ? salesOrder.getSalesOrderNo() : null);
        dto.setCustomerName(salesOrder != null
                ? StringUtils.defaultIfBlank(salesOrder.getCustomerNameSnapshot(),
                salesOrder.getCustomer() != null ? salesOrder.getCustomer().getCustomerName() : null) : null);
        dto.setProofType(systemConfigMapper.toDto(entity.getProofType()));
        dto.setRequired(entity.getRequired());
        dto.setCurrentRevision(entity.getCurrentRevision());
        dto.setStatus(entity.getStatus());
        dto.setAssignedSalesUserId(entity.getAssignedSalesUser() != null ? entity.getAssignedSalesUser().getId() : null);
        dto.setAssignedSalesName(entity.getAssignedSalesUser() != null ? entity.getAssignedSalesUser().getDisplayName() : null);

        UserEntity viewer = userRepository.findById(userId).orElse(null);
        String role = viewer != null ? roleOf(viewer) : null;
        boolean isOwner = entity.getAssignedSalesUser() != null
                && StringUtils.equals(entity.getAssignedSalesUser().getId(), userId);
        boolean canOverride = "SALES_MANAGER".equals(role) || "SUPER_ADMIN".equals(role);
        dto.setCanApprove(entity.getStatus() == PurchaseOrderProofStatus.PENDING_APPROVAL && (isOwner || canOverride));
        dto.setCanResubmit((entity.getStatus() == PurchaseOrderProofStatus.CHANGES_REQUESTED
                || entity.getStatus() == PurchaseOrderProofStatus.CANCELLED)
                && (PRIVILEGED_VIEW_ROLES.contains(role) || "SUPER_ADMIN".equals(role)));
        dto.setCanCancel(entity.getStatus() == PurchaseOrderProofStatus.PENDING_APPROVAL
                && (PRIVILEGED_VIEW_ROLES.contains(role) || "SUPER_ADMIN".equals(role)));
        dto.setRevisions(entity.getRevisions().stream().map(this::toRevisionDto).toList());
        return dto;
    }

    private SystemConfigEntity resolveProofType(String code)
            throws DataNotFoundException, InvalidRequestException {
        String normalizedCode = StringUtils.upperCase(StringUtils.trimToEmpty(code));
        if (StringUtils.isBlank(normalizedCode)) {
            throw new InvalidRequestException("proofType is required.");
        }
        return systemConfigRepository.findByIdGroupCodeAndIdCode(
                        SystemConstant.PURCHASE_ORDER_PROOF_TYPE, normalizedCode)
                .orElseThrow(() -> new DataNotFoundException(
                        "Purchase order proof type " + normalizedCode + " not found."));
    }

    private String proofTypeCode(PurchaseOrderProofEntity proof) {
        return proof.getProofType() != null && proof.getProofType().getId() != null
                ? proof.getProofType().getId().getCode()
                : null;
    }

    private String proofTypeLabel(PurchaseOrderProofEntity proof) {
        if (proof.getProofType() == null) return "-";
        return StringUtils.defaultIfBlank(
                proof.getProofType().getNameTh(),
                StringUtils.defaultIfBlank(proof.getProofType().getNameEn(), proofTypeCode(proof))
        );
    }

    private PurchaseOrderProofRevisionDto toRevisionDto(PurchaseOrderProofRevisionEntity entity) {
        PurchaseOrderProofRevisionDto dto = new PurchaseOrderProofRevisionDto();
        dto.setId(entity.getId());
        dto.setRevisionNo(entity.getRevisionNo());
        dto.setTitle(entity.getTitle());
        dto.setProcurementNote(entity.getProcurementNote());
        dto.setStatus(entity.getStatus());
        dto.setRequestedByUserId(entity.getRequestedByUser() != null ? entity.getRequestedByUser().getId() : null);
        dto.setRequestedByName(entity.getRequestedByUser() != null ? entity.getRequestedByUser().getDisplayName() : null);
        dto.setRequestedAt(entity.getRequestedAt());
        dto.setDueDate(entity.getDueDate());
        dto.setActedByUserId(entity.getActedByUser() != null ? entity.getActedByUser().getId() : null);
        dto.setActedByName(entity.getActedByUser() != null ? entity.getActedByUser().getDisplayName() : null);
        dto.setActedAt(entity.getActedAt());
        dto.setSalesComment(entity.getSalesComment());
        dto.setChangeReason(entity.getChangeReason());
        dto.setApprovalRequestId(entity.getApprovalRequest() != null ? entity.getApprovalRequest().getId() : null);
        dto.setAttachments(entity.getAttachments().stream().map(this::toAttachmentDto).toList());
        return dto;
    }

    private PurchaseOrderProofAttachmentDto toAttachmentDto(PurchaseOrderProofAttachmentEntity entity) {
        PurchaseOrderProofAttachmentDto dto = new PurchaseOrderProofAttachmentDto();
        dto.setId(entity.getId());
        dto.setFileName(entity.getFileName());
        dto.setOriginalFileName(entity.getOriginalFileName());
        dto.setFileUrl(entity.getFileUrl());
        dto.setContentType(entity.getContentType());
        dto.setFileSize(entity.getFileSize());
        dto.setMediaType(entity.getMediaType());
        dto.setThumbnailUrl(entity.getThumbnailUrl());
        dto.setSortOrder(entity.getSortOrder());
        return dto;
    }
}
