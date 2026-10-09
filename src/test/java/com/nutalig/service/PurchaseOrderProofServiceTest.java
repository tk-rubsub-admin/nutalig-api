package com.nutalig.service;

import com.nutalig.constant.*;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderProofRequest;
import com.nutalig.dto.ApprovalRequestDto;
import com.nutalig.entity.*;
import com.nutalig.entity.id.SystemConfigId;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.SystemConfigMapper;
import com.nutalig.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderProofServiceTest {
    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock PurchaseOrderProofRepository proofRepository;
    @Mock PurchaseOrderProofRevisionRepository revisionRepository;
    @Mock UserRepository userRepository;
    @Mock SystemConfigRepository systemConfigRepository;
    @Mock SystemConfigMapper systemConfigMapper;
    @Mock ApprovalRequestRepository approvalRequestRepository;
    @Mock ApprovalService approvalService;
    @Mock FileStorageService fileStorageService;
    @Mock ActivityHistoryService activityHistoryService;
    @Mock LineMessageService lineMessageService;
    @Mock PurchaseOrderMilestoneService purchaseOrderMilestoneService;
    @InjectMocks PurchaseOrderProofService service;

    private PurchaseOrderEntity po;
    private PurchaseOrderProofEntity proof;
    private SystemConfigEntity proofType;
    private UserEntity sales;
    private CreatePurchaseOrderProofRequest request;
    private final MockMultipartFile image = new MockMultipartFile("attachments", "proof.png", "image/png", new byte[]{1});

    @BeforeEach
    void setup() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId("SALES-1");
        SalesOrderEntity so = new SalesOrderEntity();
        so.setSales(employee);
        po = new PurchaseOrderEntity();
        po.setPurchaseOrderNo("PO-1");
        po.setSalesOrder(so);
        po.setStatus(PurchaseOrderStatus.PRODUCTION_RUNNING);
        sales = new UserEntity();
        sales.setId("sales-user");
        sales.setStatus(Status.ACTIVE);
        proofType = new SystemConfigEntity();
        SystemConfigId configId = new SystemConfigId();
        configId.setGroupCode(SystemConstant.PURCHASE_ORDER_PROOF_TYPE);
        configId.setCode("DIGITAL_PROOF");
        proofType.setId(configId);
        proof = new PurchaseOrderProofEntity();
        proof.setId(11L);
        proof.setPurchaseOrder(po);
        proof.setProofType(proofType);
        proof.setAssignedSalesUser(sales);
        request = new CreatePurchaseOrderProofRequest();
        request.setProofType("DIGITAL_PROOF");
        request.setTitle("Proof");
    }

    private void stubSubmission() throws Exception {
        UserEntity requester = new UserEntity();
        requester.setId("requester");
        UserRoleEntity role = new UserRoleEntity();
        role.setRoleCode("PROCUREMENT");
        requester.setUserRoleEntity(role);
        when(userRepository.findById("requester")).thenReturn(Optional.of(requester));
        when(fileStorageService.uploadFile(any(), anyString())).thenReturn(new UploadFileResponse("proof.png", "/proof.png", "image/png"));
        AtomicReference<PurchaseOrderProofRevisionEntity> saved = new AtomicReference<>();
        when(revisionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PurchaseOrderProofRevisionEntity revision = invocation.getArgument(0);
            if (revision.getId() == null) {
                assertNull(saved.get(), "A revision must be inserted only once");
                revision.setId(100L);
                saved.set(revision);
            }
            assertSame(saved.get(), revision, "Approval must update the persisted revision instance");
            return revision;
        });
        when(proofRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PurchaseOrderProofEntity entity = invocation.getArgument(0);
            assertNotNull(saved.get(), "Revision must be saved before cascading the proof");
            assertSame(saved.get(), entity.getRevisions().getLast());
            assertNotNull(entity.getRevisions().getLast().getId());
            return entity;
        });
        ApprovalRequestDto approval = new ApprovalRequestDto();
        approval.setId(220L);
        when(approvalService.createPurchaseOrderProofApprovalRequest(any(), any(), eq("requester")))
                .thenAnswer(invocation -> {
                    assertNotNull(((PurchaseOrderProofRevisionEntity) invocation.getArgument(1)).getId());
                    return approval;
                });
        ApprovalRequestEntity approvalEntity = new ApprovalRequestEntity();
        approvalEntity.setId(220L);
        when(approvalRequestRepository.getReferenceById(220L)).thenReturn(approvalEntity);
        when(proofRepository.findDetailedById(11L)).thenAnswer(invocation -> Optional.of(proof));
    }

    @Test
    void creationInsertsOneRevisionBeforeCreatingApprovalAndUpdatesThatSameRevision() throws Exception {
        stubSubmission();
        when(purchaseOrderRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
        when(systemConfigRepository.findByIdGroupCodeAndIdCode(SystemConstant.PURCHASE_ORDER_PROOF_TYPE, "DIGITAL_PROOF"))
                .thenReturn(Optional.of(proofType));
        when(userRepository.findByEmployeeEntity_EmployeeId("SALES-1")).thenReturn(Optional.of(sales));
        when(proofRepository.save(any())).thenAnswer(invocation -> {
            proof = invocation.getArgument(0);
            proof.setId(11L);
            return proof;
        });
        var dto = service.createAndSubmit("PO-1", request, List.of(image), "requester");
        assertEquals(1, dto.getCurrentRevision());
        assertEquals(1, dto.getRevisions().size());
        assertEquals(100L, dto.getRevisions().getFirst().getId());
        assertEquals(220L, dto.getRevisions().getFirst().getApprovalRequestId());
        assertEquals(1, dto.getRevisions().getFirst().getAttachments().size());
    }

    @Test
    void resubmissionLocksTheProofAndPersistsTheNextRevision() throws Exception {
        proof.setCurrentRevision(1);
        proof.setStatus(PurchaseOrderProofStatus.CHANGES_REQUESTED);
        PurchaseOrderProofRevisionEntity previous = new PurchaseOrderProofRevisionEntity();
        previous.setId(99L);
        previous.setRevisionNo(1);
        proof.addRevision(previous);
        when(proofRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(proof));
        stubSubmission();
        var dto = service.resubmit(11L, request, List.of(image), "requester");
        assertEquals(2, dto.getCurrentRevision());
        assertEquals(2, dto.getRevisions().size());
        verify(proofRepository).findByIdForUpdate(11L);
    }

    @Test
    void aSecondResubmissionAfterTheFirstCompletesIsRejectedWithoutCreatingAnotherRevision() {
        proof.setStatus(PurchaseOrderProofStatus.PENDING_APPROVAL);
        proof.setCurrentRevision(2);
        when(proofRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(proof));
        assertThrows(InvalidRequestException.class, () -> service.resubmit(11L, request, List.of(image), "requester"));
        verifyNoInteractions(revisionRepository, approvalService, fileStorageService);
    }
}
