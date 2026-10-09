package com.nutalig.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutalig.config.LineConfiguration;
import com.nutalig.constant.*;
import com.nutalig.entity.*;
import com.nutalig.entity.id.SystemConfigId;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderProofApprovalNotificationTest {
    @Mock ApprovalRequestRepository approvalRequestRepository;
    @Mock ApprovalRequestStepRepository approvalRequestStepRepository;
    @Mock ApprovalRequestAuditLogRepository approvalRequestAuditLogRepository;
    @Mock UserRepository userRepository;
    @Mock GeneratedIdSequenceService generatedIdSequenceService;
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @Mock LineMessageService lineMessageService;
    @Mock ApprovalTemplateService approvalTemplateService;
    @Mock ApprovalBusinessService approvalBusinessService;
    @Mock UserTodoService userTodoService;
    @Mock UserProfileService userProfileService;
    @Mock ActivityHistoryService activityHistoryService;
    @Mock LineConfiguration lineConfiguration;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks ApprovalService service;

    private PurchaseOrderProofEntity proof;
    private PurchaseOrderProofRevisionEntity revision;
    private UserEntity sales;
    private ApprovalRequestEntity savedRequest;

    @BeforeEach
    void setup() {
        sales = new UserEntity();
        sales.setId("sales-user");
        sales.setStatus(Status.ACTIVE);
        sales.setLineUserId("line-sales");
        SystemConfigId id = new SystemConfigId();
        id.setGroupCode(SystemConstant.PURCHASE_ORDER_PROOF_TYPE);
        id.setCode("DIGITAL_PROOF");
        SystemConfigEntity type = new SystemConfigEntity();
        type.setId(id);
        PurchaseOrderEntity po = new PurchaseOrderEntity();
        po.setPurchaseOrderNo("PO-1");
        proof = new PurchaseOrderProofEntity();
        proof.setId(11L);
        proof.setPurchaseOrder(po);
        proof.setProofType(type);
        proof.setAssignedSalesUser(sales);
        revision = new PurchaseOrderProofRevisionEntity();
        revision.setId(100L);
        revision.setRevisionNo(1);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void clearTransaction() {
        TransactionSynchronizationManager.clear();
    }

    private void stubCreation() {
        when(generatedIdSequenceService.getNextIdWithMonth(any(), anyInt())).thenReturn("APR-1");
        when(userProfileService.getNameFromId("requester")).thenReturn("Requester");
        when(approvalRequestRepository.save(any())).thenAnswer(invocation -> {
            savedRequest = invocation.getArgument(0);
            savedRequest.setId(220L);
            savedRequest.getSteps().getFirst().setId(221L);
            return savedRequest;
        });
    }

    private void stubCommittedNotification() throws Exception {
        when(approvalRequestRepository.findById(220L)).thenAnswer(invocation -> Optional.of(savedRequest));
        when(approvalRequestStepRepository.findById(221L)).thenAnswer(invocation -> Optional.of(savedRequest.getSteps().getFirst()));
        when(userRepository.findById(anyString())).thenAnswer(invocation ->
                "sales-user".equals(invocation.getArgument(0)) ? Optional.of(sales) : Optional.empty());
        when(lineConfiguration.getLoginSuccessUrl()).thenReturn("https://example.test/login-success");
        var message = objectMapper.createObjectNode();
        when(approvalTemplateService.renderTemplate(anyString(), any())).thenReturn(message);
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            TransactionDefinition definition = invocation.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            return new SimpleTransactionStatus();
        });
    }

    @Test
    void sendsLineOnlyAfterCommitWithPersistedActionKeysAndCommitsTheNotificationAuditSeparately() throws Exception {
        stubCreation();
        stubCommittedNotification();
        service.createPurchaseOrderProofApprovalRequest(proof, revision, "requester");
        var step = savedRequest.getSteps().getFirst();
        String approveKey = step.getApproveActionKey();
        String rejectKey = step.getRejectActionKey();
        assertNotNull(approveKey);
        assertNotNull(rejectKey);
        assertNull(step.getSentAt());
        verifyNoInteractions(lineMessageService);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(lineMessageService).sendFlexMessage(eq("line-sales"), any());
        assertEquals(approveKey, step.getApproveActionKey());
        assertEquals(rejectKey, step.getRejectActionKey());
        assertNotNull(step.getSentAt());
        verify(transactionManager).commit(any());
    }

    @Test
    void doesNotSendAnOrphanProofLinkWhenTheSubmissionRollsBack() throws Exception {
        stubCreation();
        service.createPurchaseOrderProofApprovalRequest(proof, revision, "requester");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(lineMessageService, transactionManager);
    }

    @Test
    void notificationFailureDoesNotFailTheAlreadyCommittedProof() throws Exception {
        stubCreation();
        stubCommittedNotification();
        doThrow(new RuntimeException("LINE unavailable")).when(lineMessageService).sendFlexMessage(anyString(), any());
        service.createPurchaseOrderProofApprovalRequest(proof, revision, "requester");
        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit));
        verify(transactionManager).commit(argThat(status -> status.isRollbackOnly()));
    }

    @Test
    void refusesToNotifyForAnUnpersistedRevision() {
        revision.setId(null);
        assertThrows(InvalidRequestException.class,
                () -> service.createPurchaseOrderProofApprovalRequest(proof, revision, "requester"));
        verifyNoInteractions(approvalRequestRepository, lineMessageService);
    }
}
