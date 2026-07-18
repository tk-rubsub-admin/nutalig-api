package com.nutalig.service;

import com.nutalig.constant.Currency;
import com.nutalig.constant.*;
import com.nutalig.controller.file.response.UploadFileResponse;
import com.nutalig.controller.purchaseorder.request.CreatePurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.SearchPurchaseOrderRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderDetailRequest;
import com.nutalig.controller.purchaseorder.request.UpdatePurchaseOrderRequest;
import com.nutalig.controller.request.DocumentRequest;
import com.nutalig.controller.request.PageableRequest;
import com.nutalig.controller.response.Pageable;
import com.nutalig.controller.response.Pagination;
import com.nutalig.dto.*;
import com.nutalig.dto.document.DownloadDocumentDto;
import com.nutalig.dto.document.PurchaseOrderDocumentDto;
import com.nutalig.dto.document.PurchaseOrderItemDocumentDto;
import com.nutalig.entity.*;
import com.nutalig.exception.DataNotFoundException;
import com.nutalig.exception.InvalidRequestException;
import com.nutalig.mapper.SupplierMapper;
import com.nutalig.mapper.UserMapper;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import com.nutalig.utils.DocumentStatusResolver;
import com.nutalig.utils.PdfMergeUtil;
import com.nutalig.utils.RfqAttachmentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static com.nutalig.constant.BusinessConstant.DocumentPrefix.PURCHASE_ORDER_PREFIX;
import static com.nutalig.repository.specification.PurchaseOrderSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final GeneratedIdSequenceService generatedIdSequenceService;
    private final FileStorageService fileStorageService;
    private final PurchaseOrderAttachmentRepository purchaseOrderAttachmentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierShippingRepository supplierShippingRepository;
    private final UserRepository userRepository;
    private final SupplierMapper supplierMapper;
    private final UserMapper userMapper;
    private final ActivityHistoryService activityHistoryService;
    private final SystemConfigService systemConfigService;
    private final ReportService reportService;

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderEntity createPurchaseOrder(CreatePurchaseOrderRequest request, List<MultipartFile> attachments, String userId)
            throws Exception {
        validateCreateRequest(request, attachments);

        SalesOrderEntity salesOrder = salesOrderRepository.findById(request.getSalesOrderNo())
                .orElseThrow(() -> new DataNotFoundException("Sales order " + request.getSalesOrderNo() + " not found."));
        SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new DataNotFoundException("Supplier " + request.getSupplierId() + " not found."));
        SupplierShippingEntity supplierShipping = supplierShippingRepository
                .findByIdAndActiveTrue(request.getSupplierShippingId())
                .orElseThrow(() -> new DataNotFoundException(
                        "Supplier shipping " + request.getSupplierShippingId() + " not found."
                ));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        if (salesOrder.getProcurementStatus() != ProcurementStatus.READY_FOR_PO) {
            throw new InvalidRequestException("Sales order is not ready for purchase order creation");
        }

        List<PurchaseOrderEntity> existingOrders = purchaseOrderRepository
                .findBySalesOrderSalesOrderNoAndSupplierShippingIdOrderByCreatedDateDesc(
                        salesOrder.getSalesOrderNo(),
                        supplierShipping.getId()
                );
        boolean hasActiveOrder = existingOrders.stream()
                .anyMatch(item -> item.getStatus() != PurchaseOrderStatus.CANCELLED);
        if (hasActiveOrder) {
            throw new InvalidRequestException("Purchase order already exists for this sales order and supplier shipping");
        }

        List<SalesOrderDetailEntity> sourceItems = salesOrder.getItems().stream()
                .filter(item -> item.getSupplier() != null && StringUtils.equals(item.getSupplier().getId(), supplier.getId()))
                .filter(item -> StringUtils.equalsIgnoreCase(item.getShippingMethod(), supplierShipping.getShippingMethod().name()))
                .sorted(Comparator.comparing(item -> Optional.ofNullable(item.getLineNo()).orElse(0)))
                .toList();
        if (sourceItems.isEmpty()) {
            throw new InvalidRequestException("No sales order items found for selected supplier shipping");
        }

        Currency currency = null;
        for (SalesOrderDetailEntity item : sourceItems) {
            if (item.getSupplierCurrency() == null || item.getSupplierUnitPrice() == null
                    || item.getSupplierTotalUnitCost() == null) {
                throw new InvalidRequestException("Sales order detail " + item.getId() + " is missing supplier cost snapshot");
            }

            if (currency == null) {
                currency = item.getSupplierCurrency();
            } else if (currency != item.getSupplierCurrency()) {
                throw new InvalidRequestException("All selected items must use the same supplier currency");
            }
        }

        LocalDate docDate = request.getDocDate() != null
                ? request.getDocDate()
                : LocalDate.now(DateUtil.getTimeZone());
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.setPurchaseOrderNo(generatePurchaseOrderNo());
        entity.setSalesOrder(salesOrder);
        entity.setSupplier(supplier);
        entity.setSupplierShipping(supplierShipping);
        entity.setDocDate(docDate);
        entity.setProductionLeadTimeDay(request.getProductionLeadTimeDay());
        entity.setShippingLeadTimeDay(request.getShippingLeadTimeDay());
        entity.setStatus(PurchaseOrderStatus.CREATED);
        entity.setCurrency(currency);
        entity.setRemark(StringUtils.trimToNull(request.getRemark()));
        entity.setRevNo(1);
        entity.setSupplierNameSnapshot(supplier.getSupplierName());
        entity.setSupplierAddressSnapshot(resolveSupplierAddressSnapshot(supplier));
        entity.setSupplierContactSnapshot(resolveSupplierContactName(supplier));
        entity.setSupplierPhoneSnapshot(resolveSupplierContactNumber(supplier));
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
        entity.setCreatedDate(now);
        entity.setUpdatedDate(now);

        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal subTotalThb = BigDecimal.ZERO;
        int lineNo = 1;
        for (SalesOrderDetailEntity sourceItem : sourceItems) {
            PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
            detail.setSalesOrderDetail(sourceItem);
            detail.setLineNo(lineNo++);
            detail.setName(sourceItem.getName());
            detail.setType(sourceItem.getType());
            detail.setCapacity(sourceItem.getCapacity());
            detail.setSize(sourceItem.getSize());
            detail.setSpec(sourceItem.getSpec());
            detail.setQuantity(defaultIfNull(sourceItem.getQuantity()));
            detail.setSupplierCurrency(sourceItem.getSupplierCurrency());
            detail.setSupplierUnitPrice(defaultIfNull(sourceItem.getSupplierUnitPrice()));
            detail.setSupplierShippingCost(defaultIfNull(sourceItem.getSupplierShippingCost()));
            detail.setSupplierTotalUnitCost(defaultIfNull(sourceItem.getSupplierTotalUnitCost()));

            BigDecimal amountSupplierCurrency = detail.getSupplierTotalUnitCost()
                    .multiply(detail.getQuantity())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal amountThb = amountSupplierCurrency;

            detail.setAmountSupplierCurrency(amountSupplierCurrency);
            detail.setAmountThb(amountThb);
            detail.setImageUrl(sourceItem.getImageUrl());
            detail.setRfqDetailId(sourceItem.getRfqDetailId());
            detail.setRfqTierId(sourceItem.getRfqTierId());
            detail.setQuotationDetailId(sourceItem.getQuotationDetailId());
            detail.setShippingMethod(sourceItem.getShippingMethod());
            detail.setSupplierQuoteTierId(sourceItem.getSupplierQuoteTierId());
            entity.addItem(detail);

            subTotal = subTotal.add(amountSupplierCurrency);
            subTotalThb = subTotalThb.add(amountThb);
        }

        entity.setSubTotal(subTotal.setScale(2, RoundingMode.HALF_UP));
        entity.setSubTotalThb(subTotalThb.setScale(2, RoundingMode.HALF_UP));
        entity.setGrandTotal(entity.getSubTotal());
        entity.setGrandTotalThb(entity.getSubTotalThb());
        attachFiles(entity, attachments, user, now);

        salesOrder.setProcurementStatus(ProcurementStatus.PO_CREATED);
        salesOrder.setUpdatedBy(user);
        salesOrder.setUpdatedDate(now);

        purchaseOrderRepository.save(entity);

        recordCreatePurchaseOrderActivity(entity, userId);
        recordSalesOrderProcurementCreatedActivity(salesOrder, entity, userId);

        return entity;
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(String purchaseOrderNo) throws DataNotFoundException {
        PurchaseOrderEntity entity = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public DownloadDocumentDto getPurchaseOrderDocumentById(String purchaseOrderNo, DocumentRequest documentRequest) throws Exception {
        PurchaseOrderEntity purchaseOrderEntity = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));

        String fileName = purchaseOrderEntity.getPurchaseOrderNo();
        if (documentRequest.getFormat().equals(ExportFileFormat.PDF)) {
            List<byte[]> pdfBytesList = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                pdfBytesList.add((byte[]) reportService.getPurchaseOrderDocument(
                        buildPurchaseOrderDocumentDto(purchaseOrderEntity, Boolean.FALSE),
                        documentRequest.getFormat()
                ));
            }
            if (documentRequest.getIsCopy()) {
                pdfBytesList.add((byte[]) reportService.getPurchaseOrderDocument(
                        buildPurchaseOrderDocumentDto(purchaseOrderEntity, Boolean.TRUE),
                        documentRequest.getFormat()
                ));
            }

            byte[] mergedPdf = PdfMergeUtil.merge(pdfBytesList);
            return new DownloadDocumentDto(
                    fileName,
                    documentRequest.getFormat(),
                    List.of(new DownloadDocumentDto.FileItem(
                            fileName + "." + documentRequest.getFormat(),
                            Base64.getEncoder().encodeToString(mergedPdf),
                            "application/pdf"
                    ))
            );
        } else if (documentRequest.getFormat().equals(ExportFileFormat.JPG)) {
            List<byte[]> pages = new ArrayList<>();

            if (documentRequest.getIsOriginal()) {
                List<byte[]> originalPages = (List<byte[]>) reportService.getPurchaseOrderDocument(
                        buildPurchaseOrderDocumentDto(purchaseOrderEntity, Boolean.FALSE),
                        documentRequest.getFormat()
                );
                pages.addAll(originalPages);
            }
            if (documentRequest.getIsCopy()) {
                List<byte[]> copyPages = (List<byte[]>) reportService.getPurchaseOrderDocument(
                        buildPurchaseOrderDocumentDto(purchaseOrderEntity, Boolean.TRUE),
                        documentRequest.getFormat()
                );
                pages.addAll(copyPages);
            }
            List<DownloadDocumentDto.FileItem> files = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                String pageFileName = fileName + "_page_" + (i + 1) + "." + documentRequest.getFormat();
                files.add(new DownloadDocumentDto.FileItem(
                        pageFileName,
                        Base64.getEncoder().encodeToString(pages.get(i)),
                        "image/jpeg"
                ));
            }
            return new DownloadDocumentDto(fileName, documentRequest.getFormat(), files);
        }

        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDto updatePurchaseOrder(String purchaseOrderNo, UpdatePurchaseOrderRequest request, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderEntity entity = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        ensureEditableStatus(entity);

        Integer oldRevNo = entity.getRevNo();
        Map<String, Object> before = buildPurchaseOrderSnapshot(entity);

        if (request.getDocDate() != null) {
            entity.setDocDate(request.getDocDate());
        }
        entity.setProductionLeadTimeDay(request.getProductionLeadTimeDay());
        entity.setShippingLeadTimeDay(request.getShippingLeadTimeDay());
        if (request.getRemark() != null) {
            entity.setRemark(StringUtils.trimToNull(request.getRemark()));
        }
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            replacePurchaseOrderItems(entity, request.getItems());
        }

        recalculateTotals(entity);
        entity.setRevNo(defaultRevNo(oldRevNo) + 1);
        entity.setUpdatedBy(user);
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));

        purchaseOrderRepository.save(entity);
        recordUpdatePurchaseOrderActivity(entity, userId, before);

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDto cancelPurchaseOrder(String purchaseOrderNo, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderEntity entity = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        if (entity.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new InvalidRequestException("Purchase order is already cancelled");
        }
        if (entity.getStatus() == PurchaseOrderStatus.CLOSED) {
            throw new InvalidRequestException("Closed purchase order cannot be cancelled");
        }

        PurchaseOrderStatus beforeStatus = entity.getStatus();
        ProcurementStatus beforeProcurementStatus = entity.getSalesOrder() != null
                ? entity.getSalesOrder().getProcurementStatus()
                : null;
        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());

        entity.setStatus(PurchaseOrderStatus.CANCELLED);
        entity.setRevNo(defaultRevNo(entity.getRevNo()) + 1);
        entity.setUpdatedBy(user);
        entity.setUpdatedDate(now);

        if (entity.getSalesOrder() != null && shouldRestoreSalesOrderProcurementStatus(entity)) {
            entity.getSalesOrder().setProcurementStatus(ProcurementStatus.READY_FOR_PO);
            entity.getSalesOrder().setUpdatedBy(user);
            entity.getSalesOrder().setUpdatedDate(now);
        }

        purchaseOrderRepository.save(entity);

        recordPurchaseOrderStatusChangeActivity(
                entity,
                userId,
                beforeStatus,
                entity.getStatus(),
                "ยกเลิกใบสั่งซื้อเลขที่ " + entity.getPurchaseOrderNo()
        );
        if (entity.getSalesOrder() != null && beforeProcurementStatus != entity.getSalesOrder().getProcurementStatus()) {
            recordSalesOrderProcurementStatusChangeActivity(
                    entity.getSalesOrder(),
                    entity,
                    userId,
                    beforeProcurementStatus,
                    entity.getSalesOrder().getProcurementStatus(),
                    "ยกเลิก Purchase Order เลขที่ " + entity.getPurchaseOrderNo()
            );
        }

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDto closePurchaseOrder(String purchaseOrderNo, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderEntity entity = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));

        if (entity.getStatus() == PurchaseOrderStatus.CLOSED) {
            throw new InvalidRequestException("Purchase order is already closed");
        }
        if (entity.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new InvalidRequestException("Cancelled purchase order cannot be closed");
        }

        PurchaseOrderStatus beforeStatus = entity.getStatus();
        entity.setStatus(PurchaseOrderStatus.CLOSED);
        entity.setRevNo(defaultRevNo(entity.getRevNo()) + 1);
        entity.setUpdatedBy(user);
        entity.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));

        purchaseOrderRepository.save(entity);

        recordPurchaseOrderStatusChangeActivity(
                entity,
                userId,
                beforeStatus,
                entity.getStatus(),
                "ปิดใบสั่งซื้อเลขที่ " + entity.getPurchaseOrderNo()
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDto addAttachments(String purchaseOrderNo, List<MultipartFile> attachments, String userId)
            throws Exception {
        if (attachments == null || attachments.isEmpty()) {
            throw new InvalidRequestException("Attachments are required");
        }

        PurchaseOrderEntity entity = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        ensureEditableStatus(entity);

        ZonedDateTime now = ZonedDateTime.now(DateUtil.getTimeZone());
        attachFiles(entity, attachments, user, now);

        entity.setUpdatedBy(user);
        entity.setUpdatedDate(now);
        purchaseOrderRepository.save(entity);

        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                entity.getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.WEB,
                "เพิ่มไฟล์แนบของใบสั่งซื้อเลขที่ " + entity.getPurchaseOrderNo(),
                null
        );

        return mapToDto(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrderDto deleteAttachment(String purchaseOrderNo, Long attachmentId, String userId)
            throws DataNotFoundException, InvalidRequestException {
        PurchaseOrderEntity entity = purchaseOrderRepository.findById(purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order " + purchaseOrderNo + " not found."));
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User " + userId + " not found."));
        ensureEditableStatus(entity);

        PurchaseOrderAttachmentEntity attachment = purchaseOrderAttachmentRepository
                .findByIdAndPurchaseOrderPurchaseOrderNoAndActiveTrue(attachmentId, purchaseOrderNo)
                .orElseThrow(() -> new DataNotFoundException("Purchase order attachment " + attachmentId + " not found."));

        attachment.setActive(Boolean.FALSE);
        attachment.setUpdatedBy(user);
        attachment.setUpdatedDate(ZonedDateTime.now(DateUtil.getTimeZone()));
        purchaseOrderAttachmentRepository.save(attachment);

        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                entity.getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.WEB,
                "ลบไฟล์แนบของใบสั่งซื้อเลขที่ " + entity.getPurchaseOrderNo(),
                Map.of(
                        "attachmentId", attachment.getId(),
                        "fileName", attachment.getFileName(),
                        "originalFileName", attachment.getOriginalFileName()
                )
        );

        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public Pageable<PurchaseOrderDto> searchPurchaseOrders(SearchPurchaseOrderRequest request, PageableRequest pageableRequest) {
        SearchPurchaseOrderRequest criteria = Optional.ofNullable(request).orElseGet(SearchPurchaseOrderRequest::new);
        if (pageableRequest.getSortBy() == null || pageableRequest.getSortDirection() == null) {
            pageableRequest.setSortBy("docDate");
            pageableRequest.setSortDirection(Sort.Direction.DESC);
        }

        Page<PurchaseOrderDto> page = purchaseOrderRepository
                .findAll(buildSearchCriteria(criteria), pageableRequest.build())
                .map(this::mapToDto);

        Pageable<PurchaseOrderDto> response = new Pageable<>();
        response.setRecords(page.getContent());
        response.setPagination(Pagination.build(page));
        return response;
    }

    private Specification<PurchaseOrderEntity> buildSearchCriteria(SearchPurchaseOrderRequest request) {
        return Specification.<PurchaseOrderEntity>where(null)
                .and(purchaseOrderNoEqual(request.getPurchaseOrderNo()))
                .and(salesOrderNoEqual(request.getSalesOrderNo()))
                .and(supplierIdEqual(request.getSupplierId()))
                .and(statusEqual(request.getStatus()))
                .and(statusIn(request.getStatuses()))
                .and(docDateBetween(request.getDocDateStart(), request.getDocDateEnd()))
                .and(keywordContains(request.getKeyword()));
    }

    private void validateCreateRequest(CreatePurchaseOrderRequest request, List<MultipartFile> attachments) throws InvalidRequestException {
        if (request == null) {
            throw new InvalidRequestException("request is required");
        }
        if (StringUtils.isBlank(request.getSalesOrderNo())) {
            throw new InvalidRequestException("salesOrderNo is required");
        }
        if (StringUtils.isBlank(request.getSupplierId())) {
            throw new InvalidRequestException("supplierId is required");
        }
        if (request.getSupplierShippingId() == null) {
            throw new InvalidRequestException("supplierShippingId is required");
        }
        if (attachments == null || attachments.stream().noneMatch(item -> item != null && !item.isEmpty())) {
            throw new InvalidRequestException("At least one attachment is required");
        }
    }

    private void ensureEditableStatus(PurchaseOrderEntity entity) throws InvalidRequestException {
        if (entity.getStatus() != PurchaseOrderStatus.CREATED) {
            throw new InvalidRequestException("Only created purchase orders can be edited");
        }
    }

    private String generatePurchaseOrderNo() {
        return generatedIdSequenceService.getNextIdWithMonth(PURCHASE_ORDER_PREFIX, 6);
    }

    private void replacePurchaseOrderItems(PurchaseOrderEntity entity, List<UpdatePurchaseOrderDetailRequest> itemRequests) {
        Map<Long, PurchaseOrderDetailEntity> existingById = new HashMap<>();
        for (PurchaseOrderDetailEntity item : entity.getItems()) {
            if (item.getId() != null) {
                existingById.put(item.getId(), item);
            }
        }

        List<PurchaseOrderDetailEntity> existingItems = new ArrayList<>(entity.getItems());
        for (PurchaseOrderDetailEntity existingItem : existingItems) {
            entity.removeItem(existingItem);
        }

        int lineNo = 1;
        for (UpdatePurchaseOrderDetailRequest itemRequest : itemRequests) {
            PurchaseOrderDetailEntity previousItem = itemRequest.getId() != null
                    ? existingById.get(itemRequest.getId())
                    : null;

            PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();
            detail.setSalesOrderDetail(previousItem != null ? previousItem.getSalesOrderDetail() : null);
            detail.setLineNo(lineNo++);
            detail.setName(StringUtils.trimToNull(itemRequest.getName()));
            detail.setType(StringUtils.trimToNull(itemRequest.getType()));
            detail.setCapacity(StringUtils.trimToNull(itemRequest.getCapacity()));
            detail.setSize(StringUtils.trimToNull(itemRequest.getSize()));
            detail.setSpec(StringUtils.trimToNull(itemRequest.getSpec()));
            detail.setQuantity(defaultIfNull(itemRequest.getQuantity()));
            detail.setSupplierCurrency(itemRequest.getSupplierCurrency() != null ? itemRequest.getSupplierCurrency() : entity.getCurrency());
            detail.setSupplierUnitPrice(defaultIfNull(itemRequest.getSupplierUnitPrice()));
            detail.setSupplierShippingCost(defaultIfNull(itemRequest.getSupplierShippingCost()));
            detail.setSupplierTotalUnitCost(detail.getSupplierUnitPrice().add(detail.getSupplierShippingCost()));
            detail.setImageUrl(StringUtils.trimToNull(itemRequest.getImageUrl()));
            detail.setRfqDetailId(itemRequest.getRfqDetailId());
            detail.setRfqTierId(itemRequest.getRfqTierId());
            detail.setQuotationDetailId(itemRequest.getQuotationDetailId());
            detail.setShippingMethod(StringUtils.trimToNull(itemRequest.getShippingMethod()));
            detail.setSupplierQuoteTierId(itemRequest.getSupplierQuoteTierId());
            entity.addItem(detail);
        }
    }

    private void attachFiles(
            PurchaseOrderEntity entity,
            List<MultipartFile> attachments,
            UserEntity user,
            ZonedDateTime now
    ) throws Exception {
        int nextSortOrder = entity.getAttachments().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .map(PurchaseOrderAttachmentEntity::getSortOrder)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }

            UploadFileResponse upload = fileStorageService.uploadFile(attachment);
            PurchaseOrderAttachmentEntity attachmentEntity = new PurchaseOrderAttachmentEntity();
            attachmentEntity.setFileName(upload.getFileName());
            attachmentEntity.setOriginalFileName(StringUtils.trimToNull(attachment.getOriginalFilename()));
            attachmentEntity.setFileUrl(upload.getUrl());
            attachmentEntity.setContentType(StringUtils.trimToNull(upload.getContentType()));
            attachmentEntity.setFileSize(attachment.getSize());
            attachmentEntity.setSortOrder(nextSortOrder++);
            attachmentEntity.setActive(Boolean.TRUE);
            attachmentEntity.setCreatedBy(user);
            attachmentEntity.setUpdatedBy(user);
            attachmentEntity.setCreatedDate(now);
            attachmentEntity.setUpdatedDate(now);
            entity.addAttachment(attachmentEntity);
        }
    }

    private void recalculateTotals(PurchaseOrderEntity entity) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal subTotalThb = BigDecimal.ZERO;

        for (PurchaseOrderDetailEntity detail : entity.getItems()) {
            BigDecimal quantity = defaultIfNull(detail.getQuantity());
            BigDecimal supplierUnitPrice = defaultIfNull(detail.getSupplierUnitPrice());
            BigDecimal supplierShippingCost = defaultIfNull(detail.getSupplierShippingCost());
            BigDecimal supplierTotalUnitCost = supplierUnitPrice.add(supplierShippingCost);

            detail.setQuantity(quantity);
            detail.setSupplierUnitPrice(supplierUnitPrice);
            detail.setSupplierShippingCost(supplierShippingCost);
            detail.setSupplierTotalUnitCost(supplierTotalUnitCost);

            BigDecimal amountSupplierCurrency = supplierTotalUnitCost.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amountThb = amountSupplierCurrency;
            detail.setAmountSupplierCurrency(amountSupplierCurrency);
            detail.setAmountThb(amountThb);

            subTotal = subTotal.add(amountSupplierCurrency);
            subTotalThb = subTotalThb.add(amountThb);
        }

        entity.setSubTotal(subTotal.setScale(2, RoundingMode.HALF_UP));
        entity.setSubTotalThb(subTotalThb.setScale(2, RoundingMode.HALF_UP));
        entity.setGrandTotal(entity.getSubTotal());
        entity.setGrandTotalThb(entity.getSubTotalThb());
    }

    private boolean shouldRestoreSalesOrderProcurementStatus(PurchaseOrderEntity entity) {
        if (entity.getSalesOrder() == null) {
            return false;
        }
        return purchaseOrderRepository.findBySalesOrderSalesOrderNoOrderByCreatedDateDesc(entity.getSalesOrder().getSalesOrderNo()).stream()
                .filter(item -> !StringUtils.equals(item.getPurchaseOrderNo(), entity.getPurchaseOrderNo()))
                .noneMatch(item -> item.getStatus() != PurchaseOrderStatus.CANCELLED);
    }

    private PurchaseOrderDocumentDto buildPurchaseOrderDocumentDto(PurchaseOrderEntity purchaseOrderEntity, Boolean isCopy) {
        PurchaseOrderDocumentDto dto = new PurchaseOrderDocumentDto();
        dto.setDocNo(purchaseOrderEntity.getPurchaseOrderNo());
        dto.setDocDate(purchaseOrderEntity.getDocDate() != null ? purchaseOrderEntity.getDocDate().format(DateUtil.DD_MM_YY) : null);
        dto.setIsCopy(isCopy);
        dto.setSupplierName(purchaseOrderEntity.getSupplierNameSnapshot());
        dto.setSupplierAddress(purchaseOrderEntity.getSupplierAddressSnapshot());
        dto.setRemark(purchaseOrderEntity.getRemark());
        dto.setTotalAmount(defaultIfNull(purchaseOrderEntity.getGrandTotal()));
        dto.setDiscount(BigDecimal.ZERO);
        dto.setFreight(BigDecimal.ZERO);
        dto.setSubTotal(defaultIfNull(purchaseOrderEntity.getSubTotal()));
        dto.setVat(BigDecimal.ZERO);
        dto.setGrandTotal(defaultIfNull(purchaseOrderEntity.getGrandTotal()));
        dto.setSalesId(
                purchaseOrderEntity.getSalesOrder() != null && purchaseOrderEntity.getSalesOrder().getSales() != null
                        ? purchaseOrderEntity.getSalesOrder().getSales().getEmployeeId()
                        : null
        );
        dto.setShippingType(
                purchaseOrderEntity.getSupplierShipping() != null && purchaseOrderEntity.getSupplierShipping().getShippingMethod() != null
                        ? purchaseOrderEntity.getSupplierShipping().getShippingMethod().name()
                        : null
        );
        dto.setShippingLocation(resolveShippingLocation(purchaseOrderEntity));
        dto.setShippingAddress(resolveShippingAddress(purchaseOrderEntity));
        dto.setShippingRemark(purchaseOrderEntity.getSupplierShipping().getRemark());
        dto.setCarCode(purchaseOrderEntity.getSupplierShipping() != null ? purchaseOrderEntity.getSupplierShipping().getCarCode() : null);
        dto.setProcurementName(resolveProcurementName(purchaseOrderEntity));
        dto.setProcurementMobileNo(resolveProcurementMobileNo(purchaseOrderEntity));
        dto.setLeadTime(purchaseOrderEntity.getProductionLeadTimeDay() != null ? String.valueOf(purchaseOrderEntity.getProductionLeadTimeDay()) : "");
        dto.setDueDate(resolveDueDate(purchaseOrderEntity));
        dto.setSalesOrderNo(purchaseOrderEntity.getSalesOrder() != null ? purchaseOrderEntity.getSalesOrder().getSalesOrderNo() : null);
        dto.setItems(getPurchaseOrderItemDocumentDtos(purchaseOrderEntity));
        return dto;
    }

    private String resolveSupplierAddressSnapshot(SupplierEntity supplier) {
        return StringUtils.firstNonBlank(
                StringUtils.trimToNull(supplier.getFullAddress()),
                buildSupplierAddressFallback(supplier)
        );
    }

    private String buildSupplierAddressFallback(SupplierEntity supplier) {
        return String.join(" ",
                Arrays.stream(new String[]{
                        supplier.getDetailAddress(),
                        supplier.getStreet(),
                        supplier.getDistrict(),
                        supplier.getCity(),
                        supplier.getProvince(),
                        supplier.getPostalCode(),
                        supplier.getCountryCode()
                }).filter(StringUtils::isNotBlank).toArray(String[]::new)
        ).trim();
    }

    private String resolveSupplierContactName(SupplierEntity supplier) {
        return resolveDefaultSupplierContact(supplier).map(SupplierContactEntity::getContactName).orElse(null);
    }

    private String resolveSupplierContactNumber(SupplierEntity supplier) {
        return resolveDefaultSupplierContact(supplier).map(SupplierContactEntity::getContactNumber).orElse(null);
    }

    private Optional<SupplierContactEntity> resolveDefaultSupplierContact(SupplierEntity supplier) {
        return supplier.getContacts().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsDefault()))
                .findFirst()
                .or(() -> supplier.getContacts().stream().findFirst());
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<PurchaseOrderItemDocumentDto> getPurchaseOrderItemDocumentDtos(PurchaseOrderEntity purchaseOrderEntity) {
        List<PurchaseOrderItemDocumentDto> itemDocuments = new ArrayList<>();
        for (PurchaseOrderDetailEntity detail : purchaseOrderEntity.getItems()) {
            PurchaseOrderItemDocumentDto item = new PurchaseOrderItemDocumentDto();
            if (StringUtils.isNotBlank(detail.getImageUrl())) {
                item.setImage(loadImageAsInputStream(detail.getImageUrl()));
            }
            item.setNo(detail.getLineNo());
            item.setName(detail.getName());
            item.setType(detail.getType());
            item.setCapacity(detail.getCapacity());
            item.setSize(detail.getSize());
            item.setSpec(detail.getSpec());
            item.setPrice(detail.getSupplierTotalUnitCost());
            item.setQuantity(detail.getQuantity());
            item.setAmount(detail.getAmountSupplierCurrency());
            itemDocuments.add(item);
        }
//        while (itemDocuments.size() < 2) {
//            itemDocuments.add(new PurchaseOrderItemDocumentDto());
//        }
        return itemDocuments;
    }

    private InputStream loadImageAsInputStream(String imageUrl) {
        try {
            String fileName = RfqAttachmentUtil.extractFileName(imageUrl);
            if (StringUtils.isNotBlank(fileName)) {
                InputStream localFile = fileStorageService.openUploadedFile(fileName);
                if (localFile != null) {
                    return localFile;
                }
            }
            return new URL(imageUrl).openStream();
        } catch (Exception e) {
            log.warn("Cannot load image from url: {}", imageUrl, e);
            return null;
        }
    }

    private String resolveSalesName(PurchaseOrderEntity purchaseOrderEntity) {
        if (purchaseOrderEntity.getSalesOrder() == null || purchaseOrderEntity.getSalesOrder().getSales() == null) {
            return null;
        }
        EmployeeEntity sales = purchaseOrderEntity.getSalesOrder().getSales();
        return StringUtils.trimToNull((StringUtils.defaultString(sales.getFirstNameTh()) + " " + StringUtils.defaultString(sales.getLastNameTh())).trim());
    }

    private String resolveSalesMobileNo(PurchaseOrderEntity purchaseOrderEntity) {
        if (purchaseOrderEntity.getSalesOrder() == null || purchaseOrderEntity.getSalesOrder().getSales() == null) {
            return null;
        }
        return purchaseOrderEntity.getSalesOrder().getSales().getPhoneNumber();
    }

    private String resolveProcurementName(PurchaseOrderEntity purchaseOrderEntity) {
        if (purchaseOrderEntity.getCreatedBy() == null) {
            return null;
        }
        if (purchaseOrderEntity.getCreatedBy().getEmployeeEntity() != null) {
            EmployeeEntity employee = purchaseOrderEntity.getCreatedBy().getEmployeeEntity();
            String fullName = (StringUtils.defaultString(employee.getFirstNameTh()) + " " + StringUtils.defaultString(employee.getLastNameTh())).trim();
            if (StringUtils.isNotBlank(fullName)) {
                return fullName;
            }
        }
        return StringUtils.defaultIfBlank(purchaseOrderEntity.getCreatedBy().getDisplayName(), purchaseOrderEntity.getCreatedBy().getUsername());
    }

    private String resolveProcurementMobileNo(PurchaseOrderEntity purchaseOrderEntity) {
        if (purchaseOrderEntity.getCreatedBy() == null || purchaseOrderEntity.getCreatedBy().getEmployeeEntity() == null) {
            return null;
        }
        return purchaseOrderEntity.getCreatedBy().getEmployeeEntity().getPhoneNumber();
    }

    private String resolveShippingLocation(PurchaseOrderEntity purchaseOrderEntity) {
        if (purchaseOrderEntity.getSupplierShipping() == null || purchaseOrderEntity.getSupplierShipping().getDestinations() == null) {
            return null;
        }
        return purchaseOrderEntity.getSupplierShipping().getDestinations().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .sorted(Comparator.comparing(item -> Optional.ofNullable(item.getSortOrder()).orElse(0)))
                .map(item -> StringUtils.defaultIfBlank(item.getDestinationName(), item.getFullAddress()))
                .filter(StringUtils::isNotBlank)
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }

    private String resolveShippingAddress(PurchaseOrderEntity purchaseOrderEntity) {
        if (purchaseOrderEntity.getSupplierShipping() == null || purchaseOrderEntity.getSupplierShipping().getDestinations() == null) {
            return null;
        }
        return purchaseOrderEntity.getSupplierShipping().getDestinations().stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .sorted(Comparator.comparing(item -> Optional.ofNullable(item.getSortOrder()).orElse(0)))
                .map(SupplierShippingDestinationEntity::getFullAddress)
                .filter(StringUtils::isNotBlank)
                .reduce((left, right) -> left + "\n" + right)
                .orElse(null);
    }

    private String resolveDueDate(PurchaseOrderEntity purchaseOrderEntity) {
        if (purchaseOrderEntity.getDocDate() == null) {
            return null;
        }
        int productionDays = Optional.ofNullable(purchaseOrderEntity.getProductionLeadTimeDay()).orElse(0);
        int shippingDays = Optional.ofNullable(purchaseOrderEntity.getShippingLeadTimeDay()).orElse(0);
        return purchaseOrderEntity.getDocDate().plusDays((long) productionDays + shippingDays).format(DateUtil.DD_MM_YY);
    }

    private PurchaseOrderDto mapToDto(PurchaseOrderEntity entity) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setPurchaseOrderNo(entity.getPurchaseOrderNo());
        dto.setSalesOrderNo(entity.getSalesOrder() != null ? entity.getSalesOrder().getSalesOrderNo() : null);
        dto.setDocDate(entity.getDocDate() != null ? entity.getDocDate().format(DateUtil.DD_MM_YY) : null);
        dto.setProductionLeadTimeDay(entity.getProductionLeadTimeDay());
        dto.setShippingLeadTimeDay(entity.getShippingLeadTimeDay());
        dto.setStatus(entity.getStatus());
        dto.setStatusProfile(DocumentStatusResolver.resolvePurchaseOrder(entity.getStatus()));
        dto.setCurrency(entity.getCurrency());
        dto.setSupplier(supplierMapper.toDto(entity.getSupplier()));
        dto.setSupplierShipping(buildSupplierShippingDto(entity.getSupplierShipping()));
        dto.setSubTotal(entity.getSubTotal());
        dto.setSubTotalThb(entity.getSubTotalThb());
        dto.setGrandTotal(entity.getGrandTotal());
        dto.setGrandTotalThb(entity.getGrandTotalThb());
        dto.setRemark(entity.getRemark());
        dto.setRevNo(entity.getRevNo());
        dto.setSupplierNameSnapshot(entity.getSupplierNameSnapshot());
        dto.setSupplierAddressSnapshot(entity.getSupplierAddressSnapshot());
        dto.setSupplierContactSnapshot(entity.getSupplierContactSnapshot());
        dto.setSupplierPhoneSnapshot(entity.getSupplierPhoneSnapshot());
        dto.setCreatedBy(userMapper.toDto(entity.getCreatedBy()));
        dto.setUpdatedBy(userMapper.toDto(entity.getUpdatedBy()));

        List<PurchaseOrderAttachmentDto> attachments = new ArrayList<>();
        for (PurchaseOrderAttachmentEntity attachment : entity.getAttachments()) {
            if (!Boolean.TRUE.equals(attachment.getActive())) {
                continue;
            }
            PurchaseOrderAttachmentDto attachmentDto = new PurchaseOrderAttachmentDto();
            attachmentDto.setId(attachment.getId());
            attachmentDto.setPurchaseOrderNo(entity.getPurchaseOrderNo());
            attachmentDto.setFileName(attachment.getFileName());
            attachmentDto.setOriginalFileName(attachment.getOriginalFileName());
            attachmentDto.setFileUrl(attachment.getFileUrl());
            attachmentDto.setContentType(attachment.getContentType());
            attachmentDto.setFileSize(attachment.getFileSize());
            attachmentDto.setRemark(attachment.getRemark());
            attachmentDto.setSortOrder(attachment.getSortOrder());
            attachments.add(attachmentDto);
        }
        dto.setAttachments(attachments);

        List<PurchaseOrderDetailDto> items = new ArrayList<>();
        for (PurchaseOrderDetailEntity detail : entity.getItems()) {
            PurchaseOrderDetailDto item = new PurchaseOrderDetailDto();
            item.setId(detail.getId());
            item.setSalesOrderDetailId(detail.getSalesOrderDetail() != null ? detail.getSalesOrderDetail().getId() : null);
            item.setLineNo(detail.getLineNo());
            item.setName(detail.getName());
            item.setType(detail.getType());
            item.setCapacity(detail.getCapacity());
            item.setSize(detail.getSize());
            item.setSpec(detail.getSpec());
            item.setQuantity(detail.getQuantity());
            item.setSupplierCurrency(detail.getSupplierCurrency());
            item.setSupplierUnitPrice(detail.getSupplierUnitPrice());
            item.setSupplierShippingCost(detail.getSupplierShippingCost());
            item.setSupplierTotalUnitCost(detail.getSupplierTotalUnitCost());
            item.setAmountSupplierCurrency(detail.getAmountSupplierCurrency());
            item.setAmountThb(detail.getAmountThb());
            item.setImageUrl(detail.getImageUrl());
            item.setRfqDetailId(detail.getRfqDetailId());
            item.setRfqTierId(detail.getRfqTierId());
            item.setQuotationDetailId(detail.getQuotationDetailId());
            item.setShippingMethod(detail.getShippingMethod());
            item.setSupplierQuoteTierId(detail.getSupplierQuoteTierId());
            items.add(item);
        }
        dto.setItems(items);
        return dto;
    }

    private Integer defaultRevNo(Integer revNo) {
        return revNo == null ? 0 : revNo;
    }

    private SupplierShippingDto buildSupplierShippingDto(SupplierShippingEntity entity) {
        if (entity == null) {
            return null;
        }

        SupplierShippingDto dto = supplierMapper.toDto(entity);
        List<SupplierShippingDestinationDto> destinations = new ArrayList<>();
        if (entity.getDestinations() != null) {
            entity.getDestinations().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .sorted(Comparator
                            .comparing(SupplierShippingDestinationEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(SupplierShippingDestinationEntity::getId, Comparator.nullsLast(Long::compareTo)))
                    .forEach(item -> {
                        SupplierShippingDestinationDto destinationDto = supplierMapper.toDto(item);
                        destinationDto.setSupplierShippingId(entity.getId());
                        destinations.add(destinationDto);
                    });
        }
        dto.setDestinations(destinations);
        return dto;
    }

    private Map<String, Object> buildPurchaseOrderSnapshot(PurchaseOrderEntity entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("purchaseOrderNo", entity.getPurchaseOrderNo());
        snapshot.put("salesOrderNo", entity.getSalesOrder() != null ? entity.getSalesOrder().getSalesOrderNo() : null);
        snapshot.put("supplierId", entity.getSupplier() != null ? entity.getSupplier().getId() : null);
        snapshot.put("supplierShippingId", entity.getSupplierShipping() != null ? entity.getSupplierShipping().getId() : null);
        snapshot.put("shippingMethod", entity.getSupplierShipping() != null && entity.getSupplierShipping().getShippingMethod() != null
                ? entity.getSupplierShipping().getShippingMethod().name()
                : null);
        snapshot.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        snapshot.put("docDate", entity.getDocDate() != null ? entity.getDocDate().toString() : null);
        snapshot.put("productionLeadTimeDay", entity.getProductionLeadTimeDay());
        snapshot.put("shippingLeadTimeDay", entity.getShippingLeadTimeDay());
        snapshot.put("currency", entity.getCurrency() != null ? entity.getCurrency().name() : null);
        snapshot.put("subTotal", entity.getSubTotal());
        snapshot.put("subTotalThb", entity.getSubTotalThb());
        snapshot.put("grandTotal", entity.getGrandTotal());
        snapshot.put("grandTotalThb", entity.getGrandTotalThb());
        snapshot.put("remark", entity.getRemark());
        snapshot.put("itemCount", entity.getItems() != null ? entity.getItems().size() : 0);
        return snapshot;
    }

    private void recordCreatePurchaseOrderActivity(PurchaseOrderEntity entity, String userId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("purchaseOrderNo", entity.getPurchaseOrderNo());
        detail.put("salesOrderNo", entity.getSalesOrder() != null ? entity.getSalesOrder().getSalesOrderNo() : null);
        detail.put("supplierId", entity.getSupplier() != null ? entity.getSupplier().getId() : null);
        detail.put("supplierShippingId", entity.getSupplierShipping() != null ? entity.getSupplierShipping().getId() : null);
        detail.put("shippingMethod", entity.getSupplierShipping() != null ? entity.getSupplierShipping().getShippingMethod() : null);
        detail.put("status", entity.getStatus());
        detail.put("currency", entity.getCurrency());
        detail.put("subTotal", entity.getSubTotal());
        detail.put("subTotalThb", entity.getSubTotalThb());
        detail.put("itemCount", entity.getItems() != null ? entity.getItems().size() : 0);

        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                entity.getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.CREATE,
                ActivitySource.API,
                "สร้างใบสั่งซื้อเลขที่ " + entity.getPurchaseOrderNo(),
                detail
        );
    }

    private void recordSalesOrderProcurementCreatedActivity(SalesOrderEntity salesOrder, PurchaseOrderEntity purchaseOrder, String userId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("salesOrderNo", salesOrder.getSalesOrderNo());
        detail.put("procurementStatus", salesOrder.getProcurementStatus());
        detail.put("purchaseOrderNo", purchaseOrder.getPurchaseOrderNo());
        detail.put("supplierId", purchaseOrder.getSupplier() != null ? purchaseOrder.getSupplier().getId() : null);
        detail.put("supplierShippingId", purchaseOrder.getSupplierShipping() != null ? purchaseOrder.getSupplierShipping().getId() : null);

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                salesOrder.getSalesOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.STATUS_CHANGE,
                ActivitySource.API,
                "สร้าง Purchase Order จากใบยืนยันสั่งซื้อเลขที่ " + salesOrder.getSalesOrderNo(),
                detail
        );
    }

    private void recordUpdatePurchaseOrderActivity(PurchaseOrderEntity entity, String userId, Map<String, Object> before) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", buildPurchaseOrderSnapshot(entity));
        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                entity.getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.UPDATE,
                ActivitySource.API,
                "แก้ไขใบสั่งซื้อเลขที่ " + entity.getPurchaseOrderNo(),
                detail
        );
    }

    private void recordPurchaseOrderStatusChangeActivity(
            PurchaseOrderEntity entity,
            String userId,
            PurchaseOrderStatus beforeStatus,
            PurchaseOrderStatus afterStatus,
            String summary
    ) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", beforeStatus != null ? beforeStatus.name() : null);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", afterStatus != null ? afterStatus.name() : null);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", after);

        activityHistoryService.record(
                ActivityEntityType.PURCHASE_ORDER,
                entity.getPurchaseOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.STATUS_CHANGE,
                ActivitySource.API,
                summary,
                detail
        );
    }

    private void recordSalesOrderProcurementStatusChangeActivity(
            SalesOrderEntity salesOrder,
            PurchaseOrderEntity purchaseOrder,
            String userId,
            ProcurementStatus beforeStatus,
            ProcurementStatus afterStatus,
            String summary
    ) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("procurementStatus", beforeStatus != null ? beforeStatus.name() : null);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("procurementStatus", afterStatus != null ? afterStatus.name() : null);
        after.put("purchaseOrderNo", purchaseOrder.getPurchaseOrderNo());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", after);

        activityHistoryService.record(
                ActivityEntityType.SALES_ORDER,
                salesOrder.getSalesOrderNo(),
                userId,
                ActivityActorType.USER,
                ActivityAction.STATUS_CHANGE,
                ActivitySource.API,
                summary,
                detail
        );
    }
}
