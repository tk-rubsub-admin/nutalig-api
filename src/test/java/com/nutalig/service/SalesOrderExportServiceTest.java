package com.nutalig.service;

import com.nutalig.constant.Currency;
import com.nutalig.constant.ProcurementStatus;
import com.nutalig.constant.SalesOrderPaymentStatus;
import com.nutalig.constant.SalesOrderStatus;
import com.nutalig.controller.salesorder.request.SearchSalesOrderRequest;
import com.nutalig.entity.CustomerEntity;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.entity.SalesOrderEntity;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.repository.SalesOrderRepository;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderExportServiceTest {
    @Mock SalesOrderRepository repository;
    @InjectMocks SalesOrderService service;

    private final Sort exportSort = Sort.by(Sort.Direction.DESC, "docDate", "salesOrderNo");

    @Test
    void exportsReadableWorkbookWithDocumentSnapshotNumericAmountsAndLiteralText() throws Exception {
        SalesOrderEntity order = new SalesOrderEntity();
        order.setSalesOrderNo("NTL-SO2026100006");
        order.setDocDate(LocalDate.of(2026, 10, 9));
        order.setExpireDate(LocalDate.of(2026, 11, 9));
        order.setStatus(SalesOrderStatus.CREATED);
        order.setQuotationNo("NTL-CUST-00070-003");
        order.setCustomerNameSnapshot("ชื่อลูกค้า ณ วันออกเอกสาร");
        CustomerEntity customer = new CustomerEntity();
        customer.setId("NTL-CUST-00070");
        customer.setCustomerName("ชื่อใหม่");
        SystemConfigEntity tier = new SystemConfigEntity();
        tier.setNameEn("VIP");
        customer.setCustomerTier(tier);
        order.setCustomer(customer);
        EmployeeEntity sales = new EmployeeEntity();
        sales.setEmployeeId("EMP-001");
        sales.setFirstNameTh("สมชาย");
        sales.setLastNameTh("ขายดี");
        order.setSales(sales);
        order.setShippingType("LAND");
        order.setProcurementStatus(ProcurementStatus.READY_FOR_PO);
        order.setPaymentStatus(SalesOrderPaymentStatus.PARTIALLY_PAID);
        order.setCurrency(Currency.THB);
        order.setSubTotal(new BigDecimal("1000.12345"));
        order.setGrandTotal(new BigDecimal("1070.13"));
        order.setPaidTotal(new BigDecimal("321.039"));
        order.setPaymentScheduleDate(LocalDate.of(2026, 10, 15));
        order.setRemark("=SUM(A1:A2)");
        when(repository.findAll(any(Specification.class), eq(exportSort))).thenReturn(List.of(order));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(service.exportSalesOrders(new SearchSalesOrderRequest())))) {
            var sheet = workbook.getSheet("Sales Orders");
            assertEquals(1, sheet.getLastRowNum());
            assertEquals(24, sheet.getRow(0).getLastCellNum());
            var row = sheet.getRow(1);
            assertEquals("NTL-SO2026100006", row.getCell(0).getStringCellValue());
            assertEquals("09/10/2026", row.getCell(1).getStringCellValue());
            assertEquals("CREATED", row.getCell(3).getStringCellValue());
            assertEquals("NTL-CUST-00070-003", row.getCell(4).getStringCellValue());
            assertEquals("ชื่อลูกค้า ณ วันออกเอกสาร", row.getCell(6).getStringCellValue());
            assertEquals("VIP", row.getCell(7).getStringCellValue());
            assertEquals("สมชาย ขายดี", row.getCell(9).getStringCellValue());
            assertEquals("READY_FOR_PO", row.getCell(11).getStringCellValue());
            assertEquals("PARTIALLY_PAID", row.getCell(12).getStringCellValue());
            assertEquals(CellType.NUMERIC, row.getCell(14).getCellType());
            assertEquals(1000.12345, row.getCell(14).getNumericCellValue(), 0.000001);
            assertEquals(1070.13, row.getCell(18).getNumericCellValue(), 0.000001);
            assertEquals(321.039, row.getCell(20).getNumericCellValue(), 0.000001);
            assertEquals("15/10/2026", row.getCell(22).getStringCellValue());
            assertEquals(CellType.STRING, row.getCell(23).getCellType());
            assertEquals("=SUM(A1:A2)", row.getCell(23).getStringCellValue());
        }
        verify(repository).findAll(any(Specification.class), eq(exportSort));
    }

    @Test
    void emptyResultAndMissingCriteriaStillReturnValidHeaderOnlyWorkbook() throws Exception {
        when(repository.findAll(any(Specification.class), eq(exportSort))).thenReturn(List.of());
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(service.exportSalesOrders(null)))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(0, sheet.getLastRowNum());
            assertEquals("เลขที่ใบยืนยันสั่งซื้อ", sheet.getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void exportsAllMatchingRowsAndHandlesLegacyMissingFields() throws Exception {
        List<SalesOrderEntity> orders = IntStream.range(0, 51).mapToObj(index -> {
            SalesOrderEntity order = new SalesOrderEntity();
            order.setSalesOrderNo("SO-" + index);
            return order;
        }).toList();
        CustomerEntity customer = new CustomerEntity();
        customer.setCompanyName("บริษัทตัวอย่าง");
        orders.get(0).setCustomer(customer);
        EmployeeEntity sales = new EmployeeEntity();
        sales.setNickName("Nut");
        orders.get(0).setSales(sales);
        when(repository.findAll(any(Specification.class), eq(exportSort))).thenReturn(orders);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(service.exportSalesOrders(null)))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(51, sheet.getLastRowNum());
            assertEquals("SO-50", sheet.getRow(51).getCell(0).getStringCellValue());
            assertEquals("บริษัทตัวอย่าง", sheet.getRow(1).getCell(6).getStringCellValue());
            assertEquals("Nut", sheet.getRow(1).getCell(9).getStringCellValue());
            assertEquals("", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("", sheet.getRow(2).getCell(6).getStringCellValue());
            assertEquals(0, sheet.getRow(2).getCell(18).getNumericCellValue());
        }
        verify(repository).findAll(any(Specification.class), eq(exportSort));
        verifyNoMoreInteractions(repository);
    }
}
