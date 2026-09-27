package com.nutalig.controller.purchaseorder;

import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderProofRequest;
import com.nutalig.controller.purchaseorder.request.PurchaseOrderProofDecisionRequest;
import com.nutalig.controller.response.GeneralResponse;
import com.nutalig.dto.PurchaseOrderProofDto;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.service.PurchaseOrderProofService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.nutalig.constant.ResponseStatus.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class PurchaseOrderProofController {

    private final PurchaseOrderProofService purchaseOrderProofService;

    @GetMapping("/purchase-orders/{purchaseOrderNo}/proofs")
    @PreAuthorize("hasAuthority('PERM_PO_PROOF_VIEW')")
    public GeneralResponse<List<PurchaseOrderProofDto>> getProofs(
            @PathVariable String purchaseOrderNo,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderProofService.getProofs(purchaseOrderNo, userId));
    }

    @PostMapping(path = "/purchase-orders/{purchaseOrderNo}/proofs", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_PO_PROOF_REQUEST')")
    public GeneralResponse<PurchaseOrderProofDto> createProof(
            @PathVariable String purchaseOrderNo,
            @ModelAttribute CreatePurchaseOrderProofRequest request,
            @RequestPart("attachments") List<MultipartFile> attachments,
            @RequestHeader("userId") String userId
    ) throws Exception {
        return new GeneralResponse<>(SUCCESS,
                purchaseOrderProofService.createAndSubmit(purchaseOrderNo, request, attachments, userId));
    }

    @GetMapping("/purchase-order-proofs/{proofId}")
    @PreAuthorize("hasAuthority('PERM_PO_PROOF_VIEW')")
    public GeneralResponse<PurchaseOrderProofDto> getProof(
            @PathVariable Long proofId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderProofService.getProof(proofId, userId));
    }

    @PostMapping(path = "/purchase-order-proofs/{proofId}/revisions", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_PO_PROOF_REQUEST')")
    public GeneralResponse<PurchaseOrderProofDto> resubmitProof(
            @PathVariable Long proofId,
            @ModelAttribute CreatePurchaseOrderProofRequest request,
            @RequestPart("attachments") List<MultipartFile> attachments,
            @RequestHeader("userId") String userId
    ) throws Exception {
        return new GeneralResponse<>(SUCCESS,
                purchaseOrderProofService.resubmit(proofId, request, attachments, userId));
    }

    @PostMapping("/purchase-order-proofs/{proofId}/approve")
    @PreAuthorize("hasAuthority('PERM_PO_PROOF_APPROVE')")
    public GeneralResponse<PurchaseOrderProofDto> approveProof(
            @PathVariable Long proofId,
            @RequestBody(required = false) PurchaseOrderProofDecisionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderProofService.approve(
                proofId, request != null ? request.getComment() : null, userId));
    }

    @PostMapping("/purchase-order-proofs/{proofId}/request-changes")
    @PreAuthorize("hasAuthority('PERM_PO_PROOF_APPROVE')")
    public GeneralResponse<PurchaseOrderProofDto> requestChanges(
            @PathVariable Long proofId,
            @RequestBody PurchaseOrderProofDecisionRequest request,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS,
                purchaseOrderProofService.requestChanges(proofId, request.getReason(), userId));
    }

    @PostMapping("/purchase-order-proofs/{proofId}/cancel")
    @PreAuthorize("hasAuthority('PERM_PO_PROOF_REQUEST')")
    public GeneralResponse<PurchaseOrderProofDto> cancelProof(
            @PathVariable Long proofId,
            @RequestHeader("userId") String userId
    ) throws DataNotFoundException, InvalidRequestException {
        return new GeneralResponse<>(SUCCESS, purchaseOrderProofService.cancel(proofId, userId));
    }
}
