package com.nutalig.controller.salesorder;

import com.nutalig.constant.ProcurementStatus;
import com.nutalig.constant.SalesOrderStatus;
import com.nutalig.constant.UrgentRequestStatus;
import com.nutalig.controller.salesorder.request.SearchSalesOrderRequest;
import com.nutalig.service.PurchaseOrderService;
import com.nutalig.service.SalesOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderExportControllerTest {
    @Mock SalesOrderService service;
    @Mock PurchaseOrderService purchaseOrderService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SalesOrderController(service, purchaseOrderService)).build();
    }

    @Test
    void bindsAllSearchCriteriaAndReturnsDownloadHeadersWithRawFileBytes() throws Exception {
        byte[] file = {80, 75, 3, 4};
        when(service.exportSalesOrders(any())).thenReturn(file);
        mvc.perform(post("/v1/sales-orders/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"salesOrderNo":"NTL-SO2026100006", "docDateStart":"2026-10-01",
                                 "docDateEnd":"2026-10-31", "customerId":"NTL-CUST-00070", "salesId":"EMP-001",
                                 "status":"CREATED", "statuses":["CREATED","ISSUED"],
                                 "urgentRequestStatus":"APPROVED", "procurementStatus":["READY_FOR_PO"],
                                 "shippingType":"LAND", "keyword":"ขวด"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales-order-export.xlsx\""))
                .andExpect(content().bytes(file));
        var captured = ArgumentCaptor.forClass(SearchSalesOrderRequest.class);
        verify(service).exportSalesOrders(captured.capture());
        var criteria = captured.getValue();
        assertEquals("NTL-SO2026100006", criteria.getSalesOrderNo());
        assertEquals(LocalDate.of(2026, 10, 1), criteria.getDocDateStart());
        assertEquals(LocalDate.of(2026, 10, 31), criteria.getDocDateEnd());
        assertEquals("NTL-CUST-00070", criteria.getCustomerId());
        assertEquals("EMP-001", criteria.getSalesId());
        assertEquals(SalesOrderStatus.CREATED, criteria.getStatus());
        assertEquals(List.of(SalesOrderStatus.CREATED, SalesOrderStatus.ISSUED), criteria.getStatuses());
        assertEquals(UrgentRequestStatus.APPROVED, criteria.getUrgentRequestStatus());
        assertEquals(List.of(ProcurementStatus.READY_FOR_PO), criteria.getProcurementStatus());
        assertEquals("LAND", criteria.getShippingType());
        assertEquals("ขวด", criteria.getKeyword());
    }

    @Test
    void acceptsOmittedBodyWithoutRequiringPaginationParameters() throws Exception {
        when(service.exportSalesOrders(null)).thenReturn(new byte[]{1});
        mvc.perform(post("/v1/sales-orders/export"))
                .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1}));
        verify(service).exportSalesOrders(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"INVALID\"}", "{\"docDateStart\":\"not-a-date\"}"})
    void invalidCriteriaReturnBadRequestBeforeExporting(String body) throws Exception {
        mvc.perform(post("/v1/sales-orders/export").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
