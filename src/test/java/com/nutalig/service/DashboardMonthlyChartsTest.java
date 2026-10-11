package com.nutalig.service;

import com.nutalig.config.AppProperties;
import com.nutalig.constant.CalendarEventType;
import com.nutalig.constant.RfqStatus;
import com.nutalig.constant.SystemConstant;
import com.nutalig.dto.*;
import com.nutalig.entity.*;
import com.nutalig.entity.id.RfqStatusTimelineId;
import com.nutalig.entity.id.SystemConfigId;
import com.nutalig.repository.*;
import com.nutalig.utils.DateUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardMonthlyChartsTest {
    @Mock RequestPriceHeaderRepository rfqRepository;
    @Mock QuotationRepository quotationRepository;
    @Mock ApprovalRequestRepository approvalRequestRepository;
    @Mock RfqStatusTimelineRepository timelineRepository;
    @Mock BusinessDurationService businessDurationService;
    @Mock AppProperties appProperties;
    @InjectMocks DashboardService service;

    @BeforeEach
    void authenticate() {
        UserDto user = new UserDto();
        UserRoleDto role = new UserRoleDto();
        role.setRoleCode("ADMIN");
        user.setRole(role);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(user, null));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void separatesAllBarChartsAcrossYearsIncludesEmptyMonthAndUsesBangkokRequestMonth() {
        RfqHeaderEntity november = rfq("NOV-A", "2025-11-10T09:00:00+07:00", "A", "VIP", 30L);
        RfqHeaderEntity pending = rfq("NOV-B", "2025-11-20T09:00:00+07:00", "B", null, null);
        // This timestamp belongs to January in Bangkok, although its UTC date is December 31.
        RfqHeaderEntity january = rfq("JAN-A", "2025-12-31T18:30:00Z", "A", "VIP", 180L);
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(november, pending, january));
        ZonedDateTime acceptedAt = november.getRequestedDate().plusHours(1);
        when(timelineRepository.findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(anyCollection(), eq(RfqStatus.IN_PROGRESS)))
                .thenReturn(List.of(timeline(november.getId(), RfqStatus.IN_PROGRESS, acceptedAt)));
        when(timelineRepository.findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(anyCollection(), eq(RfqStatus.SUPPLIER_QUOTED)))
                .thenReturn(List.of(timeline(november.getId(), RfqStatus.SUPPLIER_QUOTED, acceptedAt.plusHours(2))));
        when(businessDurationService.calculateBusinessDurationMinutes(acceptedAt, acceptedAt.plusHours(2)))
                .thenReturn(120L);

        DashboardDataDto dashboard = service.getDashboard("2025-11-01", "2026-01-31", "", "");
        DashboardTrendChartDto volume = dashboard.getTrendCharts().get(0);
        assertEquals(List.of("11/2025", "12/2025", "01/2026"), volume.getLabels());
        assertEquals(List.of(2.0, 0.0, 1.0), volume.getSeries().get(0).getData());

        var acceptance = dashboard.getAcceptWorkDurationChart().getSeries();
        assertEquals(List.of("2025-11", "2025-12", "2026-01"), acceptance.stream().map(DashboardSeriesDto::getName).toList());
        assertEquals(1.0, acceptance.get(0).getData().get(0));
        assertEquals(1.0, acceptance.get(0).getData().get(7));
        assertTrue(acceptance.get(1).getData().stream().allMatch(value -> value == 0));
        assertEquals(1.0, acceptance.get(2).getData().get(2));
        var supplier = dashboard.getSupplierQuoteDurationChart().getSeries();
        assertEquals(1.0, supplier.get(0).getData().get(1));
        assertEquals(1.0, supplier.get(0).getData().get(7));
        assertEquals(1.0, supplier.get(2).getData().get(7));

        assertMonthlyTotals(dashboard.getSalesCountChart());
        assertMonthlyTotals(dashboard.getCustomerTypeCountChart());
        var sales = dashboard.getSalesCountChart();
        int index = labels(sales).indexOf("A");
        assertEquals(1.0, sales.getSeries().get(0).getData().get(index));
        assertEquals(0.0, sales.getSeries().get(1).getData().get(index));
        assertEquals(1.0, sales.getSeries().get(2).getData().get(index));
        assertEquals(acceptance.get(0).getColor(), sales.getSeries().get(0).getColor());
    }

    @Test
    void emptyRangeStillShowsEveryMonthWithZeroVolume() {
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        var dashboard = service.getDashboard("2026-08-01", "2026-10-31", null, null);
        var volume = dashboard.getTrendCharts().get(0);
        assertEquals(List.of("08/2026", "09/2026", "10/2026"), volume.getLabels());
        volume.getSeries().forEach(series -> assertEquals(List.of(0.0, 0.0, 0.0), series.getData()));
        assertNull(dashboard.getAcceptWorkDurationChart());
        assertNull(dashboard.getProcurementKpiChart());
        assertTrue(rfqTypeChart(dashboard).getItems().isEmpty());
    }

    @Test
    void oneMonthProducesOneSeriesAndDoesNotExpandPartialMonthFilters() {
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(rfq("A", "2026-02-15T12:00:00+07:00", "A", "VIP", 60L)));
        var dashboard = service.getDashboard("2026-02-10", "2026-02-20", null, null);
        assertEquals("2026-02-10", dashboard.getDateFrom());
        assertEquals("2026-02-20", dashboard.getDateTo());
        assertEquals(List.of("02/2026"), dashboard.getTrendCharts().get(0).getLabels());
        assertEquals(1, dashboard.getSalesCountChart().getSeries().size());
        assertEquals(List.of(1.0), dashboard.getSalesCountChart().getSeries().get(0).getData());
    }

    @Test
    void rfqTypeDonutCountsHeadersByTypeCodeWithNameAndMissingTypeFallbacks() {
        var rows = List.of(
                rfqWithType("A", "NEW", "งานใหม่", "New"),
                rfqWithType("B", "NEW", "งานใหม่", "New"),
                rfqWithType("C", "REPEAT_PRICE", "", "Repeat price"),
                rfqWithType("D", "REORDER", null, null),
                rfqWithType("E", null, null, null));
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(rows);

        var chart = rfqTypeChart(service.getDashboard("2026-10-01", "2026-10-31", "SALES-A", "PROC-A"));
        Map<String, Long> counts = chart.getItems().stream().collect(Collectors.toMap(
                DashboardDistributionItemDto::getLabel, DashboardDistributionItemDto::getValue));
        assertEquals(Map.of("งานใหม่", 2L, "Repeat price", 1L, "REORDER", 1L,
                "dashboard.rfq.charts.rfqType.unspecified", 1L), counts);
        assertEquals(rows.size(), chart.getItems().stream().mapToLong(DashboardDistributionItemDto::getValue).sum());
        assertEquals("dashboard.rfq.charts.rfqType.title", chart.getTitle());
        assertTrue(chart.getVisibleTo().containsAll(List.of("SALES", "SALES_MANAGER", "PROCUREMENT")));
        assertTrue(chart.getItems().stream().allMatch(item -> item.getColor() != null));
    }

    @Test
    void rfqTypeDonutKeepsEveryTypeAndDoesNotMergeDifferentCodesWithTheSameDisplayName() {
        var rows = IntStream.range(0, 12)
                .mapToObj(index -> rfqWithType("RFQ-" + index, "TYPE-" + index, "ชื่อเดียวกัน", null))
                .toList();
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(rows);

        var chart = rfqTypeChart(service.getDashboard("2026-10-01", "2026-10-31", null, null));
        assertEquals(12, chart.getItems().size());
        assertTrue(chart.getItems().stream().allMatch(item -> item.getValue() == 1L));
    }

    @Test
    void contactChannelDonutSplitsLineBySalesTeamAndPreservesOtherChannels() {
        RfqHeaderEntity online = rfqWithChannel("ONLINE-A", "LINE", "SALES_ONLINE");
        // Procurement belongs to the opposite team; only the RFQ's sales owner determines the channel.
        online.setProcurement(rfqWithChannel("PROC", "LINE", "SALES_OFFLINE").getSales());
        var rows = List.of(
                online,
                rfqWithChannel("ONLINE-B", " line ", "SALES_ONLINE"),
                rfqWithChannel("OFFLINE", "LINE", "SALES_OFFLINE"),
                rfqWithChannel("EMAIL", "EMAIL", "SALES_ONLINE"),
                rfqWithChannel("PHONE", "PHONE", "SALES_OFFLINE"));
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(rows);

        var chart = contactChannelChart(service.getDashboard("2026-10-01", "2026-10-31", null, null));
        Map<String, Long> counts = chart.getItems().stream().collect(Collectors.toMap(
                DashboardDistributionItemDto::getLabel, DashboardDistributionItemDto::getValue));
        assertEquals(Map.of("LINE Online", 2L, "LINE Offline", 1L, "EMAIL", 1L, "PHONE", 1L), counts);
        assertEquals(rows.size(), chart.getItems().stream().mapToLong(DashboardDistributionItemDto::getValue).sum());
    }

    @Test
    void contactChannelDonutKeepsUnassignedLineAndEveryOtherChannelWithoutLosingCounts() {
        RfqHeaderEntity missingSales = rfqWithChannel("NO-SALES", "LINE", null);
        missingSales.setSales(null);
        RfqHeaderEntity missingTeamId = rfqWithChannel("NO-TEAM-ID", "LINE", null);
        missingTeamId.getSales().setTeam(new SystemConfigEntity());
        var rows = new java.util.ArrayList<>(List.of(
                missingSales,
                missingTeamId,
                rfqWithChannel("NO-TEAM", "LINE", null),
                rfqWithChannel("OTHER-TEAM", "LINE", "OTHER"),
                rfqWithChannel("EMPTY-CHANNEL", "  ", "SALES_ONLINE"),
                rfqWithChannel("ONLINE", "LINE", "SALES_ONLINE"),
                rfqWithChannel("OFFLINE", "LINE", "SALES_OFFLINE")));
        IntStream.range(0, 8).forEach(index -> rows.add(rfqWithChannel(
                "OTHER-" + index, "CHANNEL-" + index, "SALES_ONLINE")));
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(rows);

        var chart = contactChannelChart(service.getDashboard("2026-10-01", "2026-10-31", null, null));
        Map<String, Long> counts = chart.getItems().stream().collect(Collectors.toMap(
                DashboardDistributionItemDto::getLabel, DashboardDistributionItemDto::getValue));
        assertEquals(4L, counts.get("LINE"));
        assertEquals(1L, counts.get("-"));
        assertEquals(1L, counts.get("LINE Online"));
        assertEquals(1L, counts.get("LINE Offline"));
        assertEquals(12, chart.getItems().size());
        assertEquals(rows.size(), chart.getItems().stream().mapToLong(DashboardDistributionItemDto::getValue).sum());
    }

    @Test
    void procurementKpiStartsAtNewFallsBackToRequestedDateAndSeparatesPendingFromMissingHistory() {
        when(appProperties.getRfqPendingAcceptance()).thenReturn(new AppProperties.RfqPendingAcceptance());
        var completed = rfq("NOV-DONE", "2025-11-10T09:00:00+07:00", "A", null, 30L);
        completed.setStatus(RfqStatus.QUOTED);
        var pending = rfq("NOV-PENDING", "2025-11-11T09:00:00+07:00", "A", null, 30L);
        var legacy = rfq("JAN-LEGACY", "2026-01-10T09:00:00+07:00", "A", null, 30L);
        legacy.setStatus(RfqStatus.SUPPLIER_QUOTED);
        var missing = rfq("JAN-MISSING", "2026-01-11T09:00:00+07:00", "A", null, 30L);
        missing.setStatus(RfqStatus.QUOTED);
        var invalid = rfq("JAN-INVALID", "2026-01-12T09:00:00+07:00", "A", null, 30L);
        invalid.setStatus(RfqStatus.SUPPLIER_QUOTED);
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(completed, pending, legacy, missing, invalid));
        ZonedDateTime newAt = completed.getRequestedDate().plusMinutes(10);
        ZonedDateTime acceptedAt = newAt.plusHours(2);
        ZonedDateTime quotedAt = newAt.plusHours(3);
        ZonedDateTime legacyQuotedAt = legacy.getRequestedDate().plusDays(1);
        when(timelineRepository.findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(anyCollection(), eq(RfqStatus.NEW)))
                .thenReturn(List.of(timeline(completed.getId(), RfqStatus.NEW, newAt)));
        when(timelineRepository.findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(anyCollection(), eq(RfqStatus.IN_PROGRESS)))
                .thenReturn(List.of(timeline(completed.getId(), RfqStatus.IN_PROGRESS, acceptedAt)));
        when(timelineRepository.findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(anyCollection(), eq(RfqStatus.SUPPLIER_QUOTED)))
                .thenReturn(List.of(timeline(completed.getId(), RfqStatus.SUPPLIER_QUOTED, quotedAt),
                        timeline(legacy.getId(), RfqStatus.SUPPLIER_QUOTED, legacyQuotedAt),
                        timeline(invalid.getId(), RfqStatus.SUPPLIER_QUOTED, invalid.getRequestedDate().minusHours(1))));
        when(businessDurationService.calculateBusinessDurationMinutes(acceptedAt, quotedAt)).thenReturn(60L);
        when(businessDurationService.calculateBusinessDurationMinutesWithCutoff(newAt, quotedAt, LocalTime.of(17, 0), LocalTime.of(9, 0)))
                .thenReturn(180L);
        when(businessDurationService.calculateBusinessDurationMinutesWithCutoff(legacy.getRequestedDate(), legacyQuotedAt, LocalTime.of(17, 0), LocalTime.of(9, 0)))
                .thenReturn(1_440L);

        var dashboard = service.getDashboard("2025-11-01", "2026-01-31", null, null);
        var chart = dashboard.getProcurementKpiChart();
        assertEquals(List.of("2025-11", "2025-12", "2026-01"), chart.getSeries().stream().map(DashboardSeriesDto::getName).toList());
        assertEquals(List.of(1.0, 0.0, 0.0, 0.0, 1.0, 0.0), chart.getSeries().get(0).getData());
        assertTrue(chart.getSeries().get(1).getData().stream().allMatch(value -> value == 0));
        assertEquals(List.of(0.0, 1.0, 0.0, 0.0, 0.0, 2.0), chart.getSeries().get(2).getData());
        assertEquals(1.0, dashboard.getSupplierQuoteDurationChart().getSeries().get(0).getData().get(1));
        assertEquals(dashboard.getAcceptWorkDurationChart().getSeries().get(0).getColor(), chart.getSeries().get(0).getColor());
        assertTrue(chart.getVisibleTo().containsAll(List.of("SALES", "SALES_MANAGER", "PROCUREMENT")));
        verify(businessDurationService).calculateBusinessDurationMinutesWithCutoff(newAt, quotedAt, LocalTime.of(17, 0), LocalTime.of(9, 0));
        verify(businessDurationService, never()).calculateBusinessDurationMinutesWithCutoff(
                eq(invalid.getRequestedDate()), eq(invalid.getRequestedDate().minusHours(1)), any(), any());
    }

    @Test
    void procurementKpiGroupsHoursIntoDaysAndPlacesExactDayBoundariesInNonOverlappingBuckets() {
        long[] minutes = {0, 59, 60, 179, 180, 359, 360, 1_439, 1_440, 2_879, 2_880, 4_319, 4_320};
        List<RfqHeaderEntity> rows = new ArrayList<>();
        List<RfqStatusTimelineEntity> quoted = new ArrayList<>();
        for (int index = 0; index < minutes.length; index++) {
            var row = rfq("BOUNDARY-" + index, "2026-10-01T09:00:00+07:00", "A", null, 30L);
            row.setRequestedDate(row.getRequestedDate().plusMinutes(index));
            row.setStatus(RfqStatus.SUPPLIER_QUOTED);
            rows.add(row);
            quoted.add(timeline(row.getId(), RfqStatus.SUPPLIER_QUOTED, row.getRequestedDate().plusMinutes(minutes[index])));
        }
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(rows);
        when(timelineRepository.findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(anyCollection(), any(RfqStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(1) == RfqStatus.SUPPLIER_QUOTED ? quoted : List.of());
        when(businessDurationService.calculateBusinessDurationMinutesWithCutoff(any(), any(), any(), any())).thenAnswer(invocation ->
                java.time.Duration.between(invocation.<ZonedDateTime>getArgument(0), invocation.<ZonedDateTime>getArgument(1)).toMinutes());

        var chart = service.getDashboard("2026-10-01", "2026-10-31", null, null).getProcurementKpiChart();
        assertEquals(List.of(8.0, 2.0, 2.0, 1.0, 0.0, 0.0), chart.getSeries().get(0).getData());
        assertEquals(minutes.length, chart.getSeries().get(0).getData().stream().mapToDouble(Double::doubleValue).sum());
    }

    @ParameterizedTest
    @CsvSource({
            "2026-10-05T16:30:00+07:00, 2026-10-06T16:30:00+07:00, 17:00, 09:00, 1",
            "2026-10-05T17:00:00+07:00, 2026-10-06T17:00:00+07:00, 17:00, 09:00, 1",
            "2026-10-05T17:30:00+07:00, 2026-10-06T17:30:00+07:00, 17:00, 09:00, 0",
            "2026-10-05T10:30:00Z, 2026-10-06T10:30:00Z, 17:00, 09:00, 0",
            "2026-10-05T15:30:00+07:00, 2026-10-06T15:30:00+07:00, 15:00, 09:00, 0",
            "2026-10-16T17:30:00+07:00, 2026-10-20T00:00:00+07:00, 17:00, 09:00, 1",
            "2026-10-09T17:30:00+07:00, 2026-10-14T10:00:00+07:00, 17:00, 09:00, 1",
            "2026-10-05T17:30:00+07:00, 2026-10-05T17:45:00+07:00, 17:00, 09:00, 0",
            "2026-10-05T17:30:00+07:00, 2026-10-07T10:00:00+07:00, 17:00, 09:00, 1",
            "2026-10-05T17:30:00+07:00, 2026-10-07T10:00:00+07:00, 17:00, 13:00, 0"
    })
    void procurementKpiAppliesConfiguredCutoffAndBusinessCalendar(
            String requestedAt, String quotedAt, String cutoff, String nextDayStart, int expectedBucket
    ) {
        var calendarRepository = mock(CalendarEventRepository.class);
        if (requestedAt.startsWith("2026-10-09")) {
            var holiday = new CalendarEventEntity();
            holiday.setEventType(CalendarEventType.HOLIDAY);
            holiday.setStartAt(ZonedDateTime.parse("2026-10-12T00:00:00+07:00"));
            holiday.setEndAt(ZonedDateTime.parse("2026-10-12T23:59:59+07:00"));
            when(calendarRepository.findAllByActiveTrueAndEventTypeAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtAsc(
                    eq(CalendarEventType.HOLIDAY), any(), any())).thenReturn(List.of(holiday));
        }
        var config = new AppProperties();
        config.getRfqPendingAcceptance().setCutoffTime(LocalTime.parse(cutoff));
        config.getRfqPendingAcceptance().setNextDayStartTime(LocalTime.parse(nextDayStart));
        var realDurationService = new BusinessDurationService(calendarRepository);
        var dashboardService = new DashboardService(
                approvalRequestRepository, rfqRepository, quotationRepository, timelineRepository,
                realDurationService, config
        );
        var row = rfq("CUTOFF", requestedAt, "A", null, 30L);
        row.setStatus(RfqStatus.SUPPLIER_QUOTED);
        when(rfqRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(row));
        when(timelineRepository.findAllByIdRfqIdInAndIdStatusOrderByStatusDatetimeAsc(anyCollection(), any(RfqStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(1) == RfqStatus.SUPPLIER_QUOTED
                        ? List.of(timeline(row.getId(), RfqStatus.SUPPLIER_QUOTED, ZonedDateTime.parse(quotedAt)))
                        : List.of());

        var chart = dashboardService.getDashboard("2026-10-01", "2026-10-31", null, null).getProcurementKpiChart();

        assertEquals(1.0, chart.getSeries().get(0).getData().get(expectedBucket));
        assertEquals(1.0, chart.getSeries().get(0).getData().stream().mapToDouble(Double::doubleValue).sum());
    }

    private DashboardDistributionChartDto contactChannelChart(DashboardDataDto dashboard) {
        return dashboard.getDistributionCharts().stream()
                .filter(chart -> "rfq-by-channel".equals(chart.getId())).findFirst().orElseThrow();
    }

    private RfqHeaderEntity rfqWithChannel(String id, String channel, String teamCode) {
        RfqHeaderEntity rfq = rfq(id, "2026-10-10T12:00:00+07:00", "A", null, 60L);
        rfq.setContactChannel(channel);
        if (teamCode != null) {
            SystemConfigEntity team = new SystemConfigEntity();
            SystemConfigId configId = new SystemConfigId();
            configId.setGroupCode(SystemConstant.TEAM);
            configId.setCode(teamCode);
            team.setId(configId);
            rfq.getSales().setTeam(team);
        }
        return rfq;
    }

    private DashboardDistributionChartDto rfqTypeChart(DashboardDataDto dashboard) {
        return dashboard.getDistributionCharts().stream()
                .filter(chart -> "rfq-by-type".equals(chart.getId())).findFirst().orElseThrow();
    }

    private RfqHeaderEntity rfqWithType(String id, String code, String nameTh, String nameEn) {
        RfqHeaderEntity rfq = rfq(id, "2026-10-10T12:00:00+07:00", "A", null, 60L);
        if (code != null) {
            SystemConfigEntity type = new SystemConfigEntity();
            SystemConfigId configId = new SystemConfigId();
            configId.setGroupCode(SystemConstant.RFQ_TYPE);
            configId.setCode(code);
            type.setId(configId);
            type.setNameTh(nameTh);
            type.setNameEn(nameEn);
            rfq.setRfqType(type);
        }
        return rfq;
    }

    private void assertMonthlyTotals(DashboardDistributionChartDto chart) {
        assertEquals(List.of("2025-11", "2025-12", "2026-01"), chart.getSeries().stream().map(DashboardSeriesDto::getName).toList());
        for (int index = 0; index < chart.getItems().size(); index++) {
            int column = index;
            double total = chart.getSeries().stream().mapToDouble(series -> series.getData().get(column)).sum();
            assertEquals(chart.getItems().get(index).getValue().doubleValue(), total);
        }
    }

    private List<String> labels(DashboardDistributionChartDto chart) {
        return chart.getItems().stream().map(DashboardDistributionItemDto::getLabel).toList();
    }

    private RfqHeaderEntity rfq(String id, String requestedAt, String salesName, String tierName, Long acceptMinutes) {
        RfqHeaderEntity rfq = new RfqHeaderEntity();
        rfq.setId(id);
        rfq.setRequestedDate(ZonedDateTime.parse(requestedAt));
        rfq.setStatus(RfqStatus.NEW);
        rfq.setAcceptWorkDurationMinutes(acceptMinutes);
        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeId(salesName);
        employee.setNickName(salesName);
        rfq.setSales(employee);
        if (tierName != null) {
            CustomerEntity customer = new CustomerEntity();
            customer.setId("CUST-" + id);
            SystemConfigEntity tier = new SystemConfigEntity();
            SystemConfigId configId = new SystemConfigId();
            configId.setCode(tierName);
            tier.setId(configId);
            tier.setNameEn(tierName);
            customer.setCustomerTier(tier);
            rfq.setCustomer(customer);
        }
        return rfq;
    }

    private RfqStatusTimelineEntity timeline(String id, RfqStatus status, ZonedDateTime at) {
        RfqStatusTimelineId timelineId = new RfqStatusTimelineId();
        timelineId.setRfqId(id);
        timelineId.setStatus(status);
        RfqStatusTimelineEntity timeline = new RfqStatusTimelineEntity();
        timeline.setId(timelineId);
        timeline.setStatusDatetime(at);
        return timeline;
    }
}
