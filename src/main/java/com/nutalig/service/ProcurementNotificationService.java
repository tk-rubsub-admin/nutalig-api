package com.nutalig.service;

import com.nutalig.config.LineConfiguration;
import com.nutalig.constant.ProcurementStatus;
import com.nutalig.constant.Status;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.entity.RfqHeaderEntity;
import com.nutalig.entity.SalesOrderEntity;
import com.nutalig.entity.UserEntity;
import com.nutalig.repository.RequestPriceHeaderRepository;
import com.nutalig.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementNotificationService {
    private final RequestPriceHeaderRepository requestPriceHeaderRepository;
    private final UserRepository userRepository;
    private final LineMessageService lineMessageService;
    private final LineConfiguration lineConfiguration;

    public void notifyReadyForPurchaseOrder(SalesOrderEntity salesOrder, ProcurementStatus status) {
        if (salesOrder == null || (status != ProcurementStatus.READY_FOR_PO
                && status != ProcurementStatus.READY_FOR_PO_OVERRIDE)) {
            return;
        }

        try {
            RfqHeaderEntity rfq = requestPriceHeaderRepository
                    .findFirstBySaleOrderId(salesOrder.getSalesOrderNo())
                    .orElse(null);
            EmployeeEntity procurementOwner = rfq == null ? null : rfq.getProcurement();
            if (procurementOwner == null || StringUtils.isBlank(procurementOwner.getEmployeeId())) {
                log.warn("No procurement owner found for sales order {}", salesOrder.getSalesOrderNo());
                return;
            }

            UserEntity user = userRepository
                    .findByEmployeeEntity_EmployeeId(procurementOwner.getEmployeeId())
                    .orElse(null);
            if (user == null || user.getStatus() != Status.ACTIVE
                    || StringUtils.isBlank(user.getLineUserId())) {
                log.warn("No active LINE-bound procurement owner found for sales order {}",
                        salesOrder.getSalesOrderNo());
                return;
            }

            String salesOrderNo = salesOrder.getSalesOrderNo();
            String userId = user.getId();
            String detailUrl = buildSalesOrderUrl(salesOrderNo);
            String statusLabel = status == ProcurementStatus.READY_FOR_PO_OVERRIDE
                    ? "พร้อมสร้าง PO (Override)" : "พร้อมสร้าง PO";
            String altText = "SO " + salesOrderNo + " " + statusLabel;
            String detail = "เลขที่ SO: " + salesOrderNo
                    + "\nสถานะ: " + statusLabel
                    + "\nกรุณาสร้างใบสั่งซื้อ";

            Runnable send = () -> {
                try {
                    lineMessageService.sendNotificationCard(
                            userId,
                            altText,
                            "Sales Order พร้อมสร้างใบสั่งซื้อ",
                            detail,
                            detailUrl
                    );
                } catch (Exception exception) {
                    log.warn("Cannot send procurement ready notification for sales order {}",
                            salesOrderNo, exception);
                }
            };

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        send.run();
                    }
                });
            } else {
                send.run();
            }
        } catch (Exception exception) {
            log.warn("Cannot prepare procurement ready notification for sales order {}",
                    salesOrder.getSalesOrderNo(), exception);
        }
    }

    private String buildSalesOrderUrl(String salesOrderNo) {
        URI loginSuccessUri = URI.create(lineConfiguration.getLoginSuccessUrl());
        return UriComponentsBuilder
                .fromUriString(loginSuccessUri.getScheme() + "://" + loginSuccessUri.getAuthority())
                .pathSegment("sales-order", salesOrderNo)
                .build()
                .toUriString();
    }
}
