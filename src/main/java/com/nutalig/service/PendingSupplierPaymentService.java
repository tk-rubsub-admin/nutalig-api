package com.nutalig.service;

import com.nutalig.controller.purchaseorder.request.SearchPendingSupplierPaymentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.PendingSupplierPaymentDto;
import com.nutalig.constant.PurchaseOrderPaymentStatus;
import com.nutalig.entity.PurchaseOrderPaymentEntity;
import com.nutalig.entity.PurchaseOrderPaymentScheduleEntity;
import com.nutalig.repository.PurchaseOrderPaymentScheduleRepository;
import com.nutalig.repository.specification.PendingSupplierPaymentSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PendingSupplierPaymentService {

    private final PurchaseOrderPaymentScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public Pageable<PendingSupplierPaymentDto> search(
            SearchPendingSupplierPaymentRequest request,
            PageableRequest pageableRequest
    ) {
        if (pageableRequest.getSortBy() == null || pageableRequest.getSortDirection() == null) {
            pageableRequest.setSortBy("dueDate");
            pageableRequest.setSortDirection(Sort.Direction.ASC);
        }

        Page<PurchaseOrderPaymentScheduleEntity> page = scheduleRepository
                .findAll(PendingSupplierPaymentSpecification.search(request), pageableRequest.build());
        Pageable<PendingSupplierPaymentDto> response = new Pageable<>();
        response.setRecords(page.getContent().stream().map(this::toDto).toList());
        response.setPagination(Pagination.build(page));
        return response;
    }

    private PendingSupplierPaymentDto toDto(PurchaseOrderPaymentScheduleEntity schedule) {
        var purchaseOrder = schedule.getPurchaseOrder();
        BigDecimal pendingAmount = sumPending(schedule, false);
        BigDecimal pendingAmountThb = sumPending(schedule, true);
        BigDecimal outstandingAmount = nonNegative(
                defaultAmount(schedule.getExpectedAmount()).subtract(defaultAmount(schedule.getPaidAmount())));
        BigDecimal outstandingAmountThb = nonNegative(
                defaultAmount(schedule.getExpectedAmountThb()).subtract(defaultAmount(schedule.getPaidAmountThb())));

        PendingSupplierPaymentDto dto = new PendingSupplierPaymentDto();
        dto.setPurchaseOrderNo(purchaseOrder.getPurchaseOrderNo());
        dto.setSalesOrderNo(purchaseOrder.getSalesOrder() != null
                ? purchaseOrder.getSalesOrder().getSalesOrderNo() : null);
        dto.setSupplierId(purchaseOrder.getSupplier() != null ? purchaseOrder.getSupplier().getId() : null);
        dto.setSupplierName(StringUtils.defaultIfBlank(
                purchaseOrder.getSupplierNameSnapshot(),
                purchaseOrder.getSupplier() != null ? purchaseOrder.getSupplier().getSupplierName() : null
        ));
        dto.setDocDate(purchaseOrder.getDocDate());
        dto.setCurrency(purchaseOrder.getCurrency());
        dto.setGrandTotal(purchaseOrder.getGrandTotal());
        dto.setGrandTotalThb(purchaseOrder.getGrandTotalThb());
        dto.setScheduleId(schedule.getId());
        dto.setInstallmentNo(schedule.getInstallmentNo());
        dto.setPaymentType(schedule.getPaymentType());
        dto.setPercentage(schedule.getPercentage());
        dto.setExpectedAmount(schedule.getExpectedAmount());
        dto.setPaidAmount(schedule.getPaidAmount());
        dto.setPendingAmount(pendingAmount);
        dto.setOutstandingAmount(outstandingAmount);
        dto.setPayableAmount(nonNegative(outstandingAmount.subtract(pendingAmount)));
        dto.setExpectedAmountThb(schedule.getExpectedAmountThb());
        dto.setPaidAmountThb(schedule.getPaidAmountThb());
        dto.setPendingAmountThb(pendingAmountThb);
        dto.setOutstandingAmountThb(outstandingAmountThb);
        dto.setPayableAmountThb(nonNegative(outstandingAmountThb.subtract(pendingAmountThb)));
        dto.setDueDate(schedule.getDueDate());
        return dto;
    }

    private BigDecimal sumPending(PurchaseOrderPaymentScheduleEntity schedule, boolean thb) {
        return schedule.getPayments().stream()
                .filter(payment -> payment.getStatus() == PurchaseOrderPaymentStatus.PENDING)
                .map(payment -> thb ? payment.getAmountThb() : payment.getAmount())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
