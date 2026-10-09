package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.constant.RfqStatus;
import com.nutalig.dto.DashboardDataDto;
import com.nutalig.dto.UserDto;
import com.nutalig.dto.UserRoleDto;
import com.nutalig.entity.EmployeeEntity;
import com.nutalig.entity.RfqHeaderEntity;
import com.nutalig.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardSalesManagerTest {
    @Mock ApprovalRequestRepository approvalRequestRepository;
    @Mock RequestPriceHeaderRepository requestPriceHeaderRepository;
    @Mock QuotationRepository quotationRepository;
    @Mock RfqStatusTimelineRepository rfqStatusTimelineRepository;
    @Mock BusinessDurationService businessDurationService;
    @Spy AppProperties appProperties = new AppProperties();
    @InjectMocks DashboardService service;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerSeesSalesDashboardAndCreateActionWithDataFromMultipleSales() {
        authenticate("SALES_MANAGER");
        when(requestPriceHeaderRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(rfq("RFQ-A", "SALES-A"), rfq("RFQ-B", "SALES-B")));

        DashboardDataDto dashboard = service.getDashboard("2026-10-01", "2026-10-08", null, null);

        assertEquals("2", dashboard.getMetrics().getFirst().getValue());
        dashboard.getMetrics().forEach(metric -> assertTrue(metric.getVisibleTo().contains("SALES_MANAGER")));
        dashboard.getTrendCharts().forEach(chart -> assertTrue(chart.getVisibleTo().contains("SALES_MANAGER")));
        dashboard.getDistributionCharts().forEach(chart -> assertTrue(chart.getVisibleTo().contains("SALES_MANAGER")));
        assertTrue(dashboard.getAcceptWorkDurationChart().getVisibleTo().contains("SALES_MANAGER"));
        assertTrue(dashboard.getSupplierQuoteDurationChart().getVisibleTo().contains("SALES_MANAGER"));
        assertEquals(2, dashboard.getSalesCountChart().getItems().size());
        dashboard.getWorkQueues().stream().filter(queue -> queue.getVisibleTo().contains("SALES"))
                .forEach(queue -> assertTrue(queue.getVisibleTo().contains("SALES_MANAGER")));
        assertTrue(dashboard.getQuickLinks().stream().anyMatch(link -> "/rfq-create".equals(link.getHref())));
        dashboard.getQuickLinks().forEach(link -> assertFalse(link.getHref().contains("salesId=")));
    }

    @Test
    void managerCanExplicitlyFilterByASalesEmployee() {
        authenticate("SALES_MANAGER");
        when(requestPriceHeaderRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        DashboardDataDto dashboard = service.getDashboard("2026-10-01", "2026-10-08", "SALES-B", null);

        var rfqLink = dashboard.getQuickLinks().stream().filter(link -> "rfq-list".equals(link.getId())).findFirst().orElseThrow();
        assertTrue(rfqLink.getHref().contains("salesId=SALES-B"));
        assertFalse(rfqLink.getHref().contains("MANAGER-LOGIN"));
    }

    private void authenticate(String roleCode) {
        UserRoleDto role = new UserRoleDto();
        role.setRoleCode(roleCode);
        UserDto user = new UserDto();
        user.setRole(role);
        user.setEmployeeId("MANAGER-LOGIN");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private RfqHeaderEntity rfq(String id, String salesId) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(salesId);
        employee.setNickName(salesId);
        RfqHeaderEntity rfq = new RfqHeaderEntity();
        rfq.setId(id);
        rfq.setStatus(RfqStatus.NEW);
        rfq.setRequestedDate(ZonedDateTime.parse("2026-10-08T10:00:00+07:00"));
        rfq.setSales(employee);
        return rfq;
    }
}
