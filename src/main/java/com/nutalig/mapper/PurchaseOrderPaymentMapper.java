package com.nutalig.mapper;

import com.nutalig.dto.PurchaseOrderPaymentAttachmentDto;
import com.nutalig.dto.PurchaseOrderPaymentDto;
import com.nutalig.entity.PurchaseOrderPaymentAttachmentEntity;
import com.nutalig.entity.PurchaseOrderPaymentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PurchaseOrderPaymentMapper {

    private final UserMapper userMapper;

    public PurchaseOrderPaymentDto toDto(PurchaseOrderPaymentEntity entity) {
        if (entity == null) return null;

        PurchaseOrderPaymentDto dto = new PurchaseOrderPaymentDto();
        dto.setId(entity.getId());
        dto.setScheduleId(entity.getSchedule() == null ? null : entity.getSchedule().getId());
        dto.setPaymentType(entity.getPaymentType());
        dto.setInstallmentNo(entity.getInstallmentNo());
        dto.setPaymentDate(entity.getPaymentDate());
        dto.setAmount(entity.getAmount());
        dto.setCurrency(entity.getCurrency());
        dto.setExchangeRate(entity.getExchangeRate());
        dto.setAmountThb(entity.getAmountThb());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setTransferReference(entity.getTransferReference());
        dto.setChequeBank(entity.getChequeBank());
        dto.setChequeNo(entity.getChequeNo());
        dto.setChequeDate(entity.getChequeDate());
        dto.setChequeBranch(entity.getChequeBranch());
        dto.setRemark(entity.getRemark());
        dto.setStatus(entity.getStatus());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setApprovedBy(userMapper.toDto(entity.getApprovedBy()));
        dto.setApprovedDate(entity.getApprovedDate());
        dto.setRejectedBy(userMapper.toDto(entity.getRejectedBy()));
        dto.setRejectedDate(entity.getRejectedDate());
        dto.setVoidedBy(userMapper.toDto(entity.getVoidedBy()));
        dto.setVoidedDate(entity.getVoidedDate());
        dto.setVoidReason(entity.getVoidReason());
        dto.setRequestKey(entity.getRequestKey());
        dto.setCreatedBy(userMapper.toDto(entity.getCreatedBy()));
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedBy(userMapper.toDto(entity.getUpdatedBy()));
        dto.setUpdatedDate(entity.getUpdatedDate());
        dto.setAttachments(entity.getAttachments().stream()
                .sorted(Comparator.comparing(
                        PurchaseOrderPaymentAttachmentEntity::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ).thenComparing(PurchaseOrderPaymentAttachmentEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toAttachmentDto)
                .toList());
        return dto;
    }

    public List<PurchaseOrderPaymentDto> toDtos(Iterable<PurchaseOrderPaymentEntity> entities) {
        if (entities == null) return List.of();
        java.util.ArrayList<PurchaseOrderPaymentDto> result = new java.util.ArrayList<>();
        entities.forEach(entity -> result.add(toDto(entity)));
        return result;
    }

    private PurchaseOrderPaymentAttachmentDto toAttachmentDto(PurchaseOrderPaymentAttachmentEntity entity) {
        PurchaseOrderPaymentAttachmentDto dto = new PurchaseOrderPaymentAttachmentDto();
        dto.setId(entity.getId());
        dto.setFileName(entity.getFileName());
        dto.setOriginalFileName(entity.getOriginalFileName());
        dto.setFileUrl(entity.getFileUrl());
        dto.setContentType(entity.getContentType());
        dto.setFileSize(entity.getFileSize());
        dto.setSortOrder(entity.getSortOrder());
        return dto;
    }
}
